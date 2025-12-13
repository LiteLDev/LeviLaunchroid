package org.levimc.launcher.core.mods.memoryeditor;

import java.util.ArrayList;
import java.util.List;

public class MemorySearchEngine {
    private static volatile MemorySearchEngine instance;
    private ValueType currentType = ValueType.DWORD;
    private boolean isXorMode = false;
    private long xorKey = 0;
    private boolean initialized = false;

    private MemorySearchEngine() {}

    public static MemorySearchEngine getInstance() {
        if (instance == null) {
            synchronized (MemorySearchEngine.class) {
                if (instance == null) {
                    instance = new MemorySearchEngine();
                }
            }
        }
        return instance;
    }

    public void init() {
        if (!initialized) {
            MemoryEditorNative.nativeInit();
            initialized = true;
        }
    }

    public void setValueType(ValueType type) {
        this.currentType = type;
        if (type == ValueType.XOR) {
            isXorMode = true;
        }
    }

    public ValueType getValueType() { return currentType; }
    public void setXorMode(boolean xor) { this.isXorMode = xor; }
    public boolean isXorMode() { return isXorMode; }
    public void setXorKey(long key) { this.xorKey = key; }
    public long getXorKey() { return xorKey; }

    public void search(String valueStr) {
        MemoryEditorNative.nativeRefreshRegions();
        try {
            switch (currentType) {
                case BYTE:
                    MemoryEditorNative.nativeSearchByte(Byte.parseByte(valueStr), isXorMode, xorKey);
                    break;
                case WORD:
                    MemoryEditorNative.nativeSearchWord(Short.parseShort(valueStr), isXorMode, xorKey);
                    break;
                case DWORD:
                case XOR:
                    MemoryEditorNative.nativeSearchDword(Integer.parseInt(valueStr), isXorMode, xorKey);
                    break;
                case QWORD:
                    MemoryEditorNative.nativeSearchQword(Long.parseLong(valueStr), isXorMode, xorKey);
                    break;
                case FLOAT:
                    MemoryEditorNative.nativeSearchFloat(Float.parseFloat(valueStr), isXorMode, xorKey);
                    break;
                case DOUBLE:
                    MemoryEditorNative.nativeSearchDouble(Double.parseDouble(valueStr), isXorMode, xorKey);
                    break;
                case AUTO:
                    autoSearch(valueStr);
                    break;
            }
        } catch (NumberFormatException ignored) {}
    }

    private void autoSearch(String valueStr) {
        if (valueStr.contains(".")) {
            MemoryEditorNative.nativeSearchFloat(Float.parseFloat(valueStr), false, 0);
        } else {
            long val = Long.parseLong(valueStr);
            if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                MemoryEditorNative.nativeSearchDword((int) val, false, 0);
            } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
                MemoryEditorNative.nativeSearchDword((int) val, false, 0);
            } else if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                MemoryEditorNative.nativeSearchDword((int) val, false, 0);
            } else {
                MemoryEditorNative.nativeSearchQword(val, false, 0);
            }
        }
    }

    public void filter(String valueStr, SearchCondition condition) {
        try {
            switch (currentType) {
                case BYTE:
                    MemoryEditorNative.nativeFilterByte(Byte.parseByte(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case WORD:
                    MemoryEditorNative.nativeFilterWord(Short.parseShort(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case DWORD:
                case XOR:
                    MemoryEditorNative.nativeFilterDword(Integer.parseInt(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case QWORD:
                    MemoryEditorNative.nativeFilterQword(Long.parseLong(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case FLOAT:
                    MemoryEditorNative.nativeFilterFloat(Float.parseFloat(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case DOUBLE:
                    MemoryEditorNative.nativeFilterDouble(Double.parseDouble(valueStr), condition.getId(), isXorMode, xorKey);
                    break;
                case AUTO:
                    autoFilter(valueStr, condition);
                    break;
            }
        } catch (NumberFormatException ignored) {}
    }

    private void autoFilter(String valueStr, SearchCondition condition) {
        int type = MemoryEditorNative.nativeGetSearchType();
        switch (type) {
            case 0: MemoryEditorNative.nativeFilterByte(Byte.parseByte(valueStr), condition.getId(), false, 0); break;
            case 1: MemoryEditorNative.nativeFilterWord(Short.parseShort(valueStr), condition.getId(), false, 0); break;
            case 2: MemoryEditorNative.nativeFilterDword(Integer.parseInt(valueStr), condition.getId(), false, 0); break;
            case 3: MemoryEditorNative.nativeFilterQword(Long.parseLong(valueStr), condition.getId(), false, 0); break;
            case 4: MemoryEditorNative.nativeFilterFloat(Float.parseFloat(valueStr), condition.getId(), false, 0); break;
            case 5: MemoryEditorNative.nativeFilterDouble(Double.parseDouble(valueStr), condition.getId(), false, 0); break;
        }
    }

    public int getResultCount() {
        return MemoryEditorNative.nativeGetResultCount();
    }

    public List<MemoryAddress> getResults(int offset, int count) {
        long[] addresses = MemoryEditorNative.nativeGetResults(offset, count);
        List<MemoryAddress> results = new ArrayList<>();
        ValueType type = currentType == ValueType.AUTO ? getAutoDetectedType() : currentType;
        if (type == ValueType.XOR) type = ValueType.DWORD;
        for (long addr : addresses) {
            results.add(new MemoryAddress(addr, type));
        }
        return results;
    }

    private ValueType getAutoDetectedType() {
        int type = MemoryEditorNative.nativeGetSearchType();
        return ValueType.fromId(type);
    }

    public void clearResults() {
        MemoryEditorNative.nativeClearResults();
    }

    public void close() {
        MemoryEditorNative.nativeClose();
        initialized = false;
    }
}
