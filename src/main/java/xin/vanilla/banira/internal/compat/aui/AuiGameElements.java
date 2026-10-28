package xin.vanilla.banira.internal.compat.aui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.EffectIconWidget;
import xin.vanilla.banira.client.gui.widget.ImageWidget;
import xin.vanilla.banira.client.gui.widget.ItemWidget;

import java.util.Collections;
import java.util.List;

public final class AuiGameElements {
    static boolean scrollExtentReady(double measured, double required) {
        return Double.isFinite(measured) && Double.isFinite(required) && measured + .5 >= Math.max(0, required);
    }

    private AuiGameElements() {
    }

    public static ScreenCoordinate iconBounds(ScreenCoordinate slot, int maximum, int inset) {
        double left = Math.ceil(slot.x() + inset), top = Math.ceil(slot.y() + inset);
        double right = Math.floor(slot.x() + slot.width() - inset), bottom = Math.floor(slot.y() + slot.height() - inset);
        int size = Math.max(0, (int) Math.min(maximum, Math.min(right - left, bottom - top)));
        return new ScreenCoordinate(left + Math.floor((right - left - size) / 2),
                top + Math.floor((bottom - top - size) / 2), size, size);
    }

    private static ScreenCoordinate compactIcon(ScreenCoordinate slot) {
        return iconBounds(slot, 16, slot.width() >= 20 && slot.height() >= 20 ? 2 : 0);
    }

    public static void renderElement(GuiGraphics graphics, xin.vanilla.banira.api.client.ui.UiGameElement element,
                                     ScreenCoordinate bounds, ScreenCoordinate clip, xin.vanilla.banira.client.data.BaniraColorConfig theme) {
        if (element == null || bounds.width() <= 0 || bounds.height() <= 0 || bounds.x() >= clip.x() + clip.width()
                || bounds.y() >= clip.y() + clip.height() || bounds.x() + bounds.width() <= clip.x() || bounds.y() + bounds.height() <= clip.y())
            return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        graphics.flush();
        graphics.enableScissor((int) Math.max(bounds.x(), clip.x()), (int) Math.max(bounds.y(), clip.y()),
                (int) Math.ceil(Math.min(bounds.x() + bounds.width(), clip.x() + clip.width())), (int) Math.ceil(Math.min(bounds.y() + bounds.height(), clip.y() + clip.height())));
        // Scissor changes may flush GuiGraphics and re-enable depth testing.
        RenderSystem.disableDepthTest();
        try {
            switch (element.kind()) {
                case SPRITE:
                    ImageWidget.blitBlend(graphics.pose(), element.option().texture()[0],
                            bounds.xInt(), bounds.yInt(), bounds.widthInt(), bounds.heightInt());
                    break;
                case OPTION:
                    renderOption(graphics, element.option(), bounds, null);
                    break;
                case EFFECT:
                    renderEffect(graphics, element.effect(), compactIcon(bounds));
                    break;
                case DIAGNOSTIC:
                    xin.vanilla.banira.internal.client.ui.page.DebugUiSession.drawDiagnostics(graphics, bounds, element, theme);
                    break;
                case PRESET:
                    xin.vanilla.banira.client.gui.widget.ButtonWidget.drawPresetIcon(graphics.pose(), element.preset(), bounds.xInt() + 4, bounds.yInt() + 4,
                            Math.max(0, bounds.widthInt() - 8), Math.max(0, bounds.heightInt() - 8), theme == null ? 0xff24272c : theme.textPrimary(), 1);
                    break;
            }
        } finally {
            graphics.flush();
            graphics.disableScissor();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    public static final class TextLayout {
        private final Component text;
        private List<FormattedCharSequence> lines = Collections.emptyList();
        private int width = -1;

        public TextLayout(Component text) {
            this.text = text.copy();
        }

        public boolean prepare(int availableWidth) {
            if (availableWidth <= 0 || width == availableWidth) return false;
            width = availableWidth;
            lines = Minecraft.getInstance().font.split(text, width);
            return true;
        }

        public int height() {
            return lines.size() * Minecraft.getInstance().font.lineHeight;
        }

        public boolean preparedFor(int availableWidth) {
            return availableWidth > 0 && width == availableWidth;
        }

        public net.minecraft.network.chat.Style styleAt(ScreenCoordinate bounds, ScreenCoordinate clip, double x, double y) {
            int index = lineAt(bounds, clip, x, y, Minecraft.getInstance().font.lineHeight, lines.size());
            return index < 0 ? null : Minecraft.getInstance().font.getSplitter().componentStyleAtWidth(lines.get(index), (int) (x - bounds.x()));
        }
    }

    public static int lineAt(ScreenCoordinate bounds, ScreenCoordinate clip, double x, double y, int lineHeight, int lines) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || lineHeight <= 0 || x < bounds.x() || x >= bounds.x() + bounds.width()
                || x < clip.x() || x >= clip.x() + clip.width() || y < clip.y() || y >= clip.y() + clip.height())
            return -1;
        int index = (int) Math.floor((y - bounds.y()) / lineHeight);
        return index >= 0 && index < lines ? index : -1;
    }

    public static void renderText(GuiGraphics graphics, TextLayout layout, ScreenCoordinate bounds, ScreenCoordinate clip) {
        if (layout == null || bounds.width() <= 0 || bounds.height() <= 0) return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        graphics.flush();
        graphics.enableScissor((int) clip.x(), (int) clip.y(), (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        RenderSystem.disableDepthTest();
        try {
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int first = Math.max(0, (int) Math.floor((clip.y() - bounds.y()) / lineHeight));
            int last = Math.min(layout.lines.size(), (int) Math.ceil((clip.y() + clip.height() - bounds.y()) / lineHeight));
            for (int index = first; index < last; index++)
                Minecraft.getInstance().font.drawInBatch(
                        layout.lines.get(index), (float) bounds.x(), (float) bounds.y() + index * lineHeight, 0xff24272c, false,
                        graphics.pose().last().pose(), graphics.bufferSource(), net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        } finally {
            try {
                graphics.flush();
            } finally {
                try {
                    graphics.disableScissor();
                } finally {
                    if (depth) RenderSystem.enableDepthTest();
                    else RenderSystem.disableDepthTest();
                }
            }
        }
    }

    public static void renderEffect(GuiGraphics graphics, MobEffectInstance effect, ScreenCoordinate bounds) {
        if (effect != null && bounds.width() > 0 && bounds.height() > 0)
            EffectIconWidget.drawEffectIcon(graphics.pose(), effect, bounds.xInt(), bounds.yInt(), bounds.widthInt(), bounds.heightInt(), false);
    }

    public static void renderOption(GuiGraphics graphics, DropdownOption option, ScreenCoordinate bounds,
                                    ScreenCoordinate clip) {
        if (option == null || !option.hasIcon() || bounds.width() <= 0 || bounds.height() <= 0) return;
        if (clip != null && (bounds.y() + bounds.height() <= clip.y() || bounds.y() >= clip.y() + clip.height()))
            return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        graphics.flush();
        if (clip != null) graphics.enableScissor((int) clip.x(), (int) clip.y(),
                (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        RenderSystem.disableDepthTest();
        try {
            ScreenCoordinate icon = compactIcon(bounds);
            int size = icon.widthInt(), x = icon.xInt(), y = icon.yInt();
            if (size == 0) return;
            if (option.hasTexture()) {
                for (Texture texture : option.texture())
                    ImageWidget.blitBlend(graphics.pose(), texture, x, y, size, size);
            } else ItemWidget.renderGuiItemScaled(graphics, option.icon(), x, y, size);
        } finally {
            graphics.flush();
            if (clip != null) graphics.disableScissor();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }
}
