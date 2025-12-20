package org.levimc.launcher.core.content.leveldb;

public class LevelDBEntry {
    private final LevelDBKey key;
    private byte[] value;

    public LevelDBEntry(byte[] key, byte[] value) {
        this.key = new LevelDBKey(key);
        this.value = value;
    }

    public LevelDBKey getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

}
