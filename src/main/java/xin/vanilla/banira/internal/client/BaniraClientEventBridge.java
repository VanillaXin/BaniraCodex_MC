package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.KeyValue;

import javax.annotation.Nonnull;

/**
 * MC 原生客户端对象到 Banira 公共事件对象的内部桥接层。
 */
public final class BaniraClientEventBridge {
    private BaniraClientEventBridge() {
    }

    public static void fireGuiChanged(Screen screen) {
        BaniraClientEvents.Client.fireGuiChanged(new BaniraScreenOpenEvent(screenInfo(screen)));
    }

    public static void fireDrawScreenPre(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                         double mouseX, double mouseY, float partialTick) {
        InputStateManager.instance().handleDrawScreenPre(mouseX, mouseY);
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().tickInteraction(screen, (int) Math.round(mouseX), (int) Math.round(mouseY));
        }
        BaniraClientEvents.Client.fireDrawScreenPre(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
    }

    public static void fireDrawScreenPost(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                          double mouseX, double mouseY, float partialTick) {
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().render(nativeGraphics, screen, (int) Math.round(mouseX), (int) Math.round(mouseY), partialTick);
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
        NotificationManager.get().render(nativeGraphics);
        BaniraClientEvents.Client.fireDrawScreenPost(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
    }

    public static void fireRenderOverlayPost(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                             float partialTick, boolean screenOpen) {
        if (element == HudOverlayElement.ALL && !screenOpen) {
            NotificationManager.get().render(nativeGraphics);
        }
        BaniraClientEvents.Client.fireRenderOverlayPost(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
    }

    public static void dispatchRenderOverlayPre(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                                float partialTick, boolean screenOpen) {
        BaniraClientEvents.dispatchRenderOverlayPre(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
    }

    public static BaniraScreenInfo screenInfo(Screen screen) {
        if (screen == null) {
            return BaniraScreenInfo.closed();
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        return new BaniraScreenInfo(screen.getClass().getName(), title, screen.width, screen.height, true);
    }

    private static BaniraDrawScreenEvent drawScreenEvent(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                                         double mouseX, double mouseY, float partialTick) {
        return new BaniraDrawScreenEvent(drawContext(nativeGraphics, partialTick), screenInfo(screen), mouseX, mouseY, partialTick);
    }

    private static BaniraOverlayRenderEvent overlayEvent(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                                         float partialTick, boolean screenOpen) {
        return new BaniraOverlayRenderEvent(element, hudContext(nativeGraphics, partialTick), partialTick, screenOpen);
    }

    private static BaniraDrawContext drawContext(@Nonnull PoseStack nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraDrawContext(BaniraClientDrawBridge.handle(nativeGraphics), screen.key(), screen.val(), partialTick);
    }

    private static BaniraHudRenderContext hudContext(@Nonnull PoseStack nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraHudRenderContext(drawContext(nativeGraphics, partialTick), screen.key(), screen.val(), partialTick);
    }
}
