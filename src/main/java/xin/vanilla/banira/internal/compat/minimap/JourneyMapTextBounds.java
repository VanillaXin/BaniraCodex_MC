package xin.vanilla.banira.internal.compat.minimap;

import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

public final class JourneyMapTextBounds {
    private JourneyMapTextBounds() {
    }

    /** Compatibility entry point; JourneyMap retains its native text and background rendering. */
    @Deprecated
    public static boolean drawOwned(net.minecraft.client.gui.GuiGraphics graphics,
                                     net.minecraft.network.chat.Component text, float x, float y,
                                     int color, boolean shadow, org.joml.Matrix4f matrix,
                                     net.minecraft.client.renderer.MultiBufferSource buffers) {
        return false;
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
