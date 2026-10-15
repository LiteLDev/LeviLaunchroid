package org.levimc.launcher.core.content.world;

final class LegacyBlocks {
    private LegacyBlocks() {}

    static String name(int id, int data) {
        String name =
                switch (id) {
                    case 0 -> "air";
                    case 1 -> "stone";
                    case 2 -> "grass_block";
                    case 3 -> "dirt";
                    case 4 -> "cobblestone";
                    case 5 -> "oak_planks";
                    case 7 -> "bedrock";
                    case 8, 9 -> "water";
                    case 10, 11 -> "lava";
                    case 12 -> "sand";
                    case 13 -> "gravel";
                    case 14 -> "gold_ore";
                    case 15 -> "iron_ore";
                    case 16 -> "coal_ore";
                    case 17 -> "oak_log";
                    case 18 -> "oak_leaves";
                    case 20 -> "glass";
                    case 21 -> "lapis_ore";
                    case 22 -> "lapis_block";
                    case 24 -> "sandstone";
                    case 35 -> "white_wool";
                    case 41 -> "gold_block";
                    case 42 -> "iron_block";
                    case 45 -> "bricks";
                    case 48 -> "mossy_cobblestone";
                    case 49 -> "obsidian";
                    case 50 -> "torch";
                    case 52 -> "mob_spawner";
                    case 54 -> "chest";
                    case 56 -> "diamond_ore";
                    case 57 -> "diamond_block";
                    case 58 -> "crafting_table";
                    case 60 -> "farmland";
                    case 61, 62 -> "furnace";
                    case 73, 74 -> "redstone_ore";
                    case 78 -> "snow_layer";
                    case 79 -> "ice";
                    case 80 -> "snow";
                    case 82 -> "clay";
                    case 86 -> "pumpkin";
                    case 87 -> "netherrack";
                    case 88 -> "soul_sand";
                    case 89 -> "glowstone";
                    case 98 -> "stone_bricks";
                    case 103 -> "melon_block";
                    case 110 -> "mycelium";
                    case 112 -> "nether_bricks";
                    case 121 -> "end_stone";
                    case 129 -> "emerald_ore";
                    case 133 -> "emerald_block";
                    case 152 -> "redstone_block";
                    case 153 -> "quartz_ore";
                    case 155 -> "quartz_block";
                    case 159 -> "white_terracotta";
                    case 165 -> "slime";
                    case 168 -> "prismarine";
                    case 169 -> "sea_lantern";
                    case 172 -> "hardened_clay";
                    case 173 -> "coal_block";
                    case 174 -> "packed_ice";
                    case 179 -> "red_sandstone";
                    case 201 -> "purpur_block";
                    case 206 -> "end_bricks";
                    case 213 -> "magma";
                    case 214 -> "nether_wart_block";
                    case 215 -> "red_nether_brick";
                    case 216 -> "bone_block";
                    default -> null;
                };
        return name == null ? null : "minecraft:" + name;
    }
}
