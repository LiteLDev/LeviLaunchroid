package org.levimc.launcher.core.mods.inbuilt;

import android.app.Activity;
import android.content.Context;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager;

import java.util.ArrayList;
import java.util.List;

public final class InbuiltModuleProvider {
    private static final String GROUP_ID = "inbuilt";
    private static final String MOD_ID = "inbuilt";

    private static final String CFG_OVERLAY_SIZE = "overlay_size";
    private static final String CFG_OVERLAY_OPACITY = "overlay_opacity";
    private static final String CFG_OVERLAY_LOCK = "overlay_lock";
    private static final String CFG_AUTO_SPRINT_KEYBIND = "auto_sprint_keybind";
    private static final String CFG_CURSOR_SENSITIVITY = "cursor_sensitivity";
    private static final String CFG_ZOOM_LEVEL = "zoom_level";
    private static final String CFG_ZOOM_TRANSITION = "zoom_transition";
    private static final String CFG_ZOOM_KEYBIND = "zoom_keybind";

    private InbuiltModuleProvider() {
    }

    public static List<UnifiedMod> load(Activity activity) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        String groupName = activity.getString(R.string.mod_menu_group_inbuilt);
        List<UnifiedMod> mods = new ArrayList<>();

        mods.add(create(activity, manager, overlayManager, ModIds.QUICK_DROP,
                R.string.inbuilt_mod_quick_drop, R.string.inbuilt_mod_quick_drop_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CAMERA_PERSPECTIVE,
                R.string.inbuilt_mod_camera, R.string.inbuilt_mod_camera_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TOGGLE_HUD,
                R.string.inbuilt_mod_hud, R.string.inbuilt_mod_hud_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.AUTO_SPRINT,
                R.string.inbuilt_mod_autosprint, R.string.inbuilt_mod_autosprint_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CHICK_PET,
                R.string.inbuilt_mod_chick_pet, R.string.inbuilt_mod_chick_pet_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.ZOOM,
                R.string.inbuilt_mod_zoom, R.string.inbuilt_mod_zoom_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.FPS_DISPLAY,
                R.string.inbuilt_mod_fps_display, R.string.inbuilt_mod_fps_display_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CPS_DISPLAY,
                R.string.inbuilt_mod_cps_display, R.string.inbuilt_mod_cps_display_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.SNAPLOOK,
                R.string.inbuilt_mod_snaplook, R.string.inbuilt_mod_snaplook_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.VIRTUAL_CURSOR,
                R.string.inbuilt_mod_virtual_cursor, R.string.inbuilt_mod_virtual_cursor_desc,
                groupName));

        return mods;
    }

    private static UnifiedMod create(Activity activity, InbuiltModManager manager,
                                     InbuiltOverlayManager overlayManager, String id,
                                     int nameRes, int descRes, String groupName) {
        boolean active = overlayManager != null
                ? overlayManager.isModActive(id)
                : manager.resolveInbuiltModEnabled(id, false);
        return new UnifiedMod(
                id,
                activity.getString(nameRes),
                activity.getString(descRes),
                MOD_ID,
                UnifiedMod.Source.INBUILT,
                active,
                createConfigs(activity, manager, id),
                false,
                GROUP_ID,
                groupName,
                (mod, enabled) -> setEnabled(manager, mod, enabled),
                (mod, config, value) -> setConfig(manager, mod, config, value)
        );
    }

    private static List<UnifiedMod.ConfigEntry> createConfigs(Context context,
                                                              InbuiltModManager manager,
                                                              String modId) {
        List<UnifiedMod.ConfigEntry> configs = new ArrayList<>();
        if (!ModIds.CHICK_PET.equals(modId)) {
            configs.add(config(CFG_OVERLAY_SIZE,
                    context.getString(R.string.mod_config_overlay_button_size_dp),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "56", "20", "100",
                    String.valueOf(manager.getOverlayButtonSize(modId))));
            configs.add(config(CFG_OVERLAY_OPACITY,
                    context.getString(R.string.mod_config_overlay_opacity_percent),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "10", "100",
                    String.valueOf(manager.getOverlayOpacity(modId))));
            configs.add(config(CFG_OVERLAY_LOCK,
                    context.getString(R.string.overlay_button_lock),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isOverlayLocked(modId))));
        }

        if (ModIds.AUTO_SPRINT.equals(modId)) {
            configs.add(config(CFG_AUTO_SPRINT_KEYBIND,
                    context.getString(R.string.mod_config_auto_sprint_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getAutoSprintKeybind())));
        } else if (ModIds.VIRTUAL_CURSOR.equals(modId)) {
            configs.add(config(CFG_CURSOR_SENSITIVITY,
                    context.getString(R.string.mod_config_cursor_sensitivity_percent),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "120", "10", "200",
                    String.valueOf(manager.getCursorSensitivity())));
        } else if (ModIds.ZOOM.equals(modId)) {
            configs.add(config(CFG_ZOOM_LEVEL,
                    context.getString(R.string.mod_config_zoom_level_percent),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "10", "-20", "100",
                    String.valueOf(manager.getZoomLevel())));
            configs.add(config(CFG_ZOOM_TRANSITION,
                    context.getString(R.string.mod_config_zoom_transition),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "150", "0", "1000",
                    String.valueOf(manager.getZoomTransitionDuration())));
            configs.add(config(CFG_ZOOM_KEYBIND,
                    context.getString(R.string.mod_config_zoom_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getZoomKeybind())));
        }
        return configs;
    }

    private static UnifiedMod.ConfigEntry config(String key, String displayName,
                                                 UnifiedMod.ConfigType type,
                                                 String defaultValue, String minValue,
                                                 String maxValue, String currentValue) {
        return new UnifiedMod.ConfigEntry(
                key, displayName, type, defaultValue, minValue, maxValue,
                currentValue, ""
        );
    }

    private static void setEnabled(InbuiltModManager manager, UnifiedMod mod, boolean enabled) {
        manager.setInbuiltModEnabled(mod.getId(), enabled);
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.handleModToggle(mod.getId(), enabled);
        }
    }

    private static void setConfig(InbuiltModManager manager, UnifiedMod mod, UnifiedMod.ConfigEntry config,
                                  String value) {
        switch (config.key) {
            case CFG_OVERLAY_SIZE:
                manager.setOverlayButtonSize(mod.getId(), parseInt(value, manager.getOverlayButtonSize(mod.getId())));
                break;
            case CFG_OVERLAY_OPACITY:
                manager.setOverlayOpacity(mod.getId(), parseInt(value, manager.getOverlayOpacity(mod.getId())));
                break;
            case CFG_OVERLAY_LOCK:
                manager.setOverlayLocked(mod.getId(), parseBoolean(value));
                break;
            case CFG_AUTO_SPRINT_KEYBIND:
                manager.setAutoSprintKeybind(parseInt(value, manager.getAutoSprintKeybind()));
                break;
            case CFG_CURSOR_SENSITIVITY:
                manager.setCursorSensitivity(parseInt(value, manager.getCursorSensitivity()));
                break;
            case CFG_ZOOM_LEVEL:
                manager.setZoomLevel(parseInt(value, manager.getZoomLevel()));
                break;
            case CFG_ZOOM_TRANSITION:
                manager.setZoomTransitionDuration(parseInt(value, manager.getZoomTransitionDuration()));
                break;
            case CFG_ZOOM_KEYBIND:
                manager.setZoomKeybind(parseInt(value, manager.getZoomKeybind()));
                break;
            default:
                break;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
