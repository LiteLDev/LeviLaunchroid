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
    private static final String KEY_OVERLAY_BUTTON_SIZE = "overlay_button_size";
    private static final int DEFAULT_OVERLAY_BUTTON_SIZE = 48;

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

    public int getAutoSprintKey() {
        return prefs.getInt(KEY_AUTOSPRINT_KEY, KeyEvent.KEYCODE_CTRL_LEFT);
    }

    public void setAutoSprintKey(int keyCode) {
        prefs.edit().putInt(KEY_AUTOSPRINT_KEY, keyCode).apply();
    }

    public int getOverlayButtonSize() {
        return prefs.getInt(KEY_OVERLAY_BUTTON_SIZE, DEFAULT_OVERLAY_BUTTON_SIZE);
    }

    public void setOverlayButtonSize(int sizeDp) {
        prefs.edit().putInt(KEY_OVERLAY_BUTTON_SIZE, sizeDp).apply();
    }

    private void savePrefs() {
        prefs.edit().putStringSet(KEY_ADDED_MODS, new HashSet<>(addedMods)).apply();
    }
}