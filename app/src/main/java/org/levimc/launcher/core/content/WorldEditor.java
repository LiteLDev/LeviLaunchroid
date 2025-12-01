package org.levimc.launcher.core.content;

import android.util.Log;

import org.levimc.launcher.core.content.nbt.BedrockNbtReader;
import org.levimc.launcher.core.content.nbt.BedrockNbtWriter;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldEditor {
    private static final String TAG = "WorldEditor";

    private final File worldDir;
    private final File levelDatFile;
    
    private NbtTag levelDatRoot;
    private int levelDatVersion;
    private boolean levelDatLoaded = false;

    public WorldEditor(File worldDir) {
        this.worldDir = worldDir;
        this.levelDatFile = new File(worldDir, "level.dat");
    }

    public void loadLevelDat() throws IOException {
        if (!levelDatFile.exists()) {
            throw new IOException("level.dat not found");
        }

        BedrockNbtReader reader = new BedrockNbtReader();
        levelDatRoot = reader.readFile(levelDatFile);
        levelDatVersion = reader.getHeaderVersion();
        levelDatLoaded = true;
        
        Log.d(TAG, "Loaded level.dat, version: " + levelDatVersion);
    }

    public void saveLevelDat() throws IOException {
        if (!levelDatLoaded || levelDatRoot == null) {
            throw new IOException("level.dat not loaded");
        }

        File backup = new File(worldDir, "level.dat.backup");
        if (levelDatFile.exists()) {
            copyFile(levelDatFile, backup);
        }

        BedrockNbtWriter writer = new BedrockNbtWriter();
        writer.setHeaderVersion(levelDatVersion);
        writer.writeFile(levelDatFile, levelDatRoot);

        updateLevelNameFile();
        
        Log.d(TAG, "Saved level.dat");
    }

    private void updateLevelNameFile() {
        if (levelDatRoot == null || levelDatRoot.getType() != NbtTag.TAG_COMPOUND) {
            return;
        }
        
        NbtTag levelNameTag = levelDatRoot.getTag("LevelName");
        if (levelNameTag != null && levelNameTag.getType() == NbtTag.TAG_STRING) {
            String levelName = levelNameTag.getString();
            if (levelName != null && !levelName.isEmpty()) {
                File levelNameFile = new File(worldDir, "levelname.txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(levelNameFile)) {
                    fos.write(levelName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    Log.d(TAG, "Updated levelname.txt: " + levelName);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to update levelname.txt", e);
                }
            }
        }
    }

    public List<WorldProperty> getLevelDatProperties() {
        List<WorldProperty> properties = new ArrayList<>();
        
        if (levelDatRoot == null || levelDatRoot.getType() != NbtTag.TAG_COMPOUND) {
            return properties;
        }

        Map<String, NbtTag> compound = levelDatRoot.getCompound();
        extractProperties(compound, "", properties);
        
        return properties;
    }

    private void extractProperties(Map<String, NbtTag> compound, String prefix, List<WorldProperty> properties) {
        for (Map.Entry<String, NbtTag> entry : compound.entrySet()) {
            NbtTag tag = entry.getValue();
            String fullPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            
            if (tag.getType() == NbtTag.TAG_COMPOUND) {
                extractProperties(tag.getCompound(), fullPath, properties);
            } else if (tag.isEditable()) {
                properties.add(new WorldProperty(fullPath, tag));
            }
        }
    }

    public void updateLevelDatProperty(String path, Object newValue) {
        if (levelDatRoot == null) return;
        
        String[] parts = path.split("\\.");
        NbtTag current = levelDatRoot;

        for (int i = 0; i < parts.length - 1; i++) {
            if (current.getType() != NbtTag.TAG_COMPOUND) return;
            current = current.getTag(parts[i]);
            if (current == null) return;
        }

        if (current.getType() == NbtTag.TAG_COMPOUND) {
            NbtTag target = current.getTag(parts[parts.length - 1]);
            if (target != null) {
                target.setValue(convertValue(newValue, target.getType()));
            }
        }
    }

    private Object convertValue(Object value, byte targetType) {
        if (value instanceof String) {
            String str = (String) value;
            return switch (targetType) {
                case NbtTag.TAG_BYTE -> Byte.parseByte(str);
                case NbtTag.TAG_SHORT -> Short.parseShort(str);
                case NbtTag.TAG_INT -> Integer.parseInt(str);
                case NbtTag.TAG_LONG -> Long.parseLong(str);
                case NbtTag.TAG_FLOAT -> Float.parseFloat(str);
                case NbtTag.TAG_DOUBLE -> Double.parseDouble(str);
                case NbtTag.TAG_STRING -> str;
                default -> value;
            };
        }
        return value;
    }

    public boolean hasLevelDat() {
        return levelDatFile.exists();
    }

    public boolean isLevelDatLoaded() {
        return levelDatLoaded;
    }

    private void copyFile(File src, File dst) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(src);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    public static class WorldProperty {
        private final String path;
        private final NbtTag tag;
        private final String category;

        public WorldProperty(String path, NbtTag tag) {
            this.path = path;
            this.tag = tag;
            this.category = categorize(path);
        }

        public String getPath() { return path; }
        public NbtTag getTag() { return tag; }
        public String getName() { return tag.getName(); }
        public String getCategory() { return category; }
        public byte getType() { return tag.getType(); }
        public Object getValue() { return tag.getValue(); }
        
        public String getDisplayName() {
            String name = tag.getName();
            if (name.isEmpty()) {
                String[] parts = path.split("\\.");
                name = parts[parts.length - 1];
            }
            return formatName(name);
        }

        public String getValueString() {
            Object value = tag.getValue();
            if (value == null) return "";
            return value.toString();
        }

        public String getTypeString() {
            return NbtTag.getTypeName(tag.getType());
        }

        private String formatName(String name) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (i > 0 && Character.isUpperCase(c)) {
                    result.append(' ');
                }
                result.append(i == 0 ? Character.toUpperCase(c) : c);
            }
            return result.toString();
        }

        private String categorize(String path) {
            String lower = path.toLowerCase();
            if (lower.contains("spawn") || lower.contains("position") || lower.contains("pos")) {
                return "Position";
            }
            if (lower.contains("game") || lower.contains("mode") || lower.contains("difficulty")) {
                return "Game Settings";
            }
            if (lower.contains("time") || lower.contains("day") || lower.contains("tick")) {
                return "Time";
            }
            if (lower.contains("weather") || lower.contains("rain") || lower.contains("thunder")) {
                return "Weather";
            }
            if (lower.contains("player") || lower.contains("xp") || lower.contains("level")) {
                return "Player";
            }
            if (lower.contains("world") || lower.contains("seed") || lower.contains("generator")) {
                return "World";
            }
            if (lower.contains("cheat") || lower.contains("command") || lower.contains("allow")) {
                return "Cheats & Commands";
            }
            if (lower.contains("experiment") || lower.contains("beta")) {
                return "Experiments";
            }
            return "Other";
        }

        public boolean isBoolean() {
            if (tag.getType() == NbtTag.TAG_BYTE) {
                byte val = tag.getByte();
                return val == 0 || val == 1;
            }
            return false;
        }
    }
}