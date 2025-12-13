package org.levimc.launcher.core.mods.memoryeditor;

public enum ValueType {
    BYTE(0, "Byte", 1),
    WORD(1, "Word", 2),
    DWORD(2, "Dword", 4),
    QWORD(3, "Qword", 8),
    FLOAT(4, "Float", 4),
    DOUBLE(5, "Double", 8),
    XOR(6, "XOR", 4),
    AUTO(7, "Auto", 0);

    private final int id;
    private final String name;
    private final int size;

    ValueType(int id, String name, int size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getSize() { return size; }

    public static ValueType fromId(int id) {
        for (ValueType type : values()) {
            if (type.id == id) return type;
        }
        return DWORD;
    }
}
