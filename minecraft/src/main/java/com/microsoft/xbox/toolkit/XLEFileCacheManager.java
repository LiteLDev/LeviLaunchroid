package com.microsoft.xbox.toolkit;

import com.microsoft.xbox.toolkit.system.SystemUtil;
import com.microsoft.xboxtcui.XboxTcuiSdk;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;


public class XLEFileCacheManager {
    private static final HashMap<String, XLEFileCache> sAllCaches = new HashMap<>();
    private static final HashMap<XLEFileCache, File> sCacheRootDirMap = new HashMap<>();
    public static XLEFileCache emptyFileCache = new XLEFileCache();

    public static synchronized XLEFileCache createCache(String str, int i) {
        XLEFileCache createCache;
        synchronized (XLEFileCacheManager.class) {
            createCache = createCache(str, i, true);
        }
        return createCache;
    }

    public static synchronized XLEFileCache createCache(String str, int i, boolean z) {
        synchronized (XLEFileCacheManager.class) {
            if (i > 0) {
                if (str != null) {
                    if (str.length() > 0) {
                        XLEFileCache xLEFileCache = sAllCaches.get(str);
                        if (xLEFileCache == null) {
                            if (!z) {
                                XLEFileCache xLEFileCache2 = emptyFileCache;
                                return xLEFileCache2;
                            } else if (!SystemUtil.isSDCardAvailable()) {
                                XLEFileCache xLEFileCache3 = emptyFileCache;
                                return xLEFileCache3;
                            } else {
                                xLEFileCache = new XLEFileCache(str, i);
                                File file = new File(XboxTcuiSdk.getActivity().getCacheDir(), str);
                                if (!file.exists()) {
                                    file.mkdirs();
                                }
                                xLEFileCache.size = file.list().length;
                                sAllCaches.put(str, xLEFileCache);
                                sCacheRootDirMap.put(xLEFileCache, file);
                            }
                        } else if (xLEFileCache.maxFileNumber != i) {
                            throw new IllegalArgumentException("The same subDirectory with different maxFileNumber already exist.");
                        }
                    }
                }
                throw new IllegalArgumentException("subDirectory must be not null and at least one character length");
            }
            throw new IllegalArgumentException("maxFileNumber must be > 0");
        }
    }

    static File getCacheRootDir(XLEFileCache xLEFileCache) {
        return sCacheRootDirMap.get(xLEFileCache);
    }

    public static @NotNull String getCacheStatus() {
        return sAllCaches.values().toString();
    }
}
