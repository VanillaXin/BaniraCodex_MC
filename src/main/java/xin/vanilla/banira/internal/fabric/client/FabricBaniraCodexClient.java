package xin.vanilla.banira.internal.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraClientDrawBridge;
import xin.vanilla.banira.internal.client.BaniraClientEventBridge;
import xin.vanilla.banira.internal.fabric.network.FabricNetworkChannels;

/**
 * Fabric 客户端入口，只负责把 Fabric 事件转换成 Banira 客户端运行时回调。
 */
public final class FabricBaniraCodexClient implements ClientModInitializer {
    private static final BaniraKeyHandle NOTIFICATION_LOG_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);
    private static final BaniraKeyHandle BANIRA_HUB_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    @Override
    public void onInitializeClient() {
        BaniraClientDrawBridge.install(FabricBaniraDrawHandle::new);
        FabricNetworkChannels.registerClientReceivers();
        BaniraKeyBindings.flushPendingRegistrations();
        NotificationManager.get().loadLog();
        NotificationTypeSettingsStore.get().load();
        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup(new BaniraClientSetupEvent());

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FabricColorThemeReloadListener.INSTANCE);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                BaniraClientEventHub.dispatchClientPlayerLoggedIn(client.player);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                BaniraClientEventHub.dispatchClientPlayerLoggedOut(client.player);
                AdvancementUtils.clearAdvancementData();
                PlayerUtils.removeRemoteServerDataStatus(client.player);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BaniraScheduler.dispatchClientTick();
            BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);
            NotificationManager.get().tickOutOfScreenClick();
            if (client.screen == null) {
                if (NOTIFICATION_LOG_KEY.isDown()) {
                    client.setScreen(new NotificationLogScreen(null));
                } else if (BANIRA_HUB_KEY.isDown()) {
                    client.setScreen(new CodexNavigationScreen(null));
                }
            }
        });
        HudRenderCallback.EVENT.register((stack, tickDelta) -> {
            if (Minecraft.getInstance().screen == null) {
                NotificationManager.get().render(stack);
            }
        });
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            QuickActionOverlay.get().resetInteractionState();
            BaniraClientEventBridge.fireGuiChanged(screen);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.beforeRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                BaniraClientEventBridge.fireDrawScreenPre(stack, scr, mouseX, mouseY, tickDelta);
            });
            ScreenEvents.afterRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                BaniraClientEventBridge.fireDrawScreenPost(stack, scr, mouseX, mouseY, tickDelta);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((scr, mouseX, mouseY, button) ->
                    !QuickActionOverlay.get().handleMouseClicked(scr, mouseX, mouseY, button)
                            && !NotificationManager.get().tryHandleHudClick(mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((scr, mouseX, mouseY, button) ->
                    !QuickActionOverlay.get().handleMouseReleased(scr, mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    !QuickActionOverlay.get().handleMouseScroll(scr, mouseX, mouseY, verticalAmount));
        });
    }
}
