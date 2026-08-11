package xin.vanilla.banira.internal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 客户端浮层内部桥接层：加载器事件入口只转发原生事件，具体浮层细节留在这里。
 */
public final class BaniraClientOverlayBridge {

    private BaniraClientOverlayBridge() {
    }

    public static void loadNotificationLog() {
        NotificationManager.get().loadLog();
    }

    public static void tickOutOfScreenNotifications() {
        NotificationManager.get().tickOutOfScreenClick();
    }

    public static void resetScreenInteraction() {
        QuickActionOverlay.get().resetInteractionState();
    }

    public static void resetTextureCaches() {
        QuickActionOverlay.resetSystemIconTextureCache();
    }

    public static void renderScreenOverlay(@Nonnull GuiGraphics graphics, @Nonnull Screen screen,
                                           double mouseX, double mouseY, float partialTick) {
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().render(graphics, screen, (int) Math.round(mouseX), (int) Math.round(mouseY), partialTick);
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
        NotificationManager.get().render(graphics);
    }

    public static void renderHudOverlay(@Nonnull GuiGraphics graphics) {
        NotificationManager.get().render(graphics);
    }

    /** Fabric HUD 回调保留统一签名，具体事件分发由加载器入口完成。 */
    public static void renderHud(@Nonnull GuiGraphics graphics, float partialTick) {
        renderHudOverlay(graphics);
        BaniraClientEventHub.Client.fireRenderOverlayPostNative(
                HudOverlayElement.ALL, graphics.pose(), partialTick, false);
    }

    public static void tickScreenInteraction(@Nonnull Screen screen, double mouseX, double mouseY) {
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().tickInteraction(screen, (int) Math.round(mouseX), (int) Math.round(mouseY));
        }
    }

    public static boolean handleMouseClicked(@Nullable Screen screen, double mouseX, double mouseY, int button) {
        if (screen != null && QuickActionOverlay.get().handleMouseClicked(screen, mouseX, mouseY, button)) {
            return true;
        }
        return NotificationManager.get().tryHandleHudClick(mouseX, mouseY, button);
    }

    public static boolean handleMouseReleased(@Nullable Screen screen, double mouseX, double mouseY, int button) {
        return screen != null && QuickActionOverlay.get().handleMouseReleased(screen, mouseX, mouseY, button);
    }

    public static boolean handleMouseScrolled(@Nullable Screen screen, double mouseX, double mouseY, double scrollDelta) {
        return screen != null && QuickActionOverlay.get().handleMouseScroll(screen, mouseX, mouseY, scrollDelta);
    }

    public static boolean allowMouseClick(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        return !handleMouseClicked(screen, mouseX, mouseY, button);
    }

    public static boolean allowMouseRelease(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        return !handleMouseReleased(screen, mouseX, mouseY, button);
    }

    public static boolean allowMouseScroll(@Nonnull Screen screen, double mouseX, double mouseY, double scrollDelta) {
        return !handleMouseScrolled(screen, mouseX, mouseY, scrollDelta);
    }
}
