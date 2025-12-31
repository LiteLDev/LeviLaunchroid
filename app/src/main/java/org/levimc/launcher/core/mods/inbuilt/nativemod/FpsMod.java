package org.levimc.launcher.core.mods.inbuilt.nativemod;

public class FpsMod {

    public static boolean init() {
        if (!InbuiltModsNative.loadLibrary()) {
            return false;
        }
        return nativeInit();
    }

    public static native boolean nativeInit();
    public static native int nativeGetFps();
    public static native boolean nativeIsInitialized();
}
