package org.levimc.launcher.core.content.nbt;

public final class NbtDocument {
    public NbtTag root;
    public final int headerVersion;
    public final int bytesRead;
    public final int format;
    public final boolean compressed;

    public NbtDocument(
            NbtTag root, int headerVersion, int bytesRead, int format, boolean compressed) {
        this.root = root;
        this.headerVersion = headerVersion;
        this.bytesRead = bytesRead;
        this.format = format;
        this.compressed = compressed;
    }

    public byte[] encode() throws java.io.IOException {
        return NativeNbt.encode(root, headerVersion, format, compressed);
    }
}
