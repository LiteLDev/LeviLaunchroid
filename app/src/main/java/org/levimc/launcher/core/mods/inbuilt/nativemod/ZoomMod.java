package org.levimc.launcher.core.mods.inbuilt.nativemod;

public class ZoomMod {

    public static boolean init() {
        if (!InbuiltModsNative.loadLibrary()) {
            return false;
        }
        return nativeInit();
    }

    public static native boolean nativeInit();
    public static native void nativeOnKeyDown();
    public static native void nativeOnKeyUp();
    public static native void nativeOnScroll(float delta);

    public static native void nativeSetZoomLevel(long level);
}
