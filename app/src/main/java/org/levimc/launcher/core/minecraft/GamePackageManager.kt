package org.levimc.launcher.core.minecraft

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import org.levimc.launcher.core.versions.GameVersion
import java.io.File
import java.io.FileOutputStream
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
        "libmaesdk.so",
        "libHttpClient.Android.so",
        "libminecraftpe.so"
    )

    private val systemLoadedLibs = arrayOf(
        "libpairipcore.so",
        "libgxcore.so"
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
        val appInfo = packageContext.applicationInfo
        return if (appInfo.splitPublicSourceDirs?.isNotEmpty() == true) {
            val cacheLibDir = File(context.cacheDir, "lib/${getDeviceAbi()}")
            cacheLibDir.mkdirs()
            cacheLibDir.absolutePath
        } else {
            appInfo.nativeLibraryDir
        }
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
            if (requiredLibs.any { !File(outputDir, it).exists() }) {
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
    }

    private fun copyFromNativeDir(sourceDir: String, destDir: File) {
        val source = File(sourceDir)
        if (!source.exists()) {
            Log.w(TAG, "Source native library directory does not exist: $sourceDir")
            return
        }

        requiredLibs.forEach { lib ->
            val srcFile = File(source, lib)
            val dstFile = File(destDir, lib)
            if (srcFile.exists() && srcFile.length() > 0) {
                try {
                    srcFile.copyTo(dstFile, overwrite = true)
                    dstFile.setReadable(true)
                    dstFile.setExecutable(true)
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
                requiredLibs.forEach { lib ->
                    val entry = zip.getEntry("$abiPath/$lib")
                    if (entry == null) {
                        return@forEach
                    }
                    val output = File(outputDir, lib)
                    if (output.exists() && output.length() > 0) {
                        return@forEach
                    }
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(output).use { out ->
                            input.copyTo(out)
                        }
                    }
                    output.setReadable(true)
                    output.setExecutable(true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract libraries from $apkPath: ${e.message}")
        }
    }

    private fun verifyLibraries(dir: File) {
        val missing = requiredLibs.filterNot {
            File(dir, it).let { f -> f.exists() && f.length() > 0 }
        }
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Missing libraries in $dir: ${missing.joinToString()}")
        } else {
            Log.i(TAG, "All libraries verified in $dir")
        }
    }

    private fun logFileOperation(action: String, lib: String, extra: String? = null, e: Exception? = null) {
        val message = buildString {
            append("$action $lib")
            if (extra != null) append(" $extra")
            if (e != null) append(": ${e.message}")
        }
        if (e != null) Log.w(TAG, message) else Log.d(TAG, message)
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
                    Log.d(TAG, "Adding split APK for assets: $it")
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
                Log.d(TAG, "Added asset path: $path")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add asset path $path: ${e.message}")
            }
        }
        return assets
    }

    private fun setupSecurityProvider() {
        Log.d(TAG, "Setting up security provider...")
        try {
            java.security.Security.insertProviderAt(org.conscrypt.Conscrypt.newProvider(), 1)
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt init failed: ${e.message}")
        }
    }

    fun loadLibrary(name: String): Boolean {
        val libFile = File(nativeLibDir, if (name.startsWith("lib")) name else "lib$name.so")
        val libName = libFile.name
        return if (systemLoadedLibs.contains(libName)) {
            try {
                System.loadLibrary(name.removePrefix("lib").removeSuffix(".so"))
                Log.d(TAG, "Loaded $name as system library")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load system library $name: ${e.message}")
                false
            }
        } else {
            try {
                if (libFile.exists() && libFile.length() > 0) {
                    System.load(libFile.absolutePath)
                    Log.d(TAG, "Loaded $name from $nativeLibDir")
                    true
                } else {
                    Log.w(TAG, "Library $name not found in $nativeLibDir, attempting system load")
                    System.loadLibrary(name.removePrefix("lib").removeSuffix(".so"))
                    Log.d(TAG, "Loaded $name as system library")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load $name: ${e.message}")
                false
            }
        }
    }

    fun loadAllLibraries() {
        val allLibs = requiredLibs + systemLoadedLibs
        allLibs.forEach { lib ->
            val libName = lib.removePrefix("lib").removeSuffix(".so")
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

        @JvmStatic
        fun getInstance(context: Context, version: GameVersion? = null): GamePackageManager {
            return synchronized(this) {
                instance = null // Reset instance to ensure fresh initialization
                instance ?: GamePackageManager(context.applicationContext, version).also { instance = it }
            }
        }

        fun isInitialized() = instance != null
    }
}