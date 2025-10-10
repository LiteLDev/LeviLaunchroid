package com.mojang.minecraftpe.packagesource;

public class StubPackageSource extends PackageSource {

    private final PackageSourceListener listener;

    public StubPackageSource(PackageSourceListener listener) {
        this.listener = listener;
    }

    @Override
    public void destructor() {

    }

    @Override
    public String getMountPath(String str) {
        return null;
    }

    @Override
    public String getDownloadDirectoryPath() {
        return null;
    }

    @Override
    public void mountFiles(String str) {

    }

    @Override
    public void unmountFiles(String str) {

    }

    @Override
    public void downloadFiles(String str, long j, boolean z, boolean z2) {
        listener.onDownloadStateChanged(false, false, false, false, true, 0, 8);
    }

    @Override
    public void pauseDownload() {

    }

    @Override
    public void resumeDownload() {

    }

    @Override
    public void resumeDownloadOnCell() {

    }

    @Override
    public void abortDownload() {

    }

}