package org.levimc.launcher.core.content;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        File levelName = new File(file, "levelname.txt");
        
        if (!levelDat.exists()) {
            isValid = false;
            return;
        }

        isValid = true;

        if (levelName.exists()) {
            try (FileInputStream fis = new FileInputStream(levelName)) {
                byte[] data = new byte[(int) levelName.length()];
                fis.read(data);
                worldName = new String(data, StandardCharsets.UTF_8).trim();
                if (!worldName.isEmpty()) {
                    this.name = worldName;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to read levelname.txt for " + file.getName(), e);
            }
        }

        File worldBehaviorPacks = new File(file, "world_behavior_packs.json");
        if (worldBehaviorPacks.exists()) {
            try (FileInputStream fis = new FileInputStream(worldBehaviorPacks)) {
                byte[] data = new byte[(int) worldBehaviorPacks.length()];
                fis.read(data);
                String jsonStr = new String(data, StandardCharsets.UTF_8);
                isValid = true;
            } catch (IOException e) {
                Log.w(TAG, "Failed to read world_behavior_packs.json", e);
            }
        }

        if (gameMode == null) {
            gameMode = "Survival";
        }

        lastPlayed = file.lastModified();
    }
}