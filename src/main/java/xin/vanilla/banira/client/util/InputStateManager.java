package xin.vanilla.banira.client.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.common.data.FixedList;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraClientInputService;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一的输入状态管理器
 */
@Accessors(fluent = true)
public final class InputStateManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int KEY_HISTORY_SIZE = 5;

    // region 单例

    private static final InputStateManager INSTANCE = new InputStateManager();

    public static InputStateManager instance() {
        return INSTANCE;
    }

    // endregion

    // region 按键状态
    private final Set<Integer> pressedKeys = new LinkedHashSet<>();
    private final Map<Integer, FixedList<Boolean>> keyHistoryRecords = new HashMap<>();
    private boolean keyActive = false;
    // endregion

    // region 鼠标状态
    @Getter
    private double mouseX;
    @Getter
    private double mouseY;
    private final FixedList<Boolean> mouseLeftPressedRecord = new FixedList<>(5);
    private final FixedList<Boolean> mouseRightPressedRecord = new FixedList<>(5);
    private double mouseLeftPressedX = -1;
    private double mouseLeftPressedY = -1;
    private double mouseRightPressedX = -1;
    private double mouseRightPressedY = -1;
    private final Set<Integer> pressedMouses = new LinkedHashSet<>();
    private double mousedScroll;
    private double mouseDownX = -1;
    private double mouseDownY = -1;
    // endregion

    // region 内部工具

    private InputStateManager() {
    }

    // endregion

    // region 按键/鼠标状态

    public static boolean isKeyPressing(int key) {
        return BaniraClientInputService.isKeyDown(key);
    }

    public static boolean isShiftPressingStatic() {
        return isKeyPressing(GLFWKey.GLFW_KEY_LEFT_SHIFT) || isKeyPressing(GLFWKey.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isCtrlPressingStatic() {
        return isKeyPressing(GLFWKey.GLFW_KEY_LEFT_CONTROL) || isKeyPressing(GLFWKey.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isAltPressingStatic() {
        return isKeyPressing(GLFWKey.GLFW_KEY_LEFT_ALT) || isKeyPressing(GLFWKey.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isMousePressing(int mouseButton) {
        return BaniraClientInputService.isMouseDown(mouseButton);
    }

    // endregion

    // region 光标坐标

    public static KeyValue<Double, Double> getRawCursorPos() {
        return BaniraClientInputService.rawCursorPos();
    }

    public static KeyValue<Integer, Integer> getGuiCursorPos() {
        return BaniraClientInputService.guiCursorPos();
    }

    public static KeyValue<Integer, Integer> rawToGui(KeyValue<Double, Double> raw) {
        return BaniraClientInputService.rawToGui(raw);
    }

    public static KeyValue<Integer, Integer> rawToGui(double rawX, double rawY) {
        return BaniraClientInputService.rawToGui(rawX, rawY);
    }

    public static KeyValue<Double, Double> guiToRaw(double guiX, double guiY) {
        return BaniraClientInputService.guiToRaw(guiX, guiY);
    }

    public static void setMouseGuiPos(KeyValue<Integer, Integer> pos) {
        setMouseGuiPos(pos.key(), pos.value());
    }

    public static void setMouseGuiPos(double guiX, double guiY) {
        BaniraClientInputService.setGuiCursorPos(guiX, guiY);
    }

    public static void setMouseRawPos(KeyValue<Double, Double> pos) {
        setMouseRawPos(pos.key(), pos.value());
    }

    public static void setMouseRawPos(double rawX, double rawY) {
        BaniraClientInputService.setRawCursorPos(rawX, rawY);
    }

    // endregion

    // region 按键查询

    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public boolean isPressing(int key) {
        FixedList<Boolean> record = keyHistoryRecords.get(key);
        if (record == null) {
            return isKeyPressing(key);
        }
        return Boolean.TRUE.equals(record.getLast());
    }

    /**
     * 检测按键是否刚被释放（从按下到释放的瞬间）
     */
    public boolean isKeyJustReleased(int key) {
        FixedList<Boolean> record = keyHistoryRecords.get(key);
        if (record == null || record.size() <= 1) {
            return false;
        }
        Boolean prevState = record.get(record.size() - 2);
        Boolean currentState = record.getLast();
        return Boolean.TRUE.equals(prevState) && !Boolean.TRUE.equals(currentState);
    }

    public void registerKey(int key) {
        keyHistoryRecords.computeIfAbsent(key, k -> new FixedList<>(KEY_HISTORY_SIZE));
    }

    public void unregisterKey(int key) {
        keyHistoryRecords.remove(key);
    }

    public boolean onlyKeyPressed(int keyCode) {
        return pressedKeys.size() == 1 && isKeyPressed(keyCode);
    }

    public boolean isCtrlPressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean isShiftPressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isAltPressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_LEFT_ALT) || isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_ALT);
    }

    public boolean isSuperPressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_LEFT_SUPER) || isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_SUPER);
    }

    public boolean isShiftPressing() {
        return isPressing(GLFWKey.GLFW_KEY_LEFT_SHIFT) || isPressing(GLFWKey.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isCtrlPressing() {
        return isPressing(GLFWKey.GLFW_KEY_LEFT_CONTROL) || isPressing(GLFWKey.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean isAltPressing() {
        return isPressing(GLFWKey.GLFW_KEY_LEFT_ALT) || isPressing(GLFWKey.GLFW_KEY_RIGHT_ALT);
    }

    public boolean isEscapePressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_ESCAPE);
    }

    public boolean isEnterPressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_ENTER) || isKeyPressed(GLFWKey.GLFW_KEY_KP_ENTER);
    }

    public boolean isBackspacePressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_BACKSPACE);
    }

    public boolean isDeletePressed() {
        return isKeyPressed(GLFWKey.GLFW_KEY_DELETE);
    }

    public boolean isKeyPressed(String keyNames) {
        if (StringUtils.isNullOrEmptyEx(keyNames)) return false;
        return GLFWKeyUtils.matchKey(keyNames, pressedKeys.stream().mapToInt(i -> i).toArray());
    }

    public boolean isKeyPressedInOrder(String keyNames) {
        if (StringUtils.isNullOrEmptyEx(keyNames)) return false;
        return GLFWKeyUtils.matchKeyInOrder(keyNames, pressedKeys.stream().mapToInt(i -> i).toArray());
    }

    public boolean onlyKeyPressed(String keyNames) {
        return pressedKeys.size() == 1 && isKeyPressed(keyNames);
    }

    public boolean onlyEscapePressed() {
        return pressedKeys.size() == 1 && isEscapePressed();
    }

    public boolean onlyEnterPressed() {
        return pressedKeys.size() == 1 && isEnterPressed();
    }

    public boolean onlyBackspacePressed() {
        return pressedKeys.size() == 1 && isBackspacePressed();
    }

    public boolean onlyDeletePressed() {
        return pressedKeys.size() == 1 && isDeletePressed();
    }

    public boolean onlyCtrlPressed() {
        return pressedKeys.size() == 1 && isCtrlPressed();
    }

    public boolean onlyShiftPressed() {
        return pressedKeys.size() == 1 && isShiftPressed();
    }

    public boolean onlyAltPressed() {
        return pressedKeys.size() == 1 && isAltPressed();
    }

    public boolean onlyCtrlShiftPressed() {
        return pressedKeys.size() == 2 && isCtrlPressed() && isShiftPressed();
    }

    // endregion

    // region 鼠标查询

    public boolean isPressingLeftEx() {
        return Boolean.TRUE.equals(mouseLeftPressedRecord.getLast());
    }

    public boolean isPressingRightEx() {
        return Boolean.TRUE.equals(mouseRightPressedRecord.getLast());
    }

    public boolean isPressedLeftEx() {
        return mouseLeftPressedRecord.size() > 1
                && Boolean.TRUE.equals(mouseLeftPressedRecord.get(mouseLeftPressedRecord.size() - 2))
                && !Boolean.TRUE.equals(mouseLeftPressedRecord.getLast());
    }

    public boolean isPressedRightEx() {
        return mouseRightPressedRecord.size() > 1
                && Boolean.TRUE.equals(mouseRightPressedRecord.get(mouseRightPressedRecord.size() - 2))
                && !Boolean.TRUE.equals(mouseRightPressedRecord.getLast());
    }

    public boolean isHoverInRect(double x, double y, double width, double height) {
        return x <= mouseX && mouseX <= x + width && y <= mouseY && mouseY <= y + height;
    }

    public boolean isLeftHoverInRect(double x, double y, double width, double height) {
        return x <= mouseLeftPressedX && mouseLeftPressedX <= x + width
                && y <= mouseLeftPressedY && mouseLeftPressedY <= y + height;
    }

    public boolean isRightHoverInRect(double x, double y, double width, double height) {
        return x <= mouseRightPressedX && mouseRightPressedX <= x + width
                && y <= mouseRightPressedY && mouseRightPressedY <= y + height;
    }

    public boolean isLeftPressedInRect(double x, double y, double width, double height) {
        return isPressedLeftEx() && isHoverInRect(x, y, width, height) && isLeftHoverInRect(x, y, width, height);
    }

    public boolean isRightPressedInRect(double x, double y, double width, double height) {
        return isPressedRightEx() && isHoverInRect(x, y, width, height) && isRightHoverInRect(x, y, width, height);
    }

    public boolean isMousePressed(int mouseButton) {
        return pressedMouses.contains(mouseButton);
    }

    public boolean isPressedLeft() {
        return isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_LEFT);
    }

    public boolean isPressedRight() {
        return isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_RIGHT);
    }

    public boolean isPressedMiddle() {
        return isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_MIDDLE);
    }

    public boolean onlyPressedLeft() {
        return pressedMouses.size() == 1 && isPressedLeft();
    }

    public boolean onlyPressedRight() {
        return pressedMouses.size() == 1 && isPressedRight();
    }

    public boolean onlyPressedMiddle() {
        return pressedMouses.size() == 1 && isPressedMiddle();
    }

    public boolean onlyPressedLeftRight() {
        return pressedMouses.size() == 2 && isPressedLeft() && isPressedRight();
    }

    public boolean isDragged() {
        return mouseDownX != -1 && mouseDownY != -1;
    }

    public boolean isDragged(int mouseButton) {
        return isDragged() && isMousePressed(mouseButton);
    }

    public boolean isMoved() {
        return (mouseDownX != -1 && Math.abs(mouseX - mouseDownX) > 1)
                || (mouseDownY != -1 && Math.abs(mouseY - mouseDownY) > 1);
    }

    public boolean isMousePressed(String mouseNames) {
        if (StringUtils.isNullOrEmptyEx(mouseNames)) return false;
        return GLFWKeyUtils.matchMouse(mouseNames, pressedMouses.stream().mapToInt(i -> i).toArray());
    }

    public boolean isMousePressedInOrder(String mouseNames) {
        if (StringUtils.isNullOrEmptyEx(mouseNames)) return false;
        return GLFWKeyUtils.matchMouseInOrder(mouseNames, pressedMouses.stream().mapToInt(i -> i).toArray());
    }

    public boolean onlyMousePressed(String mouseNames) {
        return pressedMouses.size() == 1 && isMousePressed(mouseNames);
    }

    // endregion

    // region 事件监听

    public static void handleDrawScreenPre(double mouseX, double mouseY) {
        InputStateManager.instance().onDrawScreenPre((int) mouseX, (int) mouseY);
    }

    public static void handleKeyPressed(int keyCode) {
        InputStateManager.instance().onKeyPressed(keyCode);
    }

    public static void handleKeyReleased(int keyCode) {
        InputStateManager.instance().onKeyReleased(keyCode);
    }

    public static void handleMouseClicked(double mouseX, double mouseY, int button) {
        InputStateManager.instance().onMouseClicked(mouseX, mouseY, button);
    }

    public static void handleMouseReleased(double mouseX, double mouseY, int button) {
        InputStateManager.instance().onMouseReleased(mouseX, mouseY, button);
    }

    public static void handleMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        InputStateManager.instance().onMouseScrolled(mouseX, mouseY, scrollDelta);
    }

    public static void handleClientTickEnd(boolean noScreenOpen) {
        if (noScreenOpen) {
            InputStateManager.instance().onScreenClosed();
        }
    }

    // endregion

    // region 内部更新逻辑

    private void onDrawScreenPre(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tick();
    }

    private void onKeyPressed(int keyCode) {
        pressedKeys.add(keyCode);
        updateKeyHistory(keyCode, true);
    }

    private void onKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        updateKeyHistory(keyCode, false);
    }

    private void onMouseClicked(double mouseX, double mouseY, int button) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        pressedMouses.add(button);
        mouseDownX = mouseX;
        mouseDownY = mouseY;
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            mouseLeftPressedX = mouseX;
            mouseLeftPressedY = mouseY;
        } else if (button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            mouseRightPressedX = mouseX;
            mouseRightPressedY = mouseY;
        }
    }

    private void onMouseReleased(double mouseX, double mouseY, int button) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        pressedMouses.remove(button);
        mouseDownX = -1;
        mouseDownY = -1;
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            mouseLeftPressedX = -1;
            mouseLeftPressedY = -1;
        } else if (button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            mouseRightPressedX = -1;
            mouseRightPressedY = -1;
        }
    }

    private void onMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mousedScroll = scrollDelta;
    }

    private void onScreenClosed() {
        clear();
    }

    private void tick() {
        if (!BaniraClientInputService.isWindowActive()) {
            if (keyActive) {
                LOGGER.debug("Window is not active, clear all input state");
            }
            clear();
        } else {
            keyActive = true;
            syncMouseButtonState();
            syncRegisteredKeys();
        }
    }

    private void syncMouseButtonState() {
        boolean left = BaniraClientInputService.isMouseDown(GLFWKey.GLFW_MOUSE_BUTTON_LEFT);
        if (left && !Boolean.TRUE.equals(mouseLeftPressedRecord.getLast())) {
            mouseLeftPressedX = mouseX;
            mouseLeftPressedY = mouseY;
        }
        mouseLeftPressedRecord.add(left);

        boolean right = BaniraClientInputService.isMouseDown(GLFWKey.GLFW_MOUSE_BUTTON_RIGHT);
        if (right && !Boolean.TRUE.equals(mouseRightPressedRecord.getLast())) {
            mouseRightPressedX = mouseX;
            mouseRightPressedY = mouseY;
        }
        mouseRightPressedRecord.add(right);
    }

    private void syncRegisteredKeys() {
        for (Map.Entry<Integer, FixedList<Boolean>> entry : keyHistoryRecords.entrySet()) {
            int key = entry.getKey();
            FixedList<Boolean> record = entry.getValue();
            record.add(BaniraClientInputService.isKeyDown(key));
        }
    }

    private void updateKeyHistory(int keyCode, boolean pressing) {
        FixedList<Boolean> record = keyHistoryRecords.computeIfAbsent(keyCode, k -> new FixedList<>(KEY_HISTORY_SIZE));
        record.add(pressing);
    }

    private void clear() {
        keyActive = false;
        pressedKeys.clear();
        pressedMouses.clear();
        for (FixedList<Boolean> record : keyHistoryRecords.values()) {
            record.clear();
        }
        mouseLeftPressedX = -1;
        mouseLeftPressedY = -1;
        mouseRightPressedX = -1;
        mouseRightPressedY = -1;
        mouseX = -1;
        mouseY = -1;
        mousedScroll = 0;
        mouseDownX = -1;
        mouseDownY = -1;
    }

    // endregion
}
