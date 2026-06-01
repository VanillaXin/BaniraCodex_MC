package xin.vanilla.banira.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-only event hub exposed to child mods. Loader event objects are converted
 * by internal adapters before reaching this class.
 */
public final class BaniraClientEventHub {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraTextureReloadEvent>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenPostRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraHudRenderEvent>> hudPreRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraHudRenderEvent>> hudPostRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientTickEvent>> clientTickCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientChatEvent>> clientChatCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenCallbacks = new ArrayList<>();
    private static final List<Runnable> modClientSetupCallbacks = new ArrayList<>();

    private static volatile boolean codexDefaultsRegistered;

    private BaniraClientEventHub() {
    }

    public static void registerCodexDefaults() {
        if (codexDefaultsRegistered) {
            return;
        }
        codexDefaultsRegistered = true;
        ModLoadedToBoth.registerClientHandler(packet -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            for (String modid : packet.modids()) {
                PlayerUtils.setRemoteServerModInstalled(minecraft.player, modid, false);
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
        Client.onScreenChanged(event -> LogoModifier.modifyLogo());
        Client.onTextureReload(event -> {
            if (event.atlasLocation() != null && BaniraCodex.MODID.equals(event.atlasLocation().getNamespace())) {
                TextureUtils.resourceReloadEvent();
                QuickActionOverlay.resetSystemIconTextureCache();
            }
        });
        Client.onScreenPostRender(event -> {
            MatrixStack stack = event.draw() == null ? null : event.draw().nativeContext(MatrixStack.class);
            if (stack != null) {
                NotificationManager.get().render(stack);
            }
        });
        Hud.onPostRender(event -> {
            if (event.element() == BaniraHudOverlayElement.ALL && Minecraft.getInstance().screen == null) {
                NotificationManager.get().render(event.draw().nativeContext(MatrixStack.class));
            }
        });
    }

    public static void dispatchModClientSetup() {
        fire(modClientSetupCallbacks, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(PlayerEntity player) {
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(PlayerEntity player) {
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick(BaniraClientTickEvent event) {
        fire(clientTickCallbacks, event, "client tick");
    }

    public static void dispatchClientChat(BaniraClientChatEvent event) {
        fire(clientChatCallbacks, event, "client chat");
    }

    public static void dispatchClientScreen(BaniraClientScreenEvent event) {
        fire(clientScreenCallbacks, event, "client screen");
    }

    public static void dispatchClientScreenChanged(BaniraClientScreenEvent event) {
        fire(clientScreenChangedCallbacks, event, "client screen changed");
    }

    public static void dispatchClientTextureReload(BaniraTextureReloadEvent event) {
        fire(clientTextureReloadCallbacks, event, "client texture reload");
    }

    public static void dispatchClientScreenPostRender(BaniraClientScreenEvent event) {
        fire(clientScreenPostRenderCallbacks, event, "client screen post render");
    }

    public static void dispatchHudPreRender(BaniraHudRenderEvent event) {
        fire(hudPreRenderCallbacks, event, "hud pre render");
    }

    public static void dispatchHudPostRender(BaniraHudRenderEvent event) {
        fire(hudPostRenderCallbacks, event, "hud post render");
    }

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedInCallbacks.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedOutCallbacks.add(callback);
        }
    }

    public static final class Client {
        private Client() {
        }

        public static void onScreenChanged(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenChangedCallbacks.add(callback);
        }

        public static void onTextureReload(@Nonnull Consumer<BaniraTextureReloadEvent> callback) {
            clientTextureReloadCallbacks.add(callback);
        }

        public static void onScreenPostRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPostRenderCallbacks.add(callback);
        }

        public static void onClientTick(@Nonnull Consumer<BaniraClientTickEvent> callback) {
            clientTickCallbacks.add(callback);
        }

        public static void onChat(@Nonnull Consumer<BaniraClientChatEvent> callback) {
            clientChatCallbacks.add(callback);
        }

        public static void onScreenEvent(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenCallbacks.add(callback);
        }
    }

    public static final class Hud {
        private Hud() {
        }

        public static void onPreRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
            hudPreRenderCallbacks.add(callback);
        }

        public static void onPostRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
            hudPostRenderCallbacks.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Runnable callback) {
            modClientSetupCallbacks.add(callback);
        }
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

    private static void fire(List<Runnable> callbacks, String eventName) {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }
}
