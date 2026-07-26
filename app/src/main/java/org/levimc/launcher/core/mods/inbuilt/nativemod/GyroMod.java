package org.levimc.launcher.core.mods.inbuilt.nativemod;

public class GyroMod {

    public static boolean init() {
        if (!InbuiltModsNative.loadLibrary()) {
            return false;
        }
        return nativeInit();
    }

    public static native void nativePreResolve();
    public static native boolean nativeInit();
    public static native void nativeSetEnabled(boolean enabled);
    public static native void nativeUpdateDelta(float deltaYaw, float deltaPitch);
    public static native void nativeSetSensitivityX(float sensitivity);
    public static native void nativeSetSensitivityY(float sensitivity);
    public static native void nativeSetInvertX(boolean invert);
    public static native void nativeSetInvertY(boolean invert);
    public static native void nativeSetDeadzone(float deadzone);
    public static native boolean nativeIsInitialized();
    public static native boolean nativeIsEnabled();
}
