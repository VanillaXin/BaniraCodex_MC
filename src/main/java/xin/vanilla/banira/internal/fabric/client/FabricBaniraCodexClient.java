package xin.vanilla.banira.internal.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.*;
import xin.vanilla.banira.internal.fabric.network.FabricNetworkChannels;

/**
 * Fabric 客户端入口，只负责把 Fabric 事件转换成 Banira 客户端运行时回调。
 */
public final class FabricBaniraCodexClient implements ClientModInitializer {
    private static final BaniraKeyHandle NOTIFICATION_LOG_KEY = BaniraInput.registerKey(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);
    private static final BaniraKeyHandle BANIRA_HUB_KEY = BaniraInput.registerKey(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    @Override
    public void onInitializeClient() {
        BaniraClientDrawBridge.install(FabricBaniraDrawHandle::new);
        FabricNetworkChannels.registerClientReceivers();
        BaniraInput.flushPendingRegistrations();
        BaniraClientOverlayBridge.loadNotificationLog();
        registerOwnNotificationTypes();
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
            BaniraClientOverlayBridge.tickOutOfScreenNotifications();
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
                BaniraClientOverlayBridge.renderHud(stack, tickDelta);
            }
        });
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            BaniraClientOverlayBridge.resetScreenInteraction();
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
                    BaniraClientInputBridge.allowMouseClick(scr, mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((scr, mouseX, mouseY, button) ->
                    BaniraClientInputBridge.allowMouseRelease(scr, mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    BaniraClientInputBridge.allowMouseScroll(scr, mouseX, mouseY, verticalAmount));
            ScreenKeyboardEvents.allowKeyPress(screen).register(BaniraClientInputBridge::allowKeyPress);
            ScreenKeyboardEvents.allowKeyRelease(screen).register(BaniraClientInputBridge::allowKeyRelease);
        });
    }

    private static void registerOwnNotificationTypes() {
        BaniraClientNotificationTypes.registerModDisplayName(BaniraCodex.MODID,
                BaniraComponent.get().transClientAuto("mod_name"));
        BaniraClientNotificationTypes.register(NotificationTypeKeys.HELP,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_help"));
        BaniraClientNotificationTypes.register(NotificationTypeKeys.COMMAND_FEEDBACK,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_command_feedback"));
    }
}
