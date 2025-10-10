package com.microsoft.xbox.idp.util;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.microsoft.xbox.idp.toolkit.BitmapLoader;


public class BitmapLoaderCache implements BitmapLoader.Cache {
    private final LruCache<Object, Bitmap> cache;

    public BitmapLoaderCache(int i) {
        this.cache = new LruCache<>(i);
    }

    public Bitmap get(Object obj) {
        return this.cache.get(obj);
    }

    public Bitmap put(Object obj, Bitmap bitmap) {
        return this.cache.put(obj, bitmap);
    }

    public Bitmap remove(Object obj) {
        return this.cache.remove(obj);
    }

    public void clear() {
        this.cache.evictAll();
    }
}
