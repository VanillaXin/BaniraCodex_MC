package xin.vanilla.banira.client.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.common.data.FixedList;
import xin.vanilla.banira.common.data.KeyValue;

import java.nio.DoubleBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Accessors(fluent = true)
public class MouseHelper {
    private final FixedList<Boolean> mouseLeftPressedRecord = new FixedList<>(5);
    private final FixedList<Boolean> mouseRightPressedRecord = new FixedList<>(5);
    private double mouseLeftPressedX = -1;
    private double mouseLeftPressedY = -1;
    private double mouseRightPressedX = -1;
    private double mouseRightPressedY = -1;
    @Getter
    private double mouseX;
    @Getter
    private double mouseY;
    private final Set<Integer> pressedMouses = new LinkedHashSet<>();
    private double mousedScroll, mouseDownX, mouseDownY;

    private static long getWindowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    private boolean isMouseLeftPressing() {
        return isMouseLeftPressing(getWindowHandle());
    }

    private boolean isMouseLeftPressing(long windowHandle) {
        return GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private boolean isMouseRightPressing() {
        return isMouseRightPressing(getWindowHandle());
    }

    private boolean isMouseRightPressing(long windowHandle) {
        return GLFW.glfwGetMouseButton(windowHandle, GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    public void tick(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        boolean mouseLeftPressing = isMouseLeftPressing();
        if (mouseLeftPressing) {
            if (!Boolean.TRUE.equals(mouseLeftPressedRecord.getLast())) {
                mouseLeftPressedX = mouseX;
                mouseLeftPressedY = mouseY;
            }
        }
        mouseLeftPressedRecord.add(mouseLeftPressing);

        boolean mouseRightPressing = isMouseRightPressing();
        if (mouseRightPressing) {
            if (!Boolean.TRUE.equals(mouseRightPressedRecord.getLast())) {
                mouseRightPressedX = mouseX;
                mouseRightPressedY = mouseY;
            }
        }
        mouseRightPressedRecord.add(mouseRightPressing);

        if (!Minecraft.getInstance().isWindowActive()) {
            this.mouseLeftPressedX = -1;
            this.mouseLeftPressedY = -1;
            this.mouseRightPressedX = -1;
            this.mouseRightPressedY = -1;
            this.mouseX = -1;
            this.mouseY = -1;
            this.pressedMouses.clear();
            this.mousedScroll = 0;
            this.mouseDownX = -1;
            this.mouseDownY = -1;
        }
    }

    public boolean isPressingLeftEx() {
        return Boolean.TRUE.equals(mouseLeftPressedRecord.getLast());
    }

    public boolean isPressingRightEx() {
        return Boolean.TRUE.equals(mouseRightPressedRecord.getLast());
    }

    public boolean isPressedLeftEx() {
        return mouseLeftPressedRecord.size() > 1 && mouseLeftPressedRecord.get(mouseLeftPressedRecord.size() - 2) && !mouseLeftPressedRecord.getLast();
    }

    public boolean isPressedRightEx() {
        return mouseRightPressedRecord.size() > 1 && mouseRightPressedRecord.get(mouseRightPressedRecord.size() - 2) && !mouseRightPressedRecord.getLast();
    }

    public boolean isHoverInRect(int x, int y, int width, int height) {
        return x <= this.mouseX && this.mouseX <= x + width && y <= this.mouseY && this.mouseY <= y + height;
    }

    public boolean isHoverInRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return x <= mouseX && mouseX <= x + width && y <= mouseY && mouseY <= y + height;
    }

    public boolean isLeftHoverInRect(int x, int y, int width, int height) {
        return x <= mouseLeftPressedX && mouseLeftPressedX <= x + width && y <= mouseLeftPressedY && mouseLeftPressedY <= y + height;
    }

    public boolean isRightHoverInRect(int x, int y, int width, int height) {
        return x <= mouseRightPressedX && mouseRightPressedX <= x + width && y <= mouseRightPressedY && mouseRightPressedY <= y + height;
    }

    public boolean isLeftPressedInRect(int x, int y, int width, int height) {
        return isPressedLeftEx() && isHoverInRect(x, y, width, height) && isLeftHoverInRect(x, y, width, height);
    }

    public boolean isRightPressedInRect(int x, int y, int width, int height) {
        return isPressedRightEx() && isHoverInRect(x, y, width, height) && isRightHoverInRect(x, y, width, height);
    }

    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.pressedMouses.add(mouseButton);
        this.mouseDownX = mouseX;
        this.mouseDownY = mouseY;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (mouseButton == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            this.mouseLeftPressedX = mouseX;
            this.mouseLeftPressedY = mouseY;
        } else if (mouseButton == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            this.mouseRightPressedX = mouseX;
            this.mouseRightPressedY = mouseY;
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.pressedMouses.remove(mouseButton);
        this.mouseDownX = -1;
        this.mouseDownY = -1;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (mouseButton == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            this.mouseLeftPressedX = -1;
            this.mouseLeftPressedY = -1;
        } else if (mouseButton == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            this.mouseRightPressedX = -1;
            this.mouseRightPressedY = -1;
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void mouseScrolled(double mouseX, double mouseY, double mousedScroll) {
        this.mousedScroll = mousedScroll;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public boolean isPressed(int mouseButton) {
        return this.pressedMouses.contains(mouseButton);
    }

    public boolean isPressedLeft() {
        return this.isPressed(GLFWKey.GLFW_MOUSE_BUTTON_LEFT);
    }

    public boolean isPressedRight() {
        return this.isPressed(GLFWKey.GLFW_MOUSE_BUTTON_RIGHT);
    }

    public boolean isPressedMiddle() {
        return this.isPressed(GLFWKey.GLFW_MOUSE_BUTTON_MIDDLE);
    }

    public boolean onlyPressedLeft() {
        return this.pressedMouses.size() == 1 && this.isPressedLeft();
    }

    public boolean onlyPressedRight() {
        return this.pressedMouses.size() == 1 && this.isPressedRight();
    }

    public boolean onlyPressedMiddle() {
        return this.pressedMouses.size() == 1 && this.isPressedMiddle();
    }

    public boolean onlyPressedLeftRight() {
        return this.pressedMouses.size() == 2 && this.isPressedLeft() && this.isPressedRight();
    }

    public boolean isDragged() {
        return this.mouseDownX != GLFWKey.GLFW_KEY_UNKNOWN && this.mouseDownY != GLFWKey.GLFW_KEY_UNKNOWN;
    }

    public boolean isDragged(int mouseButton) {
        return this.isDragged() && this.pressedMouses.contains(mouseButton);
    }

    public boolean isMoved() {
        return (this.mouseDownX != -1 && Math.abs(this.mouseX - this.mouseDownX) > 1) || (this.mouseDownY != -1 && Math.abs(this.mouseY - this.mouseDownY) > 1);
    }


    // region static

    public static KeyValue<Double, Double> getRawCursorPos() {
        long window = getWindowHandle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xb = stack.mallocDouble(1);
            DoubleBuffer yb = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, xb, yb);
            return new KeyValue<>(xb.get(0), yb.get(0));
        }
    }

    public static KeyValue<Integer, Integer> getGuiCursorPos() {
        return rawToGui(getRawCursorPos());
    }

    public static KeyValue<Integer, Integer> rawToGui(KeyValue<Double, Double> raw) {
        return rawToGui(raw.getKey(), raw.getValue());
    }

    public static KeyValue<Integer, Integer> rawToGui(double rawX, double rawY) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int gx = (int) Math.round(rawX * (double) sw / w);
        int gy = (int) Math.round(rawY * (double) sh / h);
        return new KeyValue<>(gx, gy);
    }

    public static KeyValue<Double, Double> guiToRaw(double guiX, double guiY) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        double rx = guiX * (double) w / sw;
        double ry = guiY * (double) h / sh;
        return new KeyValue<>(rx, ry);
    }

    public static void setMouseGuiPos(KeyValue<Integer, Integer> pos) {
        setMouseGuiPos(pos.getKey(), pos.getValue());
    }

    public static void setMouseGuiPos(double guiX, double guiY) {
        long window = getWindowHandle();
        KeyValue<Double, Double> raw = guiToRaw(guiX, guiY);
        GLFW.glfwSetCursorPos(window, raw.getKey(), raw.getValue());
    }

    public static void setMouseRawPos(KeyValue<Double, Double> pos) {
        setMouseRawPos(pos.getKey(), pos.getValue());
    }

    public static void setMouseRawPos(double rawX, double rawY) {
        long window = getWindowHandle();
        GLFW.glfwSetCursorPos(window, rawX, rawY);
    }

    // endregion static

}
