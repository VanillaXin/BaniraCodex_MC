package xin.vanilla.banira.internal.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.quickaction.CustomQuickActionManager;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.client.*;
import xin.vanilla.banira.internal.client.dev.BaniraNetworkSmokeClientRunner;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.internal.fabric.compat.FabricExternalInventoryCompatibility;
import xin.vanilla.banira.internal.fabric.network.FabricNetworkChannels;

/**
 * Fabric 客户端入口，只负责把 Fabric 事件转换成 Banira 客户端运行时回调。
 */
public final class FabricBaniraCodexClient implements ClientModInitializer {
    private static final BaniraKeyHandle NOTIFICATION_LOG_KEY = BaniraInput.registerKey(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);
    private static final BaniraKeyHandle BANIRA_HUB_KEY = BaniraInput.registerKey(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    @Override
    public void onInitializeClient() {
        xin.vanilla.banira.client.util.ExtractedGuiRenderer.install(
                graphics -> graphics.guiRenderState, graphics -> graphics.scissorStack.peek());
        BaniraClientDrawBridge.install(FabricBaniraDrawHandle::new);
        if (xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract.inspect(
                FabricBaniraCodexClient.class.getClassLoader()).compatible()) {
            xin.vanilla.banira.internal.compat.aui.AuiDocumentRenderSubmission.register();
        }
        FabricNetworkChannels.registerClientReceivers();
        BaniraInput.flushPendingRegistrations();
        BaniraClientOverlayBridge.loadNotificationLog();
        registerOwnNotificationTypes();
        NotificationTypeSettingsStore.get().load();
        BaniraCodexClientBootstrap.init();
        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup(new BaniraClientSetupEvent());
        FabricExternalInventoryCompatibility.init();

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                xin.vanilla.banira.Identifier.id().create("color_themes"), FabricColorThemeReloadListener.INSTANCE);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                BaniraClientEventHub.dispatchClientPlayerLoggedIn(client.player);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BaniraNetworkSmokeClientRunner.tick(client);
            BaniraScheduler.dispatchClientTick();
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.CLIENT);
            BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);
            BaniraClientOverlayBridge.tickOutOfScreenNotifications();
            if (client.screen == null) {
                CustomQuickActionManager.get().tickKeyBindings();
                if (NOTIFICATION_LOG_KEY.isDown()) {
                    client.setScreen(new NotificationLogScreen(null));
                } else if (BANIRA_HUB_KEY.isDown()) {
                    client.setScreen(new CodexNavigationScreen(null));
                }
            }
        });
        HudElementRegistry.addLast(xin.vanilla.banira.Identifier.id().create("overlays"), (stack, tickDelta) -> {
            if (Minecraft.getInstance().screen == null) {
                BaniraClientOverlayBridge.renderHud(stack, tickDelta.getGameTimeDeltaPartialTick(false));
            }
            FabricAuiClientHooks.renderGame(stack);
        });
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            BaniraClientOverlayBridge.resetScreenInteraction();
            BaniraClientEventBridge.fireGuiChanged(screen);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.beforeExtract(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                BaniraClientEventBridge.fireDrawScreenPre(stack, scr, mouseX, mouseY, tickDelta);
            });
            ScreenEvents.afterExtract(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                BaniraClientEventBridge.fireDrawScreenPost(stack, scr, mouseX, mouseY, tickDelta);
                FabricAuiClientHooks.renderScreen(scr, stack);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((scr, event) ->
                    BaniraClientInputBridge.allowMouseClick(scr, event.x(), event.y(), event.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((scr, event) ->
                    BaniraClientInputBridge.allowMouseRelease(scr, event.x(), event.y(), event.button()));
            ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    BaniraClientInputBridge.allowMouseScroll(scr, mouseX, mouseY, verticalAmount));
            ScreenKeyboardEvents.allowKeyPress(screen).register((scr, event) ->
                    BaniraClientInputBridge.allowKeyPress(scr, event.key(), event.scancode(), event.modifiers()));
            ScreenKeyboardEvents.allowKeyRelease(screen).register((scr, event) ->
                    BaniraClientInputBridge.allowKeyRelease(scr, event.key(), event.scancode(), event.modifiers()));
        });
    }

    private static void registerOwnNotificationTypes() {
        BaniraClientNotificationTypes.registerImportance(NotificationTypeKeys.HELP, false);
        BaniraClientNotificationTypes.registerImportance(NotificationTypeKeys.COMMAND_FEEDBACK, false);
        BaniraClientEventHub.Player.onClientLoggedOut(player ->
                xin.vanilla.banira.client.notification.NotificationTypeRegistry.clearServerImportance());
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
