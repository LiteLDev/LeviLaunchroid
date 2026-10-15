package org.levimc.launcher.core.content.world;

public final class MapSettings {
    public enum Layer {
        SATELLITE,
        CAVE,
        SLIME,
        HEIGHTMAP,
        BIOMES,
        FOLIAGE,
        XRAY,
        LIGHT
    }

    public Layer layer = Layer.SATELLITE;
    public int dimension;
    public int y = 64;
    public int lowerY = -64;
    public int upperY = 319;
    public boolean grid;
    public boolean coordinates = true;
    public boolean players = true;
    public boolean entities;
    public boolean blockEntities;
    public boolean leaves = true;
    public boolean water = true;
    public boolean shading = true;
    public boolean slice;
    public boolean slimeOverlay;
    public String xray =
            "diamond_ore,ancient_debris,emerald_ore,gold_ore,iron_ore,redstone_ore,lapis_ore,coal_ore,copper_ore";
    public String entityFilter = "";
    public String blockEntityFilter = "";
    public float brightness = 1f;

    public MapSettings copy() {
        MapSettings c = new MapSettings();
        c.layer = layer;
        c.dimension = dimension;
        c.y = y;
        c.lowerY = lowerY;
        c.upperY = upperY;
        c.grid = grid;
        c.coordinates = coordinates;
        c.players = players;
        c.entities = entities;
        c.blockEntities = blockEntities;
        c.leaves = leaves;
        c.water = water;
        c.shading = shading;
        c.slice = slice;
        c.slimeOverlay = slimeOverlay;
        c.xray = xray;
        c.entityFilter = entityFilter;
        c.blockEntityFilter = blockEntityFilter;
        c.brightness = brightness;
        return c;
    }

    public String renderKey() {
        return layer
                + ":"
                + dimension
                + ":"
                + y
                + ":"
                + lowerY
                + ":"
                + upperY
                + ":"
                + leaves
                + ":"
                + water
                + ":"
                + shading
                + ":"
                + slice
                + ":"
                + xray
                + ":"
                + brightness;
    }
}
