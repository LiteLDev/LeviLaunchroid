package com.mojang.minecraftpe.packagesource;

import java.util.HashMap;
import java.util.Map;

public abstract class PackageSource {

    private static final Map<Integer, String> stringResources = new HashMap<>();

    public abstract void destructor();

    public abstract String getMountPath(String str);

    public abstract String getDownloadDirectoryPath();

    public abstract void mountFiles(String str);

    public abstract void unmountFiles(String str);

    public abstract void downloadFiles(String str, long j, boolean z, boolean z2);

    public abstract void pauseDownload();

    public abstract void resumeDownload();

    public abstract void resumeDownloadOnCell();

    public abstract void abortDownload();

    public static void setStringResource(int key, String value) {
        stringResources.put(key, value);
    }

}