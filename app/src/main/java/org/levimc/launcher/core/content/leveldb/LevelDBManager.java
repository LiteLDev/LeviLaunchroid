package org.levimc.launcher.core.content.leveldb;

import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.content.world.WorldSession;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LevelDBManager {
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<LevelDBEntry> entries = new ArrayList<>();

    public LevelDBManager(File world) {
        dbDir = new File(world, "db");
    }

    public void loadDatabase(LoadCallback callback) {
        executor.execute(
                () -> {
                    try (LevelDBReader reader = new LevelDBReader(dbDir)) {
                        entries =
                                reader.readEntries(
                                        "structuretemplate_".getBytes(StandardCharsets.UTF_8),
                                        64L * 1024 * 1024);
                        callback.onProgress(100, 100);
                        callback.onComplete(new ArrayList<>(entries));
                    } catch (Exception | LinkageError ex) {
                        callback.onError(ex.getMessage());
                    }
                });
    }

    public List<LevelDBEntry> getStructureEntries() {
        return new ArrayList<>(entries);
    }

    public void exportAllStructures(File output, StructureExportCallback callback) {
        executor.execute(
                () -> {
                    try {
                        int done = 0;
                        for (LevelDBEntry entry : entries) {
                            if (Thread.currentThread().isInterrupted())
                                throw new java.io.InterruptedIOException();
                            String id = entry.getKey().getStructureId();
                            String[] parts = id.split(":", 2);
                            String namespace = parts.length == 2 ? parts[0] : "mystructure",
                                    name = parts.length == 2 ? parts[1] : parts[0];
                            File folder = new File(output, "structures/" + safe(namespace));
                            if (!folder.isDirectory() && !folder.mkdirs())
                                throw new java.io.IOException("Cannot create export folder");
                            byte[] value = entry.getValue();
                            NativeNbt.decode(value, 0, NativeNbt.LITTLE_ENDIAN);
                            WorldSession.atomic(
                                    new File(folder, safe(name) + ".mcstructure"), value);
                            callback.onProgress(++done, entries.size(), id);
                        }
                        callback.onComplete(done, new File(output, "structures").getAbsolutePath());
                    } catch (Exception | LinkageError ex) {
                        callback.onError(ex.getMessage());
                    }
                });
    }

    private static String safe(String value) {
        String name = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.isBlank() || name.equals(".") || name.equals("..") ? "structure" : name;
    }

    public void shutdown() {
        executor.shutdownNow();
        entries = new ArrayList<>();
    }
}
