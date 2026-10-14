package org.fmod;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class FMOD {
    private static final int TYPE_REMOTE_SUBMIX = 25;

    @SuppressLint("StaticFieldLeak")
    private static volatile Context gContext;
    private static volatile PluginAudioDeviceCallback gPluginAudioDeviceCallback;
    private static final PluginBroadcastReceiver gPluginBroadcastReceiver = new PluginBroadcastReceiver();
    private static boolean gReceiverRegistered;

    public static native void OutputAAudioHeadphonesChanged();

    public static native void SetInputEnumerationChanged();

    public static native void SetOutputEnumerationChanged();

    public static synchronized void init(Context context) {
        if (gContext == context) {
            return;
        }
        close();
        gContext = context;
        if (context == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                context.registerReceiver(gPluginBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(gPluginBroadcastReceiver, filter);
            }
            gReceiverRegistered = true;
        } catch (RuntimeException error) {
            Log.w("fmod", "Failed to register headset receiver", error);
        }

        AudioManager audioManager = getAudioManager();
        if (audioManager != null && Build.VERSION.SDK_INT >= 23) {
            PluginAudioDeviceCallback callback = new PluginAudioDeviceCallback(getAudioDevices(AudioManager.GET_DEVICES_ALL));
            gPluginAudioDeviceCallback = callback;
            try {
                audioManager.registerAudioDeviceCallback(callback, null);
            } catch (RuntimeException error) {
                gPluginAudioDeviceCallback = null;
                Log.w("fmod", "Failed to register audio device callback", error);
            }
        }
    }

    public static synchronized void close() {
        Context context = gContext;
        PluginAudioDeviceCallback callback = gPluginAudioDeviceCallback;
        gContext = null;
        gPluginAudioDeviceCallback = null;
        if (context == null) {
            return;
        }

        if (gReceiverRegistered) {
            gReceiverRegistered = false;
            try {
                context.unregisterReceiver(gPluginBroadcastReceiver);
            } catch (IllegalArgumentException error) {
                Log.w("fmod", "Failed to unregister headset receiver", error);
            }
        }

        if (callback != null && Build.VERSION.SDK_INT >= 23) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.unregisterAudioDeviceCallback(callback);
            }
        }
    }

    public static boolean checkInit() {
        return gContext != null;
    }

    @androidx.annotation.Nullable
    public static AssetManager getAssetManager() {
        Context context = gContext;
        return context == null ? null : context.getAssets();
    }

    public static boolean supportsSpatial() {
        if (Build.VERSION.SDK_INT < 32) {
            return false;
        }
        AudioManager audioManager = getAudioManager();
        if (audioManager == null) {
            return false;
        }
        Spatializer spatializer = audioManager.getSpatializer();
        if (spatializer == null) {
            return false;
        }
        boolean supported = spatializer.getImmersiveAudioLevel() != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE;
        boolean available = spatializer.isAvailable();
        boolean enabled = spatializer.isEnabled();
        Log.i("fmod", "FMOD::supportsSpatial                    : Supports Spatial = " + supported + ", Spatial available = " + available + ", Spatial enabled = " + enabled);
        return supported && available && enabled;
    }

    public static boolean supportsLowLatency() {
        int outputBlockSize = getOutputBlockSize();
        boolean lowLatency = lowLatencyFlag();
        boolean proAudio = proAudioFlag();
        boolean acceptableBlockSize = outputBlockSize > 0 && outputBlockSize <= 1024;
        boolean bluetoothOn = isBluetoothOn();
        Log.i("fmod", "FMOD::supportsLowLatency                 : Low latency = " + lowLatency + ", Pro Audio = " + proAudio + ", Bluetooth On = " + bluetoothOn + ", Acceptable Block Size = " + acceptableBlockSize + " (" + outputBlockSize + ")");
        return acceptableBlockSize && lowLatency && !bluetoothOn;
    }

    public static boolean lowLatencyFlag() {
        Context context = gContext;
        return context != null && context.getPackageManager().hasSystemFeature("android.hardware.audio.low_latency");
    }

    public static boolean proAudioFlag() {
        Context context = gContext;
        return context != null && context.getPackageManager().hasSystemFeature("android.hardware.audio.pro");
    }

    public static boolean supportsAAudio() {
        return Build.VERSION.SDK_INT >= 27;
    }

    public static int getOutputSampleRate() {
        return getAudioProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
    }

    public static int getOutputBlockSize() {
        return getAudioProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
    }

    private static int getAudioProperty(String name) {
        AudioManager audioManager = getAudioManager();
        if (audioManager == null || Build.VERSION.SDK_INT < 17) {
            return 0;
        }
        String property = audioManager.getProperty(name);
        if (property == null) {
            return 0;
        }
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static AudioManager getAudioManager() {
        Context context = gContext;
        return context == null ? null : (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static boolean isBluetoothOn() {
        AudioManager audioManager = getAudioManager();
        if (audioManager == null) {
            return false;
        }
        try {
            return audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn();
        } catch (SecurityException error) {
            return false;
        }
    }

    public static AudioDeviceInfo[] filterDevices(AudioDeviceInfo[] devices) {
        ArrayList<AudioDeviceInfo> filtered = new ArrayList<>();
        if (devices != null) {
            for (AudioDeviceInfo device : devices) {
                if (device != null && device.getType() != TYPE_REMOTE_SUBMIX) {
                    filtered.add(device);
                }
            }
        }
        return filtered.toArray(new AudioDeviceInfo[filtered.size()]);
    }

    public static AudioDeviceInfo[] getAudioDevices(int direction) {
        AudioManager audioManager = getAudioManager();
        if (audioManager != null && Build.VERSION.SDK_INT >= 23) {
            return audioManager.getDevices(direction);
        }
        return new AudioDeviceInfo[0];
    }

    public static int fileDescriptorFromUri(String uri) {
        Context context = gContext;
        if (context == null || uri == null) {
            return -1;
        }
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(Uri.parse(uri), "r")) {
            return descriptor == null ? -1 : descriptor.detachFd();
        } catch (IOException | SecurityException | IllegalArgumentException error) {
            return -1;
        }
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

    static class PluginBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (gContext != null && intent != null && Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                try {
                    OutputAAudioHeadphonesChanged();
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
        }
    }

    static class PluginAudioDeviceCallback extends AudioDeviceCallback {
        private final HashSet<Integer> deviceSet = new HashSet<>();

        public PluginAudioDeviceCallback(AudioDeviceInfo[] devices) {
            for (AudioDeviceInfo device : filterDevices(devices)) {
                deviceSet.add(device.getId());
            }
        }

        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] devices) {
            updateDevices(devices, true);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
            updateDevices(devices, false);
        }

        private void updateDevices(AudioDeviceInfo[] devices, boolean added) {
            if (gContext == null || gPluginAudioDeviceCallback != this) {
                return;
            }
            boolean inputChanged = false;
            boolean outputChanged = false;
            for (AudioDeviceInfo device : filterDevices(devices)) {
                boolean changed = added ? deviceSet.add(device.getId()) : deviceSet.remove(device.getId());
                if (changed) {
                    inputChanged |= device.isSource();
                    outputChanged |= device.isSink();
                }
            }
            if (inputChanged) {
                try {
                    SetInputEnumerationChanged();
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
            if (outputChanged) {
                try {
                    SetOutputEnumerationChanged();
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
        }
    }
}
