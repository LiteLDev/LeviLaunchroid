package org.levimc.launcher.core.mods.inbuilt;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UnifiedMod {

    public enum Source {
        INBUILT,
        EXTERNAL
    }

    public enum ConfigType {
        TOGGLE,
        SLIDER_INT,
        SLIDER_FLOAT,
        RADIO,
        COLOR
    }

    public static class ConfigEntry {
        public final String key;
        public final String displayName;
        public final ConfigType type;
        public final String defaultValue;
        public final String minValue;
        public final String maxValue;
        public String currentValue;
        public final String dependsOn;

        public ConfigEntry(String key, String displayName, ConfigType type,
                           String defaultValue, String minValue, String maxValue,
                           String currentValue, String dependsOn) {
            this.key = key;
            this.displayName = displayName;
            this.type = type;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.currentValue = currentValue;
            this.dependsOn = dependsOn;
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final String modId;
    private final String groupId;
    private final String groupName;
    private final Source source;
    private boolean enabled;
    private final List<ConfigEntry> configEntries;
    private final boolean forceHasConfig;

    public UnifiedMod(String id, String name, String description, String modId,
                      Source source, boolean enabled, List<ConfigEntry> configEntries) {
        this(id, name, description, modId, source, enabled, configEntries, false);
    }

    public UnifiedMod(String id, String name, String description, String modId,
                      Source source, boolean enabled, List<ConfigEntry> configEntries,
                      boolean forceHasConfig) {
        this(id, name, description, modId, source, enabled, configEntries,
                forceHasConfig, defaultGroupId(source, modId), defaultGroupName(source, modId));
    }

    public UnifiedMod(String id, String name, String description, String modId,
                      Source source, boolean enabled, List<ConfigEntry> configEntries,
                      boolean forceHasConfig, String groupId, String groupName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.modId = modId;
        this.groupId = normalizeGroupValue(groupId, defaultGroupId(source, modId));
        this.groupName = normalizeGroupValue(groupName, defaultGroupName(source, modId));
        this.source = source;
        this.enabled = enabled;
        this.configEntries = configEntries != null ? configEntries : Collections.emptyList();
        this.forceHasConfig = forceHasConfig;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getModId() { return modId; }
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public Source getSource() { return source; }
    public String getFavoriteKey() {
        return source.name().toLowerCase(Locale.US) + ":" + id;
    }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<ConfigEntry> getConfigEntries() { return configEntries; }
    public boolean hasConfig() { return forceHasConfig || !configEntries.isEmpty(); }

    private static String defaultGroupId(Source source, String modId) {
        if (source == Source.INBUILT) {
            return "inbuilt";
        }
        String normalizedModId = trimToEmpty(modId);
        return normalizedModId.isEmpty() ? "external:ungrouped" : "external:" + normalizedModId;
    }

    private static String defaultGroupName(Source source, String modId) {
        return defaultGroupId(source, modId);
    }

    private static String normalizeGroupValue(String value, String fallback) {
        String normalized = trimToEmpty(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
