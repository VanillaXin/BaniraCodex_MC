package xin.vanilla.banira.internal.compat.aui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import xin.vanilla.banira.client.util.ExtractedGuiRenderer;
import xin.vanilla.banira.internal.client.ui.NativeGuiGraphics;
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

    // Minecraft icons paint after the DOM, so leave the opaque HTML tooltip area untouched.
    static List<ScreenCoordinate> uncovered(ScreenCoordinate bounds, ScreenCoordinate clip, ScreenCoordinate tooltip) {
        if (tooltip == null) return Collections.singletonList(clip);
        double left = Math.max(bounds.x(), clip.x()), top = Math.max(bounds.y(), clip.y());
        double right = Math.min(bounds.x() + bounds.width(), clip.x() + clip.width());
        double bottom = Math.min(bounds.y() + bounds.height(), clip.y() + clip.height());
        if (right <= left || bottom <= top) return Collections.emptyList();
        double tl = Math.max(left, Math.floor(tooltip.x())), tt = Math.max(top, Math.floor(tooltip.y()));
        double tr = Math.min(right, Math.ceil(tooltip.x() + tooltip.width()));
        double tb = Math.min(bottom, Math.ceil(tooltip.y() + tooltip.height()));
        if (tr <= tl || tb <= tt) return Collections.singletonList(clip);
        List<ScreenCoordinate> visible = new java.util.ArrayList<>(4);
        if (tt > top) visible.add(new ScreenCoordinate(left, top, right - left, tt - top));
        if (tb < bottom) visible.add(new ScreenCoordinate(left, tb, right - left, bottom - tb));
        if (tl > left) visible.add(new ScreenCoordinate(left, tt, tl - left, tb - tt));
        if (tr < right) visible.add(new ScreenCoordinate(tr, tt, right - tr, tb - tt));
        return visible;
    }

    public static void renderElement(GuiGraphicsExtractor graphics, xin.vanilla.banira.api.client.ui.UiGameElement element,
                                     ScreenCoordinate bounds, ScreenCoordinate clip, xin.vanilla.banira.client.data.BaniraColorConfig theme) {
        if (element == null || bounds.width() <= 0 || bounds.height() <= 0 || bounds.x() >= clip.x() + clip.width()
                || bounds.y() >= clip.y() + clip.height() || bounds.x() + bounds.width() <= clip.x() || bounds.y() + bounds.height() <= clip.y())
            return;
        NativeGuiGraphics.bind(graphics);
        graphics.enableScissor((int) Math.max(bounds.x(), clip.x()), (int) Math.max(bounds.y(), clip.y()),
                (int) Math.ceil(Math.min(bounds.x() + bounds.width(), clip.x() + clip.width())), (int) Math.ceil(Math.min(bounds.y() + bounds.height(), clip.y() + clip.height())));
        try (var scope = ExtractedGuiRenderer.bind(graphics)) {
            switch (element.kind()) {
                case SPRITE:
                    ImageWidget.blitBlend(graphics.pose(), element.option().texture()[0],
                            bounds.xInt(), bounds.yInt(), bounds.widthInt(), bounds.heightInt());
                    break;
                case OPTION:
                    renderOption(graphics, element.option(), bounds, null);
                    break;
                case ARTWORK:
                    if (element.option().hasTexture()) {
                        Texture texture = element.option().texture()[0];
                        ScreenCoordinate fitted = artworkBounds(bounds, texture.uWidth(), texture.vHeight());
                        if (fitted.widthInt() > 0 && fitted.heightInt() > 0)
                            ImageWidget.blitBlend(graphics.pose(), texture, fitted.xInt(), fitted.yInt(), fitted.widthInt(), fitted.heightInt());
                    } else if (element.option().hasIcon()) {
                        ScreenCoordinate icon = iconBounds(bounds, Math.max(16, (int) (Math.min(bounds.width(), bounds.height()) * .72)), 1);
                        if (icon.widthInt() > 0)
                            ItemWidget.renderGuiItemScaled(graphics, element.option().icon(), icon.xInt(), icon.yInt(), icon.widthInt());
                    }
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
            graphics.disableScissor();
        }
    }

    static ScreenCoordinate artworkBounds(ScreenCoordinate slot, int width, int height) {
        double scale = width > 0 && height > 0 ? Math.min(slot.width() / width, slot.height() / height) : 0;
        int w = Math.max(0, (int) Math.floor(width * scale)), h = Math.max(0, (int) Math.floor(height * scale));
        return new ScreenCoordinate(slot.x() + (slot.width() - w) / 2, slot.y() + (slot.height() - h) / 2, w, h);
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
            return index < 0 ? null : xin.vanilla.banira.client.util.NativeTextHit.styleAt(
                    Minecraft.getInstance().font.getSplitter(), lines.get(index), (int) (x - bounds.x()));
        }
    }

    public static int lineAt(ScreenCoordinate bounds, ScreenCoordinate clip, double x, double y, int lineHeight, int lines) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || lineHeight <= 0 || x < bounds.x() || x >= bounds.x() + bounds.width()
                || x < clip.x() || x >= clip.x() + clip.width() || y < clip.y() || y >= clip.y() + clip.height())
            return -1;
        int index = (int) Math.floor((y - bounds.y()) / lineHeight);
        return index >= 0 && index < lines ? index : -1;
    }

    public static void renderText(GuiGraphicsExtractor graphics, TextLayout layout, ScreenCoordinate bounds, ScreenCoordinate clip) {
        if (layout == null || bounds.width() <= 0 || bounds.height() <= 0) return;
        graphics.enableScissor((int) clip.x(), (int) clip.y(), (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        try {
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int first = Math.max(0, (int) Math.floor((clip.y() - bounds.y()) / lineHeight));
            int last = Math.min(layout.lines.size(), (int) Math.ceil((clip.y() + clip.height() - bounds.y()) / lineHeight));
            for (int index = first; index < last; index++) {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().translate((float) bounds.x(), (float) bounds.y() + index * lineHeight);
                    graphics.text(Minecraft.getInstance().font, layout.lines.get(index), 0, 0, 0xff24272c, false);
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    public static void renderEffect(GuiGraphicsExtractor graphics, MobEffectInstance effect, ScreenCoordinate bounds) {
        NativeGuiGraphics.bind(graphics);
        if (effect != null && bounds.width() > 0 && bounds.height() > 0)
            EffectIconWidget.drawEffectIcon(graphics.pose(), effect, bounds.xInt(), bounds.yInt(), bounds.widthInt(), bounds.heightInt(), false);
    }

    public static void renderOption(GuiGraphicsExtractor graphics, DropdownOption option, ScreenCoordinate bounds,
                                    ScreenCoordinate clip) {
        if (option == null || !option.hasIcon() || bounds.width() <= 0 || bounds.height() <= 0) return;
        if (clip != null && (bounds.y() + bounds.height() <= clip.y() || bounds.y() >= clip.y() + clip.height()))
            return;
        NativeGuiGraphics.bind(graphics);
        if (clip != null) graphics.enableScissor((int) clip.x(), (int) clip.y(),
                (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        try (var scope = ExtractedGuiRenderer.bind(graphics)) {
            ScreenCoordinate icon = compactIcon(bounds);
            int size = icon.widthInt(), x = icon.xInt(), y = icon.yInt();
            if (size == 0) return;
            if (option.hasTexture()) {
                for (Texture texture : option.texture())
                    ImageWidget.blitBlend(graphics.pose(), texture, x, y, size, size);
            } else ItemWidget.renderGuiItemScaled(graphics, option.icon(), x, y, size);
        } finally {
            if (clip != null) graphics.disableScissor();
        }
    }
}
