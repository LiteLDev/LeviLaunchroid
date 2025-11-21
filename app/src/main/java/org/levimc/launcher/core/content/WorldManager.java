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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WorldManager {
    private static final String TAG = "WorldManager";
    private static final int BUFFER_SIZE = 8192;
    
    private final Context context;
    private final ExecutorService executor;
    private File worldsDirectory;
    
    public interface WorldOperationCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int progress);
    }

    public WorldManager(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setCurrentVersion(GameVersion version) {
        if (version != null && version.versionDir != null) {
            this.worldsDirectory = new File(version.versionDir, "games/com.mojang/minecraftWorlds");
            if (!worldsDirectory.exists()) {
                worldsDirectory.mkdirs();
            }
        } else {
            this.worldsDirectory = null;
        }
    }

    public void setWorldsDirectory(File directory) {
        this.worldsDirectory = directory;
        if (worldsDirectory != null && !worldsDirectory.exists()) {
            worldsDirectory.mkdirs();
        }
    }

    public List<WorldItem> getWorlds() {
        List<WorldItem> worlds = new ArrayList<>();
        
        if (worldsDirectory == null || !worldsDirectory.exists()) {
            return worlds;
        }

        File[] worldDirs = worldsDirectory.listFiles(File::isDirectory);
        if (worldDirs != null) {
            for (File worldDir : worldDirs) {
                WorldItem world = new WorldItem(worldDir.getName(), worldDir);
                if (world.isValid()) {
                    worlds.add(world);
                }
            }
        }

        return worlds;
    }

    public void importWorld(Uri worldUri, WorldOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("WorldManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                if (worldsDirectory == null) {
                    callback.onError("No version selected");
                    return;
                }

                InputStream inputStream = context.getContentResolver().openInputStream(worldUri);
                if (inputStream == null) {
                    callback.onError("Cannot open world file");
                    return;
                }

                String tempDirName = "temp_world_" + System.currentTimeMillis();
                File tempDir = new File(context.getCacheDir(), tempDirName);
                tempDir.mkdirs();

                try {
                    extractZip(inputStream, tempDir, callback);

                    File worldDir = findWorldDirectory(tempDir);
                    if (worldDir == null) {
                        callback.onError("Invalid world file - no world data found");
                        return;
                    }

                    String worldName = generateUniqueWorldName(worldDir.getName());
                    File targetDir = new File(worldsDirectory, worldName);

                    copyDirectory(worldDir, targetDir);
                    
                    callback.onSuccess("World imported successfully");
                    
                } finally {
                    deleteDirectory(tempDir);
                    inputStream.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to import world", e);
                callback.onError("Import failed: " + e.getMessage());
            }
        });
    }

    public void exportWorld(WorldItem world, Uri exportUri, WorldOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("WorldManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(exportUri);
                if (outputStream == null) {
                    callback.onError("Cannot create export file");
                    return;
                }

                try {
                    createWorldZip(world.getFile(), outputStream, callback);
                    callback.onSuccess("World exported successfully");
                } finally {
                    outputStream.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to export world", e);
                callback.onError("Export failed: " + e.getMessage());
            }
        });
    }

    public void deleteWorld(WorldItem world, WorldOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("WorldManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                createBackup(world);
                
                if (deleteDirectory(world.getFile())) {
                    callback.onSuccess("World deleted successfully");
                } else {
                    callback.onError("Failed to delete world");
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to delete world", e);
                callback.onError("Delete failed: " + e.getMessage());
            }
        });
    }

    public void backupWorld(WorldItem world, WorldOperationCallback callback) {
        if (executor.isShutdown()) {
            callback.onError("WorldManager has been shut down");
            return;
        }
        executor.execute(() -> {
            try {
                String backupPath = createBackup(world);
                callback.onSuccess("Backup created: " + backupPath);

            } catch (Exception e) {
                Log.e(TAG, "Failed to backup world", e);
                callback.onError("Backup failed: " + e.getMessage());
            }
        });
    }

    private void extractZip(InputStream inputStream, File targetDir, WorldOperationCallback callback) throws IOException {
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry entry;
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

    private File findWorldDirectory(File searchDir) {
        File levelDat = new File(searchDir, "level.dat");
        if (levelDat.exists()) {
            return searchDir;
        }
        
        File[] files = searchDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File levelDatInSubdir = new File(file, "level.dat");
                    if (levelDatInSubdir.exists()) {
                        return file;
                    }
                    File found = findWorldDirectory(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private String generateUniqueWorldName(String baseName) {
        String worldName = baseName;
        int counter = 1;
        
        while (new File(worldsDirectory, worldName).exists()) {
            worldName = baseName + "_" + counter;
            counter++;
        }
        
        return worldName;
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

    private void createWorldZip(File worldDir, OutputStream outputStream, WorldOperationCallback callback) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        zipDirectory(worldDir, worldDir.getName(), zos);
        zos.close();
    }

    private void zipDirectory(File dir, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String entryPath = basePath + "/" + file.getName();
                if (file.isDirectory()) {
                    zipDirectory(file, entryPath, zos);
                } else {
                    ZipEntry entry = new ZipEntry(entryPath);
                    zos.putNextEntry(entry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    private String createBackup(WorldItem world) throws IOException {
        File backupDir = new File(context.getExternalFilesDir("backups"), "worlds");
        backupDir.mkdirs();
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String backupName = world.getName() + "_" + timestamp + ".mcworld";
        File backupFile = new File(backupDir, backupName);
        
        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
            createWorldZip(world.getFile(), fos, null);
        }
        
        return backupFile.getAbsolutePath();
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

    public void shutdown() {
        executor.shutdown();
    }
}