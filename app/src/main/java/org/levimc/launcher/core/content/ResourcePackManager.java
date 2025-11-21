package org.levimc.launcher.core.content;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.levimc.launcher.core.versions.GameVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourcePackManager {
    private static final String TAG = "ResourcePackManager";
    private static final int BUFFER_SIZE = 8192;
    
    private final Context context;
    private final ExecutorService executor;
    private File resourcePacksDirectory;
    private File behaviorPacksDirectory;
    private File skinPacksDirectory;
    
    public interface PackOperationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int progress);
    }

    public ResourcePackManager(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setCurrentVersion(GameVersion version) {
        if (version != null && version.versionDir != null) {
            File gameDataDir = new File(version.versionDir, "games/com.mojang");
            this.resourcePacksDirectory = new File(gameDataDir, "resource_packs");
            this.behaviorPacksDirectory = new File(gameDataDir, "behavior_packs");
            this.skinPacksDirectory = new File(gameDataDir, "skin_packs");
            
            if (!resourcePacksDirectory.exists()) {
                resourcePacksDirectory.mkdirs();
            }
            if (!behaviorPacksDirectory.exists()) {
                behaviorPacksDirectory.mkdirs();
            }
            if (!skinPacksDirectory.exists()) {
                skinPacksDirectory.mkdirs();
            }
        } else {
            this.resourcePacksDirectory = null;
            this.behaviorPacksDirectory = null;
            this.skinPacksDirectory = null;
        }
    }

    public void setPackDirectories(File resourcePacksDir, File behaviorPacksDir, File skinPacksDir) {
        this.resourcePacksDirectory = resourcePacksDir;
        this.behaviorPacksDirectory = behaviorPacksDir;
        this.skinPacksDirectory = skinPacksDir;
        
        if (resourcePacksDirectory != null && !resourcePacksDirectory.exists()) {
            resourcePacksDirectory.mkdirs();
        }
        if (behaviorPacksDirectory != null && !behaviorPacksDirectory.exists()) {
            behaviorPacksDirectory.mkdirs();
        }
        if (skinPacksDirectory != null && !skinPacksDirectory.exists()) {
            skinPacksDirectory.mkdirs();
        }
    }

    public List<ResourcePackItem> getResourcePacks() {
        List<ResourcePackItem> packs = new ArrayList<>();
        
        if (resourcePacksDirectory != null && resourcePacksDirectory.exists()) {
            addPacksFromDirectory(resourcePacksDirectory, ResourcePackItem.PackType.RESOURCE_PACK, packs);
        }
        
        return packs;
    }

    public List<ResourcePackItem> getBehaviorPacks() {
        List<ResourcePackItem> packs = new ArrayList<>();
        
        if (behaviorPacksDirectory != null && behaviorPacksDirectory.exists()) {
            addPacksFromDirectory(behaviorPacksDirectory, ResourcePackItem.PackType.BEHAVIOR_PACK, packs);
        }
        
        return packs;
    }

    public List<ResourcePackItem> getSkinPacks() {
        List<ResourcePackItem> packs = new ArrayList<>();
        
        if (skinPacksDirectory != null && skinPacksDirectory.exists()) {
            addPacksFromDirectory(skinPacksDirectory, ResourcePackItem.PackType.RESOURCE_PACK, packs);
        }
        
        return packs;
    }

    private void addPacksFromDirectory(File directory, ResourcePackItem.PackType packType, List<ResourcePackItem> packs) {
        File[] packDirs = directory.listFiles();
        if (packDirs != null) {
            for (File packDir : packDirs) {
                if (packDir.isDirectory()) {
                    ResourcePackItem pack = new ResourcePackItem(packDir.getName(), packDir, packType);
                    if (pack.isValid()) {
                        packs.add(pack);
                    }
                }
            }
        }
    }

    public void importPack(Uri packUri, PackOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("ResourcePackManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(packUri);
                if (inputStream == null) {
                    callback.onError("Cannot open pack file");
                    return;
                }

                String fileName = getFileName(packUri);
                if (fileName == null) {
                    fileName = "imported_pack_" + System.currentTimeMillis();
                }

                if (resourcePacksDirectory == null || behaviorPacksDirectory == null || skinPacksDirectory == null) {
                    callback.onError("No version selected");
                    inputStream.close();
                    return;
                }

                String tempDirName = "temp_pack_" + System.currentTimeMillis();
                File tempDir = new File(context.getCacheDir(), tempDirName);
                tempDir.mkdirs();

                try {
                    extractZip(inputStream, tempDir);

                    File packContentDir = findPackDirectory(tempDir);
                    if (packContentDir == null) {
                        callback.onError("Invalid pack file - no manifest found");
                        return;
                    }

                    File manifestFile = new File(packContentDir, "manifest.json");
                    if (!manifestFile.exists()) {
                        callback.onError("Invalid pack file - no manifest found");
                        return;
                    }

                    ResourcePackItem.PackType packType = determinePackType(manifestFile);
                    
                    File targetDir = switch (packType) {
                        case BEHAVIOR_PACK -> behaviorPacksDirectory;
                        case RESOURCE_PACK -> {
                            if (isSkinPack(manifestFile)) {
                                yield skinPacksDirectory;
                            }
                            yield resourcePacksDirectory;
                        }
                        default -> resourcePacksDirectory;
                    };

                    String baseName;
                    if (packContentDir.equals(tempDir)) {
                        baseName = fileName.replaceAll("\\.(mcpack|mcaddon|zip)$", "");
                    } else {
                        baseName = packContentDir.getName();
                    }
                    
                    String uniqueName = generateUniquePackName(baseName, targetDir);
                    File finalDir = new File(targetDir, uniqueName);

                    copyDirectory(packContentDir, finalDir);

                    callback.onSuccess("Pack imported successfully");
                    
                } finally {
                    deleteFile(tempDir);
                    inputStream.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to import pack", e);
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }

    public void deletePack(ResourcePackItem pack, PackOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("ResourcePackManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                if (deleteFile(pack.getFile())) {
                    callback.onSuccess("Pack deleted successfully");
                } else {
                    callback.onError("Failed to delete pack");
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete pack", e);
                callback.onError("Delete failed: " + e.getMessage());
            }
        });
    }

    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return path.substring(lastSlash + 1);
            }
        }
        return null;
    }

    private String generateUniquePackName(String baseName, File directory) {
        String sanitized = sanitizeFileName(baseName);
        String packName = sanitized;
        int counter = 1;
        
        while (new File(directory, packName).exists()) {
            String nameWithoutExt = sanitized;
            String extension = "";
            
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                nameWithoutExt = sanitized.substring(0, lastDot);
                extension = sanitized.substring(lastDot);
            }
            
            packName = nameWithoutExt + "_" + counter + extension;
            counter++;
        }
        
        return packName;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "pack";
        return fileName.replaceAll("[:\\\\/*\"?|<>]", "_");
    }

    private void copyStream(InputStream input, FileOutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
        }
        output.close();
    }

    private boolean deleteFile(File file) {
        if (file == null || !file.exists()) return false;
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
        }
        return file.delete();
    }

    private void extractZip(InputStream inputStream, File targetDir) throws IOException {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(inputStream);
        java.util.zip.ZipEntry entry;
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while ((entry = zis.getNextEntry()) != null) {
            File entryFile = new File(targetDir, entry.getName());

            if (!entryFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
                continue;
            }
            
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                entryFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    private File findPackDirectory(File searchDir) {
        File manifest = new File(searchDir, "manifest.json");
        if (manifest.exists()) {
            return searchDir;
        }
        
        File[] files = searchDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File manifestInSubdir = new File(file, "manifest.json");
                    if (manifestInSubdir.exists()) {
                        return file;
                    }
                    File found = findPackDirectory(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private ResourcePackItem.PackType determinePackType(File manifestFile) {
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            byte[] data = new byte[(int) manifestFile.length()];
            fis.read(data);
            String jsonStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            
            org.json.JSONObject manifest = new org.json.JSONObject(jsonStr);
            
            if (manifest.has("modules")) {
                org.json.JSONArray modules = manifest.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    org.json.JSONObject module = modules.getJSONObject(i);
                    if (module.has("type")) {
                        String moduleType = module.getString("type");
                        if ("data".equals(moduleType) || "script".equals(moduleType)) {
                            return ResourcePackItem.PackType.BEHAVIOR_PACK;
                        }
                    }
                }
            }
            
            return ResourcePackItem.PackType.RESOURCE_PACK;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine pack type", e);
            return ResourcePackItem.PackType.RESOURCE_PACK;
        }
    }

    private boolean isSkinPack(File manifestFile) {
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            byte[] data = new byte[(int) manifestFile.length()];
            fis.read(data);
            String jsonStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            
            org.json.JSONObject manifest = new org.json.JSONObject(jsonStr);
            
            if (manifest.has("modules")) {
                org.json.JSONArray modules = manifest.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    org.json.JSONObject module = modules.getJSONObject(i);
                    if (module.has("type")) {
                        String moduleType = module.getString("type");
                        if ("skin_pack".equals(moduleType)) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }
            
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyDirectory(file, new File(target, file.getName()));
                }
            }
        } else {
            copyFile(source, target);
        }
    }

    private void copyFile(File source, File target) throws IOException {
        target.getParentFile().mkdirs();
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}