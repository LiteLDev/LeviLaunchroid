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

    public static native Object[] nativeGetDrawCommands();

    public static class DrawCommand {
        public static final int TYPE_TEXT = 0;
        public static final int TYPE_RECT = 1;
        public static final int TYPE_LINE = 2;

        public int type;
        public float x, y, w, h;
        public int color;
        public float size;
        public String text;
        public String moduleId;
    }

    public static DrawCommand[] getDrawCommands() {
        if (!ModManager.ensurePreloaderLoaded()) return new DrawCommand[0];
        try {
            Object[] arrays = nativeGetDrawCommands();
            if (arrays == null || arrays.length < 6) return new DrawCommand[0];

            int[] types = (int[]) arrays[0];
            float[] rects = (float[]) arrays[1];
            int[] colors = (int[]) arrays[2];
            float[] sizes = (float[]) arrays[3];
            String[] texts = (String[]) arrays[4];
            String[] moduleIds = (String[]) arrays[5];

            if (types == null) return new DrawCommand[0];

            int n = types.length;
            DrawCommand[] cmds = new DrawCommand[n];
            for (int i = 0; i < n; i++) {
                DrawCommand cmd = new DrawCommand();
                cmd.type = types[i];
                cmd.x = rects[i * 4 + 0];
                cmd.y = rects[i * 4 + 1];
                cmd.w = rects[i * 4 + 2];
                cmd.h = rects[i * 4 + 3];
                cmd.color = colors[i];
                cmd.size = sizes[i];
                cmd.text = texts != null ? texts[i] : null;
                cmd.moduleId = moduleIds != null ? moduleIds[i] : null;
                cmds[i] = cmd;
            }
            return cmds;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "nativeGetDrawCommands not available", e);
            return new DrawCommand[0];
        }
    }
}
