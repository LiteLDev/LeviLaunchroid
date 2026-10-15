package org.levimc.launcher.core.content.leveldb;

import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.minecraft.MinecraftActivityState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class WorldDatabase implements AutoCloseable {
    private long handle;

    public WorldDatabase(File world) throws IOException {
        if (MinecraftActivityState.isRunning())
            throw new IOException("Close Minecraft before opening its world database");
        File db = new File(world, "db");
        if (!new File(db, "CURRENT").isFile())
            throw new IOException("World database was not found");
        NativeNbt.ensureLoaded();
        handle = openNative(db.getAbsolutePath());
    }

    private void requireOpen() throws IOException {
        if (handle == 0) throw new IOException("World database is closed");
        if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException();
        if (MinecraftActivityState.isRunning())
            throw new IOException("Close Minecraft before editing its world");
    }

    public synchronized byte[] get(byte[] key) throws IOException {
        requireOpen();
        return getNative(handle, key);
    }

    public byte[] get(String key) throws IOException {
        return get(key.getBytes(StandardCharsets.UTF_8));
    }

    public synchronized Page scan(byte[] prefix, byte[] after, int limit, String contains)
            throws IOException {
        requireOpen();
        byte[][] result = scanNative(handle, prefix, after, limit, contains);
        return new Page(Arrays.copyOfRange(result, 1, result.length), result[0]);
    }

    public synchronized void write(byte[][] keys, byte[][] values) throws IOException {
        requireOpen();
        writeNative(handle, keys, values);
    }

    @Override
    public synchronized void close() {
        if (handle != 0) {
            closeNative(handle);
            handle = 0;
        }
    }

    public static final class Page {
        public final byte[][] keys;
        public final byte[] next;

        Page(byte[][] keys, byte[] next) {
            this.keys = keys;
            this.next = next;
        }
    }

    private static native long openNative(String path) throws IOException;

    private static native void closeNative(long handle);

    private static native byte[] getNative(long handle, byte[] key) throws IOException;

    private static native byte[][] scanNative(
            long handle, byte[] prefix, byte[] after, int limit, String contains)
            throws IOException;

    private static native void writeNative(long handle, byte[][] keys, byte[][] values)
            throws IOException;
}
