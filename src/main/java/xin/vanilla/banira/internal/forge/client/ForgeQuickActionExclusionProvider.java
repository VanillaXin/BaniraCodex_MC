package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.widget.Widget;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Forge 1.16.5 背包界面占用区域适配。 */
public final class ForgeQuickActionExclusionProvider {
    private static final int CREATIVE_TOP_EXTRA = 52;
    private static final int CREATIVE_BOTTOM_EXTRA = 32;

    private ForgeQuickActionExclusionProvider() {
    }

    public static List<QuickActionRect> collect(Object nativeScreen) {
        if (!(nativeScreen instanceof Screen)) {
            return Collections.emptyList();
        }
        Screen screen = (Screen) nativeScreen;
        Set<QuickActionRect> areas = new LinkedHashSet<>();
        if (screen instanceof ContainerScreen) {
            ContainerScreen<?> container = (ContainerScreen<?>) screen;
            areas.add(containerBounds(container.getGuiLeft(), container.getGuiTop(),
                    container.getXSize(), container.getYSize(), screen instanceof CreativeScreen));
            if (screen instanceof DisplayEffectsScreen && Minecraft.getInstance().player != null) {
                int visibleEffects = (int) Minecraft.getInstance().player.getActiveEffects().stream()
                        .filter(effect -> effect.shouldRender())
                        .count();
                if (visibleEffects > 0) {
                    areas.add(effectBounds(container.getGuiLeft(), container.getGuiTop(), visibleEffects));
                }
            }
        }
        for (IGuiEventListener child : screen.children()) {
            if (child instanceof Widget) {
                Widget widget = (Widget) child;
                if (widget.visible && widget.getWidth() > 0 && widget.getHeight() > 0) {
                    areas.add(new QuickActionRect(widget.x, widget.y, widget.getWidth(), widget.getHeight()));
                }
            }
        }
        return new ArrayList<>(areas);
    }

    static QuickActionRect containerBounds(int left, int top, int width, int height, boolean creative) {
        if (!creative) {
            return new QuickActionRect(left, top, width, height);
        }
        return new QuickActionRect(left, top - CREATIVE_TOP_EXTRA, width,
                height + CREATIVE_TOP_EXTRA + CREATIVE_BOTTOM_EXTRA);
    }

    static QuickActionRect effectBounds(int guiLeft, int guiTop, int visibleEffects) {
        int count = Math.max(1, visibleEffects);
        int rowSpacing = count > 5 ? 132 / (count - 1) : 33;
        return new QuickActionRect(guiLeft - 124, guiTop, 140, 32 + (count - 1) * rowSpacing);
    }
}
