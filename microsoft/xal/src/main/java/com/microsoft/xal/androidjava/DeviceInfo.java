package com.microsoft.xal.androidjava;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import org.jetbrains.annotations.NotNull;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class DeviceInfo {
    public static String GetOsVersion() {
        return Build.VERSION.RELEASE;
    }

    @NotNull
    public static String GetDeviceId(@NotNull Context context) {
        String paddedId;
        @SuppressLint("HardwareIds") String deviceId = Settings.Secure.getString(context.getContentResolver(), "android_id");
        int[] chunkSizes = {8, 4, 4, 4, 12};
        int idLength = deviceId.length();
        int index = 0;
        if (idLength < 32) {
            paddedId = String.format("%0" + (32 - idLength) + "d", 0) + deviceId;
        } else {
            paddedId = deviceId;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunkSizes.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            int chunkSize = chunkSizes[i];
            sb.append(paddedId.substring(index, index + chunkSize));
            index += chunkSize;
        }
        return sb.toString();
    }
}
