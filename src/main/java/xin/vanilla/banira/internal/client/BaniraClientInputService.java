package xin.vanilla.banira.internal.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.DoubleBuffer;

/**
 * Version/window-system input access for client utilities.
 */
public final class BaniraClientInputService {
    private BaniraClientInputService() {
    }

    public static boolean isKeyDown(int key) {
        long window = safeWindowHandle();
        return window != 0L && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseDown(int mouseButton) {
        long window = safeWindowHandle();
        return window != 0L && GLFW.glfwGetMouseButton(window, mouseButton) == GLFW.GLFW_PRESS;
    }

    public static boolean isWindowActive() {
        try {
            return clientReady() && BaniraClientAccess.isWindowActive();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static KeyValue<Double, Double> rawCursorPos() {
        long window = safeWindowHandle();
        if (window == 0L) {
            return new KeyValue<>(0.0D, 0.0D);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, x, y);
            return new KeyValue<>(x.get(0), y.get(0));
        }
    }

    public static KeyValue<Integer, Integer> guiCursorPos() {
        return rawToGui(rawCursorPos());
    }

    public static KeyValue<Integer, Integer> rawToGui(KeyValue<Double, Double> raw) {
        return rawToGui(raw.key(), raw.val());
    }

    public static KeyValue<Integer, Integer> rawToGui(double rawX, double rawY) {
        KeyValue<Integer, Integer> pixel = guiPixelSize();
        KeyValue<Integer, Integer> scaled = guiScaledSize();
        int width = Math.max(1, pixel.key());
        int height = Math.max(1, pixel.val());
        int scaledWidth = scaled.key();
        int scaledHeight = scaled.val();
        int guiX = (int) Math.round(rawX * (double) scaledWidth / width);
        int guiY = (int) Math.round(rawY * (double) scaledHeight / height);
        return new KeyValue<>(guiX, guiY);
    }

    public static KeyValue<Double, Double> guiToRaw(double guiX, double guiY) {
        KeyValue<Integer, Integer> pixel = guiPixelSize();
        KeyValue<Integer, Integer> scaled = guiScaledSize();
        int width = pixel.key();
        int height = pixel.val();
        int scaledWidth = Math.max(1, scaled.key());
        int scaledHeight = Math.max(1, scaled.val());
        return new KeyValue<>(
                guiX * (double) width / scaledWidth,
                guiY * (double) height / scaledHeight
        );
    }

    public static void setGuiCursorPos(double guiX, double guiY) {
        KeyValue<Double, Double> raw = guiToRaw(guiX, guiY);
        setRawCursorPos(raw.key(), raw.val());
    }

    public static void setRawCursorPos(double rawX, double rawY) {
        long window = safeWindowHandle();
        if (window != 0L) {
            GLFW.glfwSetCursorPos(window, rawX, rawY);
        }
    }

    private static boolean clientReady() {
        try {
            return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient();
        } catch (Throwable ignored) {
            // 单元测试或早期启动阶段可能没有完整 Forge 客户端环境，输入查询应安全降级。
            return false;
        }
    }

    private static long windowHandle() {
        return BaniraClientAccess.windowHandle();
    }

    private static long safeWindowHandle() {
        if (!clientReady()) {
            return 0L;
        }
        try {
            return windowHandle();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static KeyValue<Integer, Integer> guiPixelSize() {
        if (!clientReady()) {
            return new KeyValue<>(1, 1);
        }
        try {
            return BaniraClientAccess.guiPixelSize();
        } catch (Throwable ignored) {
            return new KeyValue<>(1, 1);
        }
    }

    private static KeyValue<Integer, Integer> guiScaledSize() {
        if (!clientReady()) {
            return new KeyValue<>(1, 1);
        }
        try {
            return BaniraClientAccess.guiScaledSize();
        } catch (Throwable ignored) {
            return new KeyValue<>(1, 1);
        }
    }
}
