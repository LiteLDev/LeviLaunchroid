package org.levimc.launcher.core.mods.inbuilt.nativemod;

public final class AutoSprintMod {
    private static boolean initialized;
    private static boolean initAttempted;

    private AutoSprintMod() {}

    public static synchronized boolean initialize() {
        if (initialized) return true;
        if (initAttempted) return false;
        initAttempted = true;
        if (!InbuiltModsNative.loadLibrary()) return false;
        initialized = nativeInit();
        return initialized;
    }

    public static boolean setEnabled(boolean enabled) {
        if (enabled && !initialize()) return false;
        if (!InbuiltModsNative.isLoaded()) return !enabled;
        if (initialized) nativeSetEnabled(enabled);
        return !enabled || initialized;
    }

    public static boolean isEnabled() {
        return initialized && nativeIsEnabled();
    }

    private static native boolean nativeInit();
    private static native void nativeSetEnabled(boolean enabled);
    private static native boolean nativeIsEnabled();
}
