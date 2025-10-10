package com.microsoft.xbox.toolkit;

import android.util.LruCache;

import org.jetbrains.annotations.NotNull;


public class XLEMemoryCache<K, V> {
    private final LruCache<K, XLEMemoryCacheEntry<V>> lruCache;
    private final int maxFileSizeBytes;
    private int itemCount = 0;

    public XLEMemoryCache(int sizeInBytes, int maxFileSizeInBytes) {
        if (sizeInBytes < 0) {
            throw new IllegalArgumentException("sizeInBytes");
        } else if (maxFileSizeInBytes < 0) {
            throw new IllegalArgumentException("maxFileSizeInBytes");
        } else {
            maxFileSizeBytes = maxFileSizeInBytes;
            if (sizeInBytes == 0) {
            } else {
                new LruCache<K, XLEMemoryCacheEntry<V>>(sizeInBytes) {
                    public int sizeOf(K k, XLEMemoryCacheEntry<V> value) {
                        return value.getByteCount();
                    }

                    public void entryRemoved(boolean evicted, K k, XLEMemoryCacheEntry<V> xLEMemoryCacheEntry, XLEMemoryCacheEntry<V> xLEMemoryCacheEntry2) {
                        access(XLEMemoryCache.this);
                    }
                };
            }
        }
        lruCache = null;
    }

    static int access(@NotNull XLEMemoryCache x0) {
        int i = x0.itemCount - 1;
        x0.itemCount = i;
        return i;
    }

    public int getBytesCurrent() {
        if (lruCache == null) {
            return 0;
        }
        return lruCache.size();
    }

    public int getItemsInCache() {
        return itemCount;
    }

    public int getBytesFree() {
        if (lruCache == null) {
            return 0;
        }
        return lruCache.maxSize() - lruCache.size();
    }

    public boolean add(K filename, V data, int fileByteCount) {
        if (fileByteCount > maxFileSizeBytes || lruCache == null) {
            return false;
        }
        XLEMemoryCacheEntry<V> entry = new XLEMemoryCacheEntry<>(data, fileByteCount);
        itemCount++;
        lruCache.put(filename, entry);
        return true;
    }

    public V get(K filename) {
        XLEMemoryCacheEntry<V> entry;
        if (lruCache == null || (entry = lruCache.get(filename)) == null) {
            return null;
        }
        return entry.getValue();
    }
}