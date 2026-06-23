package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;

import javax.annotation.Nonnull;

/**
 * 客户端浮层的内部桥接层；加载器入口只转发原生事件，不直接维护浮层细节。
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

    public static void renderHud(@Nonnull PoseStack stack, float tickDelta) {
        BaniraClientEventBridge.fireRenderOverlayPost(HudOverlayElement.ALL, stack, tickDelta, false);
    }

    public static void resetScreenInteraction() {
        QuickActionOverlay.get().resetInteractionState();
    }

    public static boolean allowMouseClick(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        return !QuickActionOverlay.get().handleMouseClicked(screen, mouseX, mouseY, button)
                && !NotificationManager.get().tryHandleHudClick(mouseX, mouseY, button);
    }

    public static boolean allowMouseRelease(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        return !QuickActionOverlay.get().handleMouseReleased(screen, mouseX, mouseY, button);
    }

    public static boolean allowMouseScroll(@Nonnull Screen screen, double mouseX, double mouseY, double verticalAmount) {
        return !QuickActionOverlay.get().handleMouseScroll(screen, mouseX, mouseY, verticalAmount);
    }
}
