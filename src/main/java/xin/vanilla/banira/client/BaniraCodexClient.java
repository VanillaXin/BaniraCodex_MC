package xin.vanilla.banira.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
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
import xin.vanilla.banira.internal.network.NetworkInit;

public final class BaniraCodexClient implements ClientModInitializer {
    public static final KeyMapping NOTIFICATION_LOG_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyMapping BANIRA_HUB_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    @Override
    public void onInitializeClient() {
        NetworkInit.registerClientReceivers();
        BaniraKeyBindings.flushPendingRegistrations(KeyBindingHelper::registerKeyBinding);
        NotificationManager.get().loadLog();
        NotificationTypeSettingsStore.get().load();
        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup();
        BaniraCodex.ClientProxy.init();

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(BaniraColorThemeLoader.INSTANCE);
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
            BaniraScheduler.onClientTick();
            BaniraClientEventHub.dispatchClientTick();
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
            BaniraClientEventHub.Client.fireGuiChanged(screen);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.beforeRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                if (QuickActionOverlay.isSupportedInventoryScreen(scr)) {
                    QuickActionOverlay.get().tickInteraction(scr, mouseX, mouseY);
                }
            });
            ScreenEvents.afterRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
                BaniraClientEventHub.Client.fireDrawScreenPost(stack);
                if (QuickActionOverlay.isSupportedInventoryScreen(scr)) {
                    QuickActionOverlay.get().render(stack, scr, mouseX, mouseY, tickDelta);
                    QuickActionOverlay.get().flushSaveIfNeeded();
                }
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
