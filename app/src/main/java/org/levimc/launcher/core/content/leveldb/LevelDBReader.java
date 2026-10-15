package org.levimc.launcher.core.content.leveldb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class LevelDBReader implements AutoCloseable {
    private final File world;

    public LevelDBReader(File dbPath) {
        world = dbPath.getParentFile();
    }

    public List<LevelDBEntry> readAllEntries() throws IOException {
        return readEntries(new byte[0], 128 * 1024 * 1024);
    }

    public List<LevelDBEntry> readEntries(byte[] prefix, long maximumBytes) throws IOException {
        List<LevelDBEntry> entries = new ArrayList<>();
        long size = 0;
        try (WorldDatabase db = new WorldDatabase(world)) {
            byte[] after = null;
            do {
                WorldDatabase.Page page = db.scan(prefix, after, 128, "");
                for (byte[] key : page.keys) {
                    byte[] value = db.get(key);
                    if (value == null) continue;
                    size += value.length + key.length;
                    if (size > maximumBytes)
                        throw new IOException(
                                "Selection exceeds the memory budget; use the paged database"
                                        + " browser");
                    entries.add(new LevelDBEntry(key, value));
                }
                after = page.next;
            } while (after != null);
        }
        return entries;
    }

    @Override
    public void close() {}
}
