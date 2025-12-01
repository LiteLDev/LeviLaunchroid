package org.levimc.launcher.core.content.nbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BedrockNbtReader {
    
    private ByteBuffer buffer;
    private int headerVersion;
    private int payloadLength;

    public NbtTag readFile(File file) throws IOException {
        byte[] data = readFileBytes(file);
        return readFromBytes(data);
    }

    public NbtTag readFromBytes(byte[] data) throws IOException {
        if (data.length < 8) {
            throw new IOException("File too small to be valid NBT");
        }

        ByteBuffer headerBuf = ByteBuffer.wrap(data, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
        headerVersion = headerBuf.getInt();
        payloadLength = headerBuf.getInt();

        if (payloadLength > data.length - 8 || payloadLength < 0) {
            buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            headerVersion = 0;
            payloadLength = data.length;
        } else {
            buffer = ByteBuffer.wrap(data, 8, payloadLength).order(ByteOrder.LITTLE_ENDIAN);
        }

        return readTag();
    }

    public int getHeaderVersion() {
        return headerVersion;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    private NbtTag readTag() throws IOException {
        if (!buffer.hasRemaining()) {
            return null;
        }

        byte type = buffer.get();
        if (type == NbtTag.TAG_END) {
            return new NbtTag(NbtTag.TAG_END, "", null);
        }

        String name = readString();
        Object value = readPayload(type);
        
        return new NbtTag(type, name, value);
    }

    private Object readPayload(byte type) throws IOException {
        return switch (type) {
            case NbtTag.TAG_BYTE -> buffer.get();
            case NbtTag.TAG_SHORT -> buffer.getShort();
            case NbtTag.TAG_INT -> buffer.getInt();
            case NbtTag.TAG_LONG -> buffer.getLong();
            case NbtTag.TAG_FLOAT -> buffer.getFloat();
            case NbtTag.TAG_DOUBLE -> buffer.getDouble();
            case NbtTag.TAG_BYTE_ARRAY -> readByteArray();
            case NbtTag.TAG_STRING -> readString();
            case NbtTag.TAG_LIST -> readList();
            case NbtTag.TAG_COMPOUND -> readCompound();
            case NbtTag.TAG_INT_ARRAY -> readIntArray();
            case NbtTag.TAG_LONG_ARRAY -> readLongArray();
            default -> throw new IOException("Unknown tag type: " + type);
        };
    }

    private String readString() throws IOException {
        int length = buffer.getShort() & 0xFFFF;
        if (length == 0) return "";
        
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readByteArray() throws IOException {
        int length = buffer.getInt();
        if (length < 0 || length > MAX_ARRAY_SIZE) {
            throw new IOException("Invalid byte array length: " + length);
        }
        if (length > buffer.remaining()) {
            throw new IOException("Byte array length exceeds buffer: " + length);
        }
        byte[] array = new byte[length];
        buffer.get(array);
        return array;
    }

    private int[] readIntArray() throws IOException {
        int length = buffer.getInt();
        if (length < 0 || length > MAX_ARRAY_SIZE / 4) {
            throw new IOException("Invalid int array length: " + length);
        }
        if (length * 4 > buffer.remaining()) {
            throw new IOException("Int array length exceeds buffer: " + length);
        }
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = buffer.getInt();
        }
        return array;
    }

    private long[] readLongArray() throws IOException {
        int length = buffer.getInt();
        if (length < 0 || length > MAX_ARRAY_SIZE / 8) {
            throw new IOException("Invalid long array length: " + length);
        }
        if (length * 8 > buffer.remaining()) {
            throw new IOException("Long array length exceeds buffer: " + length);
        }
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = buffer.getLong();
        }
        return array;
    }

    private static final int MAX_LIST_SIZE = 65536;
    private static final int MAX_ARRAY_SIZE = 1024 * 1024;

    private List<NbtTag> readList() throws IOException {
        byte listType = buffer.get();
        int length = buffer.getInt();

        if (length < 0 || length > MAX_LIST_SIZE) {
            throw new IOException("Invalid list length: " + length);
        }
        
        List<NbtTag> list = new ArrayList<>(Math.min(length, 1024));
        for (int i = 0; i < length; i++) {
            Object value = readPayload(listType);
            list.add(new NbtTag(listType, "", value));
        }
        return list;
    }

    private Map<String, NbtTag> readCompound() throws IOException {
        Map<String, NbtTag> compound = new LinkedHashMap<>();
        
        while (buffer.hasRemaining()) {
            byte type = buffer.get();
            if (type == NbtTag.TAG_END) {
                break;
            }
            
            String name = readString();
            Object value = readPayload(type);
            compound.put(name, new NbtTag(type, name, value));
        }
        
        return compound;
    }

    private byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read != data.length) {
                throw new IOException("Could not read entire file");
            }
            return data;
        }
    }
}