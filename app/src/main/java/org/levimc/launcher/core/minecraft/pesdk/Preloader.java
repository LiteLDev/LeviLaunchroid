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
package org.levimc.launcher.core.minecraft.pesdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.levimc.launcher.core.minecraft.pesdk.nativeapi.LibraryLoader;
import org.levimc.launcher.core.minecraft.pesdk.utils.MinecraftInfo;
import org.levimc.launcher.core.minecraft.pesdk.utils.SplitParser;
import org.levimc.launcher.core.minecraft.pesdk.utils.ABIInfo;
import org.levimc.launcher.core.minecraft.pesdk.utils.MinecraftPackageHelper;

import java.io.File;
import java.security.Security;
import java.util.ArrayList;

import org.conscrypt.Conscrypt;

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class Preloader {
   private final PESdk mPESdk;
    private Bundle mBundle;
    private PreloadListener mPreloadListener;
    private ArrayList<String> mAssetsArrayList = new ArrayList<>();
    private ArrayList<String> mLoadedNativeLibs = new ArrayList<>();

    public Preloader(PESdk pesdk, Bundle bundle, PreloadListener listener) {
        mBundle = bundle;
        mPreloadListener = listener;
        mPESdk = pesdk;
        if (mPreloadListener == null)
            mPreloadListener = new PreloadListener();
    }

    public void preload(Context context) throws PreloadException {
        mPreloadListener.onStart();

        if (mBundle == null)
            mBundle = new Bundle();

        try {
            Log.i("Preloader", "Starting native library preloading process");

            Log.d("Preloader", "Extracting Minecraft libraries...");
            new SplitParser(context).parseMinecraft();

            mPreloadListener.onLoadNativeLibs();

            String nativeLibDir = MinecraftInfo.getMinecraftPackageNativeLibraryDir();
            Log.d("Preloader", "Native library directory: " + nativeLibDir);

            mPreloadListener.onLoadCppSharedLib();
            Log.d("Preloader", "Loading libc++_shared.so...");
            LibraryLoader.loadCppShared(nativeLibDir);

            mPreloadListener.onLoadFModLib();
            Log.d("Preloader", "Loading libfmod.so...");
            LibraryLoader.loadFMod(nativeLibDir);

            mPreloadListener.onLoadMediaDecoders();
            Log.d("Preloader", "Loading libMediaDecoders_Android.so...");
            LibraryLoader.loadMediaDecoders(nativeLibDir);

            Log.d("Preloader", "Loading libpairipcore.so...");
            LibraryLoader.loadPairipCore(nativeLibDir);

            Log.d("Preloader", "Initializing Conscrypt security provider...");
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1);
                Log.d("Preloader", "Conscrypt provider initialized successfully");
            } catch (Exception e) {
                Log.w("Preloader", "Failed to initialize Conscrypt provider: " + e.getMessage());
            }

            Log.d("Preloader", "Loading libmaesdk.so...");
            LibraryLoader.loadMaeSdk(nativeLibDir);

            mPreloadListener.onLoadMinecraftPELib();
            Log.d("Preloader", "Loading libminecraftpe.so...");
            LibraryLoader.loadMinecraftPE(nativeLibDir);

            if (mBundle != null && mBundle.getBoolean("MODS_ENABLED", false)) {
                try {
                    Log.d("Preloader", "Loading mods after libminecraftpe.so...");
                    org.levimc.launcher.core.mods.ModNativeLoader.loadEnabledSoMods(
                            org.levimc.launcher.core.mods.ModManager.getInstance(),
                            context.getCacheDir());
                    Log.i("Preloader", "Successfully loaded mods");
                } catch (Exception e) {
                    Log.e("Preloader", "Error loading mods: " + e.getMessage(), e);
                }
            }

            mPreloadListener.onLoadGameLauncherLib();
            Log.d("Preloader", "Loading launcher core...");
            LibraryLoader.loadLauncher(nativeLibDir);

            mPreloadListener.onFinishedLoadingNativeLibs();
            Log.i("Preloader", "Native library preloading completed successfully");
        } catch (Throwable throwable) {
            Log.e("Preloader", "Failed to load native libraries", throwable);

            String errorDetails = getDetailedErrorInfo(context, throwable);
            Log.e("Preloader", "Error details: " + errorDetails);

            throw new PreloadException(PreloadException.TYPE_LOAD_LIBS_FAILED, throwable);
        }
        mAssetsArrayList = new ArrayList<>();
        mLoadedNativeLibs = new ArrayList<>();
        mAssetsArrayList.add(MinecraftInfo.getMinecraftPackageContext().getPackageResourcePath());
        mPreloadListener.onFinish(mBundle);
    }

    private String getDetailedErrorInfo(Context context, Throwable throwable) {
        StringBuilder info = new StringBuilder();
        info.append("Error: ").append(throwable.getMessage()).append("\n");
        info.append("Device ABI: ").append(ABIInfo.getABI()).append("\n\n");

        info.append("Minecraft Installation Info:\n");
        info.append(MinecraftPackageHelper.INSTANCE.getMinecraftInstallationInfo(context));
        info.append("\n");

        try {
            String nativeDir = MinecraftInfo.getMinecraftPackageNativeLibraryDir();
            File nativeDirFile = new File(nativeDir);
            info.append("Native Library Directory:\n");
            info.append("- Path: ").append(nativeDir).append("\n");
            info.append("- Exists: ").append(nativeDirFile.exists()).append("\n");
            if (nativeDirFile.exists()) {
                File[] files = nativeDirFile.listFiles();
                info.append("- File count: ").append(files != null ? files.length : 0).append("\n");
                if (files != null && files.length > 0) {
                    info.append("- Files: ");
                    for (File file : files) {
                        info.append(file.getName()).append(" (").append(file.length()).append(" bytes), ");
                    }
                    info.append("\n");
                }
            }
        } catch (Exception e) {
            info.append("- Error checking native dir: ").append(e.getMessage()).append("\n");
        }

        info.append("\nSuggested fixes:\n");
        for (String suggestion : MinecraftPackageHelper.INSTANCE.suggestFixes(context)) {
            info.append("- ").append(suggestion).append("\n");
        }

        return info.toString();
    }

    public static class PreloadListener {
        public static String TAG = "PreloadListener";

        public void onStart() {
            Log.e(TAG, "onStart()");
        }

        public void onLoadNativeLibs() {
            Log.e(TAG, "onLoadNativeLibs()");
        }

        public void onLoadGameLauncherLib() {
            Log.e(TAG, "onLoadGameLauncherLib()");
        }

        public void onLoadFModLib() {
            Log.e(TAG, "onLoadFModLib()");
        }

        public void onLoadMediaDecoders() {
            Log.e(TAG, "onLoadMediaDecoders()");
        }

        public void onLoadMinecraftPELib() {
            Log.e(TAG, "onLoadMinecraftPELib()");
        }

        public void onLoadCppSharedLib() {
            Log.e(TAG, "onLoadCppSharedLib()");
        }

        public void onLoadPESdkLib() {
            Log.e(TAG, "onLoadPESdkLib()");
        }

        public void onFinishedLoadingNativeLibs() {
            Log.e(TAG, "onFinishedLoadingNativeLibs()");
        }
        public void onFinish(Bundle bundle) {
            Log.e(TAG, "onFinish()");
        }
    }
}
