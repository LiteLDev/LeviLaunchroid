package org.levimc.launcher.core.content.world;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ChunkKey {
    public final int x, z, dimension, tag, section;
    public final boolean subChunk;

    private ChunkKey(int x, int z, int dimension, int tag, int section, boolean subChunk) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
        this.tag = tag;
        this.section = section;
        this.subChunk = subChunk;
    }

    public static ChunkKey parse(byte[] key) {
        if (key.length != 9 && key.length != 10 && key.length != 13 && key.length != 14)
            return null;
        boolean text = true;
        for (byte b : key)
            if ((b & 255) < 32 || (b & 255) > 126) {
                text = false;
                break;
            }
        if (text) return null;
        int offset = key.length > 10 ? 12 : 8;
        int tag = key[offset] & 255;
        if ((tag < 0x2b || tag > 0x41) && tag != 0x76) return null;
        boolean section = key.length == offset + 2;
        if (section != (tag == 0x2f)) return null;
        ByteBuffer b = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        int x = b.getInt(), z = b.getInt(), dimension = offset == 12 ? b.getInt() : 0;
        return new ChunkKey(x, z, dimension, tag, section ? key[offset + 1] : 0, section);
    }

    public static byte[] prefix(int x, int z) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(x).putInt(z).array();
    }

    public static byte[] of(int x, int z, int dimension, int tag) {
        return of(x, z, dimension, tag, 0);
    }

    public static byte[] of(int x, int z, int dimension, int tag, int section) {
        ByteBuffer b =
                ByteBuffer.allocate((dimension == 0 ? 9 : 13) + (tag == 0x2f ? 1 : 0))
                        .order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(x).putInt(z);
        if (dimension != 0) b.putInt(dimension);
        b.put((byte) tag);
        if (tag == 0x2f) b.put((byte) section);
        return b.array();
    }
}
