package xin.vanilla.banira.internal.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Banira 内部客户端事件接线；子 mod 应使用 {@link BaniraClientEvents} 注册回调。
 */
public final class BaniraClientEventHub {
    private static volatile boolean codexDefaultsRegistered;

    private BaniraClientEventHub() {
    }

    /**
     * BaniraCodex 在客户端的默认监听。
     */
    public static void registerCodexDefaults() {
        if (codexDefaultsRegistered) {
            return;
        }
        codexDefaultsRegistered = true;
        ModLoadedToBoth.registerClientHandler(packet -> {
            LocalPlayer player = BaniraClientRuntime.localPlayer();
            if (player == null) {
                return;
            }
            for (String modid : packet.modids()) {
                PlayerUtils.setRemoteServerModInstalled(player, modid, false);
            }
        });
        BaniraClientEvents.Player.onClientLoggedIn(player -> {
            List<String> ids = ModLoadedPresence.announcedModIds();
            if (!ids.isEmpty()) {
                PacketUtils.sendPacketToServer(new ModLoadedToBoth(ids));
            }
        });
        BaniraClientEvents.Client.onGuiChanged(event -> {
            BaniraClientEvents.resetInputTrackers();
            QuickActionOverlay.get().resetInteractionState();
            LogoModifier.modifyLogo();
        });
        BaniraClientEvents.Client.onTextureReload(event -> {
            // 公共事件只暴露字符串，内部再转换成当前 MC 版本的资源位置类型。
            ResourceLocation atlasLocation = ResourceLocation.tryParse(event.atlasLocation());
            if (atlasLocation != null && Banira.MOD_ID.equals(atlasLocation.getNamespace())) {
                TextureUtils.resourceReloadEvent();
                QuickActionOverlay.resetSystemIconTextureCache();
            }
        });
        BaniraClientEvents.Client.onKeyPressedPre(event -> InputStateManager.instance().handleKeyPressed(event.keyCode()));
        BaniraClientEvents.Client.onKeyReleasedPost(event -> InputStateManager.instance().handleKeyReleased(event.keyCode()));
        BaniraClientEvents.Client.onClientTick(event -> {
            if (event == BaniraClientTickEvent.END && BaniraClientRuntime.currentScreen() == null) {
                InputStateManager.instance().handleScreenClosed();
            }
        });
    }

    public static void dispatchModClientSetup(@Nonnull BaniraClientSetupEvent event) {
        BaniraClientEvents.dispatchModClientSetup(event);
    }

    public static void dispatchClientPlayerLoggedIn(@Nonnull net.minecraft.world.entity.player.Player player) {
        BaniraClientEvents.dispatchClientPlayerLoggedIn(toPlayerEvent(player));
    }

    public static void dispatchClientPlayerLoggedOut(@Nonnull net.minecraft.world.entity.player.Player player) {
        AdvancementUtils.clearAdvancementData();
        PlayerUtils.removeRemoteServerDataStatus(player);
        BaniraClientEvents.dispatchClientPlayerLoggedOut(toPlayerEvent(player));
    }

    private static BaniraClientPlayerEvent toPlayerEvent(@Nonnull net.minecraft.world.entity.player.Player player) {
        return new BaniraClientPlayerEvent(player.getUUID(), player.getName().getString());
    }

    public static void dispatchClientTick(@Nonnull BaniraClientTickEvent event) {
        BaniraClientEvents.dispatchClientTick(event);
    }

    public static void dispatchClientChat(@Nonnull BaniraChatEvent event) {
        BaniraClientEvents.dispatchClientChat(event);
    }

    public static void dispatchGuiScreen(@Nonnull BaniraScreenEvent event) {
        BaniraClientEvents.dispatchGuiScreen(event);
    }

    public static void dispatchRenderOverlayPre(@Nonnull BaniraOverlayRenderEvent event) {
        BaniraClientEvents.dispatchRenderOverlayPre(event);
    }

    public static void dispatchMouseClickedPre(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseClickedPre(event);
    }

    public static void dispatchMouseReleasedPre(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseReleasedPre(event);
    }

    public static void dispatchMouseReleasedPost(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseReleasedPost(event);
    }

    public static void dispatchMouseScrolledPre(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseScrolledPre(event);
    }

    public static void dispatchMouseDraggedPre(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseDraggedPre(event);
    }

    public static void dispatchKeyPressedPre(@Nonnull BaniraKeyboardEvent event) {
        BaniraClientEvents.dispatchKeyPressedPre(event);
    }

    public static void dispatchKeyReleasedPost(@Nonnull BaniraKeyboardEvent event) {
        BaniraClientEvents.dispatchKeyReleasedPost(event);
    }

    public static void dispatchCharTypedPre(@Nonnull BaniraKeyboardEvent event) {
        BaniraClientEvents.dispatchCharTypedPre(event);
    }
}
