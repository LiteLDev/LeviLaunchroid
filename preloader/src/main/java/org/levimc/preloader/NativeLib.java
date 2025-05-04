package org.levimc.preloader;

public class NativeLib {

    // Used to load the 'preloader' library on application startup.
    static {
        System.loadLibrary("preloader");
    }

    /**
     * A native method that is implemented by the 'preloader' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}