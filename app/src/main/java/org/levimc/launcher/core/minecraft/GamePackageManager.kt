package org.levimc.launcher.core.minecraft

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import org.levimc.launcher.core.versions.GameVersion
import org.levimc.launcher.util.NativeBridgeHelper
import org.levimc.launcher.util.NativeImageGuard
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

class GamePackageManager private constructor(private val context: Context, private val version: GameVersion?) {

    private val packageContext: Context
    private val assetManager: AssetManager
    private val nativeLibDir: String
    private val applicationInfo: ApplicationInfo

    private val knownPackages = arrayOf(
        "com.mojang.minecraftpe",
        "com.mojang.minecraftpe.beta",
        "com.mojang.minecraftpe.preview"
    )

    private val requiredLibs = arrayOf(
        "libc++_shared.so",
        "libfmod.so",
        "libMediaDecoders_Android.so",
        "libminecraftpe.so",
    )

    private val optionalLibs = arrayOf(
        "libHttpClient.Android.so",
    )

    private val extractableLibs = requiredLibs + optionalLibs

    private val systemLoadedLibs = arrayOf(
        "libPlayFabMultiplayer.so",
        "libmaesdk.so",
        "libgxcore.so",
    )

    init {
        val packageName = detectGamePackage() ?: throw IllegalStateException("Minecraft not found")
        packageContext = context.createPackageContext(
            packageName,
            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
        )
        
        if (version != null && !version.isInstalled) {
            applicationInfo = MinecraftLauncher(context).createFakeApplicationInfo(version, MinecraftLauncher.MC_PACKAGE_NAME)
            nativeLibDir = applicationInfo.nativeLibraryDir
        } else {
            applicationInfo = packageContext.applicationInfo
            nativeLibDir = resolveNativeLibDir()
        }
        
        extractLibraries()
        assetManager = createAssetManager()
        setupSecurityProvider()
    }

    private fun detectGamePackage(): String? {
        return knownPackages.firstOrNull { isPackageInstalled(it) }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun resolveNativeLibDir(): String {
        val cacheName = version?.directoryName ?: packageContext.packageName
        val cacheLibDir = File(context.cacheDir, "lib/$cacheName/${getDeviceAbi()}")
        cacheLibDir.mkdirs()
        return cacheLibDir.absolutePath
    }

    private fun getDeviceAbi(): String {
        return Build.SUPPORTED_64_BIT_ABIS.firstOrNull {
            it.contains("arm64-v8a") || it.contains("x86_64")
        } ?: Build.SUPPORTED_32_BIT_ABIS.firstOrNull {
            it.contains("armeabi-v7a") || it.contains("x86")
        } ?: (Build.SUPPORTED_ABIS.firstOrNull() ?: "armeabi-v7a")
    }

    private fun extractLibraries() {
        val outputDir = File(nativeLibDir)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        var markerString: String
        if (version != null && !version.isInstalled) {
            val baseApk = File(applicationInfo.sourceDir)
            markerString = "isolated_" + if (baseApk.exists()) baseApk.lastModified().toString() else "0"
        } else {
            val baseApk = applicationInfo.sourceDir?.let { File(it) }
            markerString = "installed_" + if (baseApk != null && baseApk.exists()) baseApk.lastModified().toString() else "0"
        }
        markerString += "_" + getDeviceAbi() + "_" + NativeImageGuard.TOKEN

        val markerFile = File(outputDir, ".extraction_marker")
        var markerMatches = false
        if (markerFile.exists()) {
            try {
                if (markerFile.readText().trim() == markerString) {
                    markerMatches = true
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        val cacheRequiredLibs = getCacheRequiredLibs()
        var allPresent = markerMatches
        if (allPresent) {
            for (lib in cacheRequiredLibs) {
                val file = File(outputDir, lib)
                if (!file.exists() || file.length() == 0L) {
                    allPresent = false
                    break
                }
            }
        }
        
        if (allPresent) {
            for (lib in extractableLibs) {
                try {
                    val file = File(outputDir, lib)
                    if (file.exists()) {
                        ensureReadOnly(file)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed ensureReadOnly: ${e.message}")
                }
            }
            return
        }

        if (version != null && !version.isInstalled) {
            val apkPaths = mutableListOf<String>()
            val baseApk = File(applicationInfo.sourceDir)
            if (baseApk.exists()) {
                apkPaths.add(applicationInfo.sourceDir)
            } else {
                Log.w(TAG, "Base APK not found: ${applicationInfo.sourceDir}")
            }
            applicationInfo.splitSourceDirs?.forEach {
                if (File(it).exists()) {
                    apkPaths.add(it)
                } else {
                    Log.w(TAG, "Split APK not found: $it")
                }
            }
            apkPaths.forEach { extractFromApk(it, outputDir, getDeviceAbi()) }
            if (cacheRequiredLibs.any { !File(outputDir, it).exists() }) {
                Log.w(TAG, "Primary ABI ${getDeviceAbi()} libraries missing, trying fallback ABIs")
                val fallbackAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
                fallbackAbis.filter { it != getDeviceAbi() }.forEach { abi ->
                    apkPaths.forEach { extractFromApk(it, outputDir, abi) }
                }
            }
        } else {
            val appInfo = packageContext.applicationInfo
            if (File(appInfo.nativeLibraryDir).exists()) {
                copyFromNativeDir(appInfo.nativeLibraryDir, outputDir)
            }
            val apkPaths = mutableListOf<String>()
            appInfo.sourceDir?.let { apkPaths.add(it) }
            appInfo.splitPublicSourceDirs?.let { apkPaths.addAll(it) }
            apkPaths.forEach { extractFromApk(it, outputDir, getDeviceAbi()) }
        }
        verifyLibraries(outputDir)
        processNativeImages(outputDir)

        if (cacheRequiredLibs.all { File(outputDir, it).let { f -> f.exists() && f.length() > 0 } }) {
            try {
                markerFile.writeText(markerString)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write marker file: ${e.message}")
            }
        }
    }

    private fun copyFromNativeDir(sourceDir: String, destDir: File) {
        val source = File(sourceDir)
        if (!source.exists()) {
            Log.w(TAG, "Source native library directory does not exist: $sourceDir")
            return
        }

        extractableLibs.forEach { lib ->
            val srcFile = File(source, lib)
            val dstFile = File(destDir, lib)
            if (srcFile.exists() && srcFile.length() > 0) {
                try {
                    srcFile.inputStream().use { input ->
                        copyStreamToReadOnlyFile(input, dstFile)
                    }
                    if (!processNativeImage(dstFile, true)) {
                        dstFile.delete()
                        throw IOException("Failed to prepare native library: ${dstFile.name}")
                    }
                    logFileOperation("Copied", lib)
                } catch (e: Exception) {
                    logFileOperation("Failed to copy", lib, e = e)
                }
            } else {
                Log.w(TAG, "Library $lib not found in $sourceDir")
            }
        }
    }

    private fun extractFromApk(apkPath: String, outputDir: File, abi: String) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            Log.w(TAG, "APK file does not exist: $apkPath")
            return
        }
        if (!apkPath.contains("arm") && !apkPath.contains("x86") && !apkPath.contains("base.apk")) {
            return
        }

        try {
            ZipFile(apkPath).use { zip ->
                val abiPath = "lib/$abi"
                extractableLibs.forEach { lib ->
                    val entry = zip.getEntry("$abiPath/$lib")
                    if (entry == null) {
                        return@forEach
                    }
                    val output = File(outputDir, lib)
                    zip.getInputStream(entry).use { input ->
                        copyStreamToReadOnlyFile(input, output)
                    }
                    if (!processNativeImage(output, true)) {
                        output.delete()
                        throw IOException("Failed to prepare native library: ${output.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract libraries from $apkPath: ${e.message}")
        }
    }

    private fun copyStreamToReadOnlyFile(input: InputStream, output: File) {
        ensureParentDirectory(output)
        if (output.exists() && !output.delete()) {
            throw IOException("Failed to replace existing file: ${output.absolutePath}")
        }

        val tempFile = File(output.absolutePath + ".tmp")
        if (tempFile.exists()) tempFile.delete()

        FileOutputStream(tempFile).use { out ->
            input.copyTo(out)
            out.fd.sync()
        }

        if (!tempFile.renameTo(output)) {
            tempFile.delete()
            throw IOException("Failed to rename temporary file to ${output.absolutePath}")
        }

        ensureReadOnly(output)
    }

    private fun ensureParentDirectory(file: File) {
        val parent = file.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create parent directory: ${parent.absolutePath}")
        }
    }

    private fun ensureReadOnly(file: File) {
        if (!file.isFile) {
            throw IOException("Expected regular file: ${file.absolutePath}")
        }
        if (!file.setReadable(true, true) && !file.canRead()) {
            throw IOException("Failed to mark file readable: ${file.absolutePath}")
        }
        if (!file.setReadOnly() && file.canWrite()) {
            throw IOException("Failed to keep file read-only: ${file.absolutePath}")
        }
    }

    private fun verifyLibraries(dir: File) {
        val missing = getCacheRequiredLibs().filterNot {
            File(dir, it).let { f -> f.exists() && f.length() > 0 }
        }
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Missing libraries in $dir: ${missing.joinToString()}")
        }
    }

    private fun processNativeImage(file: File, force: Boolean = false): Boolean {
        if (!file.name.endsWith(".so")) {
            return true
        }
        return if (force) {
            NativeImageGuard.processRequired(file)
        } else {
            NativeImageGuard.processIfNeeded(file)
        }
    }

    private fun processNativeImages(dir: File) {
        NativeImageGuard.processDirectory(dir)
    }

    private fun getCacheRequiredLibs(): Array<String> {
        return if (shouldLoadHttpClient()) {
            requiredLibs + optionalLibs
        } else {
            requiredLibs
        }
    }

    private fun shouldLoadHttpClient(): Boolean {
        val versionCode = version?.versionCode ?: return false
        val targetVersion = if (versionCode.contains("beta")) "1.21.130.20" else "1.21.130"
        return isVersionAtLeast(versionCode, targetVersion)
    }

    private fun isVersionAtLeast(currentVersion: String, targetVersion: String): Boolean {
        return try {
            val current = currentVersion.replace(Regex("[^0-9.]"), "").split(".")
            val target = targetVersion.split(".")
            val maxLength = maxOf(current.size, target.size)

            for (i in 0 until maxLength) {
                val currentPart = current.getOrNull(i)?.toIntOrNull() ?: 0
                val targetPart = target.getOrNull(i)?.toIntOrNull() ?: 0

                if (currentPart > targetPart) return true
                if (currentPart < targetPart) return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun logFileOperation(action: String, lib: String, extra: String? = null, e: Exception? = null) {
        val message = buildString {
            append("$action $lib")
            if (extra != null) append(" $extra")
            if (e != null) append(": ${e.message}")
        }
        if (e != null) Log.w(TAG, message)
    }

    private fun createAssetManager(): AssetManager {
        val assets = AssetManager::class.java.newInstance()
        val addAssetPathMethod = AssetManager::class.java.getMethod("addAssetPath", String::class.java)

        val paths = mutableListOf<String>()
        
        if (version != null && !version.isInstalled) {
            val baseApk = File(applicationInfo.sourceDir)
            if (baseApk.exists()) {
                paths.add(applicationInfo.sourceDir)
            } else {
                Log.w(TAG, "Base APK for assets not found: ${applicationInfo.sourceDir}")
            }
            applicationInfo.splitSourceDirs?.forEach {
                if (File(it).exists()) {
                    paths.add(it)
                } else {
                    Log.w(TAG, "Split APK for assets not found: $it")
                }
            }
        } else {
            paths.add(packageContext.packageResourcePath)
            val splitPath = packageContext.packageResourcePath.replace("base.apk", "split_install_pack.apk")
            if (File(splitPath).exists()) paths.add(splitPath)
        }
        
        paths.add(context.packageResourcePath)

        paths.forEach { path ->
            try {
                addAssetPathMethod.invoke(assets, path)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add asset path $path: ${e.message}")
            }
        }
        return assets
    }

    private fun setupSecurityProvider() {
        try {
            java.security.Security.insertProviderAt(org.conscrypt.Conscrypt.newProvider(), 1)
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt init failed: ${e.message}")
        }
    }

    fun resolveLibraryPath(name: String): String? {
        val libFile = File(nativeLibDir, if (name.startsWith("lib")) name else "lib$name.so")
        return if (libFile.exists() && libFile.length() > 0) {
            libFile.absolutePath
        } else {
            Log.w(TAG, "Library $name not found in $nativeLibDir")
            null
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadLibrary(name: String): Boolean {
        val resolvedPath = resolveLibraryPath(name)
        val libFile = resolvedPath?.let(::File)
            ?: File(nativeLibDir, if (name.startsWith("lib")) name else "lib$name.so")
        val libName = libFile.name
        return if (systemLoadedLibs.contains(libName)) {
            try {
                val normalizedName = name.removePrefix("lib").removeSuffix(".so")
                if (normalizedName == "gxcore") {
                    NativeBridgeHelper.bootstrapGxCore()
                } else {
                    System.loadLibrary(normalizedName)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load system library $name: ${e.message}")
                false
            }
        } else {
            try {
                if (libFile.exists() && libFile.length() > 0) {
                    ensureReadOnly(libFile)
                    System.load(libFile.absolutePath)
                    true
                } else {
                    Log.w(TAG, "Library $name not found in $nativeLibDir, skipping")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load $name: ${e.message}")
                false
            }
        }
    }

    fun loadAllLibraries(excludeLibs: Set<String> = emptySet()) {
        val allLibs = requiredLibs + systemLoadedLibs
        allLibs.forEach { lib ->
            val libName = lib.removePrefix("lib").removeSuffix(".so")
            if (excludeLibs.contains(libName) || excludeLibs.contains(lib)) {
                return@forEach
            }
            if (!loadLibrary(libName)) {
                Log.e(TAG, "Failed to load required library $libName")
            }
        }
    }

    fun getAssets(): AssetManager = assetManager

    fun getPackageContext(): Context = packageContext

    fun getApplicationInfo(): ApplicationInfo = applicationInfo

    fun getVersionName(): String? {
        return try {
            context.packageManager.getPackageInfo(packageContext.packageName, 0).versionName
        } catch (e: Exception) {
            version?.versionCode
        }
    }

    companion object {
        private const val TAG = "GamePackageManager"

        @Volatile
        private var instance: GamePackageManager? = null
        private var lastVersionCode: String? = null

        @JvmStatic
        fun getInstance(context: Context, version: GameVersion? = null): GamePackageManager {
            return synchronized(this) {
                val newVersionCode = version?.versionCode
                if (instance == null || (newVersionCode != null && newVersionCode != lastVersionCode)) {
                    instance = GamePackageManager(context.applicationContext, version)
                    lastVersionCode = newVersionCode
                }
                instance!!
            }
        }

        fun isInitialized() = instance != null
    }
}
