package org.levimc.launcher.core.content;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePackItem extends ContentItem {
    private static final String TAG = "ResourcePackItem";
    
    public enum PackType {
        RESOURCE_PACK,
        BEHAVIOR_PACK,
        ADDON
    }
    
    private String packName;
    private String version;
    private String description;
    private PackType packType;
    private boolean isValid;
    private String uuid;

    public ResourcePackItem(String name, File packFile, PackType packType) {
        super(name, packFile);
        this.packType = packType;
        this.packName = name;
        loadPackInfo();
    }

    @Override
    public String getType() {
        switch (packType) {
            case RESOURCE_PACK:
                return "Resource Pack";
            case BEHAVIOR_PACK:
                return "Behavior Pack";
            case ADDON:
                return "Add-On";
            default:
                return "Pack";
        }
    }

    @Override
    public String getDescription() {
        if (!isValid) return "Invalid pack";
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return String.format("Version: %s", version != null ? version : "Unknown");
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    public String getPackName() {
        return packName;
    }



    public String getVersion() {
        return version;
    }



    private void loadPackInfo() {
        if (file == null || !file.exists()) {
            isValid = false;
            return;
        }

        if (file.isDirectory()) {
            loadPackInfoFromDirectory();
        } else if (file.getName().toLowerCase().endsWith(".mcpack") || 
                   file.getName().toLowerCase().endsWith(".mcaddon")) {
            loadPackInfoFromZip();
        } else {
            isValid = false;
        }
    }

    private void loadPackInfoFromDirectory() {
        File manifest = new File(file, "manifest.json");
        if (!manifest.exists()) {
            isValid = false;
            return;
        }

        try (FileInputStream fis = new FileInputStream(manifest)) {
            byte[] data = new byte[(int) manifest.length()];
            fis.read(data);
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            parseManifest(jsonStr);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read manifest.json from directory", e);
            isValid = false;
        }
    }

    private void loadPackInfoFromZip() {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    byte[] buffer = new byte[1024];
                    StringBuilder sb = new StringBuilder();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
                    }
                    parseManifest(sb.toString());
                    return;
                }
            }
            isValid = false;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read manifest.json from zip", e);
            isValid = false;
        }
    }

    private void parseManifest(String jsonStr) {
        try {
            JSONObject manifest = new JSONObject(jsonStr);

            if (manifest.has("header")) {
                JSONObject header = manifest.getJSONObject("header");
                
                if (header.has("name")) {
                    packName = header.getString("name");
                    this.name = packName;
                }
                
                if (header.has("description")) {
                    description = header.getString("description");
                }
                
                if (header.has("version")) {
                    Object versionObj = header.get("version");
                    if (versionObj instanceof String) {
                        version = (String) versionObj;
                    } else if (versionObj instanceof org.json.JSONArray) {
                        org.json.JSONArray versionArray = (org.json.JSONArray) versionObj;
                        StringBuilder versionBuilder = new StringBuilder();
                        for (int i = 0; i < versionArray.length(); i++) {
                            if (i > 0) versionBuilder.append(".");
                            versionBuilder.append(versionArray.getInt(i));
                        }
                        version = versionBuilder.toString();
                    }
                }
                
                if (header.has("uuid")) {
                    uuid = header.getString("uuid");
                }
            }

            if (manifest.has("modules")) {
                org.json.JSONArray modules = manifest.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    JSONObject module = modules.getJSONObject(i);
                    if (module.has("type")) {
                        String moduleType = module.getString("type");
                        if ("resources".equals(moduleType)) {
                            packType = PackType.RESOURCE_PACK;
                        } else if ("data".equals(moduleType) || "script".equals(moduleType)) {
                            packType = PackType.BEHAVIOR_PACK;
                        }
                    }
                }
            }
            
            isValid = true;
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse manifest.json", e);
            isValid = false;
        }
    }

    public boolean isResourcePack() {
        return packType == PackType.RESOURCE_PACK;
    }

    public boolean isBehaviorPack() {
        return packType == PackType.BEHAVIOR_PACK;
    }
}