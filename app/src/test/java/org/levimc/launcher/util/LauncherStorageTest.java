package org.levimc.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.levimc.launcher.settings.FeatureSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class LauncherStorageTest {
    @Test
    public void sanitizeProfileIdKeepsSupportedCharacters() {
        assertEquals("Minecraft_1.21.80-beta", LauncherStorage.sanitizeProfileId("Minecraft_1.21.80-beta"));
    }

    @Test
    public void sanitizeProfileIdReplacesUnsupportedCharacters() {
        assertEquals("My_Version__1", LauncherStorage.sanitizeProfileId("My Version:/1"));
    }

    @Test
    public void sanitizeProfileIdAvoidsReservedNames() {
        assertEquals("_shared_profile", LauncherStorage.sanitizeProfileId("_shared"));
        assertEquals("_legacy_unclassified_profile", LauncherStorage.sanitizeProfileId("_legacy_unclassified"));
    }

    @Test
    public void isReservedProfileIdDetectsRawReservedNames() {
        assertTrue(LauncherStorage.isReservedProfileId("_shared"));
        assertTrue(LauncherStorage.isReservedProfileId("_legacy_unclassified"));
        assertFalse(LauncherStorage.isReservedProfileId("Minecraft_1.21.80"));
    }

    @Test
    public void mediaAppRootUsesPackageMediaDirectory() {
        File mediaDir = new File("/storage/emulated/0/Android/media/org.levimc.launcher");

        assertEquals(mediaDir, LauncherStorage.buildTargetMediaAppRoot(mediaDir));
    }

    @Test
    public void displayPathUsesAndroidMediaPackageDirectory() {
        assertEquals(
                "Android/media/org.levimc.launcher",
                LauncherStorage.buildTargetAppRootDisplayPath("org.levimc.launcher")
        );
    }

    @Test
    public void profileCacheRootUsesInternalCacheDirectoryAndVersionId() {
        File appCacheDir = new File("/data/user/0/org.levimc.launcher/cache");

        assertEquals(
                new File(appCacheDir, "minecraft/Minecraft_1.21.80-beta"),
                LauncherStorage.buildProfileCacheRoot(appCacheDir, "Minecraft_1.21.80-beta")
        );
    }

    @Test
    public void sharedStorageModeUsesNewLayoutWhenLegacySharedRootsAreEmpty() throws Exception {
        File temp = Files.createTempDirectory("levi-shared-layout").toFile();
        File internalRoot = new File(temp, "internal");
        File externalRoot = new File(temp, "external");
        assertTrue(internalRoot.mkdirs());
        assertTrue(externalRoot.mkdirs());

        assertEquals(
                LauncherStorage.SHARED_MODE_NEW,
                LauncherStorage.chooseSharedStorageMode(internalRoot, externalRoot)
        );
        deleteRecursively(temp);
    }

    @Test
    public void sharedStorageModeUsesLegacyLayoutWhenInternalHasResources() throws Exception {
        File temp = Files.createTempDirectory("levi-shared-layout").toFile();
        File internalRoot = new File(temp, "internal");
        File externalRoot = new File(temp, "external");
        File worldsDir = new File(internalRoot, LauncherStorage.GAME_DATA_RELATIVE_PATH + "/minecraftWorlds/world1");
        assertTrue(worldsDir.mkdirs());
        assertTrue(externalRoot.mkdirs());
        writeBytes(new File(worldsDir, "level.dat"), 3);

        assertEquals(
                LauncherStorage.SHARED_MODE_LEGACY,
                LauncherStorage.chooseSharedStorageMode(internalRoot, externalRoot)
        );
        deleteRecursively(temp);
    }

    @Test
    public void sharedStorageModeUsesLegacyLayoutWhenExternalHasResources() throws Exception {
        File temp = Files.createTempDirectory("levi-shared-layout").toFile();
        File internalRoot = new File(temp, "internal");
        File externalRoot = new File(temp, "external");
        File packsDir = new File(externalRoot, LauncherStorage.GAME_DATA_RELATIVE_PATH + "/resource_packs/pack1");
        assertTrue(internalRoot.mkdirs());
        assertTrue(packsDir.mkdirs());
        writeBytes(new File(packsDir, "manifest.json"), 2);

        assertEquals(
                LauncherStorage.SHARED_MODE_LEGACY,
                LauncherStorage.chooseSharedStorageMode(internalRoot, externalRoot)
        );
        deleteRecursively(temp);
    }

    @Test
    public void resolveSharedFilesRootHonorsSelectedMode() {
        File legacyRoot = new File("legacy");
        File newRoot = new File("new");

        assertEquals(legacyRoot, LauncherStorage.resolveSharedFilesRoot(legacyRoot, newRoot, LauncherStorage.SHARED_MODE_LEGACY));
        assertEquals(newRoot, LauncherStorage.resolveSharedFilesRoot(legacyRoot, newRoot, LauncherStorage.SHARED_MODE_NEW));
    }

    @Test
    public void resolveSharedFilesRootCanSelectSharedRuntimeRoots() {
        File legacyRuntimeRoot = new File("legacy-runtime");
        File newRuntimeRoot = new File("new-runtime");

        assertEquals(
                legacyRuntimeRoot,
                LauncherStorage.resolveSharedFilesRoot(
                        legacyRuntimeRoot,
                        newRuntimeRoot,
                        LauncherStorage.SHARED_MODE_LEGACY
                )
        );
        assertEquals(
                newRuntimeRoot,
                LauncherStorage.resolveSharedFilesRoot(
                        legacyRuntimeRoot,
                        newRuntimeRoot,
                        LauncherStorage.SHARED_MODE_NEW
                )
        );
    }

    @Test
    public void normalizeContentStorageTypeUsesVersionIsolationRootsWhenEnabled() {
        assertEquals(
                FeatureSettings.StorageType.VERSION_ISOLATION_INTERNAL,
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.INTERNAL, true)
        );
        assertEquals(
                FeatureSettings.StorageType.VERSION_ISOLATION_EXTERNAL,
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.EXTERNAL, true)
        );
    }

    @Test
    public void normalizeContentStorageTypeUsesSharedRootsWhenVersionIsolationDisabled() {
        assertEquals(
                FeatureSettings.StorageType.INTERNAL,
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.VERSION_ISOLATION_INTERNAL, false)
        );
        assertEquals(
                FeatureSettings.StorageType.EXTERNAL,
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.VERSION_ISOLATION_EXTERNAL, false)
        );
    }

    @Test
    public void cleanupLegacyRootRejectsIncompleteMigration() throws Exception {
        File temp = Files.createTempDirectory("levi-cleanup").toFile();
        File legacyRoot = new File(temp, "legacy");
        File targetRoot = new File(temp, "target");
        assertTrue(legacyRoot.mkdirs());
        assertTrue(targetRoot.mkdirs());

        LauncherStorage.LegacyCleanupResult result = LauncherStorage.cleanupLegacyRoot(legacyRoot, targetRoot, false);

        assertFalse(result.success);
        assertTrue(legacyRoot.exists());
        deleteRecursively(temp);
    }

    @Test
    public void cleanupLegacyRootTreatsMissingDirectoryAsSuccess() throws Exception {
        File temp = Files.createTempDirectory("levi-cleanup").toFile();
        File targetRoot = new File(temp, "target");
        assertTrue(targetRoot.mkdirs());

        LauncherStorage.LegacyCleanupResult result = LauncherStorage.cleanupLegacyRoot(new File(temp, "missing"), targetRoot, true);

        assertTrue(result.success);
        assertEquals(0, result.deletedFiles);
        assertEquals(0L, result.deletedBytes);
        deleteRecursively(temp);
    }

    @Test
    public void cleanupLegacyRootRejectsTargetOverlap() throws Exception {
        File temp = Files.createTempDirectory("levi-cleanup").toFile();
        File targetRoot = new File(temp, "target");
        File legacyRoot = new File(targetRoot, "legacy");
        assertTrue(legacyRoot.mkdirs());

        LauncherStorage.LegacyCleanupResult result = LauncherStorage.cleanupLegacyRoot(legacyRoot, targetRoot, true);

        assertFalse(result.success);
        assertTrue(legacyRoot.exists());
        deleteRecursively(temp);
    }

    @Test
    public void cleanupLegacyRootDeletesFilesAndCountsBytes() throws Exception {
        File temp = Files.createTempDirectory("levi-cleanup").toFile();
        File legacyRoot = new File(temp, "legacy");
        File targetRoot = new File(temp, "target");
        File nested = new File(legacyRoot, "nested");
        assertTrue(nested.mkdirs());
        assertTrue(targetRoot.mkdirs());
        writeBytes(new File(legacyRoot, "one.bin"), 3);
        writeBytes(new File(nested, "two.bin"), 5);

        LauncherStorage.LegacyCleanupResult result = LauncherStorage.cleanupLegacyRoot(legacyRoot, targetRoot, true);

        assertTrue(result.success);
        assertEquals(2, result.deletedFiles);
        assertEquals(8L, result.deletedBytes);
        assertFalse(legacyRoot.exists());
        deleteRecursively(temp);
    }

    private static void writeBytes(File file, int size) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            for (int i = 0; i < size; i++) {
                out.write(i);
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
