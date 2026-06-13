package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.event.BaniraHudOverlayElement;
import xin.vanilla.banira.client.util.NotificationManager;

public final class BaniraClientNotificationDefaults {

    private BaniraClientNotificationDefaults() {
    }

    public static void register() {
        BaniraClientEventHub.Screen.onPostRender(event -> {
            MatrixStack stack = event.draw() == null ? null : event.draw().nativeContext(MatrixStack.class);
            if (stack != null) {
                NotificationManager.get().render(stack);
            }
        });
        BaniraClientEventHub.Hud.onPostRender(event -> {
            if (event.element() == BaniraHudOverlayElement.ALL && !BaniraClientAccess.hasScreen()) {
                NotificationManager.get().render(event.draw().nativeContext(MatrixStack.class));
            }
        });
    }
}
