package org.levimc.launcher.util;

import android.content.Context;
import android.net.Uri;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkUtils {
    public static String extractMinecraftVersionNameFromUri(Context context, Uri uri) {
        File tempFile = null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return "Error Apk";

            tempFile = new File(context.getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
            try (ApkFile apkFile = new ApkFile(tempFile)) {
                ApkMeta apkMeta = apkFile.getApkMeta();
                String packageName = apkMeta.getPackageName();
                String versionName = apkMeta.getVersionName();

                if ("com.mojang.minecraftpe".equals(packageName) && versionName != null && !versionName.isEmpty()) {
                    return "Minecraft_" + versionName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return "Error Apk";
    }

    public static String extractMinecraftVersionNameFromApksUri(Context context, Uri uri) {
        File tempApksFile = null;
        File tempApkFile = null;
        try {
            tempApksFile = new File(context.getCacheDir(), "temp_apks_" + System.currentTimeMillis() + ".apks");
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(tempApksFile)) {
                if (is == null) return "Error Apk";
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempApksFile)))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().equals("base.apk")) {
                        tempApkFile = new File(context.getCacheDir(), "temp_base_" + System.currentTimeMillis() + ".apk");
                        try (OutputStream os = new FileOutputStream(tempApkFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                        }
                        
                        try (ApkFile apkFile = new ApkFile(tempApkFile)) {
                            ApkMeta apkMeta = apkFile.getApkMeta();
                            String packageName = apkMeta.getPackageName();
                            String versionName = apkMeta.getVersionName();
                            if ("com.mojang.minecraftpe".equals(packageName) && versionName != null && !versionName.isEmpty()) {
                                return "Minecraft_" + versionName;
                            }
                        }
                        break;
                    }
                    zis.closeEntry();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempApksFile != null && tempApksFile.exists()) {
                tempApksFile.delete();
            }
            if (tempApkFile != null && tempApkFile.exists()) {
                tempApkFile.delete();
            }
        }
        return "Error Apk";
    }

    public static String abiToSystemLibDir(String abi) {
        if (abi == null) return "unknown";
        switch (abi) {
            case "armeabi-v7a":
                return "arm";
            case "arm64-v8a":
                return "arm64";
            default:
                return abi;
        }
    }

    public static void unzipLibsToSystemAbi(File libBaseDir, ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.startsWith("lib/") && !entry.isDirectory()) {
                String[] parts = name.split("/");
                if (parts.length < 3) continue;
                String abi = parts[1];
                String systemAbi = abiToSystemLibDir(abi);
                String soName = parts[2];
                File outFile = new File(libBaseDir, systemAbi + "/" + soName);
                File parent = outFile.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            zis.closeEntry();
        }
    }
}
