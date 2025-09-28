package com.mojang.minecraftpe.packagesource;

public class StubPackageSource extends PackageSource {
    private final PackageSourceListener listener;

    public StubPackageSource(PackageSourceListener packageSourceListener) {
        listener = packageSourceListener;
    }

    @Override
    public void abortDownload() {

    }

    @Override
    public void destructor() {

    }

    @Override
    public void downloadFiles(String filename, long filesize, boolean verifyName, boolean verifySize) {
        listener.onDownloadStateChanged(false, false, false, false, true, 0, 8);
    }

    @Override
    public String getDownloadDirectoryPath() {
        return null;
    }

    @Override
    public String getMountPath(String path) {
        return null;
    }

    @Override
    public void mountFiles(String filename) {

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
    public void unmountFiles(String filename) {

    }
}
