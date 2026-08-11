package org.levimc.pojavcontrols;

import androidx.annotation.Keep;

import java.util.Arrays;

@Keep
public class ControlData {
    public static final int SPECIALBTN_KEYBOARD = -1;
    public static final int SPECIALBTN_TOGGLECTRL = -2;
    public static final int SPECIALBTN_MOUSEPRI = -3;
    public static final int SPECIALBTN_MOUSESEC = -4;
    public static final int SPECIALBTN_VIRTUALMOUSE = -5;
    public static final int SPECIALBTN_MOUSEMID = -6;
    public static final int SPECIALBTN_SCROLLUP = -7;
    public static final int SPECIALBTN_SCROLLDOWN = -8;
    public static final int SPECIALBTN_MENU = -9;

    public transient boolean isHideable;
    public String dynamicX;
    public String dynamicY;
    public boolean isToggle;
    public boolean passThruEnabled;
    public String name;
    public int[] keycodes;
    public float opacity;
    public int bgColor;
    public int strokeColor;
    public float strokeWidth;
    public float cornerRadius;
    public boolean isSwipeable;
    public boolean displayInGame;
    public boolean displayInMenu;
    public float width;
    public float height;

    public ControlData() {
        this("Button", new int[]{}, "0.5 * ${screen_width}", "0.5 * ${screen_height}", 50, 50);
    }

    public ControlData(String name, int[] keycodes, String dynamicX, String dynamicY, float width, float height) {
        this.name = name;
        this.keycodes = inflateKeycodes(keycodes);
        this.dynamicX = dynamicX;
        this.dynamicY = dynamicY;
        this.width = width;
        this.height = height;
        isToggle = false;
        passThruEnabled = false;
        opacity = 1f;
        bgColor = 0x4D000000;
        strokeColor = 0xFFFFFFFF;
        strokeWidth = 0f;
        cornerRadius = 0f;
        isSwipeable = false;
        displayInGame = true;
        displayInMenu = true;
    }

    public ControlData(ControlData source) {
        dynamicX = source.dynamicX;
        dynamicY = source.dynamicY;
        isToggle = source.isToggle;
        passThruEnabled = source.passThruEnabled;
        name = source.name;
        keycodes = inflateKeycodes(source.keycodes);
        opacity = source.opacity;
        bgColor = source.bgColor;
        strokeColor = source.strokeColor;
        strokeWidth = source.strokeWidth;
        cornerRadius = source.cornerRadius;
        isSwipeable = source.isSwipeable;
        displayInGame = source.displayInGame;
        displayInMenu = source.displayInMenu;
        width = source.width;
        height = source.height;
    }

    public void normalize() {
        if (name == null || name.isBlank()) name = "Button";
        if (dynamicX == null || dynamicX.isBlank()) dynamicX = "0.5 * ${screen_width}";
        if (dynamicY == null || dynamicY.isBlank()) dynamicY = "0.5 * ${screen_height}";
        keycodes = inflateKeycodes(keycodes);
        width = Math.max(16f, Math.min(width <= 0f ? 50f : width, 400f));
        height = Math.max(16f, Math.min(height <= 0f ? 50f : height, 400f));
        opacity = Math.max(0f, Math.min(opacity, 1f));
        strokeWidth = Math.max(0f, Math.min(strokeWidth, 20f));
        cornerRadius = Math.max(0f, Math.min(cornerRadius, 100f));
    }

    private static int[] inflateKeycodes(int[] source) {
        int[] result = new int[]{KeyMapper.GLFW_KEY_UNKNOWN};
        if (source != null) {
            for (int code : source) {
                if (code != KeyMapper.GLFW_KEY_UNKNOWN) {
                    result[0] = code;
                    break;
                }
            }
        }
        return result;
    }

    public int primaryKeycode() {
        return keycodes == null || keycodes.length == 0
                ? KeyMapper.GLFW_KEY_UNKNOWN : keycodes[0];
    }

    @Override
    public String toString() {
        return name + Arrays.toString(keycodes);
    }
}
