package org.levimc.launcher.core.mods.inbuilt.nativemod;

public final class HotbarSlotMod {
    private static boolean initialized;

    private HotbarSlotMod() {}

    public static synchronized boolean initialize() {
        if (initialized) return true;
        if (!InbuiltModsNative.loadLibrary()) return false;
        initialized = nativeInit();
        return initialized;
    }

    public static void setEnabled(boolean enabled) {
        if (enabled && !initialize()) return;
        if (!InbuiltModsNative.isLoaded()) return;
        nativeSetEnabled(enabled);
    }

    public static void setSlotState(int slot, float x, float y, float width, float height,
                                    float surfaceWidth, float surfaceHeight, float alpha,
                                    boolean visible, boolean pressed) {
        if (!initialize()) return;
        nativeSetSlotState(slot, x, y, width, height, surfaceWidth, surfaceHeight, alpha, visible, pressed);
    }

    public static boolean hasItem(int slot) {
        return initialize() && nativeHasItem(slot);
    }

    public static void clearSlot(int slot) {
        if (!InbuiltModsNative.isLoaded()) return;
        nativeClearSlot(slot);
    }

    private static native boolean nativeInit();
    private static native void nativeSetEnabled(boolean enabled);
    private static native void nativeSetSlotState(int slot, float x, float y, float width, float height,
                                                   float surfaceWidth, float surfaceHeight, float alpha,
                                                   boolean visible, boolean pressed);
    private static native boolean nativeHasItem(int slot);
    private static native void nativeClearSlot(int slot);
}
