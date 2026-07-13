package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains nested GUI-space scissor rectangles.
 */
public final class GuiScissorStack {
    private static final Deque<int[]> STACK = new ArrayDeque<>();

    private GuiScissorStack() {
    }

    public static void enable(int guiX, int guiY, int guiWidth, int guiHeight) {
        Window window = Minecraft.getInstance().getWindow();
        int scale = (int) window.getGuiScale();
        int x = guiX * scale;
        int y = window.getHeight() - (guiY + guiHeight) * scale;
        int w = Math.max(0, guiWidth * scale);
        int h = Math.max(0, guiHeight * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, w, h);
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void push(int guiX, int guiY, int guiWidth, int guiHeight) {
        Window window = Minecraft.getInstance().getWindow();
        int scale = (int) window.getGuiScale();
        int winW = window.getWidth() / scale;
        int winH = window.getHeight() / scale;
        int[] prev = new int[5];
        prev[0] = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0;
        if (prev[0] == 1) {
            int[] box = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            prev[1] = box[0] / scale;
            prev[2] = (window.getHeight() - box[1] - box[3]) / scale;
            prev[3] = box[2] / scale;
            prev[4] = box[3] / scale;
        } else {
            prev[1] = 0;
            prev[2] = 0;
            prev[3] = winW;
            prev[4] = winH;
        }
        STACK.push(prev);

        int left = Math.max(guiX, prev[1]);
        int top = Math.max(guiY, prev[2]);
        int right = Math.min(guiX + guiWidth, prev[1] + prev[3]);
        int bottom = Math.min(guiY + guiHeight, prev[2] + prev[4]);
        enable(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public static void pop() {
        int[] prev = STACK.poll();
        if (prev == null) {
            disable();
            return;
        }
        if (prev[0] == 1) {
            enable(prev[1], prev[2], prev[3], prev[4]);
        } else {
            disable();
        }
    }
}
