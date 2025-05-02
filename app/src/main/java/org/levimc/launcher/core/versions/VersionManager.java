package org.levimc.launcher.core.versions;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class VersionManager {
    private static VersionManager instance;
    private final Context context;
    private List<GameVersion> installedVersions = new ArrayList<>();
    private List<GameVersion> customVersions = new ArrayList<>();
    private GameVersion selectedVersion;
    private SharedPreferences prefs;

    public static VersionManager get(Context ctx) {
        if (instance == null) instance = new VersionManager(ctx.getApplicationContext());
        return instance;
    }

    private VersionManager(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences("version_manager", Context.MODE_PRIVATE);
        loadAllVersions();
    }

    private boolean isMinecraftPackage(String packageName) {
        return packageName.equals("com.mojang.minecraftpe")
                || packageName.startsWith("com.mojang.");
    }

    public void loadAllVersions() {
        installedVersions.clear();
        customVersions.clear();

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        for (PackageInfo pi : pkgs) {
            if (isMinecraftPackage(pi.packageName)) {
                File versionDir = new File(
                        Environment.getExternalStorageDirectory(),
                        "games/org.levimc/minecraft/" + pi.packageName
                );
                if (!versionDir.exists()) versionDir.mkdirs();
                GameVersion gv = new GameVersion(
                        pi.applicationInfo.loadLabel(pm) + " ("+pi.versionName+")",
                        pi.versionName,
                        versionDir,
                        true,
                        pi.packageName
                );
                installedVersions.add(gv);
            }
        }

        File baseDir = new File(Environment.getExternalStorageDirectory(), "games/org.levimc/minecraft/");
        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                File vj = new File(dir, "version.json");
                if (vj.exists()) {
                    boolean exists = false;
                    for (GameVersion g : installedVersions) {
                        if (g.versionDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) continue;
                    try (FileInputStream fi = new FileInputStream(vj); Scanner sc = new Scanner(fi).useDelimiter("\\A")) {
                        String json = sc.hasNext() ? sc.next() : "";
                        JSONObject obj = new JSONObject(json);
                        String versionName = obj.optString("versionName", dir.getName());
                        String versionCode = obj.optString("version", "");
                        GameVersion gv = new GameVersion(versionName + " ("+versionCode+")", versionCode, dir, false, null);
                        customVersions.add(gv);
                    } catch (Exception e) {}
                }
            }
        }

        restoreSelectedVersion();
    }

    private void restoreSelectedVersion() {
        String type = prefs.getString("selected_type", null);
        if(type != null) {
            if(type.equals("official")) {
                String pkg = prefs.getString("selected_package", null);
                for(GameVersion gv : installedVersions) {
                    if(gv.packageName != null && gv.packageName.equals(pkg)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            } else if(type.equals("custom")) {
                String dir = prefs.getString("selected_dir", null);
                for(GameVersion gv : customVersions) {
                    if(gv.versionDir.getAbsolutePath().equals(dir)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            }
        }
    }

    public List<GameVersion> getInstalledVersions() { return installedVersions; }
    public List<GameVersion> getCustomVersions() { return customVersions; }

    public GameVersion getSelectedVersion() {
        if (selectedVersion != null) return selectedVersion;

        // Try to find a valid version
        if (!installedVersions.isEmpty()) {
            selectVersion(installedVersions.get(0));
            return installedVersions.get(0);
        }
        if (!customVersions.isEmpty()) {
            selectVersion(customVersions.get(0));
            return customVersions.get(0);
        }

        return null; // No versions available
    }

    public void selectVersion(GameVersion version) {
        this.selectedVersion = version;
        SharedPreferences.Editor editor = prefs.edit();
        if(version.isInstalled) {
            editor.putString("selected_type", "installed");
            editor.putString("selected_package", version.packageName);
            editor.remove("selected_dir");
        } else {
            editor.putString("selected_type", "custom");
            editor.putString("selected_dir", version.versionDir.getAbsolutePath());
            editor.remove("selected_package");
        }
        editor.apply();
    }

    public void reload() {
        loadAllVersions();
    }
}