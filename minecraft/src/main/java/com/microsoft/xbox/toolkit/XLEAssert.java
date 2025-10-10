package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.NotNull;


public class XLEAssert {
    static final boolean $assertionsDisabled = false;

    public static void assertTrue(String str, boolean z) {
    }

    public static void assertIsUIThread() {
        assertTrue(Thread.currentThread() == ThreadManager.UIThread);
    }

    public static void assertIsNotUIThread() {
        assertTrue(Thread.currentThread() != ThreadManager.UIThread);
    }

    public static void assertTrue(boolean z) {
        assertTrue(null, z);
    }

    public static void assertNotNull(Object obj) {
        assertTrue(null, obj != null);
    }

    public static void assertNull(Object obj) {
        assertTrue(null, obj == null);
    }

    public static void assertNotNull(String str, Object obj) {
        assertTrue(str, obj != null);
    }

    public static void assertFalse(String str, boolean z) {
        assertTrue(str, !z);
    }

    public static void fail(String str) {
        assertTrue(str, false);
    }

    private static @NotNull String getCallerLocation() {
        Class<XLEAssert> cls = XLEAssert.class;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int i = 0;
        while (i < stackTrace.length && (!stackTrace[i].getClassName().equals(cls.getName()) || !stackTrace[i].getMethodName().equals("getCallerLocation"))) {
            i++;
        }
        while (i < stackTrace.length && stackTrace[i].getClassName().equals(cls.getName())) {
            i++;
        }
        return i < stackTrace.length ? stackTrace[i].toString() : "unknown";
    }
}
