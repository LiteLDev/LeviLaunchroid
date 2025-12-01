package org.levimc.launcher.core.content.nbt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NbtTag {
    public static final byte TAG_END = 0;
    public static final byte TAG_BYTE = 1;
    public static final byte TAG_SHORT = 2;
    public static final byte TAG_INT = 3;
    public static final byte TAG_LONG = 4;
    public static final byte TAG_FLOAT = 5;
    public static final byte TAG_DOUBLE = 6;
    public static final byte TAG_BYTE_ARRAY = 7;
    public static final byte TAG_STRING = 8;
    public static final byte TAG_LIST = 9;
    public static final byte TAG_COMPOUND = 10;
    public static final byte TAG_INT_ARRAY = 11;
    public static final byte TAG_LONG_ARRAY = 12;

    private byte type;
    private String name;
    private Object value;

    public NbtTag(byte type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public byte getType() { return type; }
    public String getName() { return name; }
    public Object getValue() { return value; }
    
    public void setName(String name) { this.name = name; }
    public void setValue(Object value) { this.value = value; }

    public byte getByte() { return value instanceof Number ? ((Number) value).byteValue() : 0; }
    public short getShort() { return value instanceof Number ? ((Number) value).shortValue() : 0; }
    public int getInt() { return value instanceof Number ? ((Number) value).intValue() : 0; }
    public long getLong() { return value instanceof Number ? ((Number) value).longValue() : 0; }
    public float getFloat() { return value instanceof Number ? ((Number) value).floatValue() : 0; }
    public double getDouble() { return value instanceof Number ? ((Number) value).doubleValue() : 0; }
    public String getString() { return value instanceof String ? (String) value : ""; }
    public byte[] getByteArray() { return value instanceof byte[] ? (byte[]) value : new byte[0]; }
    public int[] getIntArray() { return value instanceof int[] ? (int[]) value : new int[0]; }
    public long[] getLongArray() { return value instanceof long[] ? (long[]) value : new long[0]; }

    @SuppressWarnings("unchecked")
    public Map<String, NbtTag> getCompound() {
        return value instanceof Map ? (Map<String, NbtTag>) value : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public List<NbtTag> getList() {
        return value instanceof List ? (List<NbtTag>) value : new ArrayList<>();
    }

    public NbtTag getTag(String key) {
        if (type == TAG_COMPOUND) {
            return getCompound().get(key);
        }
        return null;
    }

    public void putTag(String key, NbtTag tag) {
        if (type == TAG_COMPOUND) {
            getCompound().put(key, tag);
        }
    }

    public static String getTypeName(byte type) {
        return switch (type) {
            case TAG_END -> "End";
            case TAG_BYTE -> "Byte";
            case TAG_SHORT -> "Short";
            case TAG_INT -> "Int";
            case TAG_LONG -> "Long";
            case TAG_FLOAT -> "Float";
            case TAG_DOUBLE -> "Double";
            case TAG_BYTE_ARRAY -> "ByteArray";
            case TAG_STRING -> "String";
            case TAG_LIST -> "List";
            case TAG_COMPOUND -> "Compound";
            case TAG_INT_ARRAY -> "IntArray";
            case TAG_LONG_ARRAY -> "LongArray";
            default -> "Unknown";
        };
    }

    public boolean isNumeric() {
        return type >= TAG_BYTE && type <= TAG_DOUBLE;
    }

    public boolean isEditable() {
        return type != TAG_END && type != TAG_BYTE_ARRAY && 
               type != TAG_INT_ARRAY && type != TAG_LONG_ARRAY;
    }

    @Override
    public String toString() {
        return String.format("NbtTag{type=%s, name='%s', value=%s}", 
            getTypeName(type), name, formatValue());
    }

    private String formatValue() {
        if (value == null) return "null";
        if (type == TAG_COMPOUND) return "{" + getCompound().size() + " entries}";
        if (type == TAG_LIST) return "[" + getList().size() + " items]";
        if (type == TAG_BYTE_ARRAY) return "byte[" + getByteArray().length + "]";
        if (type == TAG_INT_ARRAY) return "int[" + getIntArray().length + "]";
        if (type == TAG_LONG_ARRAY) return "long[" + getLongArray().length + "]";
        return value.toString();
    }
}