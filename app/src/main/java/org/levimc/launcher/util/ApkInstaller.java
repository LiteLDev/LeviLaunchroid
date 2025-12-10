package org.levimc.launcher.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkInstaller {

    public interface InstallCallback {
        void onProgress(int progress);

        void onSuccess(String versionName);

        void onError(String errorMessage);
    }

    private static final String APK_FILE_NAME = "base.apk.levi";
    private static final int BUFFER_SIZE = 8192;

    private final Context context;
    private final ExecutorService executor;
    private final InstallCallback callback;

    public ApkInstaller(Context context, ExecutorService executor, InstallCallback callback) {
        this.context = context.getApplicationContext();
        this.executor = executor;
        this.callback = callback;
    }

    public static class VersionAbi {
        public final String versionName;

        public VersionAbi(String versionName) {
            this.versionName = versionName;
        }
    }

    public void install(final Uri apkOrApksUri, final String dirName) {
        executor.submit(() -> {
            try {
                File internalDir = new File(context.getDataDir(), "minecraft/" + dirName);
                if (internalDir.exists() && !deleteDir(internalDir))
                    return;
                File externalDir = new File(Environment.getExternalStorageDirectory(), "games/org.levimc/minecraft/" + dirName);
                if (externalDir.exists() && !deleteDir(externalDir))
                    return;

                VersionAbi info = extractVersionAndAbi(apkOrApksUri);
                String versionName = info.versionName;

                File libTargetDir = new File(internalDir, "lib");
                if (libTargetDir.exists()) {
                    deleteDir(libTargetDir);
                }
                File baseDir = externalDir;
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    postError("Open base dir failed");
                    return;
                }

                String fileName = getFileName(apkOrApksUri);
                if (fileName != null && fileName.toLowerCase().endsWith(".apks")) {
                    boolean foundBaseApk = false;
                    File splitsDir = new File(baseDir, "splits");
                    
                    try (InputStream is = context.getContentResolver().openInputStream(apkOrApksUri);
                         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.isDirectory()) {
                                zis.closeEntry();
                                continue;
                            }
                            
                            String entryName = entry.getName();
                            if (!entryName.endsWith(".apk")) {
                                zis.closeEntry();
                                continue;
                            }
                            
                            File outFile;
                            String outputName;
                            
                            if (entryName.equals("base.apk") || entryName.endsWith("/base.apk")) {
                                outFile = new File(baseDir, APK_FILE_NAME);
                                outputName = APK_FILE_NAME;
                            } else {
                                if (!splitsDir.exists()) splitsDir.mkdirs();
                                String splitName = new File(entryName).getName();
                                splitName = splitName.replace(".apk", ".apk.levi");
                                outFile = new File(splitsDir, splitName);
                                outputName = splitName;
                            }
                            
                            File parent = outFile.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();

                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                copyStream(zis, fos);
                            }

                            if (outputName.equals(APK_FILE_NAME)) {
                                try (InputStream is2 = new FileInputStream(outFile);
                                     ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {
                                    ApkUtils.unzipLibsToSystemAbi(libTargetDir, zis2);
                                }
                                foundBaseApk = true;
                            } else if (outputName.contains("arm64") || outputName.contains("armeabi") || outputName.contains("x86")) {
                                try (InputStream is2 = new FileInputStream(outFile);
                                     ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {
                                    ApkUtils.unzipLibsToSystemAbi(libTargetDir, zis2);
                                }
                            }
                            zis.closeEntry();
                        }
                    }
                    if (!foundBaseApk) {
                        postError("No base.apk found in APKS bundle");
                        return;
                    }
                } else {
                    File dstApkFile = new File(baseDir, APK_FILE_NAME);
                    try (InputStream is = context.getContentResolver().openInputStream(apkOrApksUri);
                         OutputStream os = new FileOutputStream(dstApkFile)) {
                        if (is == null) {
                            postError("Open apk failed");
                            return;
                        }
                        copyStream(is, os);
                    }
                    try (InputStream is2 = new FileInputStream(dstApkFile);
                         ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {
                        ApkUtils.unzipLibsToSystemAbi(libTargetDir, zis2);
                    }
                }

                // version.txt
                writeTextFile(new File(internalDir, "version.txt"), versionName);

                postSuccess(versionName);

            } catch (Exception e) {
                postError("Install error: " + e.getMessage());
            }
        });
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private static void writeTextFile(File file, String content) throws IOException {
        try (Writer writer = new FileWriter(file, false)) {
            writer.write(content);
        }
    }

    private void postProgress(int progress) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onProgress(progress);
        });
    }

    private void postSuccess(String versionName) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onSuccess(versionName);
        });
    }

    private void postError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onError(error);
        });
    }

    private VersionAbi extractVersionAndAbi(Uri apkOrApksUri) throws Exception {
        File tempFile = new File(context.getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
        String fileName = getFileName(apkOrApksUri);
        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".apks")) {
                try (InputStream apksIs = context.getContentResolver().openInputStream(apkOrApksUri);
                     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(apksIs));
                     OutputStream os = new FileOutputStream(tempFile)) {
                    boolean found = false;
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".apk")) {
                            if (entry.getName().equals("base.apk") || !found) {
                                copyStream(zis, os);
                                found = true;
                                if (entry.getName().equals("base.apk")) break;
                            }
                        }
                        zis.closeEntry();
                    }
                    if (!found) throw new FileNotFoundException("apks no base.apk!");
                }
            } else {
                try (InputStream is = context.getContentResolver().openInputStream(apkOrApksUri);
                     OutputStream os = new FileOutputStream(tempFile)) {
                    if (is == null) throw new FileNotFoundException("打开apk失败");
                    copyStream(is, os);
                }
            }
            String versionName = extractApkVersionName(tempFile);
            return new VersionAbi(versionName);
        } finally {
            tempFile.delete();
        }
    }

    private String extractApkVersionName(File apkFile) {
        try (ApkFile apk = new ApkFile(apkFile)) {
            ApkMeta meta = apk.getApkMeta();
            String pkgName = meta.getPackageName();
            String vName = meta.getVersionName();
            if ("com.mojang.minecraftpe".equals(pkgName) && vName != null && !vName.isEmpty()) {
                return vName;
            }
        } catch (Exception ignored) {
        }
        return "unknown_version";
    }

    private String getFileName(Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        String result = null;
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return result;
    }

    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists()) return true;
        if (dir.isFile()) return dir.delete();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File child : files) {
                if (!deleteDir(child)) return false;
            }
        }
        return dir.delete();
    }
}