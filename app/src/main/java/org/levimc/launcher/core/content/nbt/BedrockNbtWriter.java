package org.levimc.launcher.core.content.nbt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class BedrockNbtWriter {

    private ByteArrayOutputStream baos;
    private int headerVersion;

    public BedrockNbtWriter() {
        this.headerVersion = 10;
    }

    public void setHeaderVersion(int version) {
        this.headerVersion = version;
    }

    public void writeFile(File file, NbtTag root) throws IOException {
        byte[] data = writeToBytes(root);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    public byte[] writeToBytes(NbtTag root) throws IOException {
        baos = new ByteArrayOutputStream();

        writeTag(root);
        byte[] payload = baos.toByteArray();

        ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();

        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(headerVersion);
        header.putInt(payload.length);
        finalOutput.write(header.array());

        finalOutput.write(payload);
        
        return finalOutput.toByteArray();
    }

    private void writeTag(NbtTag tag) throws IOException {
        if (tag == null || tag.getType() == NbtTag.TAG_END) {
            baos.write(NbtTag.TAG_END);
            return;
        }

        baos.write(tag.getType());
        writeString(tag.getName());
        writePayload(tag);
    }

    private void writePayload(NbtTag tag) throws IOException {
        switch (tag.getType()) {
            case NbtTag.TAG_BYTE -> writeByte(tag.getByte());
            case NbtTag.TAG_SHORT -> writeShort(tag.getShort());
            case NbtTag.TAG_INT -> writeInt(tag.getInt());
            case NbtTag.TAG_LONG -> writeLong(tag.getLong());
            case NbtTag.TAG_FLOAT -> writeFloat(tag.getFloat());
            case NbtTag.TAG_DOUBLE -> writeDouble(tag.getDouble());
            case NbtTag.TAG_BYTE_ARRAY -> writeByteArray(tag.getByteArray());
            case NbtTag.TAG_STRING -> writeString(tag.getString());
            case NbtTag.TAG_LIST -> writeList(tag.getList());
            case NbtTag.TAG_COMPOUND -> writeCompound(tag.getCompound());
            case NbtTag.TAG_INT_ARRAY -> writeIntArray(tag.getIntArray());
            case NbtTag.TAG_LONG_ARRAY -> writeLongArray(tag.getLongArray());
        }
    }

    private void writeByte(byte value) {
        baos.write(value);
    }

    private void writeShort(short value) {
        ByteBuffer buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(value);
        baos.write(buf.array(), 0, 2);
    }

    private void writeInt(int value) {
        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(value);
        baos.write(buf.array(), 0, 4);
    }

    private void writeLong(long value) {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(value);
        baos.write(buf.array(), 0, 8);
    }

    private void writeFloat(float value) {
        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putFloat(value);
        baos.write(buf.array(), 0, 4);
    }

    private void writeDouble(double value) {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(value);
        baos.write(buf.array(), 0, 8);
    }

    private void writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeShort((short) bytes.length);
        baos.write(bytes);
    }

    private void writeByteArray(byte[] array) throws IOException {
        writeInt(array.length);
        baos.write(array);
    }

    private void writeIntArray(int[] array) {
        writeInt(array.length);
        for (int value : array) {
            writeInt(value);
        }
    }

    private void writeLongArray(long[] array) {
        writeInt(array.length);
        for (long value : array) {
            writeLong(value);
        }
    }

    private void writeList(List<NbtTag> list) throws IOException {
        if (list.isEmpty()) {
            baos.write(NbtTag.TAG_END);
            writeInt(0);
            return;
        }

        byte listType = list.get(0).getType();
        baos.write(listType);
        writeInt(list.size());
        
        for (NbtTag tag : list) {
            writePayload(tag);
        }
    }

    private void writeCompound(Map<String, NbtTag> compound) throws IOException {
        for (NbtTag tag : compound.values()) {
            baos.write(tag.getType());
            writeString(tag.getName());
            writePayload(tag);
        }
        baos.write(NbtTag.TAG_END);
    }
}