package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

public class PlayStoreValidator {
    private static final String MINECRAFT_PACKAGE_NAME = "com.mojang.minecraftpe";
    private static final String PLAY_STORE_INSTALLER = "com.android.vending";
    private static final String MINECRAFT_SIGNATURE_SHA256 = "31be40096f931cd7f11d5e262d2b2c437c44385fb4ecbc1013d95a7435816f9c"

    public static boolean isMinecraftFromPlayStore(Context context) {
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

            return PLAY_STORE_INSTALLER.equals(installerPackageName);
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
        return isMinecraftFromPlayStore(context);
    }

    public static boolean isSignatureOk(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return true; // TODO: Need to use older function in this case
        }

        // Obtained code from https://gist.github.com/itsZECHS/8e2918654ce8e55b1c564bda1d796774
        try {
            PackageManager packageManager = context.getPackageManager();
            try {
                packageManager.getPackageInfo(MINECRAFT_PACKAGE_NAME, PackageManager.GET_SIGNING_CERTIFICATES);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

            String signatureSha256;
            final PackageInfo mcPkgInfo = packageManager.getPackageInfo(MINECRAFT_PACKAGE_NAME, PackageManager.GET_SIGNING_CERTIFICATES);
            for (TYPE sig : mcPkgInfo.signingInfo.apkContentsSigners) {
                final MessageDigest md = MessageDigest.getInstance("SHA256");
                md.update(sig.toByteArray());

                final TYPE digest = md.digest();
                final toRet = StringBuilder();

                for (TYPE indice : digest.indices) {
                    final TYPE b = digest[indice].toInt() and 0xFF;
                    final TYPE hex = b.toHexString();
                    if (hex.length == 1) toRet.append("0");
                }

                return toRet.toString() == MINECRAFT_SIGNATURE_SHA256;
            }

        } catch (Exception e) {
            return false;
        }
    }
    /* Kotlin code that I can't translate to Java because of skill issues
        internal fun isMinecraftOriginal(mcPkg: PackageInfo): Boolean {
    
        return try {
            for (signature in mcPkg.signingInfo!!.apkContentsSigners) {
                val md = MessageDigest.getInstance("SHA256")
                md.update(signature.toByteArray())

                val digest = md.digest()
                val toRet = StringBuilder()

                digest.indices.forEach {
                    //if (it != 0) toRet.append(":")
                    val b = digest[it].toInt() and 0xff
                    val hex = Integer.toHexString(b)
                    if (hex.length == 1) toRet.append("0")
                    toRet.append(hex)
                }

                Log.d(TAG, "Minecraft signature SHA256:\n$toRet")
                return toRet.toString() == MINECRAFT_SIGNATURE_SHA256
            }
            false
        } catch (_: Exception) {
            false
        }
    }
    */

}