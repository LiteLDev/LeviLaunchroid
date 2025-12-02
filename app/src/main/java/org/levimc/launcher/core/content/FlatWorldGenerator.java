package org.levimc.launcher.core.content;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.core.content.nbt.BedrockNbtWriter;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FlatWorldGenerator {
    private static final String TAG = "FlatWorldGenerator";

    public static class BlockLayer {
        public String blockName;
        public int count;

        public BlockLayer(String blockName, int count) {
            this.blockName = blockName;
            this.count = count;
        }
    }

    public static List<BlockLayer> getDefaultLayers() {
        List<BlockLayer> layers = new ArrayList<>();
        layers.add(new BlockLayer("minecraft:bedrock", 1));
        layers.add(new BlockLayer("minecraft:dirt", 2));
        layers.add(new BlockLayer("minecraft:grass_block", 1));
        return layers;
    }

    public static String buildFlatWorldLayersJson(List<BlockLayer> layers, int biomeId) {
        try {
            JSONObject root = new JSONObject();
            root.put("biome_id", biomeId);

            JSONArray blockLayers = new JSONArray();
            for (BlockLayer layer : layers) {
                JSONObject layerObj = new JSONObject();
                layerObj.put("block_name", layer.blockName);
                layerObj.put("count", layer.count);
                blockLayers.put(layerObj);
            }
            root.put("block_layers", blockLayers);
            root.put("encoding_version", 6);
            root.put("structure_options", JSONObject.NULL);
            root.put("world_version", "version.post_1_18");

            String result = root.toString();
            Log.d(TAG, "FlatWorldLayers JSON: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to build flat world layers JSON", e);
            return "{\"biome_id\":1,\"block_layers\":[{\"block_name\":\"minecraft:bedrock\",\"count\":1},{\"block_name\":\"minecraft:dirt\",\"count\":2},{\"block_name\":\"minecraft:grass_block\",\"count\":1}],\"encoding_version\":6,\"structure_options\":null,\"world_version\":\"version.post_1_18\"}";
        }
    }

    public static File generateFlatWorld(File worldsDirectory, String worldName, List<BlockLayer> layers, int biomeId, int gameMode) throws IOException {
        String safeName = worldName.replaceAll("[^a-zA-Z0-9_\\- ]", "_");
        String folderName = safeName + "_" + System.currentTimeMillis();
        File worldDir = new File(worldsDirectory, folderName);

        if (!worldDir.mkdirs()) {
            throw new IOException("Failed to create world directory");
        }

        try {
            int totalHeight = 0;
            for (BlockLayer layer : layers) {
                totalHeight += layer.count;
            }
            int spawnY = Math.max(totalHeight + 2, 4);

            writeLevelName(worldDir, worldName);
            writeLevelDat(worldDir, worldName, layers, biomeId, gameMode, spawnY);

            File levelDat = new File(worldDir, "level.dat");
            File levelDatOld = new File(worldDir, "level.dat_old");
            copyFile(levelDat, levelDatOld);

            createDbDirectory(worldDir);

            writeEmptyJsonArray(new File(worldDir, "world_behavior_packs.json"));
            writeEmptyJsonArray(new File(worldDir, "world_resource_packs.json"));

            Log.d(TAG, "Generated flat world: " + worldName + " with " + layers.size() + " layers, biome=" + biomeId + ", spawnY=" + spawnY);
            Log.d(TAG, "FlatWorldLayers: " + buildFlatWorldLayersJson(layers, biomeId));
            return worldDir;
        } catch (Exception e) {
            deleteDirectory(worldDir);
            throw new IOException("Failed to generate world: " + e.getMessage(), e);
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(src);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    private static void writeLevelName(File worldDir, String worldName) throws IOException {
        File levelNameFile = new File(worldDir, "levelname.txt");
        try (FileOutputStream fos = new FileOutputStream(levelNameFile)) {
            fos.write(worldName.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeLevelDat(File worldDir, String worldName, List<BlockLayer> layers, int biomeId, int gameMode, int spawnY) throws IOException {
        Map<String, NbtTag> compound = new LinkedHashMap<>();

        long currentTime = System.currentTimeMillis() / 1000;
        long seed = new Random().nextLong();

        String flatLayersJson = buildFlatWorldLayersJson(layers, biomeId);

        compound.put("LevelName", new NbtTag(NbtTag.TAG_STRING, "LevelName", worldName));
        compound.put("GameType", new NbtTag(NbtTag.TAG_INT, "GameType", gameMode));
        compound.put("Generator", new NbtTag(NbtTag.TAG_INT, "Generator", 2));
        compound.put("RandomSeed", new NbtTag(NbtTag.TAG_LONG, "RandomSeed", seed));
        compound.put("SpawnX", new NbtTag(NbtTag.TAG_INT, "SpawnX", 0));
        compound.put("SpawnY", new NbtTag(NbtTag.TAG_INT, "SpawnY", spawnY));
        compound.put("SpawnZ", new NbtTag(NbtTag.TAG_INT, "SpawnZ", 0));

        compound.put("FlatWorldLayers", new NbtTag(NbtTag.TAG_STRING, "FlatWorldLayers", flatLayersJson));
        compound.put("Difficulty", new NbtTag(NbtTag.TAG_INT, "Difficulty", 2));
        compound.put("ForceGameType", new NbtTag(NbtTag.TAG_BYTE, "ForceGameType", (byte) 0));
        compound.put("commandsEnabled", new NbtTag(NbtTag.TAG_BYTE, "commandsEnabled", (byte) 1));
        compound.put("dodaylightcycle", new NbtTag(NbtTag.TAG_BYTE, "dodaylightcycle", (byte) 1));
        compound.put("domobspawning", new NbtTag(NbtTag.TAG_BYTE, "domobspawning", (byte) 1));
        compound.put("doweathercycle", new NbtTag(NbtTag.TAG_BYTE, "doweathercycle", (byte) 1));
        compound.put("keepinventory", new NbtTag(NbtTag.TAG_BYTE, "keepinventory", (byte) 0));
        compound.put("mobgriefing", new NbtTag(NbtTag.TAG_BYTE, "mobgriefing", (byte) 1));
        compound.put("pvp", new NbtTag(NbtTag.TAG_BYTE, "pvp", (byte) 1));
        compound.put("showcoordinates", new NbtTag(NbtTag.TAG_BYTE, "showcoordinates", (byte) 1));
        compound.put("Time", new NbtTag(NbtTag.TAG_LONG, "Time", 0L));
        compound.put("LastPlayed", new NbtTag(NbtTag.TAG_LONG, "LastPlayed", currentTime));
        compound.put("Platform", new NbtTag(NbtTag.TAG_INT, "Platform", 2));
        compound.put("StorageVersion", new NbtTag(NbtTag.TAG_INT, "StorageVersion", 10));
        compound.put("NetworkVersion", new NbtTag(NbtTag.TAG_INT, "NetworkVersion", 748));
        compound.put("worldStartCount", new NbtTag(NbtTag.TAG_LONG, "worldStartCount", 0L));
        compound.put("hasBeenLoadedInCreative", new NbtTag(NbtTag.TAG_BYTE, "hasBeenLoadedInCreative", (byte) (gameMode == 1 ? 1 : 0)));
        compound.put("hasLockedBehaviorPack", new NbtTag(NbtTag.TAG_BYTE, "hasLockedBehaviorPack", (byte) 0));
        compound.put("hasLockedResourcePack", new NbtTag(NbtTag.TAG_BYTE, "hasLockedResourcePack", (byte) 0));
        compound.put("isFromLockedTemplate", new NbtTag(NbtTag.TAG_BYTE, "isFromLockedTemplate", (byte) 0));
        compound.put("isFromWorldTemplate", new NbtTag(NbtTag.TAG_BYTE, "isFromWorldTemplate", (byte) 0));
        compound.put("isSingleUseWorld", new NbtTag(NbtTag.TAG_BYTE, "isSingleUseWorld", (byte) 0));
        compound.put("isWorldTemplateOptionLocked", new NbtTag(NbtTag.TAG_BYTE, "isWorldTemplateOptionLocked", (byte) 0));
        compound.put("lightningLevel", new NbtTag(NbtTag.TAG_FLOAT, "lightningLevel", 0.0f));
        compound.put("lightningTime", new NbtTag(NbtTag.TAG_INT, "lightningTime", 0));
        compound.put("rainLevel", new NbtTag(NbtTag.TAG_FLOAT, "rainLevel", 0.0f));
        compound.put("rainTime", new NbtTag(NbtTag.TAG_INT, "rainTime", 0));
        compound.put("spawnMobs", new NbtTag(NbtTag.TAG_BYTE, "spawnMobs", (byte) 1));
        compound.put("texturePacksRequired", new NbtTag(NbtTag.TAG_BYTE, "texturePacksRequired", (byte) 0));
        compound.put("useMsaGamertagsOnly", new NbtTag(NbtTag.TAG_BYTE, "useMsaGamertagsOnly", (byte) 0));
        compound.put("bonusChestEnabled", new NbtTag(NbtTag.TAG_BYTE, "bonusChestEnabled", (byte) 0));
        compound.put("bonusChestSpawned", new NbtTag(NbtTag.TAG_BYTE, "bonusChestSpawned", (byte) 0));
        compound.put("CenterMapsToOrigin", new NbtTag(NbtTag.TAG_BYTE, "CenterMapsToOrigin", (byte) 0));
        compound.put("ConfirmedPlatformLockedContent", new NbtTag(NbtTag.TAG_BYTE, "ConfirmedPlatformLockedContent", (byte) 0));
        compound.put("currentTick", new NbtTag(NbtTag.TAG_LONG, "currentTick", 0L));
        compound.put("daylightCycle", new NbtTag(NbtTag.TAG_INT, "daylightCycle", 0));
        compound.put("eduOffer", new NbtTag(NbtTag.TAG_INT, "eduOffer", 0));
        compound.put("educationFeaturesEnabled", new NbtTag(NbtTag.TAG_BYTE, "educationFeaturesEnabled", (byte) 0));
        compound.put("experimentalgameplay", new NbtTag(NbtTag.TAG_BYTE, "experimentalgameplay", (byte) 0));
        compound.put("immutableWorld", new NbtTag(NbtTag.TAG_BYTE, "immutableWorld", (byte) 0));
        compound.put("LANBroadcast", new NbtTag(NbtTag.TAG_BYTE, "LANBroadcast", (byte) 1));
        compound.put("LANBroadcastIntent", new NbtTag(NbtTag.TAG_BYTE, "LANBroadcastIntent", (byte) 1));
        compound.put("MultiplayerGame", new NbtTag(NbtTag.TAG_BYTE, "MultiplayerGame", (byte) 1));
        compound.put("MultiplayerGameIntent", new NbtTag(NbtTag.TAG_BYTE, "MultiplayerGameIntent", (byte) 1));
        compound.put("XBLBroadcast", new NbtTag(NbtTag.TAG_BYTE, "XBLBroadcast", (byte) 0));
        compound.put("XBLBroadcastIntent", new NbtTag(NbtTag.TAG_BYTE, "XBLBroadcastIntent", (byte) 0));
        compound.put("requiresCopiedPackRemovalCheck", new NbtTag(NbtTag.TAG_BYTE, "requiresCopiedPackRemovalCheck", (byte) 0));
        compound.put("serverChunkTickRange", new NbtTag(NbtTag.TAG_INT, "serverChunkTickRange", 4));
        compound.put("SpawnV1Villagers", new NbtTag(NbtTag.TAG_BYTE, "SpawnV1Villagers", (byte) 0));
        compound.put("startWithMapEnabled", new NbtTag(NbtTag.TAG_BYTE, "startWithMapEnabled", (byte) 0));
        compound.put("worldPolicies", new NbtTag(NbtTag.TAG_COMPOUND, "worldPolicies", new LinkedHashMap<String, NbtTag>()));

        compound.put("BiomeOverride", new NbtTag(NbtTag.TAG_STRING, "BiomeOverride", ""));
        compound.put("LimitedWorldOriginX", new NbtTag(NbtTag.TAG_INT, "LimitedWorldOriginX", 0));
        compound.put("LimitedWorldOriginY", new NbtTag(NbtTag.TAG_INT, "LimitedWorldOriginY", 32767));
        compound.put("LimitedWorldOriginZ", new NbtTag(NbtTag.TAG_INT, "LimitedWorldOriginZ", 0));
        compound.put("limitedWorldWidth", new NbtTag(NbtTag.TAG_INT, "limitedWorldWidth", 0));
        compound.put("limitedWorldDepth", new NbtTag(NbtTag.TAG_INT, "limitedWorldDepth", 0));
        compound.put("WorldVersion", new NbtTag(NbtTag.TAG_INT, "WorldVersion", 1));
        compound.put("prid", new NbtTag(NbtTag.TAG_STRING, "prid", ""));

        Map<String, NbtTag> abilities = new LinkedHashMap<>();
        abilities.put("attackmobs", new NbtTag(NbtTag.TAG_BYTE, "attackmobs", (byte) 1));
        abilities.put("attackplayers", new NbtTag(NbtTag.TAG_BYTE, "attackplayers", (byte) 1));
        abilities.put("build", new NbtTag(NbtTag.TAG_BYTE, "build", (byte) 1));
        abilities.put("doorsandswitches", new NbtTag(NbtTag.TAG_BYTE, "doorsandswitches", (byte) 1));
        abilities.put("flySpeed", new NbtTag(NbtTag.TAG_FLOAT, "flySpeed", 0.05f));
        abilities.put("flying", new NbtTag(NbtTag.TAG_BYTE, "flying", (byte) 0));
        abilities.put("instabuild", new NbtTag(NbtTag.TAG_BYTE, "instabuild", (byte) (gameMode == 1 ? 1 : 0)));
        abilities.put("invulnerable", new NbtTag(NbtTag.TAG_BYTE, "invulnerable", (byte) (gameMode == 1 ? 1 : 0)));
        abilities.put("lightning", new NbtTag(NbtTag.TAG_BYTE, "lightning", (byte) 0));
        abilities.put("mayfly", new NbtTag(NbtTag.TAG_BYTE, "mayfly", (byte) (gameMode == 1 ? 1 : 0)));
        abilities.put("mine", new NbtTag(NbtTag.TAG_BYTE, "mine", (byte) 1));
        abilities.put("op", new NbtTag(NbtTag.TAG_BYTE, "op", (byte) 0));
        abilities.put("opencontainers", new NbtTag(NbtTag.TAG_BYTE, "opencontainers", (byte) 1));
        abilities.put("permissionsLevel", new NbtTag(NbtTag.TAG_INT, "permissionsLevel", 0));
        abilities.put("playerPermissionsLevel", new NbtTag(NbtTag.TAG_INT, "playerPermissionsLevel", 1));
        abilities.put("teleport", new NbtTag(NbtTag.TAG_BYTE, "teleport", (byte) 0));
        abilities.put("walkSpeed", new NbtTag(NbtTag.TAG_FLOAT, "walkSpeed", 0.1f));
        compound.put("abilities", new NbtTag(NbtTag.TAG_COMPOUND, "abilities", abilities));

        compound.put("baseGameVersion", new NbtTag(NbtTag.TAG_STRING, "baseGameVersion", "*"));
        compound.put("InventoryVersion", new NbtTag(NbtTag.TAG_STRING, "InventoryVersion", "1.21.124"));
        compound.put("lastOpenedWithVersion", createVersionArray("lastOpenedWithVersion"));
        compound.put("MinimumCompatibleClientVersion", createVersionArray("MinimumCompatibleClientVersion"));

        NbtTag root = new NbtTag(NbtTag.TAG_COMPOUND, "", compound);

        File levelDatFile = new File(worldDir, "level.dat");
        BedrockNbtWriter writer = new BedrockNbtWriter();
        writer.setHeaderVersion(10);
        writer.writeFile(levelDatFile, root);
    }

    private static NbtTag createVersionArray(String name) {
        List<NbtTag> version = new ArrayList<>();
        version.add(new NbtTag(NbtTag.TAG_INT, "", 1));
        version.add(new NbtTag(NbtTag.TAG_INT, "", 21));
        version.add(new NbtTag(NbtTag.TAG_INT, "", 50));
        version.add(new NbtTag(NbtTag.TAG_INT, "", 0));
        version.add(new NbtTag(NbtTag.TAG_INT, "", 0));
        return new NbtTag(NbtTag.TAG_LIST, name, version);
    }

    private static void createDbDirectory(File worldDir) {
        File dbDir = new File(worldDir, "db");
        dbDir.mkdirs();
    }

    private static void writeEmptyJsonArray(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("[]".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}