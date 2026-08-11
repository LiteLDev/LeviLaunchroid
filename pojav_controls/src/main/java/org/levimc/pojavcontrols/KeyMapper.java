package org.levimc.pojavcontrols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KeyMapper {
    public static final int GLFW_KEY_UNKNOWN = 0;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_D = 68;
    public static final int GLFW_KEY_E = 69;
    public static final int GLFW_KEY_Q = 81;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_T = 84;
    public static final int GLFW_KEY_W = 87;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static final int GLFW_KEY_ENTER = 257;
    public static final int GLFW_KEY_TAB = 258;
    public static final int GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_RIGHT = 262;
    public static final int GLFW_KEY_LEFT = 263;
    public static final int GLFW_KEY_DOWN = 264;
    public static final int GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_F1 = 290;
    public static final int GLFW_KEY_F5 = 294;
    public static final int GLFW_KEY_F12 = 301;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_LEFT_CONTROL = 341;
    public static final int GLFW_KEY_LEFT_ALT = 342;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_KEY_RIGHT_CONTROL = 345;
    public static final int GLFW_KEY_RIGHT_ALT = 346;

    public static final class Entry {
        public final String name;
        public final int glfwCode;

        Entry(String name, int glfwCode) {
            this.name = name;
            this.glfwCode = glfwCode;
        }
    }

    private static final List<Entry> ENTRIES = buildEntries();

    private KeyMapper() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static int toBedrock(int glfwCode) {
        if (glfwCode >= 'A' && glfwCode <= 'Z') return glfwCode;
        if (glfwCode >= '0' && glfwCode <= '9') return glfwCode;
        if (glfwCode >= GLFW_KEY_F1 && glfwCode <= GLFW_KEY_F12) return 112 + glfwCode - GLFW_KEY_F1;
        return switch (glfwCode) {
            case GLFW_KEY_SPACE -> 32;
            case GLFW_KEY_ESCAPE -> 27;
            case GLFW_KEY_ENTER -> 13;
            case GLFW_KEY_TAB -> 9;
            case GLFW_KEY_BACKSPACE -> 8;
            case GLFW_KEY_RIGHT -> 39;
            case GLFW_KEY_LEFT -> 37;
            case GLFW_KEY_DOWN -> 40;
            case GLFW_KEY_UP -> 38;
            case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> 16;
            case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> 17;
            case GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> 18;
            case 39 -> 222;
            case 44 -> 188;
            case 45 -> 189;
            case 46 -> 190;
            case 47 -> 191;
            case 59 -> 186;
            case 61 -> 187;
            case 91 -> 219;
            case 92 -> 220;
            case 93 -> 221;
            case 96 -> 192;
            default -> GLFW_KEY_UNKNOWN;
        };
    }

    public static String nameOf(int code) {
        for (Entry entry : ENTRIES) if (entry.glfwCode == code) return entry.name;
        return code == GLFW_KEY_UNKNOWN ? "None" : Integer.toString(code);
    }

    private static List<Entry> buildEntries() {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry("None", GLFW_KEY_UNKNOWN));
        entries.add(new Entry("SPECIAL_Keyboard", ControlData.SPECIALBTN_KEYBOARD));
        entries.add(new Entry("SPECIAL_GUI", ControlData.SPECIALBTN_TOGGLECTRL));
        entries.add(new Entry("SPECIAL_Primary mouse", ControlData.SPECIALBTN_MOUSEPRI));
        entries.add(new Entry("SPECIAL_Secondary mouse", ControlData.SPECIALBTN_MOUSESEC));
        entries.add(new Entry("SPECIAL_Middle mouse", ControlData.SPECIALBTN_MOUSEMID));
        entries.add(new Entry("SPECIAL_Virtual mouse", ControlData.SPECIALBTN_VIRTUALMOUSE));
        entries.add(new Entry("SPECIAL_Scroll up", ControlData.SPECIALBTN_SCROLLUP));
        entries.add(new Entry("SPECIAL_Scroll down", ControlData.SPECIALBTN_SCROLLDOWN));
        entries.add(new Entry("SPECIAL_Menu", ControlData.SPECIALBTN_MENU));
        entries.add(new Entry("Space", GLFW_KEY_SPACE));
        entries.add(new Entry("Escape", GLFW_KEY_ESCAPE));
        entries.add(new Entry("Enter", GLFW_KEY_ENTER));
        entries.add(new Entry("Tab", GLFW_KEY_TAB));
        entries.add(new Entry("Backspace", GLFW_KEY_BACKSPACE));
        entries.add(new Entry("Left Shift", GLFW_KEY_LEFT_SHIFT));
        entries.add(new Entry("Left Control", GLFW_KEY_LEFT_CONTROL));
        entries.add(new Entry("Left Alt", GLFW_KEY_LEFT_ALT));
        entries.add(new Entry("Up", GLFW_KEY_UP));
        entries.add(new Entry("Down", GLFW_KEY_DOWN));
        entries.add(new Entry("Left", GLFW_KEY_LEFT));
        entries.add(new Entry("Right", GLFW_KEY_RIGHT));
        for (char c = 'A'; c <= 'Z'; c++) entries.add(new Entry(String.valueOf(c), c));
        for (char c = '0'; c <= '9'; c++) entries.add(new Entry(String.valueOf(c), c));
        for (int code = GLFW_KEY_F1; code <= GLFW_KEY_F12; code++) {
            entries.add(new Entry("F" + (code - GLFW_KEY_F1 + 1), code));
        }
        return Collections.unmodifiableList(entries);
    }
}
