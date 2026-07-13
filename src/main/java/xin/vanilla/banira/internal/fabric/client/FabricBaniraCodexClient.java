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
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.api.client.event.BaniraKeyboardEvent;
import xin.vanilla.banira.api.client.event.BaniraMouseEvent;
import xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraClientGuiService;
import xin.vanilla.banira.internal.client.BaniraClientModSetup;
import xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap;
import xin.vanilla.banira.internal.client.BaniraKeyBindingService;
import xin.vanilla.banira.internal.fabric.network.FabricNetworkChannels;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/** Fabric 客户端入口，将 1.16 回调转换为稳定的 Banira 客户端事件。 */
public final class FabricBaniraCodexClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BaniraCodexClientBootstrap.init();
        FabricNetworkChannels.registerClientReceivers();
        BaniraKeyBindingService.installRegistrar(KeyBindingHelper::registerKeyBinding);
        BaniraClientModSetup.initOnClientSetup();

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FabricColorThemeReloadListener.INSTANCE);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) BaniraClientEventHub.dispatchClientPlayerLoggedIn(client.player);
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
            BaniraClientGuiService.handleClientTickEnd(client.screen == null);
        });
        HudRenderCallback.EVENT.register((stack, tickDelta) -> {
            boolean screenOpen = Minecraft.getInstance().screen != null;
            BaniraClientEventHub.dispatchRenderOverlayPreNative(HudOverlayElement.ALL, stack, tickDelta, screenOpen);
            BaniraClientEventHub.Client.fireRenderOverlayPostNative(HudOverlayElement.ALL, stack, tickDelta, screenOpen);
        });
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            BaniraClientEventHub.Client.fireGuiChanged(new BaniraScreenOpenEvent(BaniraClientEventHub.screenInfo(screen)));
            BaniraClientGuiService.handleScreenOpened();
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> registerScreenCallbacks(screen));
    }

    private static void registerScreenCallbacks(net.minecraft.client.gui.screens.Screen screen) {
        ScreenEvents.beforeRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) -> {
            BaniraClientGuiService.handleScreenPreRender(scr, mouseX, mouseY);
            BaniraClientEventHub.Client.fireDrawScreenPreNative(stack, scr, mouseX, mouseY, tickDelta);
        });
        ScreenEvents.afterRender(screen).register((scr, stack, mouseX, mouseY, tickDelta) ->
                BaniraClientEventHub.Client.fireDrawScreenPostNative(stack, scr, mouseX, mouseY, tickDelta));
        ScreenMouseEvents.allowMouseClick(screen).register((scr, mouseX, mouseY, button) -> {
            BaniraMouseEvent event = BaniraMouseEvent.clicked(BaniraClientEventHub.screenInfo(scr), mouseX, mouseY, button);
            BaniraClientEventHub.dispatchMouseClickedPre(event, scr);
            return !event.canceled() && !BaniraClientGuiService.handleMouseClicked(scr, event);
        });
        ScreenMouseEvents.allowMouseRelease(screen).register((scr, mouseX, mouseY, button) -> {
            BaniraMouseEvent event = BaniraMouseEvent.released(BaniraClientEventHub.screenInfo(scr), mouseX, mouseY, button);
            BaniraClientEventHub.dispatchMouseReleasedPre(event, scr);
            return !event.canceled() && !BaniraClientGuiService.handleMouseReleased(scr, event);
        });
        ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
            BaniraMouseEvent event = BaniraMouseEvent.scrolled(BaniraClientEventHub.screenInfo(scr), mouseX, mouseY, verticalAmount);
            BaniraClientEventHub.dispatchMouseScrolledPre(event, scr);
            return !event.canceled() && !BaniraClientGuiService.handleMouseScrolled(scr, event);
        });
        ScreenKeyboardEvents.allowKeyPress(screen).register((scr, keyCode, scanCode, modifiers) -> {
            BaniraKeyboardEvent event = BaniraKeyboardEvent.pressed(BaniraClientEventHub.screenInfo(scr), keyCode, scanCode, modifiers);
            BaniraClientEventHub.dispatchKeyPressedPre(event);
            BaniraClientGuiService.handleKeyboard(event);
            return !event.canceled();
        });
        ScreenKeyboardEvents.allowKeyRelease(screen).register((scr, keyCode, scanCode, modifiers) -> {
            BaniraKeyboardEvent event = BaniraKeyboardEvent.released(BaniraClientEventHub.screenInfo(scr), keyCode, scanCode, modifiers);
            BaniraClientEventHub.dispatchKeyReleasedPost(event);
            BaniraClientGuiService.handleKeyboard(event);
            return !event.canceled();
        });
    }
}
