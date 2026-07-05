package org.levimc.launcher.core.mods.inbuilt;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager;
import org.levimc.launcher.core.mods.inbuilt.overlay.InbuiltOverlayManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExternalModuleProvider {
    private static final String GROUP_PREFIX = "external:";
    private static final String UNGROUPED_GROUP_ID = "external:ungrouped";

    private ExternalModuleProvider() {
    }

    public static List<UnifiedMod> load(Activity activity) {
        Map<String, String> externalGroupNames = loadExternalModDisplayNames(activity);
        List<UnifiedMod> modules = new ArrayList<>();
        int count = ExternalModBridge.getExternalModCount();
        for (int i = 0; i < count; i++) {
            UnifiedMod mod = parse(activity, ExternalModBridge.getExternalModInfo(i), externalGroupNames);
            if (mod != null) {
                modules.add(mod);
            }
        }
        return modules;
    }

    private static UnifiedMod parse(Activity activity, String json,
                                    Map<String, String> externalGroupNames) {
        try {
            JSONObject obj = new JSONObject(json);
            String moduleId = obj.optString("module_id", "");
            if (moduleId.isEmpty()) return null;

            String displayName = obj.optString("display_name", moduleId);
            String description = obj.optString("description", "");
            String modId = obj.optString("mod_id", "").trim();
            boolean nativeEnabled = obj.optBoolean("enabled", false);
            boolean enabled = resolveEnabled(activity, moduleId, nativeEnabled);
            String groupId = modId.isEmpty() ? UNGROUPED_GROUP_ID : GROUP_PREFIX + modId;
            String groupName = modId.isEmpty()
                    ? activity.getString(R.string.mod_menu_group_external_ungrouped)
                    : externalGroupNames.getOrDefault(modId, modId);

            return new UnifiedMod(
                    moduleId,
                    displayName,
                    description,
                    modId,
                    UnifiedMod.Source.EXTERNAL,
                    enabled,
                    parseConfigs(obj.optJSONArray("configs")),
                    false,
                    groupId,
                    groupName,
                    (mod, enabledValue) -> setEnabled(activity, mod, enabledValue),
                    ExternalModuleProvider::setConfig
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<UnifiedMod.ConfigEntry> parseConfigs(JSONArray array) throws Exception {
        List<UnifiedMod.ConfigEntry> configs = new ArrayList<>();
        if (array == null) {
            return configs;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject cfgObj = array.getJSONObject(i);
            configs.add(new UnifiedMod.ConfigEntry(
                    cfgObj.optString("key", ""),
                    cfgObj.optString("display_name", ""),
                    parseConfigType(cfgObj.optInt("type", 0)),
                    cfgObj.optString("default_value", ""),
                    cfgObj.optString("min_value", ""),
                    cfgObj.optString("max_value", ""),
                    cfgObj.optString("current_value", ""),
                    cfgObj.optString("depends_on", "")
            ));
        }
        return configs;
    }

    private static UnifiedMod.ConfigType parseConfigType(int type) {
        switch (type) {
            case 1:
                return UnifiedMod.ConfigType.SLIDER_INT;
            case 2:
                return UnifiedMod.ConfigType.SLIDER_FLOAT;
            case 3:
                return UnifiedMod.ConfigType.RADIO;
            case 4:
                return UnifiedMod.ConfigType.COLOR;
            default:
                return UnifiedMod.ConfigType.TOGGLE;
        }
    }

    private static boolean resolveEnabled(Activity activity, String moduleId, boolean nativeEnabled) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        boolean enabled = manager.resolveExternalModuleEnabled(moduleId, nativeEnabled);
        if (enabled != nativeEnabled) {
            applyNativeEnabled(moduleId, enabled);
        }
        return enabled;
    }

    private static void setEnabled(Activity activity, UnifiedMod mod, boolean enabled) {
        InbuiltModManager.getInstance(activity).setExternalModuleEnabled(mod.getId(), enabled);
        applyNativeEnabled(mod.getId(), enabled);
    }

    private static void applyNativeEnabled(String moduleId, boolean enabled) {
        ExternalModBridge.toggleExternalMod(moduleId, enabled);
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.handleExternalModuleToggle(moduleId, enabled);
        }
    }

    private static void setConfig(UnifiedMod mod, UnifiedMod.ConfigEntry config, String value) {
        ExternalModBridge.setExternalModConfig(mod.getId(), config.key, value);
    }

    private static Map<String, String> loadExternalModDisplayNames(Activity activity) {
        Map<String, String> names = new LinkedHashMap<>();
        try {
            for (Mod mod : ModManager.getInstance().getMods()) {
                String displayName = mod.getDisplayName();
                names.put(mod.getId(),
                        displayName == null || displayName.trim().isEmpty()
                                ? mod.getId()
                                : displayName.trim());
            }
        } catch (Exception ignored) {
        }
        return names;
    }
}
