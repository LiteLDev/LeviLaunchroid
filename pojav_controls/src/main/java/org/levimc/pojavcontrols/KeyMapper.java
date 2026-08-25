package org.levimc.pojavcontrols;

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KeyMapper {
    public static final int GLFW_KEY_UNKNOWN = 0;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_APOSTROPHE = 39;
    public static final int GLFW_KEY_COMMA = 44;
    public static final int GLFW_KEY_MINUS = 45;
    public static final int GLFW_KEY_PERIOD = 46;
    public static final int GLFW_KEY_SLASH = 47;
    public static final int GLFW_KEY_SEMICOLON = 59;
    public static final int GLFW_KEY_EQUAL = 61;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_D = 68;
    public static final int GLFW_KEY_E = 69;
    public static final int GLFW_KEY_Q = 81;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_T = 84;
    public static final int GLFW_KEY_W = 87;
    public static final int GLFW_KEY_LEFT_BRACKET = 91;
    public static final int GLFW_KEY_BACKSLASH = 92;
    public static final int GLFW_KEY_RIGHT_BRACKET = 93;
    public static final int GLFW_KEY_GRAVE_ACCENT = 96;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static final int GLFW_KEY_ENTER = 257;
    public static final int GLFW_KEY_TAB = 258;
    public static final int GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_INSERT = 260;
    public static final int GLFW_KEY_DELETE = 261;
    public static final int GLFW_KEY_RIGHT = 262;
    public static final int GLFW_KEY_LEFT = 263;
    public static final int GLFW_KEY_DOWN = 264;
    public static final int GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_PAGE_UP = 266;
    public static final int GLFW_KEY_PAGE_DOWN = 267;
    public static final int GLFW_KEY_HOME = 268;
    public static final int GLFW_KEY_END = 269;
    public static final int GLFW_KEY_CAPS_LOCK = 280;
    public static final int GLFW_KEY_SCROLL_LOCK = 281;
    public static final int GLFW_KEY_NUM_LOCK = 282;
    public static final int GLFW_KEY_PRINT_SCREEN = 283;
    public static final int GLFW_KEY_PAUSE = 284;
    public static final int GLFW_KEY_F1 = 290;
    public static final int GLFW_KEY_F12 = 301;
    public static final int GLFW_KEY_F24 = 313;
    public static final int GLFW_KEY_KP_0 = 320;
    public static final int GLFW_KEY_KP_9 = 329;
    public static final int GLFW_KEY_KP_DECIMAL = 330;
    public static final int GLFW_KEY_KP_DIVIDE = 331;
    public static final int GLFW_KEY_KP_MULTIPLY = 332;
    public static final int GLFW_KEY_KP_SUBTRACT = 333;
    public static final int GLFW_KEY_KP_ADD = 334;
    public static final int GLFW_KEY_KP_ENTER = 335;
    public static final int GLFW_KEY_KP_EQUAL = 336;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_LEFT_CONTROL = 341;
    public static final int GLFW_KEY_LEFT_ALT = 342;
    public static final int GLFW_KEY_LEFT_SUPER = 343;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_KEY_RIGHT_CONTROL = 345;
    public static final int GLFW_KEY_RIGHT_ALT = 346;
    public static final int GLFW_KEY_RIGHT_SUPER = 347;
    public static final int GLFW_KEY_MENU = 348;

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
        if (glfwCode >= GLFW_KEY_F1 && glfwCode <= GLFW_KEY_F24) return 112 + glfwCode - GLFW_KEY_F1;
        if (glfwCode >= GLFW_KEY_KP_0 && glfwCode <= GLFW_KEY_KP_9) return 96 + glfwCode - GLFW_KEY_KP_0;
        return switch (glfwCode) {
            case GLFW_KEY_SPACE -> 32;
            case GLFW_KEY_ESCAPE -> 27;
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> 13;
            case GLFW_KEY_TAB -> 9;
            case GLFW_KEY_BACKSPACE -> 8;
            case GLFW_KEY_INSERT -> 45;
            case GLFW_KEY_DELETE -> 46;
            case GLFW_KEY_RIGHT -> 39;
            case GLFW_KEY_LEFT -> 37;
            case GLFW_KEY_DOWN -> 40;
            case GLFW_KEY_UP -> 38;
            case GLFW_KEY_PAGE_UP -> 33;
            case GLFW_KEY_PAGE_DOWN -> 34;
            case GLFW_KEY_HOME -> 36;
            case GLFW_KEY_END -> 35;
            case GLFW_KEY_CAPS_LOCK -> 20;
            case GLFW_KEY_SCROLL_LOCK -> 145;
            case GLFW_KEY_NUM_LOCK -> 144;
            case GLFW_KEY_PRINT_SCREEN -> 44;
            case GLFW_KEY_PAUSE -> 19;
            case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> 16;
            case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> 17;
            case GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> 18;
            case GLFW_KEY_LEFT_SUPER -> 91;
            case GLFW_KEY_RIGHT_SUPER -> 92;
            case GLFW_KEY_MENU -> 93;
            case GLFW_KEY_APOSTROPHE -> 222;
            case GLFW_KEY_COMMA -> 188;
            case GLFW_KEY_MINUS -> 189;
            case GLFW_KEY_PERIOD -> 190;
            case GLFW_KEY_SLASH -> 191;
            case GLFW_KEY_SEMICOLON -> 186;
            case GLFW_KEY_EQUAL, GLFW_KEY_KP_EQUAL -> 187;
            case GLFW_KEY_LEFT_BRACKET -> 219;
            case GLFW_KEY_BACKSLASH -> 220;
            case GLFW_KEY_RIGHT_BRACKET -> 221;
            case GLFW_KEY_GRAVE_ACCENT -> 192;
            case GLFW_KEY_KP_DECIMAL -> 110;
            case GLFW_KEY_KP_DIVIDE -> 111;
            case GLFW_KEY_KP_MULTIPLY -> 106;
            case GLFW_KEY_KP_SUBTRACT -> 109;
            case GLFW_KEY_KP_ADD -> 107;
            default -> GLFW_KEY_UNKNOWN;
        };
    }

    public static int toAndroidKeyCode(int glfwCode) {
        if (glfwCode >= 'A' && glfwCode <= 'Z') return KeyEvent.KEYCODE_A + glfwCode - 'A';
        if (glfwCode >= '0' && glfwCode <= '9') return KeyEvent.KEYCODE_0 + glfwCode - '0';
        if (glfwCode >= GLFW_KEY_F1 && glfwCode <= GLFW_KEY_F12) return KeyEvent.KEYCODE_F1 + glfwCode - GLFW_KEY_F1;
        if (glfwCode > GLFW_KEY_F12 && glfwCode <= GLFW_KEY_F24) return KeyEvent.KEYCODE_UNKNOWN;
        if (glfwCode >= GLFW_KEY_KP_0 && glfwCode <= GLFW_KEY_KP_9) return KeyEvent.KEYCODE_NUMPAD_0 + glfwCode - GLFW_KEY_KP_0;
        return switch (glfwCode) {
            case GLFW_KEY_SPACE -> KeyEvent.KEYCODE_SPACE;
            case GLFW_KEY_ESCAPE -> KeyEvent.KEYCODE_ESCAPE;
            case GLFW_KEY_ENTER -> KeyEvent.KEYCODE_ENTER;
            case GLFW_KEY_TAB -> KeyEvent.KEYCODE_TAB;
            case GLFW_KEY_BACKSPACE -> KeyEvent.KEYCODE_DEL;
            case GLFW_KEY_INSERT -> KeyEvent.KEYCODE_INSERT;
            case GLFW_KEY_DELETE -> KeyEvent.KEYCODE_FORWARD_DEL;
            case GLFW_KEY_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT;
            case GLFW_KEY_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT;
            case GLFW_KEY_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN;
            case GLFW_KEY_UP -> KeyEvent.KEYCODE_DPAD_UP;
            case GLFW_KEY_PAGE_UP -> KeyEvent.KEYCODE_PAGE_UP;
            case GLFW_KEY_PAGE_DOWN -> KeyEvent.KEYCODE_PAGE_DOWN;
            case GLFW_KEY_HOME -> KeyEvent.KEYCODE_MOVE_HOME;
            case GLFW_KEY_END -> KeyEvent.KEYCODE_MOVE_END;
            case GLFW_KEY_CAPS_LOCK -> KeyEvent.KEYCODE_CAPS_LOCK;
            case GLFW_KEY_SCROLL_LOCK -> KeyEvent.KEYCODE_SCROLL_LOCK;
            case GLFW_KEY_NUM_LOCK -> KeyEvent.KEYCODE_NUM_LOCK;
            case GLFW_KEY_PRINT_SCREEN -> KeyEvent.KEYCODE_SYSRQ;
            case GLFW_KEY_PAUSE -> KeyEvent.KEYCODE_BREAK;
            case GLFW_KEY_LEFT_SHIFT -> KeyEvent.KEYCODE_SHIFT_LEFT;
            case GLFW_KEY_RIGHT_SHIFT -> KeyEvent.KEYCODE_SHIFT_RIGHT;
            case GLFW_KEY_LEFT_CONTROL -> KeyEvent.KEYCODE_CTRL_LEFT;
            case GLFW_KEY_RIGHT_CONTROL -> KeyEvent.KEYCODE_CTRL_RIGHT;
            case GLFW_KEY_LEFT_ALT -> KeyEvent.KEYCODE_ALT_LEFT;
            case GLFW_KEY_RIGHT_ALT -> KeyEvent.KEYCODE_ALT_RIGHT;
            case GLFW_KEY_LEFT_SUPER -> KeyEvent.KEYCODE_META_LEFT;
            case GLFW_KEY_RIGHT_SUPER -> KeyEvent.KEYCODE_META_RIGHT;
            case GLFW_KEY_MENU -> KeyEvent.KEYCODE_MENU;
            case GLFW_KEY_APOSTROPHE -> KeyEvent.KEYCODE_APOSTROPHE;
            case GLFW_KEY_COMMA -> KeyEvent.KEYCODE_COMMA;
            case GLFW_KEY_MINUS -> KeyEvent.KEYCODE_MINUS;
            case GLFW_KEY_PERIOD -> KeyEvent.KEYCODE_PERIOD;
            case GLFW_KEY_SLASH -> KeyEvent.KEYCODE_SLASH;
            case GLFW_KEY_SEMICOLON -> KeyEvent.KEYCODE_SEMICOLON;
            case GLFW_KEY_EQUAL -> KeyEvent.KEYCODE_EQUALS;
            case GLFW_KEY_LEFT_BRACKET -> KeyEvent.KEYCODE_LEFT_BRACKET;
            case GLFW_KEY_BACKSLASH -> KeyEvent.KEYCODE_BACKSLASH;
            case GLFW_KEY_RIGHT_BRACKET -> KeyEvent.KEYCODE_RIGHT_BRACKET;
            case GLFW_KEY_GRAVE_ACCENT -> KeyEvent.KEYCODE_GRAVE;
            case GLFW_KEY_KP_DECIMAL -> KeyEvent.KEYCODE_NUMPAD_DOT;
            case GLFW_KEY_KP_DIVIDE -> KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            case GLFW_KEY_KP_MULTIPLY -> KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
            case GLFW_KEY_KP_SUBTRACT -> KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            case GLFW_KEY_KP_ADD -> KeyEvent.KEYCODE_NUMPAD_ADD;
            case GLFW_KEY_KP_ENTER -> KeyEvent.KEYCODE_NUMPAD_ENTER;
            case GLFW_KEY_KP_EQUAL -> KeyEvent.KEYCODE_NUMPAD_EQUALS;
            default -> KeyEvent.KEYCODE_UNKNOWN;
        };
    }

    public static int fromAndroidKeyCode(int androidCode) {
        if (androidCode >= KeyEvent.KEYCODE_A && androidCode <= KeyEvent.KEYCODE_Z) {
            return 'A' + androidCode - KeyEvent.KEYCODE_A;
        }
        if (androidCode >= KeyEvent.KEYCODE_0 && androidCode <= KeyEvent.KEYCODE_9) {
            return '0' + androidCode - KeyEvent.KEYCODE_0;
        }
        if (androidCode >= KeyEvent.KEYCODE_F1 && androidCode <= KeyEvent.KEYCODE_F12) {
            return GLFW_KEY_F1 + androidCode - KeyEvent.KEYCODE_F1;
        }
        if (androidCode >= KeyEvent.KEYCODE_NUMPAD_0 && androidCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return GLFW_KEY_KP_0 + androidCode - KeyEvent.KEYCODE_NUMPAD_0;
        }
        return switch (androidCode) {
            case KeyEvent.KEYCODE_SPACE -> GLFW_KEY_SPACE;
            case KeyEvent.KEYCODE_ESCAPE -> GLFW_KEY_ESCAPE;
            case KeyEvent.KEYCODE_ENTER -> GLFW_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB -> GLFW_KEY_TAB;
            case KeyEvent.KEYCODE_DEL -> GLFW_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_INSERT -> GLFW_KEY_INSERT;
            case KeyEvent.KEYCODE_FORWARD_DEL -> GLFW_KEY_DELETE;
            case KeyEvent.KEYCODE_DPAD_RIGHT -> GLFW_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT -> GLFW_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_DOWN -> GLFW_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP -> GLFW_KEY_UP;
            case KeyEvent.KEYCODE_PAGE_UP -> GLFW_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN -> GLFW_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME -> GLFW_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END -> GLFW_KEY_END;
            case KeyEvent.KEYCODE_CAPS_LOCK -> GLFW_KEY_CAPS_LOCK;
            case KeyEvent.KEYCODE_SCROLL_LOCK -> GLFW_KEY_SCROLL_LOCK;
            case KeyEvent.KEYCODE_NUM_LOCK -> GLFW_KEY_NUM_LOCK;
            case KeyEvent.KEYCODE_SYSRQ -> GLFW_KEY_PRINT_SCREEN;
            case KeyEvent.KEYCODE_BREAK -> GLFW_KEY_PAUSE;
            case KeyEvent.KEYCODE_SHIFT_LEFT -> GLFW_KEY_LEFT_SHIFT;
            case KeyEvent.KEYCODE_SHIFT_RIGHT -> GLFW_KEY_RIGHT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_LEFT -> GLFW_KEY_LEFT_CONTROL;
            case KeyEvent.KEYCODE_CTRL_RIGHT -> GLFW_KEY_RIGHT_CONTROL;
            case KeyEvent.KEYCODE_ALT_LEFT -> GLFW_KEY_LEFT_ALT;
            case KeyEvent.KEYCODE_ALT_RIGHT -> GLFW_KEY_RIGHT_ALT;
            case KeyEvent.KEYCODE_META_LEFT -> GLFW_KEY_LEFT_SUPER;
            case KeyEvent.KEYCODE_META_RIGHT -> GLFW_KEY_RIGHT_SUPER;
            case KeyEvent.KEYCODE_MENU -> GLFW_KEY_MENU;
            case KeyEvent.KEYCODE_APOSTROPHE -> GLFW_KEY_APOSTROPHE;
            case KeyEvent.KEYCODE_COMMA -> GLFW_KEY_COMMA;
            case KeyEvent.KEYCODE_MINUS -> GLFW_KEY_MINUS;
            case KeyEvent.KEYCODE_PERIOD -> GLFW_KEY_PERIOD;
            case KeyEvent.KEYCODE_SLASH -> GLFW_KEY_SLASH;
            case KeyEvent.KEYCODE_SEMICOLON -> GLFW_KEY_SEMICOLON;
            case KeyEvent.KEYCODE_EQUALS -> GLFW_KEY_EQUAL;
            case KeyEvent.KEYCODE_LEFT_BRACKET -> GLFW_KEY_LEFT_BRACKET;
            case KeyEvent.KEYCODE_BACKSLASH -> GLFW_KEY_BACKSLASH;
            case KeyEvent.KEYCODE_RIGHT_BRACKET -> GLFW_KEY_RIGHT_BRACKET;
            case KeyEvent.KEYCODE_GRAVE -> GLFW_KEY_GRAVE_ACCENT;
            case KeyEvent.KEYCODE_NUMPAD_DOT -> GLFW_KEY_KP_DECIMAL;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE -> GLFW_KEY_KP_DIVIDE;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> GLFW_KEY_KP_MULTIPLY;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> GLFW_KEY_KP_SUBTRACT;
            case KeyEvent.KEYCODE_NUMPAD_ADD -> GLFW_KEY_KP_ADD;
            case KeyEvent.KEYCODE_NUMPAD_ENTER -> GLFW_KEY_KP_ENTER;
            case KeyEvent.KEYCODE_NUMPAD_EQUALS -> GLFW_KEY_KP_EQUAL;
            default -> GLFW_KEY_UNKNOWN;
        };
    }

    public static boolean isKeyboardKey(int glfwCode) {
        return glfwCode > GLFW_KEY_UNKNOWN && toBedrock(glfwCode) != GLFW_KEY_UNKNOWN;
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
        entries.add(new Entry("Insert", GLFW_KEY_INSERT));
        entries.add(new Entry("Delete", GLFW_KEY_DELETE));
        entries.add(new Entry("Home", GLFW_KEY_HOME));
        entries.add(new Entry("End", GLFW_KEY_END));
        entries.add(new Entry("Page Up", GLFW_KEY_PAGE_UP));
        entries.add(new Entry("Page Down", GLFW_KEY_PAGE_DOWN));
        entries.add(new Entry("Left Shift", GLFW_KEY_LEFT_SHIFT));
        entries.add(new Entry("Right Shift", GLFW_KEY_RIGHT_SHIFT));
        entries.add(new Entry("Left Control", GLFW_KEY_LEFT_CONTROL));
        entries.add(new Entry("Right Control", GLFW_KEY_RIGHT_CONTROL));
        entries.add(new Entry("Left Alt", GLFW_KEY_LEFT_ALT));
        entries.add(new Entry("Right Alt", GLFW_KEY_RIGHT_ALT));
        entries.add(new Entry("Left Super", GLFW_KEY_LEFT_SUPER));
        entries.add(new Entry("Right Super", GLFW_KEY_RIGHT_SUPER));
        entries.add(new Entry("Menu", GLFW_KEY_MENU));
        entries.add(new Entry("Caps Lock", GLFW_KEY_CAPS_LOCK));
        entries.add(new Entry("Num Lock", GLFW_KEY_NUM_LOCK));
        entries.add(new Entry("Scroll Lock", GLFW_KEY_SCROLL_LOCK));
        entries.add(new Entry("Print Screen", GLFW_KEY_PRINT_SCREEN));
        entries.add(new Entry("Pause", GLFW_KEY_PAUSE));
        entries.add(new Entry("Up", GLFW_KEY_UP));
        entries.add(new Entry("Down", GLFW_KEY_DOWN));
        entries.add(new Entry("Left", GLFW_KEY_LEFT));
        entries.add(new Entry("Right", GLFW_KEY_RIGHT));
        entries.add(new Entry("Apostrophe", GLFW_KEY_APOSTROPHE));
        entries.add(new Entry("Comma", GLFW_KEY_COMMA));
        entries.add(new Entry("Minus", GLFW_KEY_MINUS));
        entries.add(new Entry("Period", GLFW_KEY_PERIOD));
        entries.add(new Entry("Slash", GLFW_KEY_SLASH));
        entries.add(new Entry("Semicolon", GLFW_KEY_SEMICOLON));
        entries.add(new Entry("Equal", GLFW_KEY_EQUAL));
        entries.add(new Entry("Left Bracket", GLFW_KEY_LEFT_BRACKET));
        entries.add(new Entry("Backslash", GLFW_KEY_BACKSLASH));
        entries.add(new Entry("Right Bracket", GLFW_KEY_RIGHT_BRACKET));
        entries.add(new Entry("Grave Accent", GLFW_KEY_GRAVE_ACCENT));
        for (char c = 'A'; c <= 'Z'; c++) entries.add(new Entry(String.valueOf(c), c));
        for (char c = '0'; c <= '9'; c++) entries.add(new Entry(String.valueOf(c), c));
        for (int code = GLFW_KEY_F1; code <= GLFW_KEY_F24; code++) {
            entries.add(new Entry("F" + (code - GLFW_KEY_F1 + 1), code));
        }
        for (int code = GLFW_KEY_KP_0; code <= GLFW_KEY_KP_9; code++) {
            entries.add(new Entry("Numpad " + (code - GLFW_KEY_KP_0), code));
        }
        entries.add(new Entry("Numpad Decimal", GLFW_KEY_KP_DECIMAL));
        entries.add(new Entry("Numpad Divide", GLFW_KEY_KP_DIVIDE));
        entries.add(new Entry("Numpad Multiply", GLFW_KEY_KP_MULTIPLY));
        entries.add(new Entry("Numpad Subtract", GLFW_KEY_KP_SUBTRACT));
        entries.add(new Entry("Numpad Add", GLFW_KEY_KP_ADD));
        entries.add(new Entry("Numpad Enter", GLFW_KEY_KP_ENTER));
        entries.add(new Entry("Numpad Equal", GLFW_KEY_KP_EQUAL));
        return Collections.unmodifiableList(entries);
    }
}
