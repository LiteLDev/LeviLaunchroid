package org.levimc.launcher.util;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static final int MODE_FOLLOW_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String THEME_PREFS = "theme_prefs";
    private static final String THEME_MODE_KEY = "theme_mode";
    private static int sThemeChangeGeneration = 0;
    private final SharedPreferences prefs;
    private final Activity activity;

    public ThemeManager(Activity activity) {
        this.activity = activity;
        prefs = activity.getSharedPreferences(THEME_PREFS, Activity.MODE_PRIVATE);
    }

    public void applyTheme() {
        updateNightMode();
    }

    public void setThemeMode(int mode) {
        int previous = prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM);
        prefs.edit().putInt(THEME_MODE_KEY, mode).apply();
        updateNightMode();
        if (previous != mode) {
            sThemeChangeGeneration++;
        }
        if (activity != null && previous != mode) {
            activity.recreate();
        }
    }

    private void updateNightMode() {
        int mode = prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM);
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // MODE_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public int getCurrentMode() {
        int mode = prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM);
        if (mode < MODE_FOLLOW_SYSTEM || mode > MODE_DARK) {
            mode = MODE_FOLLOW_SYSTEM;
            prefs.edit().putInt(THEME_MODE_KEY, mode).apply();
        }
        return mode;
    }

    public static int getThemeChangeGeneration() {
        return sThemeChangeGeneration;
    }
}