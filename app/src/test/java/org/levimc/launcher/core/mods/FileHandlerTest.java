package org.levimc.launcher.core.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileHandlerTest {
    @Test
    public void parseOverwriteFilesNormalizesSafeRelativeFiles() {
        JsonObject manifest = new JsonObject();
        JsonArray files = new JsonArray();
        files.add("assets/icon.png");
        files.add("config\\schema.json");
        files.add("../escape.json");
        files.add("/absolute.json");
        files.add(".");
        files.add("");
        manifest.add("overwrite_files", files);

        Set<String> expected = new HashSet<>(Arrays.asList(
                "assets/icon.png",
                "config/schema.json"));
        assertEquals(expected, FileHandler.parseOverwriteFiles(manifest));
    }

    @Test
    public void parseOverwriteFoldersNormalizesSafeRelativeDirectories() {
        JsonObject manifest = new JsonObject();
        JsonArray folders = new JsonArray();
        folders.add("assets");
        folders.add("config/schema/");
        folders.add("nested\\folder");
        folders.add("../escape");
        folders.add("/absolute");
        folders.add(".");
        folders.add("");
        manifest.add("overwrite_folders", folders);

        Set<String> expected = new HashSet<>(Arrays.asList(
                "assets",
                "config/schema",
                "nested/folder"));
        assertEquals(expected, FileHandler.parseOverwriteFolders(manifest));
    }

    @Test
    public void parseOverwriteFoldersAcceptsSingleString() {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("overwrite_folders", "assets");

        assertEquals(
                new HashSet<>(Arrays.asList("assets")),
                FileHandler.parseOverwriteFolders(manifest));
    }

    @Test
    public void canReplaceImportedPathAllowsManifestEntryAndWhitelistedFolderContents() {
        Set<String> overwriteFiles = new HashSet<>(Arrays.asList("assets/icon.png", "config/config.json"));
        Set<String> overwriteFolders = new HashSet<>(Arrays.asList("assets", "config/schema"));

        assertTrue(FileHandler.canReplaceImportedPath("manifest.json", "libmod.so", overwriteFiles, overwriteFolders));
        assertTrue(FileHandler.canReplaceImportedPath("libmod.so", "libmod.so", overwriteFiles, overwriteFolders));
        assertTrue(FileHandler.canReplaceImportedPath("assets/icon.png", "libmod.so", overwriteFiles, overwriteFolders));
        assertTrue(FileHandler.canReplaceImportedPath("config/config.json", "libmod.so", overwriteFiles, overwriteFolders));
        assertTrue(FileHandler.canReplaceImportedPath("assets/textures/icon.png", "libmod.so", overwriteFiles, overwriteFolders));
        assertTrue(FileHandler.canReplaceImportedPath("config/schema/options.json", "libmod.so", overwriteFiles, overwriteFolders));

        assertFalse(FileHandler.canReplaceImportedPath("assets", "libmod.so", overwriteFiles, overwriteFolders));
        assertFalse(FileHandler.canReplaceImportedPath("assets2/textures/icon.png", "libmod.so", overwriteFiles, overwriteFolders));
        assertFalse(FileHandler.canReplaceImportedPath("config/other.json", "libmod.so", overwriteFiles, overwriteFolders));
        assertFalse(FileHandler.canReplaceImportedPath("../outside", "libmod.so", overwriteFiles, overwriteFolders));
    }
}
