package org.levimc.launcher.core.content.nbt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class NativeNbt {
    public static final int LITTLE_ENDIAN = 0;
    public static final int LEVEL_DAT = 1;
    public static final int BIG_ENDIAN = 2;
    public static final int MAX_BYTES = 64 * 1024 * 1024;

    static {
        System.loadLibrary("leviworld");
    }

    private NativeNbt() {}

    public static void ensureLoaded() {}

    public static native NbtDocument decode(byte[] bytes, int offset, int format)
            throws IOException;

    public static native byte[] encode(NbtTag root, int version, int format, boolean gzip)
            throws IOException;

    public static native String toSnbt(NbtTag tag) throws IOException;

    public static native NbtTag fromSnbt(String text) throws IOException;

    public static NbtDocument read(File file, int format) throws IOException {
        if (file.length() > MAX_BYTES) throw new IOException("NBT file exceeds 64 MiB");
        byte[] data = Files.readAllBytes(file.toPath());
        NbtDocument doc = decode(data, 0, format);
        if (!doc.compressed && doc.bytesRead != data.length)
            throw new IOException("Unexpected bytes after NBT root");
        return doc;
    }

    public static NbtDocument readAuto(byte[] bytes) throws IOException {
        if (bytes.length > 8
                && bytes[8] == 10
                && java.nio.ByteBuffer.wrap(bytes, 4, 4)
                                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                .getInt()
                        == bytes.length - 8) return decode(bytes, 0, LEVEL_DAT);
        return decode(bytes, 0, LITTLE_ENDIAN);
    }

    public static List<NbtTag> sequence(byte[] bytes) throws IOException {
        List<NbtTag> result = new ArrayList<>();
        for (int offset = 0; offset < bytes.length; ) {
            NbtDocument doc = decode(bytes, offset, LITTLE_ENDIAN);
            if (doc.bytesRead <= 0) throw new IOException("Invalid NBT sequence");
            result.add(doc.root);
            offset += doc.bytesRead;
        }
        return result;
    }

    public static byte[] sequenceBytes(List<NbtTag> tags) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (NbtTag tag : tags) {
            byte[] bytes = encode(tag, 0, LITTLE_ENDIAN, false);
            if ((long) out.size() + bytes.length > MAX_BYTES)
                throw new IOException("NBT record exceeds 64 MiB");
            out.write(bytes);
        }
        return out.toByteArray();
    }
}
