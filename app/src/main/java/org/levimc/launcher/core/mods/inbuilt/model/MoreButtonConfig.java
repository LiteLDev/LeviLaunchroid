package org.levimc.launcher.core.mods.inbuilt.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.levimc.pojavcontrols.KeyMapper;

import java.util.UUID;

public final class MoreButtonConfig {
    public String id = UUID.randomUUID().toString();
    public String name = "Button";
    public int keyCode;
    public boolean toggle;
    public boolean visible = true;
    public int iconScale = 48;
    public int iconOffsetX;
    public int iconOffsetY;
    public String normalSvg = "";
    public String pressedSvg = "";
    public String normalSvgName = "";
    public String pressedSvgName = "";
    public String normalSvgFile = "";
    public String pressedSvgFile = "";
    public boolean keepNormalColors;
    public boolean keepPressedColors;

    public String overlayKey() {
        return "more_button:" + id;
    }

    public MoreButtonConfig copy() {
        MoreButtonConfig copy = new MoreButtonConfig();
        copy.id = id;
        copy.name = name;
        copy.keyCode = keyCode;
        copy.toggle = toggle;
        copy.visible = visible;
        copy.iconScale = iconScale;
        copy.iconOffsetX = iconOffsetX;
        copy.iconOffsetY = iconOffsetY;
        copy.normalSvg = normalSvg;
        copy.pressedSvg = pressedSvg;
        copy.normalSvgName = normalSvgName;
        copy.pressedSvgName = pressedSvgName;
        copy.normalSvgFile = normalSvgFile;
        copy.pressedSvgFile = pressedSvgFile;
        copy.keepNormalColors = keepNormalColors;
        copy.keepPressedColors = keepPressedColors;
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("key_code", keyCode);
        json.put("key_format", "glfw");
        json.put("toggle", toggle);
        json.put("visible", visible);
        json.put("icon_scale", iconScale);
        json.put("icon_offset_x", iconOffsetX);
        json.put("icon_offset_y", iconOffsetY);
        json.put("normal_svg_name", normalSvgName);
        json.put("pressed_svg_name", pressedSvgName);
        json.put("normal_svg_file", normalSvgFile);
        json.put("pressed_svg_file", pressedSvgFile);
        json.put("keep_normal_colors", keepNormalColors);
        json.put("keep_pressed_colors", keepPressedColors);
        return json;
    }

    public static MoreButtonConfig fromJson(JSONObject json) {
        MoreButtonConfig cfg = new MoreButtonConfig();
        cfg.id = json.optString("id", cfg.id);
        cfg.name = json.optString("name", "Button");
        int storedKey = json.optInt("key_code", 0);
        if ("glfw".equals(json.optString("key_format", ""))) {
            cfg.keyCode = isSupportedMapping(storedKey) ? storedKey : KeyMapper.GLFW_KEY_UNKNOWN;
        } else {
            cfg.keyCode = KeyMapper.fromAndroidKeyCode(storedKey);
        }
        cfg.toggle = json.optBoolean("toggle", false);
        cfg.visible = json.optBoolean("visible", true);
        cfg.iconScale = Math.max(20, Math.min(80, json.optInt("icon_scale", 48)));
        cfg.iconOffsetX = Math.max(-32, Math.min(32, json.optInt("icon_offset_x", 0)));
        cfg.iconOffsetY = Math.max(-32, Math.min(32, json.optInt("icon_offset_y", 0)));
        cfg.normalSvgName = json.optString("normal_svg_name", "");
        cfg.pressedSvgName = json.optString("pressed_svg_name", "");
        cfg.normalSvgFile = json.optString("normal_svg_file", "");
        cfg.pressedSvgFile = json.optString("pressed_svg_file", "");
        cfg.normalSvg = json.optString("normal_svg", "");
        cfg.pressedSvg = json.optString("pressed_svg", "");
        cfg.keepNormalColors = json.optBoolean("keep_normal_colors", false);
        cfg.keepPressedColors = json.optBoolean("keep_pressed_colors", false);
        return cfg;
    }
    public static boolean isSupportedMapping(int code) {
        return KeyMapper.isKeyboardKey(code);
    }


}
