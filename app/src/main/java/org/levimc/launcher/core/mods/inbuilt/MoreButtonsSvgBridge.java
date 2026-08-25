package org.levimc.launcher.core.mods.inbuilt;

import android.util.Log;

import org.levimc.launcher.core.mods.ModManager;

import java.nio.charset.StandardCharsets;

public final class MoreButtonsSvgBridge {
    private static final String TAG = "MoreButtonsSvgBridge";
    private MoreButtonsSvgBridge() {}

    private static native byte[] nativeRenderSvgToPng(byte[] svgData, int width, int height);

    public static byte[] renderSvg(String svg, int width, int height) {
        if (svg == null || svg.isEmpty() || !ModManager.ensurePreloaderLoaded()) return null;
        try {
            return nativeRenderSvgToPng(svg.getBytes(StandardCharsets.UTF_8), width, height);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "SVG renderer is not available", e);
            return null;
        }
    }
}
