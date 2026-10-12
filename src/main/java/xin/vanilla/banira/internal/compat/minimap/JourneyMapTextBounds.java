package xin.vanilla.banira.internal.compat.minimap;

import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

public final class JourneyMapTextBounds {
    private JourneyMapTextBounds() {
    }

    public static double scale(String text, double requested) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || text == null || text.isEmpty() || UIManager.INSTANCE.getMiniMap() == null) return requested;
        boolean minimap = false;
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().startsWith("journeymap.client.ui.minimap.")) {
                minimap = true;
                break;
            }
        }
        if (!minimap) return requested;
        for (MinimapLineSnapshot row : MinimapLineRegistry.shared().snapshot(EnumNotificationHudHost.JOURNEYMAP, System.nanoTime())) {
            if (!text.equals(row.text().getString())) continue;
            int width = Math.max(1, Math.min(mc.getWindow().getScreenWidth(),
                    UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().getSize()) - 8);
            return Math.min(requested, width / (double) Math.max(1, mc.font.width(text)));
        }
        return requested;
    }
}
