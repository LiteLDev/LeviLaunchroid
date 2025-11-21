package org.levimc.launcher.core.content;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.levimc.launcher.core.versions.GameVersion;

import java.io.File;
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
                } else if (packDir.getName().toLowerCase().endsWith(".mcpack") ||
                          packDir.getName().toLowerCase().endsWith(".mcaddon")) {
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

                File targetDir;
                ResourcePackItem.PackType packType;
                
                if (fileName.toLowerCase().endsWith(".mcpack")) {
                    targetDir = resourcePacksDirectory;
                    packType = ResourcePackItem.PackType.RESOURCE_PACK;
                } else if (fileName.toLowerCase().endsWith(".mcaddon")) {
                    targetDir = resourcePacksDirectory;
                    packType = ResourcePackItem.PackType.ADDON;
                } else if (fileName.toLowerCase().endsWith(".zip")) {
                    targetDir = resourcePacksDirectory;
                    packType = ResourcePackItem.PackType.RESOURCE_PACK;
                } else {
                    callback.onError("Unsupported pack format");
                    return;
                }

                if (targetDir == null) {
                    callback.onError("No version selected");
                    return;
                }

                String packName = generateUniquePackName(fileName, targetDir);
                File targetFile = new File(targetDir, packName);

                try {
                    copyStream(inputStream, new FileOutputStream(targetFile));

                    ResourcePackItem pack = new ResourcePackItem(packName, targetFile, packType);
                    if (!pack.isValid()) {
                        targetFile.delete();
                        callback.onError("Invalid pack file");
                        return;
                    }

                    if (packType == ResourcePackItem.PackType.ADDON) {
                        File finalTarget;
                        if (pack.isBehaviorPack()) {
                            finalTarget = new File(behaviorPacksDirectory, packName);
                            targetFile.renameTo(finalTarget);
                        }
                    }

                    callback.onSuccess("Pack imported successfully");
                    
                } finally {
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
        String packName = baseName;
        int counter = 1;
        
        while (new File(directory, packName).exists()) {
            String nameWithoutExt = baseName;
            String extension = "";
            
            int lastDot = baseName.lastIndexOf('.');
            if (lastDot > 0) {
                nameWithoutExt = baseName.substring(0, lastDot);
                extension = baseName.substring(lastDot);
            }
            
            packName = nameWithoutExt + "_" + counter + extension;
            counter++;
        }
        
        return packName;
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

    public void shutdown() {
        executor.shutdown();
    }
}