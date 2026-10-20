package xin.vanilla.banira.internal.compat.minimap;

import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import xin.vanilla.banira.internal.client.ui.MinimapUiRows;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

public final class JourneyMapTextBounds {
    private JourneyMapTextBounds() {
    }

    public static boolean drawOwned(net.minecraft.client.gui.GuiGraphics graphics,
                                     net.minecraft.network.chat.Component text, float x, float y,
                                     int color, boolean shadow, org.joml.Matrix4f matrix,
                                     net.minecraft.client.renderer.MultiBufferSource buffers) {
        if (!matrix.equals(graphics.pose().last().pose()) || !MinimapUiRows.owns(EnumNotificationHudHost.JOURNEYMAP, text))
            return false;
        Minecraft mc = Minecraft.getInstance();
        Matrix4f original = new Matrix4f(RenderSystem.getProjectionMatrix());
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        Matrix4f projection = MinimapUiRows.guiProjection(original, width, height);
        Matrix4f pose = MinimapUiRows.guiPose(matrix, original, width, height);
        if (projection == null || pose == null) return false;
        var sorting = RenderSystem.getVertexSorting();
        if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource)
            ((net.minecraft.client.renderer.MultiBufferSource.BufferSource) buffers).endBatch();
        graphics.flush();
        graphics.pose().pushPose();
        try {
            RenderSystem.setProjectionMatrix(projection, sorting);
            graphics.pose().last().pose().set(pose);
            return MinimapUiRows.drawOwned(EnumNotificationHudHost.JOURNEYMAP,
                    graphics, text, new xin.vanilla.banira.client.data.ScreenCoordinate(x, y,
                            mc.font.width(text), mc.font.lineHeight), color, shadow, graphics::flush);
        } finally {
            try { graphics.flush(); }
            finally {
                try { RenderSystem.setProjectionMatrix(original, sorting); }
                finally { graphics.pose().popPose(); }
            }
        }
    }

    public static double scale(String text, double requested) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || text == null || text.isEmpty() || UIManager.INSTANCE.getMiniMap() == null) return requested;
        for (MinimapLineSnapshot row : MinimapLineRegistry.shared().snapshot(EnumNotificationHudHost.JOURNEYMAP, System.nanoTime())) {
            if (!text.equals(row.text().getString())) continue;
            int width = Math.max(1, Math.min(mc.getWindow().getScreenWidth(),
                    UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().getSize()) - 8);
            return Math.min(requested, width / (double) Math.max(1, mc.font.width(text)));
        }
        return requested;
    }
}
