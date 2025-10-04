package org.fmod;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public class FMOD {
    @SuppressLint("StaticFieldLeak")
    private static Context gContext;

    public static void init(Context context) {
        gContext = context;
    }

    public static void close() {
        gContext = null;
    }

    public static boolean checkInit() {
        return gContext != null;
    }

    @androidx.annotation.Nullable
    public static AssetManager getAssetManager() {
        Context context = gContext;
        if (context != null) {
            return context.getAssets();
        }
        return null;
    }

    public static boolean supportsLowLatency() {
        if (gContext == null || Build.VERSION.SDK_INT < 5) {
            return false;
        }
        int outputBlockSize = getOutputBlockSize();
        boolean hasSystemFeature = gContext.getPackageManager().hasSystemFeature("android.hardware.audio.low_latency");
        boolean hasSystemFeature2 = gContext.getPackageManager().hasSystemFeature("android.hardware.audio.pro");
        boolean z = outputBlockSize > 0 && outputBlockSize <= 1024;
        boolean isBluetoothOn = isBluetoothOn();
        Log.i("fmod", "FMOD::supportsLowLatency                 : Low latency = " + hasSystemFeature + ", Pro Audio = " + hasSystemFeature2 + ", Bluetooth On = " + isBluetoothOn + ", Acceptable Block Size = " + z + " (" + outputBlockSize + ")");
        return z && hasSystemFeature && !isBluetoothOn;
    }

    public static int getOutputSampleRate() {
        String property;
        if (gContext == null || Build.VERSION.SDK_INT < 17 || (property = ((AudioManager) gContext.getSystemService(Context.AUDIO_SERVICE)).getProperty("android.media.property.OUTPUT_SAMPLE_RATE")) == null) {
            return 0;
        }
        return Integer.parseInt(property);
    }

    public static int getOutputBlockSize() {
        String property;
        if (gContext == null || Build.VERSION.SDK_INT < 17 || (property = ((AudioManager) gContext.getSystemService(Context.AUDIO_SERVICE)).getProperty("android.media.property.OUTPUT_FRAMES_PER_BUFFER")) == null) {
            return 0;
        }
        return Integer.parseInt(property);
    }

    public static boolean isBluetoothOn() {
        Context context = gContext;
        if (context != null) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            return audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn();
        }
        return false;
    }
    public static int[] getDevices(int deviceType) {
        Log.d("fmod", "FMOD::getDevices called with deviceType: " + deviceType);
        return new int[0];
    }
    public static String getDeviceName(int deviceId) {
        Log.d("fmod", "FMOD::getDeviceName called with deviceId: " + deviceId);
        return "Default Device";
    }
    public static int getDeviceType(int deviceId) {
        Log.d("fmod", "FMOD::getDeviceType called with deviceId: " + deviceId);
        return 0;
    }
}