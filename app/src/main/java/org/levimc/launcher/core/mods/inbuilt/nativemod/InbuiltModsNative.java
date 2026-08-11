package org.levimc.launcher.core.mods.inbuilt.nativemod;

public class InbuiltModsNative {
    private static boolean libraryLoaded = false;

    public static synchronized boolean loadLibrary() {
        if (libraryLoaded) return true;
        try {
            System.loadLibrary("inbuiltmods");
            libraryLoaded = true;
            return true;
        } catch (UnsatisfiedLinkError ignored) {
            return false;
        }
    }

    public static boolean isLoaded() {
        return libraryLoaded;
    }
}
