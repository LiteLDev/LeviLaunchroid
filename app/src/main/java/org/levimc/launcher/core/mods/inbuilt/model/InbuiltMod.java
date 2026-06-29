package org.levimc.launcher.core.mods.inbuilt.model;

public class InbuiltMod {
    private final String id;
    private final String name;
    private final String description;
    private final boolean hasConfig;

    public InbuiltMod(String id, String name, String description, boolean hasConfig) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hasConfig = hasConfig;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
