package org.levimc.launcher.core.content;

import org.levimc.launcher.core.content.nbt.NbtTag;
import org.levimc.launcher.core.content.world.WorldSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorldEditor implements AutoCloseable {
    private final File world;
    private final WorldSession session;
    private org.levimc.launcher.core.content.nbt.NbtDocument document;

    public WorldEditor(File world) {
        this.world = world;
        session = new WorldSession(world);
    }

    public void loadLevelDat() throws IOException {
        session.initialize();
        document = session.metadata();
    }

    public void saveLevelDat() throws IOException {
        if (document == null) throw new IOException("World NBT is not loaded");
        session.stageMetadata(document);
        session.save();
    }

    public boolean hasLevelDat() {
        return new File(world, "level.dat").isFile();
    }

    public boolean isLevelDatLoaded() {
        return document != null;
    }

    public List<WorldProperty> getLevelDatProperties() {
        List<WorldProperty> result = new ArrayList<>();
        if (document != null) extract(document.root, "", result);
        return result;
    }

    private void extract(NbtTag tag, String path, List<WorldProperty> result) {
        if (tag.getType() == NbtTag.TAG_COMPOUND)
            for (Map.Entry<String, NbtTag> e : tag.getCompound().entrySet())
                extract(
                        e.getValue(),
                        path.isEmpty() ? e.getKey() : path + "/" + e.getKey(),
                        result);
        else if (tag.getType() == NbtTag.TAG_LIST)
            for (int i = 0; i < tag.getList().size(); i++)
                extract(tag.getList().get(i), path + "/" + i, result);
        else result.add(new WorldProperty(path, tag));
    }

    public void updateLevelDatProperty(String path, Object value) {
        if (document == null) return;
        NbtTag tag = document.root;
        for (String key : path.split("/")) {
            if (tag.getType() == NbtTag.TAG_LIST) tag = tag.getList().get(Integer.parseInt(key));
            else tag = tag.getTag(key);
            if (tag == null) throw new IllegalArgumentException("Unknown NBT path");
        }
        String text = String.valueOf(value);
        tag.setValue(
                switch (tag.getType()) {
                    case NbtTag.TAG_BYTE -> Byte.parseByte(text);
                    case NbtTag.TAG_SHORT -> Short.parseShort(text);
                    case NbtTag.TAG_INT -> Integer.parseInt(text);
                    case NbtTag.TAG_LONG -> Long.parseLong(text);
                    case NbtTag.TAG_FLOAT -> Float.parseFloat(text);
                    case NbtTag.TAG_DOUBLE -> Double.parseDouble(text);
                    default -> value;
                });
    }

    @Override
    public void close() {
        session.close();
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

        public String getPath() {
            return path;
        }

        public NbtTag getTag() {
            return tag;
        }

        public String getName() {
            return tag.getName();
        }

        public String getCategory() {
            return category;
        }

        public byte getType() {
            return tag.getType();
        }

        public Object getValue() {
            return tag.getValue();
        }

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
