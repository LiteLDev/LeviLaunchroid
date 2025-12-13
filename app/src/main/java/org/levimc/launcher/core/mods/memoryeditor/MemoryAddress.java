package org.levimc.launcher.core.mods.memoryeditor;

public class MemoryAddress {
    private long address;
    private ValueType type;
    private String label;
    private boolean frozen;
    private String frozenValue;

    public MemoryAddress(long address, ValueType type) {
        this.address = address;
        this.type = type;
        this.label = "";
        this.frozen = false;
        this.frozenValue = "";
    }

    public long getAddress() { return address; }
    public void setAddress(long address) { this.address = address; }
    public ValueType getType() { return type; }
    public void setType(ValueType type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public String getFrozenValue() { return frozenValue; }
    public void setFrozenValue(String frozenValue) { this.frozenValue = frozenValue; }

    public String getAddressHex() {
        return String.format("0x%X", address);
    }

    public String readValue() {
        try {
            switch (type) {
                case BYTE: return String.valueOf(MemoryEditorNative.nativeReadByte(address));
                case WORD: return String.valueOf(MemoryEditorNative.nativeReadWord(address));
                case DWORD: return String.valueOf(MemoryEditorNative.nativeReadDword(address));
                case QWORD: return String.valueOf(MemoryEditorNative.nativeReadQword(address));
                case FLOAT: return String.valueOf(MemoryEditorNative.nativeReadFloat(address));
                case DOUBLE: return String.valueOf(MemoryEditorNative.nativeReadDouble(address));
                default: return String.valueOf(MemoryEditorNative.nativeReadDword(address));
            }
        } catch (Exception e) {
            return "???";
        }
    }

    public boolean writeValue(String value) {
        try {
            switch (type) {
                case BYTE: return MemoryEditorNative.nativeWriteByte(address, Byte.parseByte(value));
                case WORD: return MemoryEditorNative.nativeWriteWord(address, Short.parseShort(value));
                case DWORD: return MemoryEditorNative.nativeWriteDword(address, Integer.parseInt(value));
                case QWORD: return MemoryEditorNative.nativeWriteQword(address, Long.parseLong(value));
                case FLOAT: return MemoryEditorNative.nativeWriteFloat(address, Float.parseFloat(value));
                case DOUBLE: return MemoryEditorNative.nativeWriteDouble(address, Double.parseDouble(value));
                default: return MemoryEditorNative.nativeWriteDword(address, Integer.parseInt(value));
            }
        } catch (Exception e) {
            return false;
        }
    }
}
