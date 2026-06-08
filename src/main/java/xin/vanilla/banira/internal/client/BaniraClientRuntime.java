package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

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

    @Nullable
    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
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
