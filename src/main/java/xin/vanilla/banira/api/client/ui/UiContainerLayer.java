package xin.vanilla.banira.api.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.Map;

/**
 * Appearance inside an existing container screen; inventory input remains with Minecraft.
 */
public interface UiContainerLayer extends AutoCloseable {
    UiView view();

    boolean render(GuiGraphicsExtractor graphics, Map<String, ScreenCoordinate> regions);

    void fallback();

    @Override
    void close();
}
