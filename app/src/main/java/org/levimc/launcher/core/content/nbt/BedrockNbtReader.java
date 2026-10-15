package org.levimc.launcher.core.content.nbt;

import java.io.File;
import java.io.IOException;

public final class BedrockNbtReader {
    private NbtDocument document;

    public NbtTag readFile(File file) throws IOException {
        document = NativeNbt.read(file, NativeNbt.LEVEL_DAT);
        return document.root;
    }

    public NbtTag readFromBytes(byte[] bytes) throws IOException {
        document = NativeNbt.readAuto(bytes);
        return document.root;
    }

    public int getHeaderVersion() {
        return document == null ? 0 : document.headerVersion;
    }

    public int getPayloadLength() {
        return document == null
                ? 0
                : document.bytesRead - (document.format == NativeNbt.LEVEL_DAT ? 8 : 0);
    }
}
