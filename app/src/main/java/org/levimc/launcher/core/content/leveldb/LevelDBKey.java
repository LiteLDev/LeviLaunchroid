package org.levimc.launcher.core.content.leveldb;

import java.nio.charset.StandardCharsets;

public class LevelDBKey {
    public enum KeyType {
        CHUNK_VERSION(0x2c, "ChunkVersion"),
        CHUNK(0x2d, "Chunk"),
        DATA_2D(0x2d, "Data2D"),
        SUB_CHUNK(0x2f, "SubChunk"),
        BLOCK_ENTITY(0x31, "BlockEntity"),
        ENTITY(0x32, "Entity"),
        PENDING_TICKS(0x33, "PendingTicks"),
        BLOCK_EXTRA_DATA(0x34, "BlockExtraData"),
        BIOME_STATE(0x35, "BiomeState"),
        FINALIZED_STATE(0x36, "FinalizedState"),
        BORDER_BLOCKS(0x38, "BorderBlocks"),
        HARDCODED_SPAWN_AREAS(0x39, "HardcodedSpawnAreas"),
        RANDOM_TICKS(0x3a, "RandomTicks"),
        CHECKSUMS(0x3b, "Checksums"),
        GENERATION_SEED(0x3c, "GenerationSeed"),
        GENERATED_PRE_CAVES_AND_CLIFFS_BLENDING(0x3d, "GeneratedPreCavesAndCliffsBlending"),
        BLENDING_BIOME_HEIGHT(0x3e, "BlendingBiomeHeight"),
        META_DATA_HASH(0x3f, "MetaDataHash"),
        BLENDING_DATA(0x40, "BlendingData"),
        ACTOR_DIGEST_VERSION(0x41, "ActorDigestVersion"),
        LEGACY_VERSION(0x76, "LegacyVersion"),
        UNKNOWN(-1, "Unknown"),
        GENERAL(-2, "General");

        public final int id;
        public final String name;

        KeyType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public static KeyType fromId(int id) {
            for (KeyType type : values()) {
                if (type.id == id) return type;
            }
            return UNKNOWN;
        }
    }

    private final byte[] rawKey;
    private KeyType keyType = KeyType.GENERAL;
    private int subChunkIndex = -1;
    private String stringKey;
    private boolean isChunkKey;

    public LevelDBKey(byte[] key) {
        this.rawKey = key;
        parseKey();
    }

    private void parseKey() {
        if (rawKey == null || rawKey.length == 0) {
            keyType = KeyType.UNKNOWN;
            return;
        }

        if (rawKey.length == 9 || rawKey.length == 10 || rawKey.length == 13 || rawKey.length == 14) {

            if (rawKey.length == 9) {
                keyType = KeyType.fromId(rawKey[8] & 0xFF);
                isChunkKey = keyType.id >= 0;
            } else if (rawKey.length == 10) {
                keyType = KeyType.fromId(rawKey[8] & 0xFF);
                subChunkIndex = rawKey[9] & 0xFF;
                isChunkKey = keyType.id >= 0;
            } else if (rawKey.length == 13) {
                keyType = KeyType.fromId(rawKey[12] & 0xFF);
                isChunkKey = keyType.id >= 0;
            } else {
                keyType = KeyType.fromId(rawKey[12] & 0xFF);
                subChunkIndex = rawKey[13] & 0xFF;
                isChunkKey = keyType.id >= 0;
            }
        } else {
            isChunkKey = false;
            keyType = KeyType.GENERAL;
            try {
                stringKey = new String(rawKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                stringKey = bytesToHex(rawKey);
            }
        }
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public String getDisplayName() {
        if (isStructureKey()) {
            String structureId = getStructureId();
            return structureId != null ? structureId : "Unknown Structure";
        }
        if (isChunkKey) {
            String name = keyType.name;
            if (subChunkIndex >= 0) {
                name += " #" + subChunkIndex;
            }
            return name;
        }
        return stringKey != null ? stringKey : bytesToHex(rawKey);
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public boolean isStructureKey() {
        if (stringKey != null && stringKey.startsWith("structuretemplate_")) {
            return true;
        }
        if (rawKey != null && rawKey.length > 18) {
            byte[] prefix = "structuretemplate_".getBytes();
            boolean match = true;
            for (int i = 0; i < prefix.length && i < rawKey.length; i++) {
                if (rawKey[i] != prefix[i]) {
                    match = false;
                    break;
                }
            }
            return match;
        }
        return false;
    }

    public String getStructureId() {
        if (stringKey != null && stringKey.startsWith("structuretemplate_")) {
            return stringKey.substring("structuretemplate_".length());
        }
        if (rawKey != null && rawKey.length > 18) {
            try {
                String keyStr = new String(rawKey, StandardCharsets.UTF_8);
                if (keyStr.startsWith("structuretemplate_")) {
                    return keyStr.substring("structuretemplate_".length());
                }
            } catch (Exception e) {
            }
        }
        return null;
    }
}
