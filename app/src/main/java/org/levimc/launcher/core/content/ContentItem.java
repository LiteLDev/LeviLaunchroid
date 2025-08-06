package org.levimc.launcher.core.content;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class ContentItem {
    protected String name;
    protected File file;
    protected long size;
    protected long lastModified;
    protected boolean enabled;

    public ContentItem(String name, File file) {
        this.name = name;
        this.file = file;
        this.size = calculateSize(file);
        this.lastModified = file.lastModified();
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }

    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    public String getFormattedLastModified() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastModified));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public abstract String getType();
    public abstract String getDescription();
    public abstract boolean isValid();

    private long calculateSize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        
        long totalSize = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                totalSize += calculateSize(f);
            }
        }
        return totalSize;
    }
}