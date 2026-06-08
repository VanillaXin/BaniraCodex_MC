package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.common.data.KeyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 客户端运行时访问点，集中隔离 Minecraft 单例和窗口句柄。
 */
public final class BaniraClientRuntime {
    private BaniraClientRuntime() {
    }

    public static void execute(@Nonnull Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    public static boolean isWindowActive() {
        return Minecraft.getInstance().isWindowActive();
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void clipboard(@Nullable String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }

    @Nullable
    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    @Nullable
    public static LocalPlayer localPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * 当前 GUI 逻辑尺寸。若打开了 Screen，优先使用 Screen 尺寸以贴近控件布局。
     */
    public static KeyValue<Integer, Integer> screenSize() {
        Screen screen = currentScreen();
        if (screen != null) {
            return new KeyValue<>(screen.width, screen.height);
        }
        return guiScaledSize();
    }

    public static KeyValue<Integer, Integer> guiScaledSize() {
        Minecraft mc = Minecraft.getInstance();
        return new KeyValue<>(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static KeyValue<Integer, Integer> windowSize() {
        Minecraft mc = Minecraft.getInstance();
        return new KeyValue<>(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    public static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static double scaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, (double) mc.getWindow().getScreenWidth());
    }

    public static double scaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, (double) mc.getWindow().getScreenHeight());
    }

    public static boolean leftMouseDown() {
        return GLFW.glfwGetMouseButton(windowHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
