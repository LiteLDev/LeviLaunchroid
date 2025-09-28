package com.mojang.minecraftpe.packagesource;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public interface PackageSourceListener {
    public static final int DOWNLOADFAILEDREASON_CANCELED = 5;
    public static final int DOWNLOADFAILEDREASON_CONTENTVERIFY_ERROR = 8;
    public static final int DOWNLOADFAILEDREASON_CONTENTVERIFY_MISMATCH = 6;
    public static final int DOWNLOADFAILEDREASON_CONTENTVERIFY_RETRY = 7;
    public static final int DOWNLOADFAILEDREASON_FETCHING_URL = 3;
    public static final int DOWNLOADFAILEDREASON_SDCARD_FULL = 4;
    public static final int DOWNLOADFAILEDREASON_STORAGE_PERMISSION = 1;
    public static final int DOWNLOADFAILEDREASON_UNKNOWN = 0;
    public static final int DOWNLOADFAILEDREASON_UNLICENSED = 2;
    public static final int DOWNLOADPAUSEDREASON_BY_REQUEST = 2;
    public static final int DOWNLOADPAUSEDREASON_NEED_CELLULAR_PERMISSION = 4;
    public static final int DOWNLOADPAUSEDREASON_NEED_WIFI = 6;
    public static final int DOWNLOADPAUSEDREASON_NETWORK_SETUP_FAILURE = 8;
    public static final int DOWNLOADPAUSEDREASON_NETWORK_UNAVAILABLE = 1;
    public static final int DOWNLOADPAUSEDREASON_ROAMING = 7;
    public static final int DOWNLOADPAUSEDREASON_SDCARD_UNAVAILABLE = 9;
    public static final int DOWNLOADPAUSEDREASON_UNKNOWN = 0;
    public static final int DOWNLOADPAUSEDREASON_WIFI_DISABLED = 5;
    public static final int DOWNLOADPAUSEDREASON_WIFI_DISABLED_NEED_CELLULAR_PERMISSION = 3;
    public static final int MOUNTSTATE_ERROR_ALREADY_MOUNTED = 1;
    public static final int MOUNTSTATE_ERROR_COULD_NOT_MOUNT = 2;
    public static final int MOUNTSTATE_ERROR_COULD_NOT_UNMOUNT = 3;
    public static final int MOUNTSTATE_ERROR_INTERNAL = 4;
    public static final int MOUNTSTATE_ERROR_NOT_MOUNTED = 5;
    public static final int MOUNTSTATE_ERROR_PERMISSION_DENIED = 6;
    public static final int MOUNTSTATE_MOUNTED = 7;
    public static final int MOUNTSTATE_UNKNOWN = 0;
    public static final int MOUNTSTATE_UNMOUNTED = 8;

    void onDownloadProgress(long overallProgress, long overallTotal, float kbpsSpeed, long timeRemainingMilliseconds);

    void onDownloadStarted();

    void onDownloadStateChanged(boolean needCellularPermission, boolean progressIndeterminate, boolean isPaused, boolean isCompleted, boolean isFailed, int pausedReason, int failedReason);

    void onMountStateChanged(String mountPath, int mountState);
}