package org.levimc.launcher.core.mods.inbuilt.nativemod;

public final class MoreButtonsMod {
    private static boolean initialized;

    private MoreButtonsMod() {}

    public static synchronized boolean initialize() {
        if (initialized) return true;
        if (!InbuiltModsNative.loadLibrary()) return false;
        initialized = nativeInit();
        return initialized;
    }

    public static boolean sendKey(int keyCode, boolean down) {
        if (keyCode <= 0 || keyCode >= 256) return false;
        if (!initialize()) return false;
        nativeSendKey(keyCode, down);
        return true;
    }


    private static native boolean nativeInit();
    private static native void nativeSendKey(int keyCode, boolean down);
}
