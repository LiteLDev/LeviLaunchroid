package com.microsoft.xbox.toolkit;


public class XLEMemoryCacheEntry<V> {
    private final int byteCount;
    private final V data;

    public XLEMemoryCacheEntry(V v, int i) {
        if (v == null) {
            throw new IllegalArgumentException("data");
        } else if (i > 0) {
            this.data = v;
            this.byteCount = i;
        } else {
            throw new IllegalArgumentException("byteCount");
        }
    }

    public int getByteCount() {
        return this.byteCount;
    }

    public V getValue() {
        return this.data;
    }
}
