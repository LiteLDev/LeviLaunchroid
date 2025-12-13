package org.levimc.launcher.core.mods.memoryeditor;

public class MemoryEditorNative {
    static {
        System.loadLibrary("memoryeditor");
    }

    public static native void nativeInit();
    public static native void nativeRefreshRegions();
    public static native int nativeGetRegionCount();
    
    public static native void nativeSearchByte(byte value, boolean isXor, long xorKey);
    public static native void nativeSearchWord(short value, boolean isXor, long xorKey);
    public static native void nativeSearchDword(int value, boolean isXor, long xorKey);
    public static native void nativeSearchQword(long value, boolean isXor, long xorKey);
    public static native void nativeSearchFloat(float value, boolean isXor, long xorKey);
    public static native void nativeSearchDouble(double value, boolean isXor, long xorKey);
    
    public static native void nativeFilterByte(byte value, int condition, boolean isXor, long xorKey);
    public static native void nativeFilterWord(short value, int condition, boolean isXor, long xorKey);
    public static native void nativeFilterDword(int value, int condition, boolean isXor, long xorKey);
    public static native void nativeFilterQword(long value, int condition, boolean isXor, long xorKey);
    public static native void nativeFilterFloat(float value, int condition, boolean isXor, long xorKey);
    public static native void nativeFilterDouble(double value, int condition, boolean isXor, long xorKey);
    
    public static native int nativeGetResultCount();
    public static native long[] nativeGetResults(int offset, int count);
    public static native void nativeClearResults();
    
    public static native long nativeReadByte(long address);
    public static native long nativeReadWord(long address);
    public static native long nativeReadDword(long address);
    public static native long nativeReadQword(long address);
    public static native float nativeReadFloat(long address);
    public static native double nativeReadDouble(long address);
    
    public static native boolean nativeWriteByte(long address, byte value);
    public static native boolean nativeWriteWord(long address, short value);
    public static native boolean nativeWriteDword(long address, int value);
    public static native boolean nativeWriteQword(long address, long value);
    public static native boolean nativeWriteFloat(long address, float value);
    public static native boolean nativeWriteDouble(long address, double value);
    
    public static native int nativeGetSearchType();
    public static native void nativeClose();
}
