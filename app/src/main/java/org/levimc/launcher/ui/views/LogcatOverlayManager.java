package org.levimc.launcher.ui.views;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;

import org.levimc.launcher.settings.FeatureSettings;


public class LogcatOverlayManager {
    private static volatile LogcatOverlayManager instance;
    private final Application app;
    private LogcatOverlay overlay;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private LogcatOverlayManager(Application app) {
        this.app = app;
        registerLifecycleCallbacks();
        registerSettingsListener();
    }

    public static void init(Application app) {
        if (instance == null) {
            synchronized (LogcatOverlayManager.class) {
                if (instance == null) {
                    instance = new LogcatOverlayManager(app);
                }
            }
        }
    }

    public static LogcatOverlayManager getInstance() {
        return instance;
    }

    private void ensureOverlay(Context context) {
        if (overlay == null) {
            overlay = new LogcatOverlay(app.getApplicationContext());
        }
    }

    private void attachTo(Activity activity) {
        ensureOverlay(activity);
        if (overlay == null) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        // Detach from previous parent if any
        if (overlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
        }
        content.addView(overlay);
        updateVisibilityFromSettings();
    }

    private void updateVisibilityFromSettings() {
        boolean enabled = FeatureSettings.getInstance().isLogcatOverlayEnabled();
        mainHandler.post(() -> {
            if (overlay == null) return;
            if (enabled) overlay.show(); else overlay.hide();
        });
    }

    public void refreshVisibility() {
        updateVisibilityFromSettings();
    }

    private void registerLifecycleCallbacks() {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) { attachTo(activity); }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    private void registerSettingsListener() {
        SharedPreferences sp = app.getSharedPreferences("feature_settings", Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if ("settings_json".equals(key)) {
                // FeatureSettings has been updated; refresh overlay visibility
                updateVisibilityFromSettings();
            }
        });
    }
}