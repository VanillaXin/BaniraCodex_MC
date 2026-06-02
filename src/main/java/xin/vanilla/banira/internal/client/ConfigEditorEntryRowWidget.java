package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;

/**
 * Lightweight row container for a config label, value editor, reset button, and tooltip.
 */
public final class ConfigEditorEntryRowWidget extends BaseWidget {
    public ConfigEditorEntryRowWidget(BaniraScreen screen) {
        super(screen);
    }

    @Override
    public double effectiveHeight() {
        double maxBottom = 0;
        for (IWidget child : children()) {
            if (child == null || !child.visible()) {
                continue;
            }
            ScreenCoordinate b = child.bounds();
            if (b != null) {
                double bottom = b.y() + child.effectiveHeight();
                if (bottom > maxBottom) {
                    maxBottom = bottom;
                }
            }
        }
        return maxBottom > 0 ? maxBottom : (bounds() != null ? bounds().height() : 0);
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        return true;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }
        renderChildren(stack, partialTicks);
    }
}
