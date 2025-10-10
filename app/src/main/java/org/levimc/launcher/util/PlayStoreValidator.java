package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

public class PlayStoreValidator {

    private static final String MINECRAFT_PACKAGE_NAME = "com.mojang.minecraftpe";
    private static final String PLAY_STORE_INSTALLER = "com.android.vending";
    private static final String PREFS_NAME = "LauncherPrefs";
    private static final String KEY_LICENSE_VERIFIED = "license_verified";

    public static boolean isMinecraftFromPlayStore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLicenseVerified = prefs.getBoolean(KEY_LICENSE_VERIFIED, false);

        if (isLicenseVerified) {
            return true;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            try {
                packageManager.getPackageInfo(MINECRAFT_PACKAGE_NAME, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

            String installerPackageName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    installerPackageName = packageManager.getInstallSourceInfo(MINECRAFT_PACKAGE_NAME)
                            .getInstallingPackageName();
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            } else {
                installerPackageName = packageManager.getInstallerPackageName(MINECRAFT_PACKAGE_NAME);
            }

            boolean isFromPlayStore = PLAY_STORE_INSTALLER.equals(installerPackageName);
            if (isFromPlayStore) {
                prefs.edit().putBoolean(KEY_LICENSE_VERIFIED, true).apply();
            }

            return isFromPlayStore;

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isMinecraftInstalled(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo(MINECRAFT_PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isLicenseVerified(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LICENSE_VERIFIED, false);
    }

    public static void clearLicenseVerification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LICENSE_VERIFIED, false).apply();
    }
}