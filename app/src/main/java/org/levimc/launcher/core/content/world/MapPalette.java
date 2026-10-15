package org.levimc.launcher.core.content.world;

import android.graphics.Color;

import java.util.Locale;

public final class MapPalette {
    private MapPalette() {}

    public static int block(String id, int biome) {
        String n = id.toLowerCase(Locale.ROOT);
        if (WorldChunk.isAir(n)) return Color.TRANSPARENT;
        if (n.startsWith("legacy:")) return 0xffa7a7a7;
        if (n.contains("diamond")) return 0xff49dfd5;
        if (n.contains("emerald")) return 0xff36c67b;
        if (n.contains("lapis")) return 0xff3c63cc;
        if (n.contains("redstone")) return 0xffc44848;
        if (n.contains("copper")) return 0xffbd825b;
        if (n.contains("gold")) return 0xffe8c957;
        if (n.contains("iron")) return 0xffbcae9d;
        if (n.contains("coal")) return 0xff3f4147;
        if (n.contains("ancient_debris")) return 0xff97694f;
        if (n.contains("water")) return 0xff376fa4;
        if (n.contains("lava") || n.contains("fire")) return 0xfff27d2c;
        if (n.contains("snow") || n.contains("quartz") || n.contains("calcite")) return 0xffe4e6e2;
        if (n.contains("ice")) return 0xff95c6dd;
        if (n.contains("bedrock")) return 0xff3a3c42;
        if (n.contains("grass") || n.contains("fern") || n.contains("vine")) return foliage(biome);
        if (n.contains("leaves")) return shade(foliage(biome), .79f);
        if (n.contains("moss") || n.contains("azalea")) return 0xff668f48;
        if (n.contains("dirt") || n.contains("farmland") || n.contains("mud")) return 0xff8d6c4e;
        if (n.contains("mycelium")) return 0xff897184;
        if (n.contains("podzol")) return 0xff87613d;
        if (n.contains("sand") && !n.contains("soul"))
            return n.contains("red") ? 0xffc97d46 : 0xffdfd1a1;
        if (n.contains("gravel")) return 0xff8b8984;
        if (n.contains("deepslate") || n.contains("basalt")) return 0xff55575d;
        if (n.contains("obsidian")) return 0xff2b223c;
        if (n.contains("netherrack") || n.contains("nether_brick")) return 0xff884b49;
        if (n.contains("soul")) return 0xff665143;
        if (n.contains("crimson")) return 0xff974656;
        if (n.contains("warped")) return 0xff419e96;
        if (n.contains("end_stone") || n.contains("end_brick")) return 0xffd6d6a2;
        if (n.contains("purpur") || n.contains("chorus")) return 0xffb18daf;
        if (n.contains("prismarine")) return 0xff6bafa4;
        if (n.contains("glass")) return 0xffb0ccd0;
        if (n.contains("glowstone") || n.contains("lantern") || n.contains("torch"))
            return 0xffe2cc80;
        if (n.contains("amethyst")) return 0xffac85c6;
        if (n.contains("log")
                || n.contains("wood")
                || n.contains("plank")
                || n.contains("chest")
                || n.contains("barrel")) {
            if (n.contains("birch")) return 0xffd8cc9c;
            if (n.contains("spruce") || n.contains("dark_oak")) return 0xff77573a;
            if (n.contains("cherry")) return 0xffd5aaa5;
            return 0xffaa8958;
        }
        String[] colors = {
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
        };
        int[] rgb = {
            0xffdeded8,
            0xffd68a44,
            0xffb565b9,
            0xff76b4d7,
            0xffdfc763,
            0xff9dc955,
            0xffd79baf,
            0xff67686a,
            0xffaaa9a0,
            0xff4c9eab,
            0xff8d5cb4,
            0xff4d65b1,
            0xff8a6547,
            0xff69894c,
            0xffb65d53,
            0xff33363b
        };
        for (int i = 0; i < colors.length; i++) if (n.contains(colors[i] + "_")) return rgb[i];
        if (n.contains("brick")) return 0xffa87867;
        if (n.contains("stone")
                || n.contains("andesite")
                || n.contains("diorite")
                || n.contains("tuff")) return 0xff91918a;
        return 0xff96918a;
    }

    public static int foliage(int biome) {
        return switch (biome) {
            case 2, 17, 130 -> 0xffb8ac66;
            case 6, 134 -> 0xff759253;
            case 7, 24, 0, 10 -> 0xff6a9d57;
            case 12, 13, 26, 140 -> 0xff8fae88;
            case 5, 19, 30, 31, 32, 33, 160, 161 -> 0xff6e9d78;
            case 21, 22, 23, 149, 151, 168, 169 -> 0xff55a743;
            case 35, 36, 163, 164 -> 0xffaaa65c;
            case 37, 38, 39, 165, 166, 167 -> 0xffa18d56;
            default -> 0xff80ad54;
        };
    }

    public static int biome(int id) {
        if (id < 0) return 0x00000000;
        float hue = (float) ((id * .61803398875) % 1) * 360;
        return Color.HSVToColor(new float[] {hue, .39f, .86f});
    }

    public static String biomeName(int id) {
        return switch (id) {
            case -1 -> "Unavailable";
            case 0 -> "Ocean";
            case 1 -> "Plains";
            case 2 -> "Desert";
            case 3 -> "Windswept hills";
            case 4 -> "Forest";
            case 5 -> "Taiga";
            case 6 -> "Swamp";
            case 7 -> "River";
            case 8 -> "Nether wastes";
            case 9 -> "The End";
            case 10 -> "Frozen ocean";
            case 11 -> "Frozen river";
            case 12 -> "Snowy plains";
            case 14 -> "Mushroom fields";
            case 16 -> "Beach";
            case 21 -> "Jungle";
            case 24 -> "Deep ocean";
            case 27 -> "Birch forest";
            case 29 -> "Dark forest";
            case 30 -> "Snowy taiga";
            case 35 -> "Savanna";
            case 37 -> "Badlands";
            case 178 -> "Soul sand valley";
            case 179 -> "Crimson forest";
            case 180 -> "Warped forest";
            case 181 -> "Basalt deltas";
            default -> "Biome " + id;
        };
    }

    public static int shade(int color, float multiplier) {
        multiplier = Math.max(.15f, Math.min(1.8f, multiplier));
        return Color.argb(
                Color.alpha(color),
                Math.min(255, (int) (Color.red(color) * multiplier)),
                Math.min(255, (int) (Color.green(color) * multiplier)),
                Math.min(255, (int) (Color.blue(color) * multiplier)));
    }

    public static int emission(String id) {
        if (id.contains("soul_torch") || id.contains("soul_lantern") || id.contains("soul_fire"))
            return 10;
        if (id.contains("redstone_torch")) return 7;
        if (id.contains("torch")) return 14;
        if (id.contains("glow_lichen")) return 7;
        if (id.contains("glowstone")
                || id.contains("sea_lantern")
                || id.contains("shroomlight")
                || id.contains("lava")
                || id.contains("beacon")
                || id.contains("lantern")
                || id.contains("froglight")
                || id.contains("jack_o_lantern")
                || id.contains("fire")) return 15;
        if (id.contains("magma")) return 3;
        return 0;
    }
}
