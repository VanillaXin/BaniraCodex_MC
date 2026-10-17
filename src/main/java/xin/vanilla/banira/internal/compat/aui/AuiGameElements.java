package xin.vanilla.banira.internal.compat.aui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.ImageWidget;
import xin.vanilla.banira.client.gui.widget.ItemWidget;
import xin.vanilla.banira.client.gui.widget.EffectIconWidget;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.Collections;
import java.util.List;

public final class AuiGameElements {
    private AuiGameElements() { }

    public static final class TextLayout {
        private final Component text;
        private List<FormattedCharSequence> lines = Collections.emptyList();
        private int width = -1;
        public TextLayout(Component text) { this.text = text.copy(); }
        public boolean prepare(int availableWidth) {
            if (availableWidth <= 0 || width == availableWidth) return false;
            width = availableWidth;
            lines = Minecraft.getInstance().font.split(text, width);
            return true;
        }
        public int height() { return lines.size() * Minecraft.getInstance().font.lineHeight; }
    }

    public static void renderText(GuiGraphics graphics, TextLayout layout, ScreenCoordinate bounds, ScreenCoordinate clip) {
        if (layout == null || bounds.width() <= 0 || bounds.height() <= 0) return;
        graphics.enableScissor((int) clip.x(), (int) clip.y(), (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        try {
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int first = Math.max(0, (int) Math.floor((clip.y() - bounds.y()) / lineHeight));
            int last = Math.min(layout.lines.size(), (int) Math.ceil((clip.y() + clip.height() - bounds.y()) / lineHeight));
            for (int index = first; index < last; index++) graphics.drawString(Minecraft.getInstance().font,
                    layout.lines.get(index), (int) bounds.x(), (int) bounds.y() + index * lineHeight, 0xff24272c, false);
        } finally { graphics.disableScissor(); }
    }

    public static void renderEffect(GuiGraphics graphics, MobEffectInstance effect, ScreenCoordinate bounds) {
        if (effect != null && bounds.width() > 0 && bounds.height() > 0)
            EffectIconWidget.drawEffectIcon(graphics.pose(), effect, bounds.xInt(), bounds.yInt(), bounds.widthInt(), bounds.heightInt(), true);
    }

    public static void renderOption(GuiGraphics graphics, DropdownOption option, ScreenCoordinate bounds,
                                    ScreenCoordinate clip) {
        if (option == null || !option.hasIcon() || bounds.width() <= 0 || bounds.height() <= 0) return;
        if (clip != null && (bounds.y() + bounds.height() <= clip.y() || bounds.y() >= clip.y() + clip.height())) return;
        if (clip != null) graphics.enableScissor((int) clip.x(), (int) clip.y(),
                (int) Math.ceil(clip.x() + clip.width()), (int) Math.ceil(clip.y() + clip.height()));
        try {
            int size = Math.min(14, (int) bounds.height());
            int x = (int) bounds.x() + 4, y = (int) bounds.y() + ((int) bounds.height() - size) / 2;
            if (option.hasTexture()) {
                for (Texture texture : option.texture()) ImageWidget.blitBlend(graphics.pose(), texture, x, y, size, size);
            } else ItemWidget.renderGuiItemFlatBlit(graphics.pose(), Minecraft.getInstance(), option.icon(), x, y, size);
        } finally { if (clip != null) graphics.disableScissor(); }
    }
}
