package org.levimc.launcher.core.mods.inbuilt.nativemod;

import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HotbarSlotMod {
    private static volatile boolean initialized;
    private static volatile boolean itemIconsAvailable;
    private static boolean initAttempted;
    private static final AtomicBoolean initQueued = new AtomicBoolean();

    private HotbarSlotMod() {}

    public static synchronized boolean initialize() {
        if (initialized) return true;
        if (initAttempted) return false;
        if (!InbuiltModsNative.loadLibrary()) return false;
        initAttempted = true;
        boolean ready = nativeInit();
        itemIconsAvailable = ready && nativeItemIconsAvailable();
        initialized = ready;
        if (!ready || !itemIconsAvailable) {
            Log.w("HotbarSlot", "Item icon renderer unavailable for this Minecraft build; keeping numbered buttons.");
        }
        return ready;
    }

    private static void initializeInBackground() {
        if (initialized || !initQueued.compareAndSet(false, true)) return;
        Thread worker = new Thread(() -> {
            try {
                initialize();
            } catch (UnsatisfiedLinkError error) {
                Log.w("HotbarSlot", "Rebuild libinbuiltmods.so together with the launcher changes.", error);
            }
        }, "HotbarSlot-init");
        worker.setDaemon(true);
        worker.start();
    }

    public static void setEnabled(boolean enabled) {
        if (enabled && !InbuiltModsNative.loadLibrary()) return;
        if (!InbuiltModsNative.isLoaded()) return;
        nativeSetEnabled(enabled);
        if (enabled) initializeInBackground();
    }

    public static void setItemIconsEnabled(boolean enabled) {
        if (enabled && !InbuiltModsNative.loadLibrary()) return;
        if (!InbuiltModsNative.isLoaded()) return;
        nativeSetItemIconsEnabled(enabled);
        if (enabled) initializeInBackground();
    }

    public static boolean areItemIconsAvailable() {
        return itemIconsAvailable;
    }

    public static void setOverlayEnabled(boolean enabled) {
        if (enabled && !InbuiltModsNative.loadLibrary()) return;
        if (!InbuiltModsNative.isLoaded()) return;
        nativeSetOverlayEnabled(enabled);
        if (enabled) initializeInBackground();
    }

    public static int getGameplayVisibilityState() {
        return initialized ? nativeGameplayVisibilityState() : -1;
    }

    public static void setSlotState(int slot, float x, float y, float width, float height,
                                    float surfaceWidth, float surfaceHeight, float alpha,
                                    boolean visible, boolean pressed) {
        if (!InbuiltModsNative.isLoaded()) return;
        nativeSetSlotState(slot, x, y, width, height, surfaceWidth, surfaceHeight, alpha, visible, pressed);
    }

    public static boolean hasItem(int slot) {
        return initialized && nativeHasItem(slot);
    }

    public static int getItemCount(int slot) {
        return initialized ? nativeGetItemCount(slot) : 0;
    }

    public static void copyItemCounts(int[] destination) {
        if (destination == null || destination.length < 9) return;
        if (initialized) nativeCopyItemCounts(destination);
        else Arrays.fill(destination, 0);
    }

    public static void clearSlot(int slot) {
        if (!InbuiltModsNative.isLoaded()) return;
        nativeClearSlot(slot);
    }

    private static native boolean nativeInit();
    private static native void nativeSetOverlayEnabled(boolean enabled);
    private static native int nativeGameplayVisibilityState();
    private static native boolean nativeItemIconsAvailable();
    private static native void nativeCopyItemCounts(int[] destination);
    private static native void nativeSetEnabled(boolean enabled);
    private static native void nativeSetItemIconsEnabled(boolean enabled);
    private static native void nativeSetSlotState(int slot, float x, float y, float width, float height,
                                                   float surfaceWidth, float surfaceHeight, float alpha,
                                                   boolean visible, boolean pressed);
    private static native boolean nativeHasItem(int slot);
    private static native int nativeGetItemCount(int slot);
    private static native void nativeClearSlot(int slot);
}
