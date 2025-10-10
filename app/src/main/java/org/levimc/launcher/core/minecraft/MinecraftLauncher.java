package org.levimc.launcher.core.minecraft;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.mods.ModNativeLoader;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.dialogs.LoadingDialog;
import org.levimc.launcher.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class MinecraftLauncher {
    private static final String TAG = "MinecraftLauncher";
    private final Context context;
    private GamePackageManager gameManager;
    public static final String MC_PACKAGE_NAME = "com.mojang.minecraftpe";

    public MinecraftLauncher(Context context) {
        this.context = context;
    }

    public static String abiToSystemLibDir(String abi) {
        if ("arm64-v8a".equals(abi)) return "arm64";
        if ("armeabi-v7a".equals(abi)) return "arm";
        return abi;
    }

    public ApplicationInfo createFakeApplicationInfo(GameVersion version, String packageName) {
        ApplicationInfo fakeInfo = new ApplicationInfo();
        File apkFile = new File(version.versionDir, "base.apk.levi");
        fakeInfo.sourceDir = apkFile.getAbsolutePath();
        fakeInfo.publicSourceDir = fakeInfo.sourceDir;
        String systemAbi = abiToSystemLibDir(Build.SUPPORTED_ABIS[0]);
        File dstLibDir = new File(context.getDataDir(), "minecraft/" + version.directoryName + "/lib/" + systemAbi);
        fakeInfo.nativeLibraryDir = dstLibDir.getAbsolutePath();
        fakeInfo.packageName = packageName;
        fakeInfo.dataDir = version.versionDir.getAbsolutePath();

        File splitsFolder = new File(version.versionDir, "splits");
        if (splitsFolder.exists() && splitsFolder.isDirectory()) {
            File[] splits = splitsFolder.listFiles();
            if (splits != null) {
                ArrayList<String> splitPathList = new ArrayList<>();
                for (File f : splits) {
                    if (f.isFile() && f.getName().endsWith(".apk.levi")) {
                        splitPathList.add(f.getAbsolutePath());
                    }
                }
                if (!splitPathList.isEmpty()) {
                    fakeInfo.splitSourceDirs = splitPathList.toArray(new String[0]);
                }
            }
        }
        return fakeInfo;
    }

    public void launch(Intent sourceIntent, GameVersion version) {
        Activity activity = (Activity) context;

        try {
            if (version == null) {
                Logger.get().error("No version selected");
                showLaunchErrorOnUi("No version selected");
                return;
            }

            if (version.needsRepair) {
                activity.runOnUiThread(() ->
                        org.levimc.launcher.core.versions.VersionManager.attemptRepairLibs(activity, version)
                );
                return;
            }

            activity.runOnUiThread(this::showLoading);
            gameManager = GamePackageManager.Companion.getInstance(context.getApplicationContext(), version);
            fillIntentWithMcPath(sourceIntent, version);
            launchMinecraftActivity(sourceIntent, version, false);
        } catch (Exception e) {
            Logger.get().error("Launch failed: " + e.getMessage(), e);
            showLaunchErrorOnUi("Launch failed: " + e.getMessage());
        }
    }

    private void fillIntentWithMcPath(Intent sourceIntent, GameVersion version) {
        if (FeatureSettings.getInstance().isVersionIsolationEnabled()) {
            sourceIntent.putExtra("MC_PATH", version.versionDir.getAbsolutePath());
        } else {
            sourceIntent.putExtra("MC_PATH", "");
        }
    }

    private void launchMinecraftActivity(Intent sourceIntent, GameVersion version, boolean modsEnabled) {
        Activity activity = (Activity) context;

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    sourceIntent.putExtra("DISABLE_SPLASH_SCREEN", true);
                }

                sourceIntent.setClass(context, MinecraftActivity.class);
                ApplicationInfo mcInfo = version.isInstalled ?
                        gameManager.getPackageContext().getApplicationInfo() :
                        createFakeApplicationInfo(version, MC_PACKAGE_NAME);
                sourceIntent.putExtra("MC_SRC", mcInfo.sourceDir);
                if (mcInfo.splitSourceDirs != null) {
                    sourceIntent.putExtra("MC_SPLIT_SRC", new ArrayList<>(Arrays.asList(mcInfo.splitSourceDirs)));
                }
                sourceIntent.putExtra("MODS_ENABLED", modsEnabled);
                sourceIntent.putExtra("MINECRAFT_VERSION", version.versionCode);
                sourceIntent.putExtra("MINECRAFT_VERSION_DIR", version.directoryName);
                sourceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (shouldLoadMaesdk(version)) {
                    gameManager.loadAllLibraries();
                } else {
                    gameManager.loadLibrary("c++_shared");
                    gameManager.loadLibrary("fmod");
                    gameManager.loadLibrary("MediaDecoders_Android");
                    gameManager.loadLibrary("minecraftpe");
                }
                ModNativeLoader.loadEnabledSoMods(ModManager.getInstance(), context.getCacheDir());

                activity.runOnUiThread(() -> {
                    activity.finish();
                    context.startActivity(sourceIntent);
                });
            } catch (Exception e) {
                Logger.get().error("Failed to launch Minecraft activity: " + e.getMessage(), e);
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Failed to launch: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private boolean shouldLoadMaesdk(GameVersion version) {
        if (version == null || version.versionCode == null) {
            return false;
        }
        String versionCode = version.versionCode;
        String targetVersion = versionCode.contains("beta") ? "1.21.110.22" : "1.21.110";
        return isVersionAtLeast(versionCode, targetVersion);
    }

    private boolean isVersionAtLeast(String currentVersion, String targetVersion) {
        try {
            String[] current = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
            String[] target = targetVersion.split("\\.");

            int maxLength = Math.max(current.length, target.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int targetPart = i < target.length ? Integer.parseInt(target[i]) : 0;

                if (currentPart > targetPart) return true;
                if (currentPart < targetPart) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showLoading() {
        new LoadingDialog(context).show();
    }

    private void showLaunchErrorOnUi(String message) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> Toast.makeText(
                activity, "Failed to launch Minecraft: " + message, Toast.LENGTH_LONG).show()
        );
    }
}