package org.levimc.launcher.core.mods;

public class Mod {
    private final String fileName;
    private boolean enabled;
    private int order;

    public Mod(String fileName, boolean enabled) {
        this.fileName = fileName;
        this.enabled = enabled;
        this.order = 0;
    }

    public Mod(String fileName, boolean enabled, int order) {
        this.fileName = fileName;
        this.enabled = enabled;
        this.order = order;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDisplayName() {
        return fileName.replace(".so", "");
    }
}