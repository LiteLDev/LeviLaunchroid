package org.levimc.launcher.settings;

import android.content.Context;

public class FeatureSettings {
    private static volatile FeatureSettings INSTANCE;
    private static Context appContext;
    private boolean versionIsolationEnabled = false;
    private boolean launcherManagedMcLoginEnabled = false;
    private boolean logcatOverlayEnabled = false;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static FeatureSettings getInstance() {
        if (INSTANCE == null) {
            synchronized (FeatureSettings.class) {
                if (INSTANCE == null) {
                    INSTANCE = SettingsStorage.load(appContext);
                    if (INSTANCE == null) {
                        INSTANCE = new FeatureSettings();
                    }
                }
            }
        }
        return INSTANCE;
    }

    public boolean isVersionIsolationEnabled() { return versionIsolationEnabled; }
    public void setVersionIsolationEnabled(boolean enabled) { this.versionIsolationEnabled = enabled; autoSave(); }

    public boolean isLauncherManagedMcLoginEnabled() { return launcherManagedMcLoginEnabled; }
    public void setLauncherManagedMcLoginEnabled(boolean enabled) { this.launcherManagedMcLoginEnabled = enabled; autoSave(); }

    public boolean isLogcatOverlayEnabled() { return logcatOverlayEnabled; }
    public void setLogcatOverlayEnabled(boolean enabled) { this.logcatOverlayEnabled = enabled; autoSave(); }

    private void autoSave() {
        if (appContext != null) {
            SettingsStorage.save(appContext, this);
        }
    }
}