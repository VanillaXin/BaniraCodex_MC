package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
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

    public static void renderScreenOverlay(@Nonnull PoseStack stack, @Nonnull Screen screen,
                                           double mouseX, double mouseY, float partialTick) {
        NotificationManager.get().render(stack);
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().render(stack, screen, (int) Math.round(mouseX), (int) Math.round(mouseY), partialTick);
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    public static void renderHudOverlay(@Nonnull PoseStack stack) {
        NotificationManager.get().render(stack);
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
}
