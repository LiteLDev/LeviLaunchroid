package com.mojang.minecraftpe.packagesource;

import android.util.Log;
import androidx.annotation.NonNull;

import java.util.EnumMap;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public abstract class PackageSource {
    static final EnumMap<StringResourceId, String> stringMap = new EnumMap<>(StringResourceId.class);

    public static void setStringResource(int key, String value) {
        setStringResource(StringResourceId.fromInt(key), value);
    }

    public static void setStringResource(StringResourceId id, String value) {
        EnumMap<StringResourceId, String> enumMap = stringMap;
        if (enumMap.containsKey(id)) {
            Log.w("PackageSource", String.format("setStringResource - id: %s already set.", id.name()));
        }
        enumMap.put(id, value);
    }

    public static String getStringResource(StringResourceId id) {
        EnumMap<StringResourceId, String> enumMap = stringMap;
        if (enumMap.containsKey(id)) {
            return enumMap.get(id);
        }
        Log.e("PackageSource", String.format("getStringResource - id: %s is not set.", id.name()));
        return id.name();
    }

    public abstract void abortDownload();

    public abstract void destructor();

    public abstract void downloadFiles(final String filename, final long filesize, final boolean verifyName, final boolean verifySize);

    public abstract String getDownloadDirectoryPath();

    public abstract String getMountPath(String path);

    public abstract void mountFiles(final String filename);

    public abstract void pauseDownload();

    public abstract void resumeDownload();

    public abstract void resumeDownloadOnCell();

    public abstract void unmountFiles(final String filename);

    public enum StringResourceId {
        STATE_UNKNOWN(0),
        STATE_IDLE(1),
        STATE_FETCHING_URL(2),
        STATE_CONNECTING(3),
        STATE_DOWNLOADING(4),
        STATE_COMPLETED(5),
        STATE_PAUSED_NETWORK_UNAVAILABLE(6),
        STATE_PAUSED_NETWORK_SETUP_FAILURE(7),
        STATE_PAUSED_BY_REQUEST(8),
        STATE_PAUSED_WIFI_UNAVAILABLE(9),
        STATE_PAUSED_WIFI_DISABLED(10),
        STATE_PAUSED_ROAMING(11),
        STATE_PAUSED_SDCARD_UNAVAILABLE(12),
        STATE_FAILED_UNLICENSED(13),
        STATE_FAILED_FETCHING_URL(14),
        STATE_FAILED_SDCARD_FULL(15),
        STATE_FAILED_CANCELLED(16),
        STATE_FAILED(17),
        KILOBYTES_PER_SECOND(18),
        TIME_REMAINING_NOTIFICATION(19),
        NOTIFICATIONCHANNEL_NAME(20),
        NOTIFICATIONCHANNEL_DESCRIPTION(21);

        private final int value;

        StringResourceId(int value) {
            this.value = value;
        }

        @NonNull
        public static StringResourceId fromInt(int value) {
            StringResourceId[] values = values();
            for (StringResourceId stringResourceId : values) {
                if (stringResourceId.getValue() == value) {
                    return stringResourceId;
                }
            }
            throw new IllegalArgumentException("Invalid value");
        }

        public int getValue() {
            return this.value;
        }
    }
}
