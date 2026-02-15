package org.levimc.launcher.preloader;

public class PreloaderInput {
    public static native boolean nativeOnTouch(int action, int pointerId, float x, float y);

    public static boolean onTouch(int action, int pointerId, float x, float y) {
        try {
            return nativeOnTouch(action, pointerId, x, y);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
