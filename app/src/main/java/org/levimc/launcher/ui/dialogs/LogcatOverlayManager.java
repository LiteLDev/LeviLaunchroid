package org.levimc.launcher.ui.dialogs;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.ViewGroup;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.activities.SplashActivity;

public class LogcatOverlayManager {

    private static volatile LogcatOverlayManager instance;
    private final Application app;
    private LogcatOverlay overlay;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean onSplash = false;

    private LogcatOverlayManager(Application app) {
        this.app = app;
        registerLifecycleCallbacks();
        registerSettingsListener();
    }

    public static void init(Application app) {
        if (instance == null) {
            synchronized (LogcatOverlayManager.class) {
                if (instance == null) instance = new LogcatOverlayManager(app);
            }
        }
    }

    public static LogcatOverlayManager getInstance() { return instance; }

    private void attachTo(Activity act) {
        if (overlay == null) overlay = new LogcatOverlay(act.getApplicationContext());
        ViewGroup root = act.findViewById(android.R.id.content);
        if (root == null) return;
        if (overlay.getParent() != root) {
            if (overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
            root.addView(overlay);
        }
        updateVisibility();
    }

    private void updateVisibility() {
        boolean show = FeatureSettings.getInstance().isLogcatOverlayEnabled() && !onSplash;
        mainHandler.post(() -> {
            if (overlay == null) return;
            if (show) overlay.show(); else overlay.hide();
        });
    }

    private void registerLifecycleCallbacks() {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                onSplash = activity instanceof SplashActivity;
                attachTo(activity);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private void registerSettingsListener() {
        SharedPreferences sp = app.getSharedPreferences("feature_settings", Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener((prefs, key) -> {
            if ("settings_json".equals(key)) updateVisibility();
        });
    }

    public void refreshVisibility() { updateVisibility(); }


}