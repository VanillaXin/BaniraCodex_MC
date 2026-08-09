package xin.vanilla.banira.api.event;

import xin.vanilla.banira.common.util.BaniraEventBus;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 子 mod 的加载器无关事件注册入口；原生事件转换与派发由 internal adapter 负责。
 */
public final class BaniraEvents {
    private BaniraEvents() {
    }

    public static final class Server {
        private Server() {
        }

        public static BaniraEventRegistration onStarting(@Nonnull Consumer<BaniraServerEvent> callback) {
            return BaniraEventBus.Server.onStarting(callback);
        }

        public static BaniraEventRegistration onStarted(@Nonnull Consumer<BaniraServerEvent> callback) {
            return BaniraEventBus.Server.onStarted(callback);
        }

        public static BaniraEventRegistration onStopping(@Nonnull Consumer<BaniraServerEvent> callback) {
            return BaniraEventBus.Server.onStopping(callback);
        }

        public static BaniraEventRegistration onTick(@Nonnull Consumer<BaniraServerEvent> callback) {
            return BaniraEventBus.Server.onTick(callback);
        }
    }

    public static final class Player {
        private Player() {
        }

        public static BaniraEventRegistration onLoggedIn(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            return BaniraEventBus.PlayerEvents.onLoggedIn(callback);
        }

        public static BaniraEventRegistration onLoggedOut(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            return BaniraEventBus.PlayerEvents.onLoggedOut(callback);
        }

        public static BaniraEventRegistration onChangedDimension(
                @Nonnull Consumer<BaniraPlayerDimensionEvent> callback) {
            return BaniraEventBus.PlayerEvents.onChangedDimension(callback);
        }

        public static BaniraEventRegistration onSave(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            return BaniraEventBus.PlayerEvents.onSave(callback);
        }
    }

    public static final class World {
        private World() {
        }

        public static BaniraEventRegistration onUnload(@Nonnull Consumer<BaniraWorldEvent> callback) {
            return BaniraEventBus.WorldEvents.onUnload(callback);
        }

        public static BaniraEventRegistration onTick(@Nonnull Consumer<BaniraWorldEvent> callback) {
            return BaniraEventBus.WorldEvents.onTick(callback);
        }
    }

    public static final class Save {
        private Save() {
        }

        public static BaniraEventRegistration onWorldSave(@Nonnull Runnable callback) {
            return BaniraEventBus.Save.onWorldSave(callback);
        }

        public static BaniraEventRegistration onChunkSave(@Nonnull Runnable callback) {
            return BaniraEventBus.Save.onChunkSave(callback);
        }

        public static BaniraEventRegistration onPlayerSave(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            return BaniraEventBus.Save.onPlayerSave(callback);
        }
    }

    public static BaniraEventRegistration onCommonSetup(
            @Nonnull Consumer<BaniraCommonSetupEvent> callback) {
        return BaniraLifecycle.onCommonSetup(callback);
    }
}
