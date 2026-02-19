package org.levimc.launcher.core.content;

import java.io.File;

public class ScreenshotItem {
    public final String name;
    public final File file;
    public final String directory;
    public final long captureTime;

    public ScreenshotItem(String name, File file, String directory, long captureTime) {
        this.name = name;
        this.file = file;
        this.directory = directory;
        this.captureTime = captureTime;
    }
}
