package org.levimc.launcher.core.mods.inbuilt.model;

public class InbuiltMod {
    private final String id;
    private final String name;
    private final String description;
    private final boolean hasConfig;
    private boolean added;

    public InbuiltMod(String id, String name, String description) {
        this(id, name, description, false, false);
    }

    public InbuiltMod(String id, String name, String description, boolean hasConfig, boolean added) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hasConfig = hasConfig;
        this.added = added;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean hasConfig() { return hasConfig; }
    public boolean isAdded() { return added; }
    public void setAdded(boolean added) { this.added = added; }
}
