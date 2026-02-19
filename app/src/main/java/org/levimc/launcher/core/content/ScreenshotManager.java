package org.levimc.launcher.core.content;

import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotManager {
    private File screenshotsDir;

    public void setScreenshotsDirectory(File directory) {
        this.screenshotsDir = directory;
    }

    public List<ScreenshotItem> getScreenshots() {
        List<ScreenshotItem> items = new ArrayList<>();
        if (screenshotsDir == null || !screenshotsDir.exists() || !screenshotsDir.isDirectory()) {
            return items;
        }

        File[] directFiles = screenshotsDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
        if (directFiles != null) {
            for (File file : directFiles) {
                long captureTime = file.lastModified();
                String jsonPath = file.getAbsolutePath().replaceAll("\\.(jpg|png|jpeg)$", ".json");
                File jsonFile = new File(jsonPath);
                if (jsonFile.exists()) {
                    try {
                        String content = new String(Files.readAllBytes(jsonFile.toPath()));
                        JSONObject json = new JSONObject(content);
                        if (json.has("CaptureTime")) {
                            captureTime = json.getLong("CaptureTime") * 1000L;
                        }
                    } catch (Exception e) {
                    }
                }
                items.add(new ScreenshotItem(file.getName(), file, screenshotsDir.getName(), captureTime));
            }
        }

        File[] subdirs = screenshotsDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File[] files = subdir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
                if (files != null) {
                    for (File file : files) {
                        long captureTime = file.lastModified();
                        String jsonPath = file.getAbsolutePath().replaceAll("\\.(jpg|png|jpeg)$", ".json");
                        File jsonFile = new File(jsonPath);
                        if (jsonFile.exists()) {
                            try {
                                String content = new String(Files.readAllBytes(jsonFile.toPath()));
                                JSONObject json = new JSONObject(content);
                                if (json.has("CaptureTime")) {
                                    captureTime = json.getLong("CaptureTime") * 1000L;
                                }
                            } catch (Exception e) {
                            }
                        }

                        items.add(new ScreenshotItem(file.getName(), file, subdir.getName(), captureTime));
                    }
                }
            }
        }
        
        items.sort((a, b) -> Long.compare(b.captureTime, a.captureTime));
        return items;
    }

    public boolean deleteScreenshot(ScreenshotItem item) {
        if (item.file.exists() && item.file.delete()) {
            String jsonPath = item.file.getAbsolutePath().replaceAll("\\.(jpg|png|jpeg)$", ".json");
            File jsonFile = new File(jsonPath);
            if (jsonFile.exists()) {
                jsonFile.delete();
            }
            return true;
        }
        return false;
    }
}
