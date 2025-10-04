package com.microsoft.xbox.toolkit.network;

import com.microsoft.xbox.toolkit.XLEThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XLEThreadPool {
    public static XLEThreadPool biOperationsThreadPool = new XLEThreadPool(false, 1, "XLEPerfMarkerOperationsPool");
    public static XLEThreadPool nativeOperationsThreadPool = new XLEThreadPool(true, 4, "XLENativeOperationsPool");
    public static XLEThreadPool networkOperationsThreadPool = new XLEThreadPool(false, 3, "XLENetworkOperationsPool");
    public static XLEThreadPool textureThreadPool = new XLEThreadPool(false, 1, "XLETexturePool");
    private final ExecutorService executor;
    public String name;

    public XLEThreadPool(boolean singleThread, final int priority, String newname) {
        name = newname;
        ThreadFactory factory = arg0 -> {
            Thread t = new XLEThread(arg0, name);
            t.setDaemon(true);
            t.setPriority(priority);
            return t;
        };
        if (singleThread) {
            executor = Executors.newSingleThreadExecutor(factory);
        } else {
            executor = Executors.newCachedThreadPool(factory);
        }
    }

    public void run(Runnable runnable) {
        this.executor.execute(runnable);
    }
}
