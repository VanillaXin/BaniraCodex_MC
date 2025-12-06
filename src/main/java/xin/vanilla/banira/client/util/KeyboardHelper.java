package xin.vanilla.banira.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.common.data.FixedList;

import java.util.HashMap;
import java.util.Map;

public class KeyboardHelper {
    private final Map<Integer, FixedList<Boolean>> keyPressedRecords = new HashMap<>();

    private static long getWindowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    private static boolean checkKeyPressing(long windowHandle, int key) {
        return GLFW.glfwGetKey(windowHandle, key) == GLFW.GLFW_PRESS;
    }

    public void tick() {
        // 更新所有已记录的按键状态
        long windowHandle = getWindowHandle();
        for (Map.Entry<Integer, FixedList<Boolean>> entry : keyPressedRecords.entrySet()) {
            int key = entry.getKey();
            FixedList<Boolean> record = entry.getValue();
            boolean pressing = checkKeyPressing(windowHandle, key);
            record.add(pressing);
        }
    }

    public void tick(int... keys) {
        // 更新指定按键的状态
        long windowHandle = getWindowHandle();
        for (int key : keys) {
            FixedList<Boolean> record = keyPressedRecords.computeIfAbsent(key, k -> new FixedList<>(5));
            boolean pressing = checkKeyPressing(windowHandle, key);
            record.add(pressing);
        }
    }

    public boolean isPressing(int key) {
        FixedList<Boolean> record = keyPressedRecords.get(key);
        if (record == null) {
            return isKeyPressing(key);
        }
        return Boolean.TRUE.equals(record.getLast());
    }

    public boolean isPressed(int key) {
        FixedList<Boolean> record = keyPressedRecords.get(key);
        if (record == null || record.size() <= 1) {
            return false;
        }
        return Boolean.TRUE.equals(record.get(record.size() - 2)) && !Boolean.TRUE.equals(record.getLast());
    }

    public void registerKey(int key) {
        keyPressedRecords.computeIfAbsent(key, k -> new FixedList<>(5));
    }

    public void unregisterKey(int key) {
        keyPressedRecords.remove(key);
    }

    public void clear() {
        keyPressedRecords.clear();
    }

    // 常用按键的便捷方法
    public boolean isShiftPressing() {
        return isPressing(GLFW.GLFW_KEY_LEFT_SHIFT) || isPressing(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isShiftPressed() {
        return isPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || isPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isCtrlPressing() {
        return isPressing(GLFW.GLFW_KEY_LEFT_CONTROL) || isPressing(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean isCtrlPressed() {
        return isPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean isAltPressing() {
        return isPressing(GLFW.GLFW_KEY_LEFT_ALT) || isPressing(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public boolean isAltPressed() {
        return isPressed(GLFW.GLFW_KEY_LEFT_ALT) || isPressed(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public boolean isEnterPressing() {
        return isPressing(GLFW.GLFW_KEY_ENTER);
    }

    public boolean isEnterPressed() {
        return isPressed(GLFW.GLFW_KEY_ENTER);
    }

    public boolean isSpacePressing() {
        return isPressing(GLFW.GLFW_KEY_SPACE);
    }

    public boolean isSpacePressed() {
        return isPressed(GLFW.GLFW_KEY_SPACE);
    }

    public boolean isEscapePressing() {
        return isPressing(GLFW.GLFW_KEY_ESCAPE);
    }

    public boolean isEscapePressed() {
        return isPressed(GLFW.GLFW_KEY_ESCAPE);
    }

    public boolean isBackspacePressing() {
        return isPressing(GLFW.GLFW_KEY_BACKSPACE);
    }

    public boolean isBackspacePressed() {
        return isPressed(GLFW.GLFW_KEY_BACKSPACE);
    }

    public boolean isDeletePressing() {
        return isPressing(GLFW.GLFW_KEY_DELETE);
    }

    public boolean isDeletePressed() {
        return isPressed(GLFW.GLFW_KEY_DELETE);
    }

    /**
     * 检测按键状态
     */
    public static boolean isKeyPressing(int key) {
        return checkKeyPressing(getWindowHandle(), key);
    }

    /**
     * 检测按键状态
     */
    public static boolean isKeyPressing(long windowHandle, int key) {
        return checkKeyPressing(windowHandle, key);
    }

    public static boolean isShiftPressingStatic() {
        return isKeyPressing(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyPressing(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isCtrlPressingStatic() {
        return isKeyPressing(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyPressing(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isAltPressingStatic() {
        return isKeyPressing(GLFW.GLFW_KEY_LEFT_ALT) || isKeyPressing(GLFW.GLFW_KEY_RIGHT_ALT);
    }

}

