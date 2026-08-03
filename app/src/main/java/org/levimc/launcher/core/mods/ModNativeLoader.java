package org.levimc.launcher.core.mods;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModNativeLoader {
    private static final String TAG = "ModNativeLoader";
    private static final int CACHE_MANIFEST_VERSION = 4;
    private static final String DATA_DIRECTORY_NAME = "data";
    private static final String CONFIG_DIRECTORY_NAME = "config";

    private static final class CachedModEntry {
        final File sourceDirectory;
        final File targetFile;

        CachedModEntry(File sourceDirectory, File targetFile) {
            this.sourceDirectory = sourceDirectory;
            this.targetFile = targetFile;
        }
    }

    public static void loadEnabledSoMods(ModManager modManager, File cacheDir) {
        loadEnabledSoMods(modManager, cacheDir, null);
    }

    public interface LoadListener {
        void onScanStarted(int totalEnabled);
        void onModLoadStarted(Mod mod, int index, int total);
        void onModLoadFinished(Mod mod);
        void onModLoadSkipped(Mod mod, String minecraftVersion);
        void onModLoadFailed(Mod mod, Throwable error);
        void onMessage(String message);
    }

    public static void loadEnabledSoMods(ModManager modManager, File cacheDir, LoadListener listener) {
        if (modManager.getCurrentVersion() == null || modManager.getCurrentVersion().modsDir == null) {
            notifyMessage(listener, "Native mod directory is not configured");
            return;
        }
        if (cacheDir == null) {
            notifyMessage(listener, "Native mod working directory is not configured");
            return;
        }

        List<Mod> mods = modManager.getMods();
        String minecraftVersion = modManager.getCurrentVersion().versionCode;
        int totalEnabled = 0;
        for (Mod mod : mods) {
            if (mod.isEnabled()) {
                totalEnabled++;
            }
        }
        if (listener != null) {
            listener.onScanStarted(totalEnabled);
        }

        File cacheModsDir = new File(cacheDir, "mods");
        if (!cacheModsDir.exists() && !cacheModsDir.mkdirs()) {
            Log.e(TAG, "Failed to prepare native mod working directory: " + cacheModsDir.getAbsolutePath());
            notifyMessage(listener, "Failed to prepare native mod working directory: " + cacheModsDir.getAbsolutePath());
            return;
        }

        Set<String> installedModIds = new HashSet<>();
        for (Mod mod : mods) installedModIds.add(mod.getId());
        int currentIndex = 0;
        for (Mod mod : mods) {
            if (!mod.isEnabled()) {
                continue;
            }
            currentIndex++;
            if (listener != null) {
                listener.onModLoadStarted(mod, currentIndex, totalEnabled);
            }

            if (!isCompatibleWithMinecraftVersion(mod, minecraftVersion)) {
                Log.w(TAG, "Skipping incompatible mod " + mod.getDisplayName()
                        + " for Minecraft " + minecraftVersion);
                notifySkipped(listener, mod, minecraftVersion);
                continue;
            }

            try {
                CachedModEntry cachedEntry = prepareCachedEntry(modManager, cacheModsDir, mod);
                File targetFile = cachedEntry.targetFile;
                if (targetFile == null || !targetFile.isFile()) {
                    IOException error = new IOException("Entry not found after copy: " + (targetFile == null ? "<null>" : targetFile.getAbsolutePath()));
                    Log.e(TAG, error.getMessage());
                    notifyFailure(listener, mod, error);
                    continue;
                }

                ensureReadOnly(targetFile);
                System.load(targetFile.getAbsolutePath());

                if (ModManager.ensurePreloaderLoaded()) {
                    if (!ModManager.initializeLoadedMod(
                            targetFile.getAbsolutePath(),
                            cachedEntry.sourceDirectory.getAbsolutePath(),
                            mod)) {
                        Log.e(TAG, "Failed to finish native initialization for " + mod.getDisplayName());
                        notifyFailure(listener, mod, new UnsatisfiedLinkError("Failed to finish native initialization"));
                        continue;
                    }
                }
                if (listener != null) {
                    listener.onModLoadFinished(mod);
                }
            } catch (IOException | UnsatisfiedLinkError e) {
                Log.e(TAG, "Can't load " + mod.getDisplayName() + ": " + e.getMessage(), e);
                notifyFailure(listener, mod, e);
            }
        }

        pruneStaleCachedMods(cacheModsDir, installedModIds);
    }

    public static boolean isCompatibleWithMinecraftVersion(Mod mod, String minecraftVersion) {
        if (mod == null) {
            return true;
        }
        return isCompatibleWithMinecraftVersion(mod.getMinecraftVersions(), minecraftVersion);
    }

    public static boolean isCompatibleWithMinecraftVersion(List<String> patterns, String minecraftVersion) {
        if (patterns == null || patterns.isEmpty()) {
            return true;
        }

        boolean hasValidPattern = false;
        for (String pattern : patterns) {
            if (pattern == null) {
                continue;
            }

            String normalizedPattern = pattern.trim();
            if (normalizedPattern.isEmpty()) {
                continue;
            }

            hasValidPattern = true;
            if (matchesMinecraftVersionPattern(normalizedPattern, minecraftVersion)) {
                return true;
            }
        }
        return !hasValidPattern;
    }

    public static boolean matchesMinecraftVersionPattern(String pattern, String minecraftVersion) {
        if (pattern == null || minecraftVersion == null) {
            return false;
        }

        String normalizedPattern = pattern.trim();
        String normalizedVersion = minecraftVersion.trim();
        if (normalizedPattern.isEmpty() || normalizedVersion.isEmpty()) {
            return false;
        }

        int wildcardIndex = normalizedPattern.indexOf('*');
        if (wildcardIndex < 0) {
            return normalizedVersion.equals(normalizedPattern);
        }

        String prefix = normalizedPattern.substring(0, wildcardIndex);
        return normalizedVersion.startsWith(prefix);
    }

    private static void notifySkipped(LoadListener listener, Mod mod, String minecraftVersion) {
        if (listener != null) {
            listener.onModLoadSkipped(mod, minecraftVersion);
        }
    }

    private static void notifyFailure(LoadListener listener, Mod mod, Throwable error) {
        if (listener != null) {
            listener.onModLoadFailed(mod, error);
        }
    }

    private static void notifyMessage(LoadListener listener, String message) {
        if (listener != null) {
            listener.onMessage(message);
        }
    }

    private static CachedModEntry prepareCachedEntry(ModManager modManager, File cacheModsDir, Mod mod) throws IOException {
        File sourceDirectory = new File(modManager.getCurrentVersion().modsDir, mod.getId());
        if (!sourceDirectory.isDirectory()) {
            throw new IOException("Mod package directory does not exist: " + sourceDirectory.getAbsolutePath());
        }

        File targetDirectory = new File(cacheModsDir, mod.getId());
        String sourceFingerprint = buildManifest(sourceDirectory, mod);
        File manifestFile = new File(targetDirectory, ".mod_cache_manifest");
        if (targetDirectory.isDirectory() && manifestFile.isFile()) {
            String cached = readFile(manifestFile);
            File targetFile = new File(targetDirectory, mod.getEntryPath());
            if (sourceFingerprint.equals(cached) && targetFile.isFile() && targetFile.length() > 0) {
                ensureReadOnly(targetFile);
                return new CachedModEntry(sourceDirectory, targetFile);
            }
        }

        File tempDirectory = new File(cacheModsDir, mod.getId() + ".tmp");
        if (tempDirectory.exists() && !deleteRecursively(tempDirectory)) {
            throw new IOException("Failed to clear temporary mod directory: " + tempDirectory.getAbsolutePath());
        }
        copyPackageDirectory(sourceDirectory, tempDirectory);
        writeManifest(new File(tempDirectory, ".mod_cache_manifest"), sourceFingerprint);

        if (targetDirectory.exists() && !deleteRecursively(targetDirectory)) {
            deleteRecursively(tempDirectory);
            throw new IOException("Failed to prepare native mod directory: " + targetDirectory.getAbsolutePath());
        }
        if (!tempDirectory.renameTo(targetDirectory)) {
            deleteRecursively(tempDirectory);
            throw new IOException("Failed to prepare native mod directory: " + targetDirectory.getAbsolutePath());
        }

        File targetFile = new File(targetDirectory, mod.getEntryPath());
        ensureReadOnly(targetFile);
        return new CachedModEntry(sourceDirectory, targetFile);
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void writeManifest(File file, String data) throws IOException {
        ensureParentDirectory(file);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(data);
        }
    }

    private static String buildManifest(File sourceDirectory, Mod mod) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("loader=").append(CACHE_MANIFEST_VERSION).append('\n');
        sb.append("id=").append(mod.getId()).append('\n');
        sb.append("entry=").append(mod.getEntryPath()).append('\n');

        List<File> files = new ArrayList<>();
        collectFiles(sourceDirectory, files);
        files.sort(Comparator.comparing(file -> relativePath(sourceDirectory, file)));
        for (File file : files) {
            sb.append("file=")
                    .append(relativePath(sourceDirectory, file))
                    .append('|')
                    .append(file.length())
                    .append('|')
                    .append(file.lastModified())
                    .append('\n');
        }
        return sb.toString();
    }

    private static void collectFiles(File root, List<File> files) throws IOException {
        File[] children = root.listFiles();
        if (children == null) {
            throw new IOException("Failed to list mod directory: " + root.getAbsolutePath());
        }

        for (File child : children) {
            if (isRuntimeDirectory(root, child)) {
                continue;
            }
            if (child.isDirectory()) {
                collectFiles(child, files);
            } else if (child.isFile()) {
                files.add(child);
            }
        }
    }

    private static String relativePath(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        String relative = filePath.startsWith(rootPath)
                ? filePath.substring(rootPath.length())
                : file.getName();
        while (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }
        return relative.replace(File.separatorChar, '/');
    }

    private static void copyFile(File src, File dst) throws IOException {
        ensureParentDirectory(dst);
        if (dst.exists() && !dst.delete()) {
            throw new IOException("Failed to replace existing file: " + dst.getAbsolutePath());
        }

        try (InputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[262144];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        ensureReadOnly(dst);
    }

    private static void copyPackageDirectory(File src, File dst) throws IOException {
        copyDirectory(src, dst, src);
    }

    private static void copyDirectory(File src, File dst, File root) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) {
                throw new IOException("Failed to create directory: " + dst.getAbsolutePath());
            }

            File[] children = src.listFiles();
            if (children == null) {
                return;
            }

            for (File child : children) {
                if (isRuntimeDirectory(root, child)) {
                    continue;
                }
                copyDirectory(child, new File(dst, child.getName()), root);
            }
            return;
        }

        copyFile(src, dst);
    }

    private static boolean isRuntimeDirectory(File root, File file) {
        if (!file.isDirectory()) {
            return false;
        }

        File parent = file.getParentFile();
        if (parent == null || !parent.equals(root)) {
            return false;
        }

        String name = file.getName();
        return DATA_DIRECTORY_NAME.equals(name) || CONFIG_DIRECTORY_NAME.equals(name);
    }

    private static void ensureParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
        }
    }

    private static void ensureReadOnly(File file) throws IOException {
        if (!file.isFile()) {
            throw new IOException("Expected regular file: " + file.getAbsolutePath());
        }

        if (!file.canRead() && !file.setReadable(true, true)) {
            throw new IOException("Failed to mark file readable: " + file.getAbsolutePath());
        }

        if (file.canWrite() && !file.setReadOnly()) {
            throw new IOException("Failed to keep file read-only: " + file.getAbsolutePath());
        }
    }

    private static void pruneStaleCachedMods(File cacheModsDir, Set<String> installedModIds) {
        File[] cachedEntries = cacheModsDir.listFiles(File::isDirectory);
        if (cachedEntries == null) {
            return;
        }

        for (File cachedEntry : cachedEntries) {
            if (installedModIds.contains(cachedEntry.getName())) {
                continue;
            }
            if (cachedEntry.getName().endsWith(".tmp")) {
                deleteRecursively(cachedEntry);
                continue;
            }

            deleteRecursively(cachedEntry);
        }
    }

    private static boolean deleteRecursively(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
}
