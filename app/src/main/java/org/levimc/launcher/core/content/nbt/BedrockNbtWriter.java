package org.levimc.launcher.core.content.nbt;

import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BedrockNbtWriter {
    private int headerVersion = 10;

    public void setHeaderVersion(int version) {
        headerVersion = version;
    }

    public void writeFile(File file, NbtTag root) throws IOException {
        byte[] bytes = writeToBytes(root);
        AtomicFile target = new AtomicFile(file);
        FileOutputStream out = null;
        try {
            out = target.startWrite();
            out.write(bytes);
            target.finishWrite(out);
        } catch (IOException ex) {
            if (out != null) target.failWrite(out);
            throw ex;
        }
    }

    public byte[] writeToBytes(NbtTag root) throws IOException {
        return NativeNbt.encode(root, headerVersion, NativeNbt.LEVEL_DAT, false);
    }
}
