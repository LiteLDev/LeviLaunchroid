package org.levimc.launcher.core.mods.inbuilt.nativemod;

public class SnaplookMod {

    public static boolean init() {
        if (!InbuiltModsNative.loadLibrary()) {
            return false;
        }
        return nativeInit();
    }

    public static native boolean nativeInit();
    public static native void nativeOnKeyDown();
    public static native void nativeOnKeyUp();
}
