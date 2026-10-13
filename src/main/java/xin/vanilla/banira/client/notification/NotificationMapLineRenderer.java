package xin.vanilla.banira.client.notification;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.internal.client.minimap.MinimapLineLayout;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy maps expose a text region rather than a registration API.
 */
public final class NotificationMapLineRenderer {
    private NotificationMapLineRenderer() {
    }

    public static float drawRowsCentered(GuiGraphics graphics, List<MinimapLineSnapshot> rows,
                                  float centerX, float y, float width, boolean above) {
        Minecraft mc = Minecraft.getInstance();
        return drawRowsCentered(graphics, rows, centerX, y, width, .75f, above,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static float drawRowsCentered(GuiGraphics graphics, List<MinimapLineSnapshot> rows,
                                  float centerX, float y, float width, float scale, boolean above,
                                  int screenWidth, int screenHeight) {
        if (rows.isEmpty() || !Float.isFinite(width) || width <= 0 || !Float.isFinite(scale) || scale <= 0
                || screenWidth <= 0 || screenHeight <= 0) return 0;
        Minecraft mc = Minecraft.getInstance();
        width = Math.min(width, screenWidth);
        List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
        float total = 0;
        for (MinimapLineSnapshot row : rows) {
            List<FormattedCharSequence> lines = mc.font.split(row.text(), Math.max(1, (int) (width / scale)));
            wrapped.add(lines);
            total += lines.size() * mc.font.lineHeight;
        }
        float gaps = Math.max(0, rows.size() - 1) * 2f;
        if (gaps >= screenHeight || total <= 0) return 0;
        scale = Math.min(scale, (screenHeight - gaps) / total);
        float[] heights = new float[rows.size()];
        for (int i = 0; i < rows.size(); i++) heights[i] = wrapped.get(i).size() * mc.font.lineHeight * scale;
        float[] positions = MinimapLineLayout.positions(y, heights, screenHeight, above);
        int color = ClientThemeManager.getEffectiveTheme().popupItemText();
        for (int i = 0; i < rows.size(); i++) {
            float drawY = positions[i];
            for (FormattedCharSequence line : wrapped.get(i)) {
                float lineWidth = mc.font.width(line) * scale;
                float left = Math.max(0, Math.min(centerX - lineWidth / 2, screenWidth - lineWidth));
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(left, drawY, 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.drawString(mc.font, line, 0, 0, color, false);
                } finally {
                    graphics.pose().popPose();
                }
                drawY += mc.font.lineHeight * scale;
            }
        }
        return total * scale + gaps;
    }

    public static void drawCentered(GuiGraphics graphics, Component text, float centerX, float y, float width) {
        Minecraft mc = Minecraft.getInstance();
        drawCentered(graphics, text, centerX, y, width, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static void drawCentered(GuiGraphics graphics, Component text, float centerX, float y, float width,
                                    int screenWidth, int screenHeight) {
        if (text.getString().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        float scale = Math.min(.75f, width / Math.max(1, textWidth));
        float drawY = Math.max(0, Math.min(y, screenHeight - 9 * scale));
        float left = Math.max(0, Math.min(centerX - textWidth * scale / 2,
                screenWidth - textWidth * scale));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, drawY, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(mc.font, text, 0, 0, ClientThemeManager.getEffectiveTheme().popupItemText(), false);
        } finally {
            graphics.pose().popPose();
        }
    }
}
