/*
 * Copyright (C) 2018-2021 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.levimc.launcher.core.minecraft.pesdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 * @author Vologhat
 */
@SuppressLint("StaticFieldLeak")
class SplitParser(private var context: Context) {
    private val minecraftLibs = arrayOf(
        "libminecraftpe.so",
        "libc++_shared.so",
        "libfmod.so",
        "libMediaDecoders_Android.so",
        "libpairipcore.so",
        "libconscrypt_jni.so",
        "libmaesdk.so"
    )

    /**
     * Извлечение C++ библиотек из Minecraft
     */
    fun parseMinecraft() {
        val abi = "/lib/${ABIInfo.getABI()}"
        val abiPath = File(context.cacheDir.path + abi)
        if (!abiPath.exists()) abiPath.mkdirs()

        Log.d("SplitParser", "Starting library extraction to: ${abiPath.absolutePath}")
        Log.d("SplitParser", "Target ABI: ${ABIInfo.getABI()}")

        try {
            val mcContext = MinecraftInfo.getMinecraftPackageContext()
            if (mcContext == null) {
                Log.e("SplitParser", "Minecraft package context is null - Minecraft may not be installed")
                return
            }

            val mcAppInfo = mcContext.applicationInfo
            Log.d("SplitParser", "Minecraft package: ${mcContext.packageName}")
            Log.d("SplitParser", "Is app bundle: ${isAppBundle()}")

            if (isAppBundle() && mcAppInfo != null) {
                val splitDirs = mcAppInfo.splitPublicSourceDirs
                Log.d("SplitParser", "Split source dirs: ${splitDirs?.joinToString()}")

                splitDirs?.forEach { path ->
                    val name = File(path).name
                    Log.d("SplitParser", "Processing APK: $name")

                    if (name.contains("arm") || name.contains("x86")) {
                        extractLibrariesFromApk(path, abi, abiPath)
                    } else {
                        Log.d("SplitParser", "Skipping APK (no arch match): $name")
                    }
                }
            } else {
                Log.d("SplitParser", "Not an app bundle, trying main APK")
                val mainApk = mcAppInfo?.sourceDir
                if (mainApk != null) {
                    extractLibrariesFromApk(mainApk, abi, abiPath)
                }
            }

            val mainApk = mcAppInfo?.sourceDir
            if (mainApk != null && !mainApk.contains("split_")) {
                Log.d("SplitParser", "Also trying main base.apk: $mainApk")
                extractLibrariesFromApk(mainApk, abi, abiPath)
            }

            val actualNativeDir = mcAppInfo?.nativeLibraryDir
            if (actualNativeDir != null) {
                Log.d("SplitParser", "Trying to copy from actual native lib dir: $actualNativeDir")
                copyLibrariesFromNativeDir(actualNativeDir, abiPath)
            }

            verifyExtractedLibraries(abiPath)

        } catch (e: Exception) {
            Log.e("SplitParser", "Error during library extraction", e)
        }
    }

    private fun extractLibrariesFromApk(apkPath: String, abi: String, outputDir: File) {
        Log.d("SplitParser", "Extracting from APK: $apkPath")

        try {
            ZipFile(apkPath).use { zipFile ->
                val buffer = ByteArray(8192)

                for (so in minecraftLibs) {
                    val entryPath = "$abi/$so"
                    val entry = zipFile.getEntry(entryPath)

                    if (entry != null) {
                        Log.d("SplitParser", "Found library: $so")
                        val outputFile = File(outputDir, so)

                        zipFile.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                        }

                        outputFile.setReadable(true)
                        outputFile.setExecutable(true)

                        Log.d("SplitParser", "Successfully extracted: $so (${outputFile.length()} bytes)")
                    } else {
                        Log.w("SplitParser", "Library not found in APK: $so (entry: $entryPath)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SplitParser", "Error extracting from APK: $apkPath", e)
        }
    }
    private fun copyLibrariesFromNativeDir(nativeDir: String, abiPath: File) {
        try {
            val nativeDirFile = File(nativeDir)
            if (!nativeDirFile.exists() || !nativeDirFile.isDirectory) {
                Log.w("SplitParser", "Native directory does not exist: $nativeDir")
                return
            }

            for (libName in minecraftLibs) {
                val sourceFile = File(nativeDirFile, libName)
                val destFile = File(abiPath, libName)

                if (sourceFile.exists() && sourceFile.length() > 0) {
                    try {
                        sourceFile.copyTo(destFile, overwrite = true)
                        destFile.setReadable(true)
                        destFile.setExecutable(true)
                        Log.d("SplitParser", "✓ Copied $libName (${sourceFile.length()} bytes)")
                    } catch (e: Exception) {
                        Log.w("SplitParser", "Failed to copy $libName: ${e.message}")
                    }
                } else {
                    Log.d("SplitParser", "- $libName not found in native dir")
                }
            }
        } catch (e: Exception) {
            Log.e("SplitParser", "Error copying from native directory: ${e.message}")
        }
    }

    private fun verifyExtractedLibraries(abiPath: File) {
        Log.d("SplitParser", "Verifying extracted libraries in: ${abiPath.absolutePath}")

        var extractedCount = 0
        for (so in minecraftLibs) {
            val libFile = File(abiPath, so)
            if (libFile.exists() && libFile.length() > 0) {
                Log.d("SplitParser", "✓ $so: ${libFile.length()} bytes")
                extractedCount++
            } else {
                Log.w("SplitParser", "✗ $so: missing or empty")
            }
        }

        Log.i("SplitParser", "Library extraction complete: $extractedCount/${minecraftLibs.size} libraries extracted")
    }

    fun isAppBundle(): Boolean {
        val mcContext = MinecraftInfo.getMinecraftPackageContext()
        return mcContext?.applicationInfo?.splitPublicSourceDirs?.isNotEmpty() == true
    }
}