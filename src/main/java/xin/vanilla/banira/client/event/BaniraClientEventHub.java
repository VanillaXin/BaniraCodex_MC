package xin.vanilla.banira.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.client.event.BaniraChatEvent;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.event.BaniraClientPlayerEvent;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.api.client.event.BaniraScreenEvent;
import xin.vanilla.banira.api.client.event.BaniraScreenInfo;
import xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent;
import xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端专用事件回调与转发
 */
@OnlyIn(Dist.CLIENT)
public final class BaniraClientEventHub {

    private BaniraClientEventHub() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedOutCallbacks = new ArrayList<>();

    private static final List<Consumer<ScreenEvent.Opening>> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<TextureStitchEvent.Post>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<ScreenEvent.Render.Post>> clientDrawScreenPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraGuiOverlayEvent.Pre>> clientRenderOverlayPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraGuiOverlayEvent.Post>> clientRenderOverlayPostCallbacks = new ArrayList<>();

    private static final List<Consumer<TickEvent.ClientTickEvent>> clientTickCallbacks = new ArrayList<>();
    private static final List<Consumer<ClientChatEvent>> clientChatCallbacks = new ArrayList<>();
    private static final List<Consumer<ScreenEvent>> clientGuiScreenCallbacks = new ArrayList<>();

    private static final List<Consumer<FMLClientSetupEvent>> modClientSetupCallbacks = new ArrayList<>();

    private static volatile boolean codexDefaultsRegistered;

    /**
     * BaniraCodex 在客户端的默认监听
     */
    public static void registerCodexDefaults() {
        if (codexDefaultsRegistered) {
            return;
        }
        codexDefaultsRegistered = true;
        ModLoadedToBoth.registerClientHandler(packet -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            for (String modid : packet.modids()) {
                PlayerUtils.setRemoteServerModInstalled(mc.player, modid, false);
            }
        });
        Player.onClientLoggedIn(player -> {
            List<String> ids = ModLoadedPresence.announcedModIds();
            if (!ids.isEmpty()) {
                PacketUtils.sendPacketToServer(new ModLoadedToBoth(ids));
            }
        });
        Player.onClientLoggedOut(player -> {
            if (player == null) return;
            AdvancementUtils.clearAdvancementData();
            PlayerUtils.removeRemoteServerDataStatus(player);
        });
        Client.onGuiChanged(event -> LogoModifier.modifyLogo());
        Client.onTextureReload(event -> {
            if (BaniraCodex.MODID.equals(event.getAtlas().location().getNamespace())) {
                TextureUtils.resourceReloadEvent();
                QuickActionOverlay.resetSystemIconTextureCache();
            }
        });
        Client.onDrawScreenPost(event -> NotificationManager.get().render(event.getGuiGraphics()));
        Client.onRenderOverlayPost(event -> {
            if (BaniraGuiOverlayEvent.PLAYER_LIST.equals(event.overlayId()) && Minecraft.getInstance().screen == null) {
                NotificationManager.get().render(event.guiGraphics());
            }
        });
    }

    public static void dispatchModClientSetup(FMLClientSetupEvent event) {
        fire(modClientSetupCallbacks, event, "mod client setup");
        BaniraClientEvents.dispatchModClientSetup(new BaniraClientSetupEvent());
    }

    public static void dispatchClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        fire(clientPlayerLoggedInCallbacks, event.getPlayer(), "player logged in");
        BaniraClientEvents.dispatchClientPlayerLoggedIn(playerEvent(event.getPlayer()));
    }

    public static void dispatchClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        fire(clientPlayerLoggedOutCallbacks, event.getPlayer(), "player logged out");
        BaniraClientEvents.dispatchClientPlayerLoggedOut(playerEvent(event.getPlayer()));
    }

    public static void dispatchClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        fire(clientTickCallbacks, event, "client tick");
        BaniraClientEvents.dispatchClientTick(BaniraClientTickEvent.END);
    }

    public static void dispatchClientChat(ClientChatEvent event) {
        fire(clientChatCallbacks, event, "client chat");
        BaniraClientEvents.dispatchClientChat(new BaniraChatEvent(event.getMessage()));
    }

    public static void dispatchGuiScreen(ScreenEvent event) {
        fire(clientGuiScreenCallbacks, event, "client gui screen");
        BaniraClientEvents.dispatchGuiScreen(new BaniraScreenEvent(screenInfo(event.getScreen())));
    }

    // region 分类 API：Player（客户端网络登录/登出）

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            clientPlayerLoggedInCallbacks.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            clientPlayerLoggedOutCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Client

    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<ScreenEvent.Opening> callback) {
            clientGuiChangedCallbacks.add(callback);
        }

        public static void fireGuiChanged(ScreenEvent.Opening event) {
            fire(clientGuiChangedCallbacks, event, "client gui changed");
            BaniraClientEvents.Client.fireGuiChanged(new BaniraScreenOpenEvent(screenInfo(event.getNewScreen())));
        }

        public static void onTextureReload(@Nonnull Consumer<TextureStitchEvent.Post> callback) {
            clientTextureReloadCallbacks.add(callback);
        }

        public static void onDrawScreenPost(@Nonnull Consumer<ScreenEvent.Render.Post> callback) {
            clientDrawScreenPostCallbacks.add(callback);
        }

        public static void onRenderOverlayPre(@Nonnull Consumer<BaniraGuiOverlayEvent.Pre> callback) {
            clientRenderOverlayPreCallbacks.add(callback);
        }

        public static void onRenderOverlayPost(@Nonnull Consumer<BaniraGuiOverlayEvent.Post> callback) {
            clientRenderOverlayPostCallbacks.add(callback);
        }

        public static void onClientTick(@Nonnull Consumer<TickEvent.ClientTickEvent> callback) {
            clientTickCallbacks.add(callback);
        }

        public static void onChat(@Nonnull Consumer<ClientChatEvent> callback) {
            clientChatCallbacks.add(callback);
        }

        public static void onGuiScreen(@Nonnull Consumer<ScreenEvent> callback) {
            clientGuiScreenCallbacks.add(callback);
        }

        public static void fireTextureReload(TextureStitchEvent.Post event) {
            fire(clientTextureReloadCallbacks, event, "client texture reload");
            BaniraClientEvents.Client.fireTextureReload(
                    new BaniraTextureReloadEvent(event.getAtlas().location().toString()));
        }

        public static void fireDrawScreenPost(ScreenEvent.Render.Post event) {
            fire(clientDrawScreenPostCallbacks, event, "client draw screen post");
        }

        public static void fireRenderOverlayPre(BaniraGuiOverlayEvent.Pre event) {
            fire(clientRenderOverlayPreCallbacks, event, "client render overlay pre");
        }

        public static void fireRenderOverlayPost(BaniraGuiOverlayEvent.Post event) {
            fire(clientRenderOverlayPostCallbacks, event, "client render overlay post");
        }
    }

    // endregion

    // region 分类 API：ModLifecycle（客户端）

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Consumer<FMLClientSetupEvent> callback) {
            modClientSetupCallbacks.add(callback);
        }
    }

    // endregion

    // region 内部回调执行

    private static BaniraClientPlayerEvent playerEvent(net.minecraft.world.entity.player.Player player) {
        return new BaniraClientPlayerEvent(player.getUUID(), player.getName().getString());
    }

    private static BaniraScreenInfo screenInfo(@Nullable Screen screen) {
        if (screen == null) {
            return BaniraScreenInfo.closed();
        }
        return new BaniraScreenInfo(screen.getClass().getName(), screen.getTitle().getString(),
                screen.width, screen.height, true);
    }

    private static <T> void fire(List<Consumer<T>> callbacks, T parameter, String eventName) {
        for (Consumer<T> callback : callbacks) {
            try {
                callback.accept(parameter);
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }

    // endregion

}
