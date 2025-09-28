package com.microsoft.xbox.idp.util;

import android.util.Log;

import com.microsoft.xbox.idp.toolkit.BitmapLoader;
import com.microsoft.xbox.idp.toolkit.ObjectLoader;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public final class CacheUtil {
    private static final String TAG = CacheUtil.class.getSimpleName();
    private static final BitmapLoader.Cache bitmapCache = new BitmapLoaderCache(50);
    private static final ObjectLoader.Cache objectLoaderCache = new ObjectLoaderCache();

    public static ObjectLoader.Cache getObjectLoaderCache() {
        return objectLoaderCache;
    }

    public static BitmapLoader.Cache getBitmapCache() {
        return bitmapCache;
    }

    public static void clearCaches() {
        Log.d(TAG, "clearCaches");
        synchronized (objectLoaderCache) {
            objectLoaderCache.clear();
        }
        synchronized (bitmapCache) {
            bitmapCache.clear();
        }
    }
}
