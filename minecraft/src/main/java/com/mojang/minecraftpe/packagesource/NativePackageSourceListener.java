package com.mojang.minecraftpe.packagesource;

public final class NativePackageSourceListener implements PackageSourceListener {

    private long handle;

    public void setListener(long handle) {
        this.handle = handle;
    }

    @Override
    public void onDownloadStateChanged(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, int i, int i2) {
        nativeOnDownloadStateChanged(handle, z, z2, z3, z4, z5, i, i2);
    }

    public native void nativeOnDownloadStateChanged(long j, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, int i, int i2);

}