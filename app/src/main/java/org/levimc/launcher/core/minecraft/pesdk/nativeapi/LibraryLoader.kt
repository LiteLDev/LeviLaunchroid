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
package org.levimc.launcher.core.minecraft.pesdk.nativeapi

import android.annotation.SuppressLint
import android.util.Log
import java.io.File

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
object LibraryLoader {
    private external fun nativeOnLauncherLoaded(libPath: String)

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadLauncher(mcLibsPath: String) {
        System.loadLibrary("launcher-core")
        nativeOnLauncherLoaded("$mcLibsPath/libminecraftpe.so")
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadFMod(mcLibsPath: String) {
        loadLibraryWithFallback("libfmod.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadMediaDecoders(mcLibsPath: String) {
        loadLibraryWithFallback("libMediaDecoders_Android.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadMinecraftPE(mcLibsPath: String) {
        loadLibraryWithFallback("libminecraftpe.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadCppShared(mcLibsPath: String) {
        loadLibraryWithFallback("libc++_shared.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadPairipCore(mcLibsPath: String) {
        loadLibraryWithFallback("libpairipcore.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadConscryptJni(mcLibsPath: String) {
        loadLibraryWithFallback("libconscrypt_jni.so", mcLibsPath)
    }

    @JvmStatic
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadMaeSdk(mcLibsPath: String) {
        loadLibraryWithFallback("libmaesdk.so", mcLibsPath)
    }

    private fun loadLibraryWithFallback(libraryName: String, primaryPath: String) {
        val primaryFile = File(primaryPath, libraryName)
        Log.d("LibraryLoader", "Attempting to load $libraryName from $primaryPath")

        if (primaryFile.exists() && primaryFile.length() > 0) {
            runCatching {
                System.load(primaryFile.absolutePath)
                Log.d("LibraryLoader", "Successfully loaded $libraryName from primary path")
                return
            }.onFailure { e ->
                Log.w("LibraryLoader", "Failed to load $libraryName from primary path", e)
            }
        } else {
            Log.w("LibraryLoader", "$libraryName not found or empty at primary path: ${primaryFile.absolutePath}")
        }

        val fallbackPaths = getFallbackPaths(libraryName)
        for (fallbackPath in fallbackPaths) {
            val fallbackFile = File(fallbackPath)
            if (fallbackFile.exists() && fallbackFile.length() > 0) {
                runCatching {
                    System.load(fallbackFile.absolutePath)
                    Log.i("LibraryLoader", "Successfully loaded $libraryName from fallback: $fallbackPath")
                    return
                }.onFailure { e ->
                    Log.w("LibraryLoader", "Failed to load $libraryName from fallback: $fallbackPath", e)
                }
            }
        }

        val libNameWithoutExtension = libraryName.removePrefix("lib").removeSuffix(".so")
        runCatching {
            System.loadLibrary(libNameWithoutExtension)
            Log.i("LibraryLoader", "Successfully loaded $libraryName as system library")
        }.onFailure { e ->
            Log.e("LibraryLoader", "Failed to load $libraryName from all sources", e)
        }
    }

    private fun getFallbackPaths(libraryName: String): List<String> {
        val context = org.levimc.launcher.core.minecraft.mcpelauncher.ModdedPEApplication.getContext()
        val fallbackPaths = mutableListOf<String>()

        fallbackPaths.add("${context.filesDir}/native/$libraryName")

        fallbackPaths.add("${context.applicationInfo.nativeLibraryDir}/$libraryName")

        fallbackPaths.add("${context.filesDir.parent}/lib/$libraryName")

        Log.d("LibraryLoader", "Fallback paths for $libraryName: ${fallbackPaths.joinToString()}")
        return fallbackPaths
    }
}
