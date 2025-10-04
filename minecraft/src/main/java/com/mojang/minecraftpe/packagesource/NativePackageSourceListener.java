package com.mojang.minecraftpe.packagesource;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class NativePackageSourceListener implements PackageSourceListener {
    long mPackageSourceListener = 0;

    public native void nativeOnDownloadProgress(long packageSourceListener, long overallProgress, long overallTotal, float kbpsSpeed, long timeRemainingMilliseconds);

    public native void nativeOnDownloadStarted(long packageSourceListener);

    public native void nativeOnDownloadStateChanged(long packageSourceListener, boolean needCelularPermission, boolean progressIndeterminate, boolean isPaused, boolean isCompleted, boolean isFailed, int pausedReason, int failedReason);

    public native void nativeOnMountStateChanged(long packageSourceListener, String path, int state);

    public void setListener(long packageSourceListener) {
        this.mPackageSourceListener = packageSourceListener;
    }

    @Override
    public void onDownloadStarted() {
        nativeOnDownloadStarted(this.mPackageSourceListener);
    }

    @Override
    public void onDownloadStateChanged(boolean needCelularPermission, boolean progressIndeterminate, boolean isPaused, boolean isCompleted, boolean isFailed, int pausedReason, int failedReason) {
        nativeOnDownloadStateChanged(mPackageSourceListener, needCelularPermission, progressIndeterminate, isPaused, isCompleted, isFailed, pausedReason, failedReason);
    }

    @Override
    public void onDownloadProgress(long overallProgress, long overallTotal, float kbpsSpeed, long timeRemainingMilliseconds) {
        nativeOnDownloadProgress(mPackageSourceListener, overallProgress, overallTotal, kbpsSpeed, timeRemainingMilliseconds);
    }

    @Override
    public void onMountStateChanged(String path, int state) {
        nativeOnMountStateChanged(mPackageSourceListener, path, state);
    }
}