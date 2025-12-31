package xin.vanilla.banira.client.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.common.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class KeyEventManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Set<Integer> pressedKeys = new LinkedHashSet<>();
    @Deprecated
    private int modifiers = GLFWKey.GLFW_KEY_UNKNOWN;

    @Deprecated
    public void keyPressed(int keyCode, int modifiers) {
        this.pressedKeys.add(keyCode);
        this.modifiers = modifiers;
    }

    @Deprecated
    public void keyReleased(int keyCode, int modifiers) {
        this.pressedKeys.remove(keyCode);
        this.modifiers = GLFWKey.GLFW_KEY_UNKNOWN;
    }

    public void keyPressed(int keyCode) {
        this.pressedKeys.add(keyCode);
    }

    public void keyReleased(int keyCode) {
        this.pressedKeys.remove(keyCode);
    }

    private boolean active = false;

    public void tick() {
        if (!Minecraft.getInstance().isWindowActive()) {
            if (this.active) LOGGER.debug("Window is not active, clear all pressed keys and mouses");
            this.active = false;
            this.pressedKeys.clear();
            this.modifiers = GLFWKey.GLFW_KEY_UNKNOWN;
        } else {
            this.active = true;
        }
    }

    public boolean isKeyPressed(int keyCode) {
        return this.pressedKeys.contains(keyCode);
    }

    /**
     * 按键是否只按下一个
     */
    public boolean onlyKeyPressed(int keyCode) {
        return this.pressedKeys.size() == 1 && this.isKeyPressed(keyCode);
    }

    public boolean isCtrlPressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_CONTROL) || this.isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean isShiftPressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_SHIFT) || this.isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isAltPressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_ALT) || this.isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_ALT);
    }

    public boolean isSuperPressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_SUPER) || this.isKeyPressed(GLFWKey.GLFW_KEY_RIGHT_SUPER);
    }

    public boolean onlyCtrlPressed() {
        return this.pressedKeys.size() == 1 && this.isCtrlPressed();
    }

    public boolean onlyShiftPressed() {
        return this.pressedKeys.size() == 1 && this.isShiftPressed();
    }

    public boolean onlyAltPressed() {
        return this.pressedKeys.size() == 1 && this.isAltPressed();
    }

    public boolean onlySuperPressed() {
        return this.pressedKeys.size() == 1 && this.isSuperPressed();
    }

    public boolean onlyCtrlShiftPressed() {
        return this.pressedKeys.size() == 2 && this.isCtrlPressed() && this.isShiftPressed();
    }

    public boolean onlyCtrlAltPressed() {
        return this.pressedKeys.size() == 2 && this.isCtrlPressed() && this.isAltPressed();
    }

    public boolean onlyCtrlSuperPressed() {
        return this.pressedKeys.size() == 2 && this.isCtrlPressed() && this.isSuperPressed();
    }

    public boolean onlyShiftAltPressed() {
        return this.pressedKeys.size() == 2 && this.isShiftPressed() && this.isAltPressed();
    }

    public boolean onlyShiftSuperPressed() {
        return this.pressedKeys.size() == 2 && this.isShiftPressed() && this.isSuperPressed();
    }

    public boolean onlyAltSuperPressed() {
        return this.pressedKeys.size() == 2 && this.isAltPressed() && this.isSuperPressed();
    }

    public boolean onlyCtrlShiftAltPressed() {
        return this.pressedKeys.size() == 3 && this.isCtrlPressed() && this.isShiftPressed() && this.isAltPressed();
    }

    public boolean isEscapePressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_ESCAPE);
    }

    public boolean isEnterPressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_ENTER) || this.isKeyPressed(GLFWKey.GLFW_KEY_KP_ENTER);
    }

    public boolean isBackspacePressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_BACKSPACE);
    }

    public boolean isDeletePressed() {
        return this.isKeyPressed(GLFWKey.GLFW_KEY_DELETE);
    }

    public boolean onlyEscapePressed() {
        return this.pressedKeys.size() == 1 && this.isEscapePressed();
    }

    public boolean onlyEnterPressed() {
        return this.pressedKeys.size() == 1 && this.isEnterPressed();
    }

    public boolean onlyBackspacePressed() {
        return this.pressedKeys.size() == 1 && this.isBackspacePressed();
    }

    public boolean onlyDeletePressed() {
        return this.pressedKeys.size() == 1 && this.isDeletePressed();
    }

    /**
     * 按键是否按下
     */
    public boolean isKeyPressed(String keyNames) {
        if (StringUtils.isNullOrEmptyEx(keyNames)) return false;
        return GLFWKeyUtils.matchKey(keyNames, this.pressedKeys.stream().mapToInt(i -> i).toArray());
    }

    /**
     * 按键是否按顺序按下
     */
    public boolean isKeyPressedInOrder(String keyNames) {
        if (StringUtils.isNullOrEmptyEx(keyNames)) return false;
        return GLFWKeyUtils.matchKeyInOrder(keyNames, this.pressedKeys.stream().mapToInt(i -> i).toArray());
    }

    /**
     * 按键是否只按下一个
     */
    public boolean onlyKeyPressed(String keyNames) {
        return this.pressedKeys.size() == 1 && this.isKeyPressed(keyNames);
    }

}
