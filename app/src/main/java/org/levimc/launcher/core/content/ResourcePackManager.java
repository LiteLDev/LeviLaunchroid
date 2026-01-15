package org.levimc.launcher.core.content;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.core.versions.GameVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
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

    private static class PackInfo {
        boolean isResourcePack = false;
        boolean isBehaviorPack = false;
        boolean isSkinPack = false;
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

                File tempFile = new File(context.getCacheDir(), "temp_import_" + System.currentTimeMillis());
                copyStreamToFile(inputStream, tempFile);
                inputStream.close();

                String lowerName = fileName.toLowerCase();
                int resourceCount = 0;
                int behaviorCount = 0;
                int skinCount = 0;

                if (lowerName.endsWith(".mcaddon")) {
                    int[] counts = importMcaddon(tempFile);
                    resourceCount = counts[0];
                    behaviorCount = counts[1];
                    skinCount = counts[2];
                } else if (lowerName.endsWith(".mcpack")) {
                    int[] counts = importMcpack(tempFile);
                    resourceCount = counts[0];
                    behaviorCount = counts[1];
                    skinCount = counts[2];
                } else {
                    int[] counts = importMcpack(tempFile);
                    resourceCount = counts[0];
                    behaviorCount = counts[1];
                    skinCount = counts[2];
                }

                tempFile.delete();

                int total = resourceCount + behaviorCount + skinCount;
                if (total == 0) {
                    callback.onError("Invalid pack file - no valid packs found");
                } else if (total == 1) {
                    callback.onSuccess("Pack imported successfully");
                } else {
                    StringBuilder msg = new StringBuilder("Imported: ");
                    if (resourceCount > 0) msg.append(resourceCount).append(" resource pack(s) ");
                    if (behaviorCount > 0) msg.append(behaviorCount).append(" behavior pack(s) ");
                    if (skinCount > 0) msg.append(skinCount).append(" skin pack(s)");
                    callback.onSuccess(msg.toString().trim());
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to import pack", e);
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }

    private int[] importMcpack(File zipFile) throws IOException {
        int[] counts = new int[3];
        
        File tempDir = new File(context.getCacheDir(), "temp_pack_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZipFile(zipFile, tempDir);
            File packDir = findPackDirectory(tempDir);
            if (packDir == null) {
                return counts;
            }

            File manifestFile = new File(packDir, "manifest.json");
            if (!manifestFile.exists()) {
                return counts;
            }

            PackInfo packInfo = parseManifest(manifestFile);
            if (packInfo == null) {
                return counts;
            }

            String packName = generateRandomName();

            if (packInfo.isResourcePack && resourcePacksDirectory != null) {
                if (!resourcePacksDirectory.exists()) resourcePacksDirectory.mkdirs();
                File targetDir = new File(resourcePacksDirectory, packName);
                copyDirectory(packDir, targetDir);
                counts[0]++;
            }
            if (packInfo.isBehaviorPack && behaviorPacksDirectory != null) {
                if (!behaviorPacksDirectory.exists()) behaviorPacksDirectory.mkdirs();
                File targetDir = new File(behaviorPacksDirectory, packName);
                copyDirectory(packDir, targetDir);
                counts[1]++;
            }
            if (packInfo.isSkinPack && skinPacksDirectory != null) {
                if (!skinPacksDirectory.exists()) skinPacksDirectory.mkdirs();
                File targetDir = new File(skinPacksDirectory, packName);
                copyDirectory(packDir, targetDir);
                counts[2]++;
            }
        } finally {
            deleteFile(tempDir);
        }

        return counts;
    }

    private int[] importMcaddon(File zipFile) throws IOException {
        int[] counts = new int[3];
        
        File tempDir = new File(context.getCacheDir(), "temp_addon_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZipFile(zipFile, tempDir);

            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".mcpack")) {
                        int[] packCounts = importMcpack(file);
                        counts[0] += packCounts[0];
                        counts[1] += packCounts[1];
                        counts[2] += packCounts[2];
                    }
                }
            }

            List<File> packDirs = findAllPackDirectories(tempDir);
            for (File packDir : packDirs) {
                File manifestFile = new File(packDir, "manifest.json");
                PackInfo packInfo = parseManifest(manifestFile);
                if (packInfo == null) continue;

                String packName = generateRandomName();

                if (packInfo.isResourcePack && resourcePacksDirectory != null) {
                    if (!resourcePacksDirectory.exists()) resourcePacksDirectory.mkdirs();
                    File targetDir = new File(resourcePacksDirectory, packName);
                    copyDirectory(packDir, targetDir);
                    counts[0]++;
                }
                if (packInfo.isBehaviorPack && behaviorPacksDirectory != null) {
                    if (!behaviorPacksDirectory.exists()) behaviorPacksDirectory.mkdirs();
                    File targetDir = new File(behaviorPacksDirectory, packName);
                    copyDirectory(packDir, targetDir);
                    counts[1]++;
                }
                if (packInfo.isSkinPack && skinPacksDirectory != null) {
                    if (!skinPacksDirectory.exists()) skinPacksDirectory.mkdirs();
                    File targetDir = new File(skinPacksDirectory, packName);
                    copyDirectory(packDir, targetDir);
                    counts[2]++;
                }
            }
        } finally {
            deleteFile(tempDir);
        }

        return counts;
    }

    private PackInfo parseManifest(File manifestFile) {
        try {
            byte[] data = new byte[(int) manifestFile.length()];
            try (FileInputStream fis = new FileInputStream(manifestFile)) {
                fis.read(data);
            }
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            jsonStr = removeJsonComments(jsonStr);
            JSONObject manifest = new JSONObject(jsonStr);

            PackInfo info = new PackInfo();

            if (manifest.has("modules")) {
                JSONArray modules = manifest.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    JSONObject module = modules.getJSONObject(i);
                    if (module.has("type")) {
                        String type = module.getString("type").toLowerCase();
                        switch (type) {
                            case "resources":
                                info.isResourcePack = true;
                                break;
                            case "data":
                            case "script":
                                info.isBehaviorPack = true;
                                break;
                            case "skin_pack":
                                info.isSkinPack = true;
                                break;
                        }
                    }
                }
            }

            return info;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse manifest", e);
            return null;
        }
    }

    private String removeJsonComments(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            char next = (i + 1 < json.length()) ? json.charAt(i + 1) : 0;

            if (inSingleLineComment) {
                if (c == '\n') {
                    inSingleLineComment = false;
                    result.append(c);
                }
                continue;
            }

            if (inMultiLineComment) {
                if (c == '*' && next == '/') {
                    inMultiLineComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                result.append(c);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                result.append(c);
                continue;
            }

            if (c == '/' && next == '/') {
                inSingleLineComment = true;
                i++;
                continue;
            }

            if (c == '/' && next == '*') {
                inMultiLineComment = true;
                i++;
                continue;
            }

            result.append(c);
        }

        return result.toString();
    }

    private List<File> findAllPackDirectories(File searchDir) {
        List<File> result = new ArrayList<>();
        findPackDirectoriesRecursive(searchDir, result, searchDir);
        return result;
    }

    private void findPackDirectoriesRecursive(File dir, List<File> result, File rootDir) {
        if (dir == null || !dir.isDirectory()) return;

        File manifest = new File(dir, "manifest.json");
        if (manifest.exists() && !dir.equals(rootDir)) {
            result.add(dir);
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findPackDirectoriesRecursive(file, result, rootDir);
                }
            }
        }
    }

    private String generateRandomName() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get filename from content URI", e);
            }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    result = path.substring(lastSlash + 1);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        Log.d(TAG, "getFileName: uri=" + uri + ", result=" + result);
        return result;
    }

    private void copyStreamToFile(InputStream input, File output) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = input.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
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

    private void extractZipFile(File zipFile, File targetDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis)) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = normalizeZipEntryName(entry.getName());
                File entryFile = new File(targetDir, entryName);

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
    }

    private String normalizeZipEntryName(String name) {
        String n = name.trim();
        n = n.replace("\\", "/");
        if (n.startsWith("./")) n = n.substring(2);
        if (n.startsWith("/")) n = n.substring(1);
        while (n.contains("//")) {
            n = n.replace("//", "/");
        }
        return n;
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

    public void transferPack(ResourcePackItem pack, File targetDirectory, PackOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("ResourcePackManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                if (targetDirectory == null) {
                    callback.onError("Target directory not available");
                    return;
                }

                if (!targetDirectory.exists()) {
                    targetDirectory.mkdirs();
                }

                File sourceDir = pack.getFile();
                if (sourceDir == null || !sourceDir.exists()) {
                    callback.onError("Source pack not found");
                    return;
                }

                if (sourceDir.getParentFile().equals(targetDirectory)) {
                    callback.onError("Content is already in this location");
                    return;
                }

                String packName = generateRandomName();
                File targetDir = new File(targetDirectory, packName);

                copyDirectory(sourceDir, targetDir);
                deleteFile(sourceDir);

                callback.onSuccess("Pack transferred successfully");

            } catch (Exception e) {
                Log.e(TAG, "Failed to transfer pack", e);
                callback.onError("Transfer failed: " + e.getMessage());
            }
        });
    }
}