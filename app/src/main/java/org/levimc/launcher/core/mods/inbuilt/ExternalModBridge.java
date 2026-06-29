package org.levimc.launcher.core.mods.inbuilt;

import android.util.Log;

import org.levimc.launcher.core.mods.ModManager;

public class ExternalModBridge {
    private static final String TAG = "ExternalModBridge";

    private static native int nativeGetExternalModCount();
    private static native String nativeGetExternalModInfo(int index);
    private static native void nativeToggleExternalMod(String moduleId, boolean enabled);
    private static native void nativeSetExternalModConfig(String moduleId, String key, String value);

    public static int getExternalModCount() {
        if (!ModManager.ensurePreloaderLoaded()) return 0;
        try {
            return nativeGetExternalModCount();
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "nativeGetExternalModCount not available", e);
            return 0;
        }
    }

    public static String getExternalModInfo(int index) {
        if (!ModManager.ensurePreloaderLoaded()) return "{}";
        try {
            return nativeGetExternalModInfo(index);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "nativeGetExternalModInfo not available", e);
            return "{}";
        }
    }

    public static void toggleExternalMod(String moduleId, boolean enabled) {
        if (!ModManager.ensurePreloaderLoaded()) return;
        try {
            nativeToggleExternalMod(moduleId, enabled);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "nativeToggleExternalMod not available", e);
        }
    }

    public static void setExternalModConfig(String moduleId, String key, String value) {
        if (!ModManager.ensurePreloaderLoaded()) return;
        try {
            nativeSetExternalModConfig(moduleId, key, value);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "nativeSetExternalModConfig not available", e);
        }
    }
}
