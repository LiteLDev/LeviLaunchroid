package com.microsoft.xbox.toolkit;

import android.app.ActivityManager;
import android.os.Debug;

import com.microsoft.xboxtcui.XboxTcuiSdk;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class MemoryMonitor {
    public static final int KB_TO_BYTES = 1024;
    public static final int MB_TO_BYTES = 1048576;
    public static final int MB_TO_KB = 1024;
    private static final MemoryMonitor instance = new MemoryMonitor();
    private final Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();

    private MemoryMonitor() {
    }

    public static MemoryMonitor instance() {
        return instance;
    }

    public static synchronized int getTotalPss() {
        int totalPss;
        synchronized (MemoryMonitor.class) {
            Debug.getMemoryInfo(instance.memoryInfo);
            totalPss = instance.memoryInfo.getTotalPss();
        }
        return totalPss;
    }

    public synchronized int getDalvikFreeMb() {
        return getDalvikFreeKb() / 1024;
    }

    public synchronized int getDalvikFreeKb() {
        Debug.getMemoryInfo(this.memoryInfo);
        return (((ActivityManager) XboxTcuiSdk.getSystemService("activity")).getMemoryClass() * 1024) - getDalvikUsedKb();
    }

    public synchronized int getDalvikUsedKb() {
        Debug.getMemoryInfo(this.memoryInfo);
        return this.memoryInfo.dalvikPss;
    }

    public synchronized int getUsedKb() {
        Debug.getMemoryInfo(this.memoryInfo);
        return this.memoryInfo.dalvikPss + this.memoryInfo.nativePss;
    }

    public int getMemoryClass() {
        return ((ActivityManager) XboxTcuiSdk.getSystemService("activity")).getLargeMemoryClass();
    }
}
