package org.levimc.launcher.core.content;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentImporter {
    private static final String TAG = "ContentImporter";
    private static final int BUFFER_SIZE = 8192;

    private final Context context;
    private final ExecutorService executor;

    public interface ImportCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int progress);
    }

    public static class ImportResult {
        public int resourcePacksImported = 0;
        public int behaviorPacksImported = 0;
        public int skinPacksImported = 0;
        public int worldsImported = 0;
    }

    public ContentImporter(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void importContent(Uri uri, File resourcePacksDir, File behaviorPacksDir, 
                              File skinPacksDir, File worldsDir, ImportCallback callback) {
        executor.execute(() -> {
            try {
                String fileName = getFileName(uri);
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "content.mcpack";
                }
                String lowerName = fileName.toLowerCase();
                Log.d(TAG, "Importing file: " + fileName);

                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    callback.onError("Cannot open file");
                    return;
                }

                File tempFile = new File(context.getCacheDir(), "temp_import_" + System.currentTimeMillis());
                copyStreamToFile(inputStream, tempFile);
                inputStream.close();

                ImportResult result = new ImportResult();

                if (lowerName.endsWith(".mcworld")) {
                    importMcworld(tempFile, worldsDir, result);
                } else if (lowerName.endsWith(".mcaddon")) {
                    importMcaddon(tempFile, resourcePacksDir, behaviorPacksDir, skinPacksDir, result);
                } else if (lowerName.endsWith(".mcpack")) {
                    importMcpack(tempFile, resourcePacksDir, behaviorPacksDir, skinPacksDir, result);
                } else {
                    importMcpack(tempFile, resourcePacksDir, behaviorPacksDir, skinPacksDir, result);
                    if (result.resourcePacksImported == 0 && result.behaviorPacksImported == 0 && result.skinPacksImported == 0) {
                        importMcaddon(tempFile, resourcePacksDir, behaviorPacksDir, skinPacksDir, result);
                    }
                    if (result.resourcePacksImported == 0 && result.behaviorPacksImported == 0 && 
                        result.skinPacksImported == 0 && result.worldsImported == 0) {
                        importMcworld(tempFile, worldsDir, result);
                    }
                }

                tempFile.delete();

                StringBuilder message = new StringBuilder();
                if (result.worldsImported > 0) {
                    message.append("Worlds: ").append(result.worldsImported).append(" ");
                }
                if (result.resourcePacksImported > 0) {
                    message.append("Resource Packs: ").append(result.resourcePacksImported).append(" ");
                }
                if (result.behaviorPacksImported > 0) {
                    message.append("Behavior Packs: ").append(result.behaviorPacksImported).append(" ");
                }
                if (result.skinPacksImported > 0) {
                    message.append("Skin Packs: ").append(result.skinPacksImported).append(" ");
                }

                if (message.length() == 0) {
                    callback.onError("No content was imported");
                } else {
                    callback.onSuccess("Imported: " + message.toString().trim());
                }

            } catch (Exception e) {
                Log.e(TAG, "Import failed", e);
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }

    private void importMcworld(File zipFile, File worldsDir, ImportResult result) throws IOException {
        if (worldsDir == null) return;
        if (!worldsDir.exists()) worldsDir.mkdirs();

        File tempDir = new File(context.getCacheDir(), "temp_world_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZip(zipFile, tempDir);
            File worldDir = findWorldDirectory(tempDir);
            if (worldDir == null) {
                return;
            }

            String worldName = generateRandomName();
            File targetDir = new File(worldsDir, worldName);
            copyDirectory(worldDir, targetDir);
            result.worldsImported++;
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void importMcpack(File zipFile, File resourcePacksDir, File behaviorPacksDir, 
                              File skinPacksDir, ImportResult result) throws IOException {
        File tempDir = new File(context.getCacheDir(), "temp_pack_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZip(zipFile, tempDir);
            File packDir = findPackDirectory(tempDir);
            if (packDir == null) {
                return;
            }

            File manifestFile = new File(packDir, "manifest.json");
            if (!manifestFile.exists()) {
                return;
            }

            PackInfo packInfo = parseManifest(manifestFile);
            if (packInfo == null) {
                return;
            }

            String packName = generateRandomName();

            if (packInfo.isResourcePack && resourcePacksDir != null) {
                if (!resourcePacksDir.exists()) resourcePacksDir.mkdirs();
                File targetDir = new File(resourcePacksDir, packName);
                copyDirectory(packDir, targetDir);
                result.resourcePacksImported++;
            }
            if (packInfo.isBehaviorPack && behaviorPacksDir != null) {
                if (!behaviorPacksDir.exists()) behaviorPacksDir.mkdirs();
                File targetDir = new File(behaviorPacksDir, packName);
                copyDirectory(packDir, targetDir);
                result.behaviorPacksImported++;
            }
            if (packInfo.isSkinPack && skinPacksDir != null) {
                if (!skinPacksDir.exists()) skinPacksDir.mkdirs();
                File targetDir = new File(skinPacksDir, packName);
                copyDirectory(packDir, targetDir);
                result.skinPacksImported++;
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void importMcaddon(File zipFile, File resourcePacksDir, File behaviorPacksDir, 
                               File skinPacksDir, ImportResult result) throws IOException {
        File tempDir = new File(context.getCacheDir(), "temp_addon_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            extractZip(zipFile, tempDir);

            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".mcpack")) {
                        importMcpack(file, resourcePacksDir, behaviorPacksDir, skinPacksDir, result);
                    }
                }
            }

            List<PackDirectory> packDirs = findAllPackDirectories(tempDir);
            for (PackDirectory packDirectory : packDirs) {
                PackInfo packInfo = parseManifest(new File(packDirectory.dir, "manifest.json"));
                if (packInfo == null) continue;

                String packName = generateRandomName();

                if (packInfo.isResourcePack && resourcePacksDir != null) {
                    if (!resourcePacksDir.exists()) resourcePacksDir.mkdirs();
                    File targetDir = new File(resourcePacksDir, packName);
                    copyDirectory(packDirectory.dir, targetDir);
                    result.resourcePacksImported++;
                }
                if (packInfo.isBehaviorPack && behaviorPacksDir != null) {
                    if (!behaviorPacksDir.exists()) behaviorPacksDir.mkdirs();
                    File targetDir = new File(behaviorPacksDir, packName);
                    copyDirectory(packDirectory.dir, targetDir);
                    result.behaviorPacksImported++;
                }
                if (packInfo.isSkinPack && skinPacksDir != null) {
                    if (!skinPacksDir.exists()) skinPacksDir.mkdirs();
                    File targetDir = new File(skinPacksDir, packName);
                    copyDirectory(packDirectory.dir, targetDir);
                    result.skinPacksImported++;
                }
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private static class PackInfo {
        boolean isResourcePack = false;
        boolean isBehaviorPack = false;
        boolean isSkinPack = false;
    }

    private static class PackDirectory {
        File dir;
        PackDirectory(File dir) {
            this.dir = dir;
        }
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

    private List<PackDirectory> findAllPackDirectories(File searchDir) {
        List<PackDirectory> result = new ArrayList<>();
        findPackDirectoriesRecursive(searchDir, result, searchDir);
        return result;
    }

    private void findPackDirectoriesRecursive(File dir, List<PackDirectory> result, File rootDir) {
        if (dir == null || !dir.isDirectory()) return;

        File manifest = new File(dir, "manifest.json");
        if (manifest.exists() && !dir.equals(rootDir)) {
            result.add(new PackDirectory(dir));
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

    private File findWorldDirectory(File searchDir) {
        File levelDat = new File(searchDir, "level.dat");
        if (levelDat.exists()) {
            return searchDir;
        }

        File[] files = searchDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findWorldDirectory(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
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
                    File found = findPackDirectory(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private String generateRandomName() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void extractZip(File zipFile, File targetDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
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

    private void copyStreamToFile(InputStream input, File output) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = input.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
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

    private boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
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

    public void shutdown() {
        executor.shutdown();
    }
}
