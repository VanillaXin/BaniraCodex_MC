package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class UiSystemCursor {
    private static final UiPointerOwnership OWNERS = new UiPointerOwnership();

    private UiSystemCursor() {
    }

    public static boolean acquire(Object token, Object screen) {
        if (Minecraft.getInstance().screen != screen) return false;
        OWNERS.acquire(token, () -> mode(GLFW.GLFW_CURSOR_HIDDEN));
        return true;
    }

    public static void release(Object token) {
        OWNERS.release(token, () -> mode(GLFW.GLFW_CURSOR_NORMAL));
    }

    private static void mode(int mode) {
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_CURSOR, mode);
    }
}
