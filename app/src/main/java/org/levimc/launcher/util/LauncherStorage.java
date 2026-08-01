package org.levimc.launcher.util;

import android.content.Context;
import android.os.Environment;

import org.levimc.launcher.settings.FeatureSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Locale;

public final class LauncherStorage {
    private static final String PREFS_NAME = "storage_migration";
    private static final String KEY_COMPLETED = "storage_migration_completed";
    private static final String STORAGE_LAYOUT_PREFS_NAME = "storage_layout";
    private static final String KEY_SHARED_INTERNAL_MODE = "shared_internal_mode";
    private static final String KEY_SHARED_EXTERNAL_MODE = "shared_external_mode";
    static final String SHARED_MODE_LEGACY = "legacy";
    static final String SHARED_MODE_NEW = "new";
    private static final String LEGACY_ROOT_PATH = "games/org.levimc";
    private static final String NO_MEDIA_FILE = ".nomedia";
    private static final String ANDROID_DIR = "Android";
    private static final String ANDROID_MEDIA_DIR = "media";
    public static final String MINECRAFT_DIR = "minecraft";
    public static final String SHARED_PROFILE_ID = "_shared";
    public static final String LEGACY_UNCLASSIFIED_DIR = "_legacy_unclassified";
    public static final String INSTALLED_MINECRAFT_PROFILE_ID = "com.mojang.minecraftpe";
    public static final String INTERNAL_STORAGE_DIR = "internal";
    public static final String EXTERNAL_STORAGE_DIR = "external";
    public static final String PROFILE_DATA_DIR = "data";
    public static final String PROFILE_CACHE_DIR = "cache";
    public static final String PROFILE_MODS_DIR = "mods";
    public static final String PROFILE_METADATA_DIR = "metadata";
    public static final String GAMES_DIR = "games";
    public static final String MOJANG_DIR = "com.mojang";
    public static final String GAME_DATA_RELATIVE_PATH = GAMES_DIR + File.separator + MOJANG_DIR;
    public static final String GAME_DATA_RELATIVE_PATH_UNIX = GAMES_DIR + "/" + MOJANG_DIR;
    public static final String CRASH_LOGS_DIR = "crash_logs";
    private static final String BACKUPS_DIR = "backups";
    private static final String WORLDS_DIR = "worlds";
    private static final Object CACHE_LOCK = new Object();
    private static volatile File cachedTargetAppRoot;

    private LauncherStorage() {
    }

    public static File getAppRoot(Context context) {
        return getTargetAppRoot(context);
    }

    public static File getTargetAppRoot(Context context) {
        File cached = cachedTargetAppRoot;
        if (cached != null) {
            return cached;
        }

        synchronized (CACHE_LOCK) {
            cached = cachedTargetAppRoot;
            if (cached != null) {
                return cached;
            }
            cachedTargetAppRoot = resolveTargetAppRoot(context);
            return cachedTargetAppRoot;
        }
    }

    private static File resolveTargetAppRoot(Context context) {
        File[] mediaDirs = context.getExternalMediaDirs();
        if (mediaDirs != null) {
            for (File mediaDir : mediaDirs) {
                File appRoot = buildTargetMediaAppRoot(mediaDir);
                if (ensureDir(appRoot)) {
                    return appRoot;
                }
            }
        }

        File internalFallback = context.getFilesDir();
        ensureDir(internalFallback);
        return internalFallback;
    }

    static File buildTargetMediaAppRoot(File mediaDir) {
        return mediaDir;
    }

    public static String getTargetAppRootDisplayPath(Context context) {
        return buildTargetAppRootDisplayPath(context.getPackageName());
    }

    static String buildTargetAppRootDisplayPath(String packageName) {
        return ANDROID_DIR + "/" + ANDROID_MEDIA_DIR + "/" + packageName;
    }

    public static File getLegacyRoot() {
        return new File(Environment.getExternalStorageDirectory(), LEGACY_ROOT_PATH);
    }

    public static boolean isMigrationCompleted(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPLETED, false);
    }

    public static void markMigrationCompleted(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPLETED, true)
                .apply();
    }

    public static void invalidateCache() {
        synchronized (CACHE_LOCK) {
            cachedTargetAppRoot = null;
        }
    }

    public static File getMinecraftRoot(Context context) {
        File root = new File(getAppRoot(context), MINECRAFT_DIR);
        ensureDir(root);
        return root;
    }

    public static File getSharedRoot(Context context) {
        File dir = new File(getMinecraftRoot(context), SHARED_PROFILE_ID);
        ensureDir(dir);
        return dir;
    }

    public static File getSharedFilesRoot(Context context, boolean external) {
        File legacyRoot = getLegacySharedFilesRoot(context, external);
        File newRoot = getNewSharedFilesRoot(context, external);
        String mode = getSharedStorageMode(context, external);
        File dir = resolveSharedFilesRoot(legacyRoot, newRoot, mode);
        ensureDir(dir);
        ensureDir(new File(dir, GAME_DATA_RELATIVE_PATH));
        return dir;
    }

    public static File getSharedGameDataDir(Context context, boolean external) {
        File dir = new File(getSharedFilesRoot(context, external), GAME_DATA_RELATIVE_PATH);
        ensureDir(dir);
        return dir;
    }

    private static File getNewSharedFilesRoot(Context context, boolean external) {
        return new File(getSharedRoot(context), external ? EXTERNAL_STORAGE_DIR : INTERNAL_STORAGE_DIR);
    }

    private static File getLegacySharedFilesRoot(Context context, boolean external) {
        if (external) {
            File externalDir = context.getExternalFilesDir(null);
            return externalDir == null ? getNewSharedFilesRoot(context, true) : externalDir;
        }
        return context.getDataDir();
    }

    private static String getSharedStorageMode(Context context, boolean external) {
        ensureSharedStorageModeInitialized(context);
        String key = external ? KEY_SHARED_EXTERNAL_MODE : KEY_SHARED_INTERNAL_MODE;
        String mode = context.getSharedPreferences(STORAGE_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, SHARED_MODE_NEW);
        return SHARED_MODE_LEGACY.equals(mode) ? SHARED_MODE_LEGACY : SHARED_MODE_NEW;
    }

    private static void ensureSharedStorageModeInitialized(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(STORAGE_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_SHARED_INTERNAL_MODE) && prefs.contains(KEY_SHARED_EXTERNAL_MODE)) {
            return;
        }

        String mode = chooseSharedStorageMode(
                getLegacySharedFilesRoot(context, false),
                getLegacySharedFilesRoot(context, true)
        );
        prefs.edit()
                .putString(KEY_SHARED_INTERNAL_MODE, mode)
                .putString(KEY_SHARED_EXTERNAL_MODE, mode)
                .apply();
    }

    public static boolean isUsingNewSharedStorage(Context context) {
        ensureSharedStorageModeInitialized(context);
        android.content.SharedPreferences prefs = context.getSharedPreferences(STORAGE_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE);
        return SHARED_MODE_NEW.equals(prefs.getString(KEY_SHARED_INTERNAL_MODE, SHARED_MODE_NEW))
                && SHARED_MODE_NEW.equals(prefs.getString(KEY_SHARED_EXTERNAL_MODE, SHARED_MODE_NEW));
    }

    public static void setUseNewSharedStorage(Context context, boolean useNewStorage) {
        String mode = useNewStorage ? SHARED_MODE_NEW : SHARED_MODE_LEGACY;
        context.getSharedPreferences(STORAGE_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SHARED_INTERNAL_MODE, mode)
                .putString(KEY_SHARED_EXTERNAL_MODE, mode)
                .apply();
    }

    public static String getSharedInternalGameDataDisplayPath(Context context) {
        return getSharedGameDataDir(context, false).getAbsolutePath();
    }

    public static String getSharedExternalGameDataDisplayPath(Context context) {
        return getSharedGameDataDir(context, true).getAbsolutePath();
    }

    static String chooseSharedStorageMode(File legacyInternalFilesRoot, File legacyExternalFilesRoot) {
        return hasAnyFile(new File(legacyInternalFilesRoot, GAME_DATA_RELATIVE_PATH))
                || hasAnyFile(new File(legacyExternalFilesRoot, GAME_DATA_RELATIVE_PATH))
                ? SHARED_MODE_LEGACY
                : SHARED_MODE_NEW;
    }

    static File resolveSharedFilesRoot(File legacyRoot, File newRoot, String mode) {
        return SHARED_MODE_LEGACY.equals(mode) ? legacyRoot : newRoot;
    }

    public static File getSharedDataRoot(Context context) {
        File dir = resolveSharedFilesRoot(
                getLegacySharedDataRoot(context),
                getNewSharedDataRoot(context),
                getSharedStorageMode(context, false)
        );
        ensureDir(dir);
        return dir;
    }

    public static File getSharedCacheRoot(Context context) {
        File dir = resolveSharedFilesRoot(
                getLegacySharedCacheRoot(context),
                getNewSharedCacheRoot(context),
                getSharedStorageMode(context, false)
        );
        ensureDir(dir);
        return dir;
    }

    private static File getNewSharedDataRoot(Context context) {
        return new File(getSharedRoot(context), PROFILE_DATA_DIR);
    }

    private static File getLegacySharedDataRoot(Context context) {
        return context.getDataDir();
    }

    private static File getNewSharedCacheRoot(Context context) {
        return new File(getSharedRoot(context), PROFILE_CACHE_DIR);
    }

    private static File getLegacySharedCacheRoot(Context context) {
        return context.getCacheDir();
    }

    public static File getVersionRoot(Context context, String profileId) {
        File dir = new File(getMinecraftRoot(context), sanitizeProfileId(profileId));
        ensureDir(dir);
        return dir;
    }

    public static File getVersionDir(Context context, String directoryName) {
        return getVersionRoot(context, directoryName);
    }

    public static File getProfileFilesRoot(Context context, String profileId) {
        return getProfileFilesRoot(context, profileId, false);
    }

    public static File getProfileFilesRoot(Context context, String profileId, boolean external) {
        File dir = new File(getVersionRoot(context, profileId), external ? EXTERNAL_STORAGE_DIR : INTERNAL_STORAGE_DIR);
        ensureDir(dir);
        ensureDir(new File(dir, GAME_DATA_RELATIVE_PATH));
        return dir;
    }

    public static File getProfileGameDataDir(Context context, String profileId) {
        return getProfileGameDataDir(context, profileId, true);
    }

    public static File getProfileGameDataDir(Context context, String profileId, boolean external) {
        File dir = new File(getProfileFilesRoot(context, profileId, external), GAME_DATA_RELATIVE_PATH);
        ensureDir(dir);
        return dir;
    }

    public static File getProfileDataRoot(Context context, String profileId) {
        File dir = new File(getVersionRoot(context, profileId), PROFILE_DATA_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getProfileCacheRoot(Context context, String profileId) {
        File dir = new File(getVersionRoot(context, profileId), PROFILE_CACHE_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getProfileModsDir(Context context, String profileId) {
        File dir = new File(getVersionRoot(context, profileId), PROFILE_MODS_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getProfileMetadataDir(Context context, String profileId) {
        File dir = new File(getVersionRoot(context, profileId), PROFILE_METADATA_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getLegacyUnclassifiedDir(Context context) {
        File dir = new File(getMinecraftRoot(context), LEGACY_UNCLASSIFIED_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getStorageFilesRoot(Context context, String profileId, boolean versionIsolation, boolean external) {
        return versionIsolation
                ? getProfileFilesRoot(context, profileId, external)
                : getSharedFilesRoot(context, external);
    }

    public static File getStorageGameDataDir(Context context, String profileId, boolean versionIsolation, boolean external) {
        return versionIsolation
                ? getProfileGameDataDir(context, profileId, external)
                : getSharedGameDataDir(context, external);
    }

    public static File getContentGameDataDir(Context context, String profileId, FeatureSettings.StorageType storageType) {
        if (storageType == FeatureSettings.StorageType.VERSION_ISOLATION
                || storageType == FeatureSettings.StorageType.VERSION_ISOLATION_EXTERNAL) {
            return getProfileGameDataDir(context, profileId, true);
        }
        if (storageType == FeatureSettings.StorageType.VERSION_ISOLATION_INTERNAL) {
            return getProfileGameDataDir(context, profileId, false);
        }
        return getSharedGameDataDir(context, storageType == FeatureSettings.StorageType.EXTERNAL);
    }

    public static FeatureSettings.StorageType normalizeContentStorageType(
            FeatureSettings.StorageType storageType,
            boolean versionIsolation
    ) {
        FeatureSettings.StorageType safeType = storageType == null
                ? FeatureSettings.StorageType.INTERNAL
                : storageType;
        if (versionIsolation) {
            return switch (safeType) {
                case EXTERNAL, VERSION_ISOLATION, VERSION_ISOLATION_EXTERNAL ->
                        FeatureSettings.StorageType.VERSION_ISOLATION_EXTERNAL;
                case INTERNAL, VERSION_ISOLATION_INTERNAL ->
                        FeatureSettings.StorageType.VERSION_ISOLATION_INTERNAL;
            };
        }
        return switch (safeType) {
            case EXTERNAL, VERSION_ISOLATION, VERSION_ISOLATION_EXTERNAL ->
                    FeatureSettings.StorageType.EXTERNAL;
            case INTERNAL, VERSION_ISOLATION_INTERNAL ->
                    FeatureSettings.StorageType.INTERNAL;
        };
    }

    public static File getStorageDataRoot(Context context, String profileId, boolean versionIsolation) {
        return versionIsolation
                ? getProfileDataRoot(context, profileId)
                : getSharedDataRoot(context);
    }

    public static File getStorageCacheRoot(Context context, String profileId, boolean versionIsolation) {
        return versionIsolation
                ? getProfileCacheRoot(context, profileId)
                : getSharedCacheRoot(context);
    }

    public static File getCrashLogsDir(Context context) {
        File dir = new File(getAppRoot(context), CRASH_LOGS_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getBackupsRoot(Context context) {
        File dir = new File(getAppRoot(context), BACKUPS_DIR);
        ensureDir(dir);
        return dir;
    }

    public static File getWorldBackupsDir(Context context) {
        File dir = new File(getBackupsRoot(context), WORLDS_DIR);
        ensureDir(dir);
        return dir;
    }

    public static void ensureNoMedia(Context context) {
        try {
            File noMediaFile = new File(getAppRoot(context), NO_MEDIA_FILE);
            File parent = noMediaFile.getParentFile();
            if (parent != null) ensureDir(parent);
            if (!noMediaFile.exists()) noMediaFile.createNewFile();
        } catch (Exception ignored) {
        }
    }

    public static boolean ensureDir(File dir) {
        return dir != null && (dir.exists() ? dir.isDirectory() : dir.mkdirs());
    }

    public static boolean hasLegacyMarker() {
        return new File(getLegacyRoot(), NO_MEDIA_FILE).isFile();
    }

    public static boolean isReservedProfileId(String value) {
        if (value == null) return true;
        String profileId = value.trim().toLowerCase(Locale.US);
        return SHARED_PROFILE_ID.equals(profileId) || LEGACY_UNCLASSIFIED_DIR.equals(profileId);
    }

    public static String sanitizeProfileId(String value) {
        if (value == null) return "default";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "default";

        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '.'
                    || c == '_'
                    || c == '-') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }

        String sanitized = builder.toString();
        while (sanitized.endsWith(".")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (sanitized.isEmpty()) return "default";
        String lower = sanitized.toLowerCase(Locale.US);
        if (SHARED_PROFILE_ID.equals(lower) || LEGACY_UNCLASSIFIED_DIR.equals(lower)) {
            return sanitized + "_profile";
        }
        return sanitized;
    }

    public static boolean legacyRootHasData() {
        File legacyRoot = getLegacyRoot();
        if (!legacyRoot.isDirectory()) {
            return false;
        }
        return hasAnyFile(legacyRoot);
    }

    private static boolean hasAnyFile(File root) {
        if (root == null || !root.isDirectory()) {
            return false;
        }
        ArrayDeque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.isFile()) return true;
                if (child.isDirectory()) stack.push(child);
            }
        }
        return false;
    }

    public static LegacyCleanupResult cleanupLegacyRoot(Context context) {
        return cleanupLegacyRoot(getLegacyRoot(), getTargetAppRoot(context), isMigrationCompleted(context));
    }

    static LegacyCleanupResult cleanupLegacyRoot(File legacyRoot, File targetRoot, boolean migrationCompleted) {
        if (!migrationCompleted) {
            return LegacyCleanupResult.failed("Migration has not completed.");
        }
        if (!legacyRoot.exists()) {
            return LegacyCleanupResult.success(0, 0L);
        }
        if (!legacyRoot.isDirectory()) {
            return LegacyCleanupResult.failed("Legacy path is not a directory: " + legacyRoot.getAbsolutePath());
        }

        try {
            String legacyPath = legacyRoot.getCanonicalPath();
            String targetPath = targetRoot.getCanonicalPath();
            if (legacyPath.equals(targetPath) || isPathWithin(legacyPath, targetPath)) {
                return LegacyCleanupResult.failed("Legacy path overlaps with active storage.");
            }

            CleanupCounter counter = new CleanupCounter();
            boolean deleted = deleteLegacyChildFirst(legacyRoot, counter);
            if (!deleted || legacyRoot.exists()) {
                return LegacyCleanupResult.failed("Could not delete the legacy directory completely.");
            }
            return LegacyCleanupResult.success(counter.files, counter.bytes);
        } catch (IOException error) {
            return LegacyCleanupResult.failed(error.getMessage());
        }
    }

    private static boolean deleteLegacyChildFirst(File file, CleanupCounter counter) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return false;
            }
            for (File child : children) {
                if (!deleteLegacyChildFirst(child, counter)) {
                    return false;
                }
            }
        } else if (file.isFile()) {
            counter.files++;
            counter.bytes += Math.max(0L, file.length());
        }
        return file.delete();
    }

    private static boolean isPathWithin(String path, String basePath) {
        return path.startsWith(basePath + File.separator);
    }

    private static class CleanupCounter {
        int files;
        long bytes;
    }

    public static class LegacyCleanupResult {
        public final boolean success;
        public final int deletedFiles;
        public final long deletedBytes;
        public final String errorMessage;

        private LegacyCleanupResult(boolean success, int deletedFiles, long deletedBytes, String errorMessage) {
            this.success = success;
            this.deletedFiles = deletedFiles;
            this.deletedBytes = deletedBytes;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }

        static LegacyCleanupResult success(int deletedFiles, long deletedBytes) {
            return new LegacyCleanupResult(true, deletedFiles, deletedBytes, "");
        }

        static LegacyCleanupResult failed(String errorMessage) {
            return new LegacyCleanupResult(false, 0, 0L, errorMessage);
        }
    }
}
