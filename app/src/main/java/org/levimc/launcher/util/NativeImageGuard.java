package org.levimc.launcher.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeImageGuard {
    public static final String TOKEN = "img_v4";
    private static final ConcurrentHashMap<String, FileState> CLEAN_FILES = new ConcurrentHashMap<>();

    private NativeImageGuard() {
    }

    public static boolean shouldProcess(File soFile) {
        if (soFile == null || !soFile.isFile() || soFile.length() == 0) {
            return false;
        }

        String path = soFile.getAbsolutePath();
        FileState state = FileState.from(soFile);
        FileState cached = CLEAN_FILES.get(path);
        if (state.equals(cached) || state.equals(readCleanMarker(soFile))) {
            CLEAN_FILES.put(path, state);
            return false;
        }

        return scan(soFile);
    }

    public static boolean processIfNeeded(File soFile) {
        return process(soFile, false);
    }

    public static boolean processRequired(File soFile) {
        return process(soFile, true);
    }

    private static boolean process(File soFile, boolean ignoreMarker) {
        if (soFile == null || !soFile.isFile() || soFile.length() == 0) {
            return false;
        }
        if (!NativeBridgeHelper.ensureGxCoreLoaded()) {
            return false;
        }
        if (ignoreMarker) {
            CLEAN_FILES.remove(soFile.getAbsolutePath());
            clearCleanMarker(soFile);
        } else if (!shouldProcess(soFile)) {
            return true;
        }

        if (!scan(soFile)) {
            markClean(soFile);
            return true;
        }
        try {
            ensureWritable(soFile);
            File tempFile = new File(soFile.getAbsolutePath() + ".img.tmp");
            if (tempFile.exists() && !tempFile.delete()) {
                throw new IOException("Failed to delete stale temp file: " + tempFile.getAbsolutePath());
            }

            if (!NativeBridgeHelper.rewriteImage(inputPath(soFile.getAbsolutePath()), tempFile.getAbsolutePath())) {
                tempFile.delete();
                return false;
            }

            Files.move(tempFile.toPath(), soFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            soFile.setReadable(true, true);
            soFile.setReadOnly();
            markClean(soFile);
            return true;
        } catch (Exception e) {
            CLEAN_FILES.remove(soFile.getAbsolutePath());
            clearCleanMarker(soFile);
            return false;
        }
    }

    public static int processDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }

        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                count += processDirectory(file);
            } else if (file.getName().endsWith(".so") && processIfNeeded(file)) {
                count++;
            }
        }
        return count;
    }

    private static void ensureWritable(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
        }
        if (file.exists() && !file.setWritable(true, true) && !file.canWrite()) {
            throw new IOException("Failed to make file writable: " + file.getAbsolutePath());
        }
    }

    private static boolean scan(File soFile) {
        if (!NativeBridgeHelper.ensureGxCoreLoaded()) {
            return false;
        }
        return NativeBridgeHelper.scanImage(inputPath(soFile.getAbsolutePath()));
    }

    private static File cleanMarker(File file) {
        File parent = file.getParentFile();
        String name = "." + file.getName() + "." + TOKEN + ".ok";
        return parent == null ? new File(name) : new File(parent, name);
    }

    private static FileState readCleanMarker(File file) {
        File marker = cleanMarker(file);
        if (!marker.isFile()) {
            return null;
        }
        try {
            String[] parts = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim().split(":");
            if (parts.length != 3 || !TOKEN.equals(parts[0])) {
                return null;
            }
            return new FileState(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void markClean(File file) {
        FileState state = FileState.from(file);
        CLEAN_FILES.put(file.getAbsolutePath(), state);
        try {
            File marker = cleanMarker(file);
            File parent = marker.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            String data = TOKEN + ":" + state.length + ":" + state.lastModified;
            Files.write(marker.toPath(), data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void clearCleanMarker(File file) {
        try {
            File marker = cleanMarker(file);
            if (marker.exists()) {
                marker.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static String inputPath(String path) {
        return path;
    }

    private static final class FileState {
        private final long length;
        private final long lastModified;

        private FileState(long length, long lastModified) {
            this.length = length;
            this.lastModified = lastModified;
        }

        static FileState from(File file) {
            return new FileState(file.length(), file.lastModified());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FileState)) {
                return false;
            }
            FileState other = (FileState) obj;
            return length == other.length && lastModified == other.lastModified;
        }

        @Override
        public int hashCode() {
            long value = length ^ (length >>> 32) ^ lastModified ^ (lastModified >>> 32);
            return (int) value;
        }
    }
}
