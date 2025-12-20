package org.levimc.launcher.core.content.leveldb;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LevelDBManager {
    private static final String TAG = "LevelDBManager";

    public interface LoadCallback {
        void onProgress(int current, int total);
        void onComplete(List<LevelDBEntry> entries);
        void onError(String error);
    }

    public interface StructureExportCallback {
        void onProgress(int current, int total, String structureName);
        void onComplete(int exportedCount, String outputPath);
        void onError(String error);
    }

    private final File dbDir;
    private final ExecutorService executor;
    private List<LevelDBEntry> entries;
    private List<LevelDBEntry> structureEntries;

    public LevelDBManager(File worldDir) {
        this.dbDir = new File(worldDir, "db");
        this.executor = Executors.newSingleThreadExecutor();
        this.entries = new ArrayList<>();
        this.structureEntries = new ArrayList<>();
    }

    public void loadDatabase(LoadCallback callback) {
        executor.execute(() -> {
            try {
                if (!dbDir.exists() || !dbDir.isDirectory()) {
                    callback.onError("Database directory not found: " + dbDir.getAbsolutePath());
                    return;
                }

                LevelDBReader reader = new LevelDBReader(dbDir);
                entries = reader.readAllEntries();
                reader.close();

                Log.d(TAG, "Loaded " + entries.size() + " entries from database");

                callback.onProgress(50, 100);

                categorizeEntries();

                Log.d(TAG, "Found " + structureEntries.size() + " structures");

                callback.onProgress(100, 100);
                callback.onComplete(entries);

            } catch (Exception e) {
                Log.e(TAG, "Failed to load database", e);
                callback.onError("Failed to load database: " + e.getMessage());
            }
        });
    }

    private void categorizeEntries() {
        structureEntries.clear();

        for (LevelDBEntry entry : entries) {
            LevelDBKey key = entry.getKey();
            if (key.isStructureKey()) {
                structureEntries.add(entry);
                String structureId = key.getStructureId();
                byte[] value = entry.getValue();
                Log.d(TAG, "Found structure: " + structureId +
                      " (value size: " + (value != null ? value.length : 0) + " bytes)");
            }
        }

        Log.d(TAG, "Categorization complete: " + structureEntries.size() + " structures found");
    }

    public List<LevelDBEntry> getStructureEntries() {
        return structureEntries;
    }

    public void exportAllStructures(File outputDir, StructureExportCallback callback) {
        executor.execute(() -> {
            try {
                if (structureEntries.isEmpty()) {
                    callback.onError("No structures found in this world");
                    return;
                }

                int total = structureEntries.size();
                int exported = 0;

                Log.d(TAG, "Starting export of " + total + " structures to " + outputDir.getAbsolutePath());

                for (int i = 0; i < structureEntries.size(); i++) {
                    LevelDBEntry entry = structureEntries.get(i);
                    String structureId = entry.getKey().getStructureId();
                    if (structureId == null || structureId.isEmpty()) {
                        Log.w(TAG, "Skipping structure with null/empty ID");
                        continue;
                    }

                    callback.onProgress(i, total, structureId);

                    String namespace = "mystructure";
                    String name = structureId;

                    int colonIndex = structureId.indexOf(':');
                    if (colonIndex > 0) {
                        namespace = structureId.substring(0, colonIndex);
                        name = structureId.substring(colonIndex + 1);
                    }

                    File structuresDir = new File(outputDir, "structures/" + namespace);
                    if (!structuresDir.exists()) {
                        boolean created = structuresDir.mkdirs();
                        Log.d(TAG, "Created directory " + structuresDir.getAbsolutePath() + ": " + created);
                    }

                    String fileName = sanitizeFileName(name) + ".mcstructure";
                    File outputFile = new File(structuresDir, fileName);

                    byte[] value = entry.getValue();
                    if (value != null && value.length > 0) {
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            fos.write(value);
                            fos.flush();
                        }
                        Log.d(TAG, "Exported structure: " + structureId + " -> " + outputFile.getAbsolutePath() +
                              " (" + value.length + " bytes)");
                        exported++;
                    } else {
                        Log.w(TAG, "Structure " + structureId + " has no data (value is null or empty)");
                    }
                }

                callback.onProgress(total, total, "Complete");
                String outputPath = new File(outputDir, "structures").getAbsolutePath();
                Log.d(TAG, "Export complete: " + exported + "/" + total + " structures exported to " + outputPath);
                callback.onComplete(exported, outputPath);

            } catch (Exception e) {
                Log.e(TAG, "Failed to export structures", e);
                callback.onError("Export failed: " + e.getMessage());
            }
        });
    }

    private String sanitizeFileName(String name) {
        return name.replace("/", "_")
                   .replace("\\", "_")
                   .replace(":", "_")
                   .replace("*", "_")
                   .replace("?", "_")
                   .replace("\"", "_")
                   .replace("<", "_")
                   .replace(">", "_")
                   .replace("|", "_");
    }

    public void shutdown() {
        executor.shutdown();
    }
}
