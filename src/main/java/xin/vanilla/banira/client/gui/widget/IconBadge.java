package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.common.util.CompactCount;

public final class IconBadge {
    private IconBadge() {
    }

    public static void draw(PoseStack pose, Font font, String amount, int x, int y, int size) {
        String text = CompactCount.format(amount);
        if (text.isEmpty() || size <= 0) return;
        int width = font.width(text);
        float scale = Math.min(1F, (float) size / Math.max(1, width));
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        buffers.endBatch();
        pose.pushPose();
        try {
            RenderSystem.disableDepthTest();
            pose.translate(x + size - width * scale, y + size - font.lineHeight * scale, 300);
            pose.scale(scale, scale, 1);
            // Normal font render types restore depth testing when their batch is submitted.
            // See-through text stays above even scaled, three-dimensional item models.
            font.drawInBatch(text, 0, 0, 0xFFFFFFFF, true, pose.last().pose(), buffers, true, 0, 15728880);
            buffers.endBatch();
        } finally {
            pose.popPose();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }
}
