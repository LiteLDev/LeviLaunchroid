package org.levimc.launcher.core.content;

import android.util.Log;

import org.levimc.launcher.core.content.nbt.BedrockNbtReader;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WorldItem extends ContentItem {
    private static final String TAG = "WorldItem";

    private String worldName;
    private String gameMode;
    private long lastPlayed;
    private boolean isValid;

    public WorldItem(String name, File worldDir) {
        super(name, worldDir);
        this.worldName = name;
        loadWorldInfo();
    }

    @Override
    public String getType() {
        return "World";
    }

    @Override
    public String getDescription() {
        if (!isValid) return "Invalid world";
        return String.format("Game Mode: %s", gameMode != null ? gameMode : "Unknown");
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    public String getWorldName() {
        return worldName;
    }

    private void loadWorldInfo() {
        if (file == null || !file.exists() || !file.isDirectory()) {
            isValid = false;
            return;
        }

        File levelDat = new File(file, "level.dat");
        File levelNameFile = new File(file, "levelname.txt");

        if (!levelDat.exists()) {
            isValid = false;
            return;
        }

        isValid = true;

        if (levelNameFile.exists()) {
            try (FileInputStream fis = new FileInputStream(levelNameFile)) {
                byte[] data = new byte[(int) levelNameFile.length()];
                fis.read(data);
                worldName = new String(data, StandardCharsets.UTF_8).trim();
                if (!worldName.isEmpty()) {
                    this.name = worldName;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to read levelname.txt for " + file.getName(), e);
            }
        }

        try {
            BedrockNbtReader reader = new BedrockNbtReader();
            NbtTag root = reader.readFile(levelDat);

            if (root != null && root.getType() == NbtTag.TAG_COMPOUND) {
                Map<String, NbtTag> compound = root.getCompound();

                NbtTag gameModeTag = compound.get("GameType");
                if (gameModeTag != null) {
                    int gameModeInt = gameModeTag.getInt();
                    gameMode = getGameModeName(gameModeInt);
                }

                if (worldName == null || worldName.isEmpty() || worldName.equals(file.getName())) {
                    NbtTag levelNameTag = compound.get("LevelName");
                    if (levelNameTag != null && levelNameTag.getType() == NbtTag.TAG_STRING) {
                        String nbtName = levelNameTag.getString();
                        if (nbtName != null && !nbtName.isEmpty()) {
                            worldName = nbtName;
                            this.name = worldName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read level.dat for " + file.getName(), e);
        }

        if (gameMode == null) {
            gameMode = "Survival";
        }

        lastPlayed = file.lastModified();
    }

    private String getGameModeName(int gameType) {
        return switch (gameType) {
            case 0 -> "Survival";
            case 1 -> "Creative";
            case 2 -> "Adventure";
            case 3 -> "Spectator";
            default -> "Unknown";
        };
    }
}