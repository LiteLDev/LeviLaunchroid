package org.levimc.launcher.core.content;

import android.content.Context;
import android.net.Uri;

import org.levimc.launcher.core.content.leveldb.WorldDatabase;
import org.levimc.launcher.core.content.nbt.NativeNbt;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StructureExtractor {
    public interface ExtractionCallback {
        void onComplete(int extractedCount, String outputPath);

        void onError(String error);
    }

    public interface StructureListCallback {
        void onComplete(List<StructureInfo> structures);

        void onError(String error);
    }

    public static final class StructureInfo {
        private final String id, name;
        private final byte[] data;

        public StructureInfo(String id, byte[] data) {
            this.id = id;
            this.name = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            this.data = data;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }

        public int getSize() {
            return data == null ? 0 : data.length;
        }

        public String getFormattedSize() {
            int size = getSize();
            return size < 1024
                    ? size + " B"
                    : size < 1048576
                            ? String.format(Locale.ROOT, "%.1f KB", size / 1024.0)
                            : String.format(Locale.ROOT, "%.1f MB", size / 1048576.0);
        }

        public String getFileName() {
            return name.replaceAll("[^a-zA-Z0-9._-]", "_") + ".mcstructure";
        }
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public StructureExtractor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void loadStructures(File world, StructureListCallback callback) {
        executor.execute(
                () -> {
                    try (WorldDatabase db = new WorldDatabase(world)) {
                        List<StructureInfo> result = new ArrayList<>();
                        byte[] after = null;
                        long size = 0;
                        byte[] prefix = "structuretemplate_".getBytes(StandardCharsets.UTF_8);
                        do {
                            if (Thread.currentThread().isInterrupted())
                                throw new java.io.InterruptedIOException();
                            WorldDatabase.Page page = db.scan(prefix, after, 128, "");
                            for (byte[] key : page.keys) {
                                byte[] data = db.get(key);
                                if (data == null) continue;
                                size += data.length;
                                if (size > 64L * 1024 * 1024)
                                    throw new IOException(
                                            "Structure selection exceeds 64 MiB; use the world"
                                                    + " workspace to export individual structures");
                                NativeNbt.decode(data, 0, NativeNbt.LITTLE_ENDIAN);
                                result.add(
                                        new StructureInfo(
                                                new String(
                                                        key,
                                                        prefix.length,
                                                        key.length - prefix.length,
                                                        StandardCharsets.UTF_8),
                                                data));
                            }
                            after = page.next;
                        } while (after != null);
                        callback.onComplete(result);
                    } catch (Exception | LinkageError ex) {
                        callback.onError(ex.getMessage());
                    }
                });
    }

    public void exportSingleStructure(
            StructureInfo structure, Uri output, ExtractionCallback callback) {
        executor.execute(
                () -> {
                    try (OutputStream stream =
                            context.getContentResolver().openOutputStream(output)) {
                        if (stream == null) throw new IOException("Cannot create output file");
                        NativeNbt.decode(structure.getData(), 0, NativeNbt.LITTLE_ENDIAN);
                        stream.write(structure.getData());
                        stream.flush();
                        callback.onComplete(1, output.toString());
                    } catch (Exception | LinkageError ex) {
                        callback.onError(ex.getMessage());
                    }
                });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
