package org.levimc.launcher.core.versions;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.ui.activities.MainActivity;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.LibsRepairDialog;
import org.levimc.launcher.util.ApkUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionManager {
    private static final String PREFS_NAME = "version_manager";
    private static final String KEY_SELECTED_TYPE = "selected_type";
    private static final String KEY_SELECTED_PACKAGE = "selected_package";
    private static final String KEY_SELECTED_DIR = "selected_dir";
    private static final int BUFFER_SIZE = 8192;

    private static VersionManager instance;
    private final Context context;
    private final List<GameVersion> installedVersions = new ArrayList<>();
    private final List<GameVersion> customVersions = new ArrayList<>();
    private GameVersion selectedVersion;
    private final SharedPreferences prefs;

    public interface LibsRepairCallback {
        void onRepairStarted();

        void onRepairProgress(int progress);

        void onRepairCompleted(boolean success);

        void onRepairFailed(Exception e);
    }

    public interface OnDeleteVersionCallback {
        void onDeleteCompleted(boolean success);

        void onDeleteFailed(Exception e);
    }

    public interface OnRenameVersionCallback {
        void onRenameCompleted(boolean success);

        void onRenameFailed(Exception e);
    }

    public static VersionManager get(Context ctx) {
        if (instance == null) instance = new VersionManager(ctx.getApplicationContext());
        return instance;
    }

    public static String getSelectedModsDir(Context ctx) {
        GameVersion v = get(ctx).getSelectedVersion();
        if (v == null || v.modsDir == null) return null;
        return v.modsDir.getAbsolutePath();
    }

    private VersionManager(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadAllVersions();
    }

    private boolean isMinecraftPackage(String packageName) {
        return packageName.equals("com.mojang.minecraftpe") || packageName.startsWith("com.mojang.");
    }

    private String readFileToString(File file) {
        if (file == null || !file.exists()) return "";
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int len = in.read(data);
            return new String(data, 0, len, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean writeStringToFile(File file, String data) {
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String inferAbiFromNativeLibDir(String nativeLibDir, GameVersion version) {
        if (version != null && !version.isInstalled) {
            File libDir = new File(context.getDataDir(), "minecraft/" + version.directoryName + "/lib/");
            String[] abiDirs = {"arm64", "arm", "x86_64", "x86"};
            for (String abiDir : abiDirs) {
                File soFile = new File(libDir, abiDir + "/libminecraftpe.so");
                if (soFile.exists()) {
                    return switch (abiDir) {
                        case "arm64" -> "arm64-v8a";
                        case "arm" -> "armeabi-v7a";
                        default -> abiDir;
                    };
                }
            }
            return "unknown";
        }
        if (nativeLibDir == null) return "unknown";
        if (nativeLibDir.contains("arm64")) return "arm64-v8a";
        if (nativeLibDir.contains("armeabi")) return "armeabi-v7a";
        if (nativeLibDir.contains("x86_64")) return "x86_64";
        if (nativeLibDir.contains("x86")) return "x86";
        return "unknown";
    }

    private String getApkVersionName(File apkFile) {
        try (net.dongliu.apk.parser.ApkFile apk = new net.dongliu.apk.parser.ApkFile(apkFile)) {
            net.dongliu.apk.parser.bean.ApkMeta meta = apk.getApkMeta();
            if (meta.getVersionName() != null)
                return meta.getVersionName();
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    public void repairLibsAsync(GameVersion version, LibsRepairCallback callback) {
        new Thread(() -> {
            callback.onRepairStarted();
            try {
                File versionDir = version.versionDir;
                boolean onlyVersionTxt = version.onlyVersionTxt;
                boolean isExtractFalse = version.isExtractFalse;

                String dirName = versionDir.getName();

                File apkFile;
                if (isExtractFalse) {
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(version.packageName, 0);
                    apkFile = new File(appInfo.sourceDir);
                } else {
                    apkFile = new File(versionDir, "base.apk.levi");
                }

                String dataDirName = isExtractFalse ? version.directoryName : dirName;
                File dataDir = new File(context.getDataDir(), "minecraft/" + dataDirName);

                if (onlyVersionTxt) {
                    writeVersionTxt(apkFile, dataDir);
                    callback.onRepairCompleted(true);
                    return;
                }

                List<File> apkFiles = new ArrayList<>();
                apkFiles.add(apkFile);

                if (!isExtractFalse) {
                    File splitsDir = new File(versionDir, "splits");
                    if (splitsDir.exists() && splitsDir.isDirectory()) {
                        File[] splitApks = splitsDir.listFiles((dir, name) -> name.endsWith(".apk.levi"));
                        if (splitApks != null) {
                            for (File splitApk : splitApks) {
                                apkFiles.add(splitApk);
                            }
                        }
                    }
                }

                long totalSize = 0;
                for (File apk : apkFiles) {
                    totalSize += getLibsTotalSize(apk);
                }
                if (totalSize == 0) totalSize = 1;

                File libDir = new File(dataDir, "lib");
                if (libDir.exists()) {
                    deleteDir(libDir);
                }
                long[] progress = {0};
                int[] lastPercent = {-1};

                for (File currentApk : apkFiles) {
                    try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(currentApk)))) {
                        ZipEntry entry;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.getName().startsWith("lib/") && !entry.isDirectory()) {
                                String[] parts = entry.getName().split("/");
                                if (parts.length >= 3) {
                                    File outFile = new File(libDir, ApkUtils.abiToSystemLibDir(parts[1]) + "/" + parts[2]);
                                    if (!outFile.getParentFile().exists()) outFile.getParentFile().mkdirs();

                                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                        int len;
                                        while ((len = zis.read(buffer)) != -1) {
                                            fos.write(buffer, 0, len);
                                            progress[0] += len;
                                        }
                                    }

                                    int percent = (int) ((progress[0] * 100) / totalSize);
                                    if (percent != lastPercent[0]) {
                                        callback.onRepairProgress(percent);
                                        lastPercent[0] = percent;
                                    }
                                }
                            }
                        }
                    }
                }

                writeVersionTxt(apkFile, dataDir);

                callback.onRepairCompleted(true);

            } catch (Exception e) {
                callback.onRepairFailed(e);
            }
        }).start();
    }

    private void writeVersionTxt(File apkFile, File dataDir) throws IOException {
        String versionName = getApkVersionName(apkFile);
        if (!dataDir.exists()) dataDir.mkdirs();
        writeStringToFile(new File(dataDir, "version.txt"), versionName);
    }

    private long getLibsTotalSize(File apkFile) throws IOException {
        long totalSize = 0;
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(apkFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name != null && name.startsWith("lib/") && !entry.isDirectory()) {
                    long size = entry.getSize();
                    if (size < 0) {
                        size = entry.getCompressedSize();
                    }
                    if (size > 0) {
                        totalSize += size;
                    }
                }
            }
        }
        return totalSize;
    }

    public void loadAllVersions() {
        installedVersions.clear();
        customVersions.clear();

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);

        for (PackageInfo pi : pkgs) {
            if (!isMinecraftPackage(pi.packageName)) continue;

            File versionDir = getVersionDirForPackage(pi.packageName);
            if (!versionDir.exists()) versionDir.mkdirs();
            
            File gamesDir = new File(versionDir, "games/com.mojang");
            if (!gamesDir.exists()) gamesDir.mkdirs();

            String displayName = pi.applicationInfo.loadLabel(pm) + " (" + pi.versionName + ")";
            boolean hasSoFiles = hasSoFilesInDir(new File(pi.applicationInfo.nativeLibraryDir));

            GameVersion gv = new GameVersion(
                    pi.packageName + "_" + pi.versionCode,
                    displayName,
                    pi.versionName,
                    versionDir,
                    true,
                    pi.packageName,
                    "unknown"
            );

            gv.needsRepair = false;
            if (!hasSoFiles) {
                gv.isExtractFalse = true;
                boolean libOk = hasLibSoUnderLibDir(gv.directoryName);
                if (!libOk) {
                    gv.needsRepair = true;
                }
            }

            gv.abiList = inferAbiFromNativeLibDir(pi.applicationInfo.nativeLibraryDir, gv);
            installedVersions.add(gv);
        }

        File baseDir = new File(Environment.getExternalStorageDirectory(), "games/org.levimc/minecraft/");
        File[] dirs = baseDir.listFiles(File::isDirectory);

        if (dirs != null) {
            for (File dir : dirs) {
                File apk = new File(dir, "base.apk.levi");
                if (!apk.exists()) continue;

                boolean libOk = hasLibSoUnderLibDir(dir.getName());
                boolean txtOk = hasValidVersionTxt(dir.getName());

                GameVersion gv = getGameVersion(dir);
                gv.needsRepair = false;
                gv.onlyVersionTxt = false;

                if (!libOk) {
                    gv.needsRepair = true;
                    appendRepairMark(gv);
                } else if (!txtOk) {
                    gv.needsRepair = true;
                    gv.onlyVersionTxt = true;
                    appendRepairMark(gv);
                }
                customVersions.add(gv);
            }
        }
        restoreSelectedVersion();
    }

    @NonNull
    private File getVersionDirForPackage(String packageName) {
        return new File(Environment.getExternalStorageDirectory(),
                "games/org.levimc/minecraft/" + packageName);
    }

    private boolean hasSoFilesInDir(File nativeLibDir) {
        if (nativeLibDir == null) return false;
        File[] files = nativeLibDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".so")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasLibSoUnderLibDir(String dirName) {
        File libDir = new File(context.getDataDir(), "minecraft/" + dirName + "/lib");
        return new File(libDir, "arm64/libminecraftpe.so").exists()
                || new File(libDir, "arm/libminecraftpe.so").exists();
    }

    private boolean hasValidVersionTxt(String dirName) {
        File versionTxt = new File(context.getDataDir(), "minecraft/" + dirName + "/version.txt");
        return versionTxt.exists() && versionTxt.length() > 0;
    }

    private void appendRepairMark(GameVersion gv) {
        if (!gv.displayName.endsWith(" ❌")) {
            gv.displayName += " ❌";
        }
    }

    @NonNull
    private GameVersion getGameVersion(File dir) {
        String versionCode = dir.getName();
        String displayName = dir.getName();

        // Read version code from version.txt
        File versionTxt = new File(context.getDataDir(), "minecraft/" + dir.getName() + "/version.txt");
        if (versionTxt.exists()) {
            String txt = readFileToString(versionTxt);
            if (!txt.isEmpty()) {
                versionCode = txt;
            }
        }

        File displayNameTxt = new File(context.getDataDir(), "minecraft/" + dir.getName() + "/display_name.txt");
        if (displayNameTxt.exists()) {
            String customName = readFileToString(displayNameTxt);
            if (!customName.isEmpty()) {
                displayName = customName;
            }
        }

        displayName = displayName + " (" + versionCode + ")";

        GameVersion gv = new GameVersion(
                dir.getName(),
                displayName,
                versionCode,
                dir,
                false,
                null,
                "unknown"
        );

        gv.isExtractFalse = false;
        gv.directoryName = dir.getName();

        gv.abiList = inferAbiFromNativeLibDir(null, gv);

        return gv;
    }

    private void restoreSelectedVersion() {
        String type = prefs.getString(KEY_SELECTED_TYPE, null);
        if (type != null) {
            if (type.equals("installed")) {
                String pkg = prefs.getString(KEY_SELECTED_PACKAGE, null);
                for (GameVersion gv : installedVersions) {
                    if (gv.packageName != null && gv.packageName.equals(pkg)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            } else if (type.equals("custom")) {
                String dir = prefs.getString(KEY_SELECTED_DIR, null);
                for (GameVersion gv : customVersions) {
                    if (gv.versionDir.getAbsolutePath().equals(dir)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            }
        }
    }

    public List<GameVersion> getInstalledVersions() {
        return installedVersions;
    }

    public List<GameVersion> getCustomVersions() {
        return customVersions;
    }

    public GameVersion getSelectedVersion() {
        if (selectedVersion != null) return selectedVersion;
        if (!installedVersions.isEmpty()) {
            selectVersion(installedVersions.get(0));
            return installedVersions.get(0);
        }
        if (!customVersions.isEmpty()) {
            selectVersion(customVersions.get(0));
            return customVersions.get(0);
        }
        return null;
    }

    public void selectVersion(GameVersion version) {
        this.selectedVersion = version;
        SharedPreferences.Editor editor = prefs.edit();
        if (version != null && version.isInstalled) {
            editor.putString(KEY_SELECTED_TYPE, "installed");
            editor.putString(KEY_SELECTED_PACKAGE, version.packageName);
            editor.remove(KEY_SELECTED_DIR);
        } else if (version != null) {
            editor.putString(KEY_SELECTED_TYPE, "custom");
            editor.putString(KEY_SELECTED_DIR, version.versionDir.getAbsolutePath());
            editor.remove(KEY_SELECTED_PACKAGE);
        } else {
            editor.remove(KEY_SELECTED_TYPE);
            editor.remove(KEY_SELECTED_DIR);
            editor.remove(KEY_SELECTED_PACKAGE);
        }
        editor.apply();
    }

    public void reload() {
        loadAllVersions();
    }

    public static void attemptRepairLibs(Activity activity, GameVersion version) {
        LibsRepairDialog repairDialog = new LibsRepairDialog(activity);

        VersionManager.LibsRepairCallback callback = new VersionManager.LibsRepairCallback() {
            @Override
            public void onRepairStarted() {
                activity.runOnUiThread(() -> {
                    repairDialog.setTitleText(activity.getString(R.string.repair_libs_in_progress));
                    repairDialog.setStatusText(activity.getString(R.string.repair_preparing));
                    repairDialog.setIndeterminate(true);
                    repairDialog.updateProgress(0);
                });
            }

            @Override
            public void onRepairProgress(int progress) {
                activity.runOnUiThread(() -> {
                    if (progress > 0) {
                        repairDialog.setStatusText(activity.getString(R.string.repair_processing));
                        repairDialog.setIndeterminate(false);
                    }
                    repairDialog.updateProgress(progress);
                });
            }

            @Override
            public void onRepairCompleted(boolean success) {
                activity.runOnUiThread(() -> {
                    repairDialog.dismiss();
                    if (success) {
                        new CustomAlertDialog(activity)
                                .setTitleText(activity.getString(R.string.repair_completed))
                                .setMessage(activity.getString(R.string.repair_libs_success_message))
                                .setPositiveButton(activity.getString(R.string.confirm), null)
                                .show();
                        VersionManager.get(activity).reload();
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).setTextMinecraftVersion();
                        }
                    } else {
                        new CustomAlertDialog(activity)
                                .setTitleText(activity.getString(R.string.repair_failed))
                                .setMessage(activity.getString(R.string.repair_libs_failed_message))
                                .setPositiveButton(activity.getString(R.string.confirm), null)
                                .show();
                    }
                });
            }

            @Override
            public void onRepairFailed(Exception e) {
                activity.runOnUiThread(() -> {
                    repairDialog.dismiss();
                    new CustomAlertDialog(activity)
                            .setTitleText(activity.getString(R.string.repair_error))
                            .setMessage(String.format(activity.getString(R.string.repair_libs_error_message), e.getMessage()))
                            .setPositiveButton(activity.getString(R.string.confirm), null)
                            .show();
                });
            }
        };

        new CustomAlertDialog(activity)
                .setTitleText(String.format(activity.getString(R.string.missing_libs_title), version.directoryName))
                .setMessage(activity.getString(R.string.missing_libs_message))
                .setPositiveButton(activity.getString(R.string.repair), v -> {
                    repairDialog.show();
                    VersionManager.get(activity).repairLibsAsync(version, callback);
                })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .show();
    }

    public void renameCustomVersion(GameVersion version, String newDisplayName, OnRenameVersionCallback callback) {
        if (version == null || version.isInstalled) {
            if (callback != null)
                callback.onRenameFailed(new IllegalArgumentException(context.getString(R.string.cannot_rename_installed)));
            return;
        }

        if (newDisplayName == null || newDisplayName.trim().isEmpty()) {
            if (callback != null)
                callback.onRenameFailed(new IllegalArgumentException("Display name cannot be empty"));
            return;
        }

        new Thread(() -> {
            try {
                File dataDir = new File(context.getDataDir(), "minecraft/" + version.directoryName);
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                File displayNameFile = new File(dataDir, "display_name.txt");
                boolean success = writeStringToFile(displayNameFile, newDisplayName.trim());

                if (success) {
                    reload();
                    if (callback != null)
                        callback.onRenameCompleted(true);
                } else {
                    if (callback != null)
                        callback.onRenameFailed(new Exception("Failed to write display name file"));
                }
            } catch (Exception e) {
                if (callback != null)
                    callback.onRenameFailed(e);
            }
        }).start();
    }

    public void deleteCustomVersion(GameVersion version, OnDeleteVersionCallback callback) {
        if (version == null || version.isInstalled) {
            if (callback != null)
                callback.onDeleteFailed(new IllegalArgumentException(context.getString(R.string.error_delete_builtin_version)));
            return;
        }

        ModManager modManager = ModManager.getInstance();
        if (modManager.getCurrentVersion() != null &&
                modManager.getCurrentVersion().equals(version)) {
            modManager.setCurrentVersion(null);
        }

        boolean isSelected = version.equals(selectedVersion);

        new Thread(() -> {
            try {
                File extDir = version.versionDir;
                if (extDir != null && extDir.exists()) {
                    File worldsDir = new File(extDir, "games/com.mojang/minecraftWorlds");
                    if (worldsDir.exists() && worldsDir.isDirectory()) {
                        String backupBase = context.getExternalFilesDir("backups").getAbsolutePath();
                        String timeStr = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                        File backupDir = new File(backupBase, timeStr);
                        backupDir.mkdirs();

                        File[] worldFolders = worldsDir.listFiles(file -> file.isDirectory());
                        if (worldFolders != null) {
                            for (File worldFolder : worldFolders) {
                                File destFolder = new File(backupDir, worldFolder.getName());
                                copyDirectory(worldFolder, destFolder);
                            }
                        }
                    }
                    deleteDir(extDir);
                }

                File intBaseDir = new File(context.getDataDir(), "minecraft");
                File intDir = new File(intBaseDir, (extDir != null ? extDir.getName() : ""));
                File intLibDir = new File(intDir, "lib");
                if (intLibDir.exists()) {
                    deleteDir(intLibDir);
                }
                if (intDir.exists()) {
                    deleteDir(intDir);
                }

                if (isSelected) {
                    selectedVersion = null;
                    reload();
                    if (!customVersions.isEmpty()) {
                        selectVersion(customVersions.get(0));
                    } else if (!installedVersions.isEmpty()) {
                        selectVersion(installedVersions.get(0));
                    } else {
                        selectVersion(null);
                    }
                } else {
                    reload();
                }

                if (callback != null)
                    callback.onDeleteCompleted(true);
            } catch (Exception e) {
                if (callback != null)
                    callback.onDeleteFailed(e);
            }
        }).start();
    }

    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (sourceDir.isDirectory()) {
            if (!targetDir.exists())
                targetDir.mkdirs();
            String[] children = sourceDir.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(sourceDir, child), new File(targetDir, child));
                }
            }
        } else {
            java.nio.file.Files.copy(sourceDir.toPath(), targetDir.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean deleteDir(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null)
                for (File c : files)
                    deleteDir(c);
        }
        return file.delete();
    }
}
