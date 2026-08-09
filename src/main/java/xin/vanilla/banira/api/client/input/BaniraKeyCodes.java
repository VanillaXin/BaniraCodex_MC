package xin.vanilla.banira.api.client.input;

import java.util.ArrayList;
import java.util.List;

/**
 * Banira 暴露给子 mod 的稳定按键常量与组合键工具。
 */
public final class BaniraKeyCodes {
    private BaniraKeyCodes() {
    }

    public static final int KEY_UNKNOWN = -1;

    public static final int KEY_0 = 48;
    public static final int KEY_1 = 49;
    public static final int KEY_2 = 50;
    public static final int KEY_3 = 51;
    public static final int KEY_4 = 52;
    public static final int KEY_5 = 53;
    public static final int KEY_6 = 54;
    public static final int KEY_7 = 55;
    public static final int KEY_8 = 56;
    public static final int KEY_9 = 57;
    public static final int KEY_A = 65;
    public static final int KEY_B = 66;
    public static final int KEY_C = 67;
    public static final int KEY_D = 68;
    public static final int KEY_E = 69;
    public static final int KEY_F = 70;
    public static final int KEY_G = 71;
    public static final int KEY_H = 72;
    public static final int KEY_I = 73;
    public static final int KEY_J = 74;
    public static final int KEY_K = 75;
    public static final int KEY_L = 76;
    public static final int KEY_M = 77;
    public static final int KEY_N = 78;
    public static final int KEY_O = 79;
    public static final int KEY_P = 80;
    public static final int KEY_Q = 81;
    public static final int KEY_R = 82;
    public static final int KEY_S = 83;
    public static final int KEY_T = 84;
    public static final int KEY_U = 85;
    public static final int KEY_V = 86;
    public static final int KEY_W = 87;
    public static final int KEY_X = 88;
    public static final int KEY_Y = 89;
    public static final int KEY_Z = 90;

    public static final int KEY_ESCAPE = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_TAB = 258;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_INSERT = 260;
    public static final int KEY_DELETE = 261;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;

    public static final int KEY_LEFT_SHIFT = 340;
    public static final int KEY_LEFT_CONTROL = 341;
    public static final int KEY_LEFT_ALT = 342;
    public static final int KEY_LEFT_SUPER = 343;
    public static final int KEY_RIGHT_SHIFT = 344;
    public static final int KEY_RIGHT_CONTROL = 345;
    public static final int KEY_RIGHT_ALT = 346;
    public static final int KEY_RIGHT_SUPER = 347;

    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_MIDDLE = 2;

    public static final int MOD_SHIFT = 0x1;
    public static final int MOD_CONTROL = 0x2;
    public static final int MOD_ALT = 0x4;
    public static final int MOD_SUPER = 0x8;
    public static final int MOD_CAPS_LOCK = 0x10;
    public static final int MOD_NUM_LOCK = 0x20;
    public static final int MOD_PRIMARY_MASK = MOD_SHIFT | MOD_CONTROL | MOD_ALT | MOD_SUPER;

    public static boolean hasShiftModifier(int modifiers) {
        return (modifiers & MOD_SHIFT) != 0;
    }

    public static boolean hasControlModifier(int modifiers) {
        return (modifiers & MOD_CONTROL) != 0;
    }

    public static boolean hasAltModifier(int modifiers) {
        return (modifiers & MOD_ALT) != 0;
    }

    public static boolean hasSuperModifier(int modifiers) {
        return (modifiers & MOD_SUPER) != 0;
    }

    /**
     * 组合键监听默认只要求指定修饰键存在，允许 CapsLock/NumLock 等锁定修饰位同时出现。
     */
    public static boolean matchesModifiers(int actualModifiers, int requiredModifiers) {
        return (actualModifiers & requiredModifiers) == requiredModifiers;
    }

    public static boolean matchesExactModifiers(int actualModifiers, int expectedModifiers) {
        return (actualModifiers & MOD_PRIMARY_MASK) == (expectedModifiers & MOD_PRIMARY_MASK);
    }

    public static String formatShortcut(int keyCode, int modifiers) {
        List<String> parts = new ArrayList<>();
        appendModifiers(parts, modifiers);
        String keyName = displayName(keyCode);
        if (!"Unknown".equals(keyName) || parts.isEmpty()) {
            parts.add(keyName);
        }
        return String.join(" + ", parts);
    }

    public static boolean matchesShortcut(String shortcut, int keyCode, int modifiers) {
        if (shortcut == null || shortcut.trim().isEmpty()) {
            return false;
        }
        return normalizeShortcut(shortcut).equals(normalizeShortcut(formatShortcut(keyCode, modifiers)));
    }

    private static String normalizeShortcut(String shortcut) {
        return shortcut.replaceAll("\\s*\\+\\s*", "+").trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String formatModifiers(int modifiers) {
        List<String> parts = new ArrayList<>();
        appendModifiers(parts, modifiers);
        return String.join(" + ", parts);
    }

    public static String displayName(int keyCode) {
        if (keyCode >= KEY_A && keyCode <= KEY_Z) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= KEY_0 && keyCode <= KEY_9) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= 290 && keyCode <= 314) {
            return "F" + (keyCode - 289);
        }
        switch (keyCode) {
            case KEY_ESCAPE:
                return "Esc";
            case KEY_ENTER:
                return "Enter";
            case KEY_TAB:
                return "Tab";
            case KEY_BACKSPACE:
                return "Backspace";
            case KEY_INSERT:
                return "Insert";
            case KEY_DELETE:
                return "Delete";
            case KEY_LEFT:
                return "Left";
            case KEY_RIGHT:
                return "Right";
            case KEY_UP:
                return "Up";
            case KEY_DOWN:
                return "Down";
            case KEY_PAGE_UP:
                return "PageUp";
            case KEY_PAGE_DOWN:
                return "PageDown";
            case KEY_HOME:
                return "Home";
            case KEY_END:
                return "End";
            case KEY_LEFT_CONTROL:
            case KEY_RIGHT_CONTROL:
                return "Ctrl";
            case KEY_LEFT_SHIFT:
            case KEY_RIGHT_SHIFT:
                return "Shift";
            case KEY_LEFT_ALT:
            case KEY_RIGHT_ALT:
                return "Alt";
            case KEY_LEFT_SUPER:
            case KEY_RIGHT_SUPER:
                return "Super";
            default:
                return "Unknown";
        }
    }

    private static void appendModifiers(List<String> parts, int modifiers) {
        if (hasControlModifier(modifiers)) {
            parts.add("Ctrl");
        }
        if (hasShiftModifier(modifiers)) {
            parts.add("Shift");
        }
        if (hasAltModifier(modifiers)) {
            parts.add("Alt");
        }
        if (hasSuperModifier(modifiers)) {
            parts.add("Super");
        }
    }
}
