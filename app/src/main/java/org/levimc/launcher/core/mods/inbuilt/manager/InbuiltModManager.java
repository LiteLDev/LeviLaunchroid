package org.levimc.launcher.core.mods.inbuilt.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.model.InbuiltMod;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InbuiltModManager {
    private static final String PREFS_NAME = "inbuilt_mods_prefs";
    private static final String KEY_ADDED_MODS = "added_mods";
    private static final String KEY_AUTOSPRINT_KEY = "autosprint_key";
    private static final String KEY_OVERLAY_BUTTON_SIZE_PREFIX = "overlay_button_size_";
    private static final String KEY_OVERLAY_BUTTON_SIZE_GLOBAL = "overlay_button_size";
    private static final String KEY_OVERLAY_OPACITY_PREFIX = "overlay_opacity_";
    private static final String KEY_MOD_MENU_ENABLED = "mod_menu_enabled";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_MOD_MENU_OPACITY = "mod_menu_opacity";
    private static final String KEY_MOD_MENU_BUTTON_OPACITY = "mod_menu_button_opacity";
    private static final String KEY_ZOOM_LEVEL = "zoom_level";
    private static final String KEY_ZOOM_KEYBIND = "zoom_keybind";
    private static final String KEY_OVERLAY_POSITION_X_PREFIX = "overlay_pos_x_";
    private static final String KEY_OVERLAY_POSITION_Y_PREFIX = "overlay_pos_y_";
    private static final String KEY_OVERLAY_LOCK_PREFIX = "overlay_lock_";
    private static final int DEFAULT_OVERLAY_BUTTON_SIZE = 56;
    private static final int DEFAULT_OVERLAY_OPACITY = 100;
    private static final int DEFAULT_ZOOM_LEVEL = 50;

    private static volatile InbuiltModManager instance;
    private final SharedPreferences prefs;
    private final Set<String> addedMods;

    private InbuiltModManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        addedMods = new HashSet<>(prefs.getStringSet(KEY_ADDED_MODS, new HashSet<>()));
    }

    public static InbuiltModManager getInstance(Context context) {
        if (instance == null) {
            synchronized (InbuiltModManager.class) {
                if (instance == null) {
                    instance = new InbuiltModManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public List<InbuiltMod> getAllMods(Context context) {
        List<InbuiltMod> mods = new ArrayList<>();
        mods.add(new InbuiltMod(ModIds.QUICK_DROP, 
            context.getString(R.string.inbuilt_mod_quick_drop),
            context.getString(R.string.inbuilt_mod_quick_drop_desc), false, addedMods.contains(ModIds.QUICK_DROP)));
        mods.add(new InbuiltMod(ModIds.CAMERA_PERSPECTIVE,
            context.getString(R.string.inbuilt_mod_camera),
            context.getString(R.string.inbuilt_mod_camera_desc), false, addedMods.contains(ModIds.CAMERA_PERSPECTIVE)));
        mods.add(new InbuiltMod(ModIds.TOGGLE_HUD,
            context.getString(R.string.inbuilt_mod_hud),
            context.getString(R.string.inbuilt_mod_hud_desc), false, addedMods.contains(ModIds.TOGGLE_HUD)));
        mods.add(new InbuiltMod(ModIds.AUTO_SPRINT,
            context.getString(R.string.inbuilt_mod_autosprint),
            context.getString(R.string.inbuilt_mod_autosprint_desc), true, addedMods.contains(ModIds.AUTO_SPRINT)));
        mods.add(new InbuiltMod(ModIds.CHICK_PET,
            context.getString(R.string.inbuilt_mod_chick_pet),
            context.getString(R.string.inbuilt_mod_chick_pet_desc), false, addedMods.contains(ModIds.CHICK_PET)));
        mods.add(new InbuiltMod(ModIds.ZOOM,
            context.getString(R.string.inbuilt_mod_zoom),
            context.getString(R.string.inbuilt_mod_zoom_desc), false, addedMods.contains(ModIds.ZOOM)));
        mods.add(new InbuiltMod(ModIds.FPS_DISPLAY,
            context.getString(R.string.inbuilt_mod_fps_display),
            context.getString(R.string.inbuilt_mod_fps_display_desc), false, addedMods.contains(ModIds.FPS_DISPLAY)));
        mods.add(new InbuiltMod(ModIds.CPS_DISPLAY,
            context.getString(R.string.inbuilt_mod_cps_display),
            context.getString(R.string.inbuilt_mod_cps_display_desc), false, addedMods.contains(ModIds.CPS_DISPLAY)));
        mods.add(new InbuiltMod(ModIds.SNAPLOOK,
            context.getString(R.string.inbuilt_mod_snaplook),
            context.getString(R.string.inbuilt_mod_snaplook_desc), false, addedMods.contains(ModIds.SNAPLOOK)));
        return mods;
    }

    public List<InbuiltMod> getAvailableMods(Context context) {
        List<InbuiltMod> all = getAllMods(context);
        List<InbuiltMod> available = new ArrayList<>();
        for (InbuiltMod mod : all) {
            if (!addedMods.contains(mod.getId())) {
                available.add(mod);
            }
        }
        return available;
    }

    public List<InbuiltMod> getAddedMods(Context context) {
        List<InbuiltMod> all = getAllMods(context);
        List<InbuiltMod> added = new ArrayList<>();
        for (InbuiltMod mod : all) {
            if (addedMods.contains(mod.getId())) {
                added.add(mod);
            }
        }
        return added;
    }

    public void addMod(String modId) {
        addedMods.add(modId);
        savePrefs();
    }

    public void removeMod(String modId) {
        addedMods.remove(modId);
        savePrefs();
    }

    public boolean isModAdded(String modId) {
        return addedMods.contains(modId);
    }

    public int getAutoSprintKeybind() {
        return prefs.getInt(KEY_AUTOSPRINT_KEY, KeyEvent.KEYCODE_CTRL_LEFT);
    }

    public void setAutoSprintKeybind(int keyCode) {
        prefs.edit().putInt(KEY_AUTOSPRINT_KEY, keyCode).apply();
    }

    public int getOverlayButtonSize(String modId) {
        return prefs.getInt(KEY_OVERLAY_BUTTON_SIZE_PREFIX + modId, DEFAULT_OVERLAY_BUTTON_SIZE);
    }

    public void setOverlayButtonSize(String modId, int sizeDp) {
        prefs.edit().putInt(KEY_OVERLAY_BUTTON_SIZE_PREFIX + modId, sizeDp).apply();
    }

    public int getOverlayOpacity(String modId) {
        return prefs.getInt(KEY_OVERLAY_OPACITY_PREFIX + modId, DEFAULT_OVERLAY_OPACITY);
    }

    public void setOverlayOpacity(String modId, int opacity) {
        prefs.edit().putInt(KEY_OVERLAY_OPACITY_PREFIX + modId, Math.max(0, Math.min(100, opacity))).apply();
    }

    public boolean isModMenuEnabled() {
        return prefs.getBoolean(KEY_MOD_MENU_ENABLED, false);
    }

    public void setModMenuEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOD_MENU_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public int getModMenuOpacity() {
        return prefs.getInt(KEY_MOD_MENU_OPACITY, DEFAULT_OVERLAY_OPACITY);
    }

    public void setModMenuOpacity(int opacity) {
        prefs.edit().putInt(KEY_MOD_MENU_OPACITY, Math.max(0, Math.min(100, opacity))).apply();
    }

    public int getModMenuButtonOpacity() {
        return prefs.getInt(KEY_MOD_MENU_BUTTON_OPACITY, DEFAULT_OVERLAY_OPACITY);
    }

    public void setModMenuButtonOpacity(int opacity) {
        prefs.edit().putInt(KEY_MOD_MENU_BUTTON_OPACITY, Math.max(0, Math.min(100, opacity))).apply();
    }

    public int getZoomLevel() {
        try {
            return prefs.getInt(KEY_ZOOM_LEVEL, DEFAULT_ZOOM_LEVEL);
        } catch (ClassCastException e) {
            prefs.edit().remove(KEY_ZOOM_LEVEL).apply();
            return DEFAULT_ZOOM_LEVEL;
        }
    }

    public void setZoomLevel(int level) {
        prefs.edit().putInt(KEY_ZOOM_LEVEL, Math.max(10, Math.min(100, level))).apply();
    }

    public int getZoomKeybind() {
        return prefs.getInt(KEY_ZOOM_KEYBIND, KeyEvent.KEYCODE_C);
    }

    public void setZoomKeybind(int keyCode) {
        prefs.edit().putInt(KEY_ZOOM_KEYBIND, keyCode).apply();
    }

    public int getOverlayPositionX(String modId, int defaultX) {
        return prefs.getInt(KEY_OVERLAY_POSITION_X_PREFIX + modId, defaultX);
    }

    public int getOverlayPositionY(String modId, int defaultY) {
        return prefs.getInt(KEY_OVERLAY_POSITION_Y_PREFIX + modId, defaultY);
    }

    public void setOverlayPosition(String modId, int x, int y) {
        prefs.edit()
            .putInt(KEY_OVERLAY_POSITION_X_PREFIX + modId, x)
            .putInt(KEY_OVERLAY_POSITION_Y_PREFIX + modId, y)
            .apply();
    }

    public boolean isOverlayLocked(String modId) {
        return prefs.getBoolean(KEY_OVERLAY_LOCK_PREFIX + modId, false);
    }

    public void setOverlayLocked(String modId, boolean locked) {
        prefs.edit().putBoolean(KEY_OVERLAY_LOCK_PREFIX + modId, locked).apply();
    }

    private void savePrefs() {
        prefs.edit().putStringSet(KEY_ADDED_MODS, new HashSet<>(addedMods)).apply();
    }
}
