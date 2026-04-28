package xin.vanilla.banira.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fabric 客户端专用事件回调与转发。
 */
@Environment(EnvType.CLIENT)
public final class BaniraClientEventHub {
    private BaniraClientEventHub() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<Screen>> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<PoseStack>> clientDrawScreenPostCallbacks = new ArrayList<>();
    private static final List<Runnable> clientTickCallbacks = new ArrayList<>();
    private static final List<Runnable> modClientSetupCallbacks = new ArrayList<>();

    private static volatile boolean codexDefaultsRegistered;

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
            PlayerUtils.removeRemoteServerDataStatus(player);
        });
        Client.onGuiChanged(screen -> LogoModifier.modifyLogo());
        Client.onDrawScreenPost(stack -> NotificationManager.get().render(stack));
    }

    public static void dispatchModClientSetup() {
        fire(modClientSetupCallbacks, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(net.minecraft.world.entity.player.Player player) {
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(net.minecraft.world.entity.player.Player player) {
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick() {
        fire(clientTickCallbacks, "client tick");
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

        public static void onGuiChanged(@Nonnull Consumer<Screen> callback) {
            clientGuiChangedCallbacks.add(callback);
        }

        public static void fireGuiChanged(Screen screen) {
            fire(clientGuiChangedCallbacks, screen, "client gui changed");
        }

        public static void onDrawScreenPost(@Nonnull Consumer<PoseStack> callback) {
            clientDrawScreenPostCallbacks.add(callback);
        }

        public static void fireDrawScreenPost(PoseStack stack) {
            fire(clientDrawScreenPostCallbacks, stack, "client draw screen post");
        }
    }

    // endregion

    // region 分类 API：ModLifecycle（客户端）

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Runnable callback) {
            modClientSetupCallbacks.add(callback);
        }
    }

    // endregion

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
