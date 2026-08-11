package org.levimc.launcher.core.mods.inbuilt.nativemod;

public final class PojavControlsMod {
    private static boolean initialized;

    private PojavControlsMod() {}

    public static synchronized boolean initialize() {
        if (initialized) return true;
        if (!InbuiltModsNative.loadLibrary()) return false;
        initialized = nativeInit();
        return initialized;
    }

    public static boolean setEnabled(boolean enabled) {
        if (enabled && !initialize()) return false;
        if (initialized) nativeSetEnabled(enabled);
        return !enabled || initialized;
    }

    public static native void nativeSendKey(int keyCode, boolean down);
    public static native void nativeSendMouseButton(int androidButton, boolean down);
    public static native void nativeSendScroll(float vertical);
    public static native void nativeSendLookDelta(float deltaX, float deltaY);
    public static native void nativeSendPointer(float x, float y);

    private static native boolean nativeInit();
    private static native void nativeSetEnabled(boolean enabled);
}
