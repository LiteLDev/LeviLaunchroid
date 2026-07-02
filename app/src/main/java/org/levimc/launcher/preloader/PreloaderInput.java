package org.levimc.launcher.preloader;

public class PreloaderInput {
    public static native boolean nativeOnTouch(int action, int pointerId, float x, float y);
    public static native boolean nativeOnKeyEvent(int keyCode, int unicodeChar, boolean isKeyDown);
    public static native boolean nativeOnMouse(int button, boolean isDown);
    public static native void nativeSetActivity(Object activity);
    public static native void nativeClearActivity();
    public static native boolean nativeIsPauseMenuOpen();
    public static native boolean nativeIsHudScreenOpen();
    public static native boolean nativeIsShowingMenu();

    public static boolean isPauseMenuOpen() {
        try {
            return nativeIsPauseMenuOpen();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean isHudScreenOpen() {
        try {
            return nativeIsHudScreenOpen();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean isShowingMenu() {
        try {
            return nativeIsShowingMenu();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onTouch(int action, int pointerId, float x, float y) {
        try {
            return nativeOnTouch(action, pointerId, x, y);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onKeyEvent(int keyCode, int unicodeChar, boolean isKeyDown) {
        try {
            return nativeOnKeyEvent(keyCode, unicodeChar, isKeyDown);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onMouse(int button, boolean isDown) {
        try {
            return nativeOnMouse(button, isDown);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static void setActivity(Object activity) {
        try {
            nativeSetActivity(activity);
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public static void clearActivity() {
        try {
            nativeClearActivity();
        } catch (UnsatisfiedLinkError e) {
        }
    }
}

