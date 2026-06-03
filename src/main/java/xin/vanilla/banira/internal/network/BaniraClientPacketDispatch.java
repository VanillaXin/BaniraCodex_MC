package xin.vanilla.banira.internal.network;

import xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;

import java.util.function.Consumer;

/**
 * Client packet callback registry used by common packet classes without loading GUI classes.
 */
public final class BaniraClientPacketDispatch {
    private static Consumer<ConfigSnapshotToClient> configSnapshotHandler = packet -> {
    };
    private static Consumer<NotificationToClient> notificationHandler = packet -> {
    };
    private static Consumer<NotificationTypesSyncToClient> notificationTypesHandler = packet -> {
    };

    private BaniraClientPacketDispatch() {
    }

    public static void registerConfigSnapshotHandler(Consumer<ConfigSnapshotToClient> handler) {
        configSnapshotHandler = handler != null ? handler : packet -> {
        };
    }

    public static void registerNotificationHandler(Consumer<NotificationToClient> handler) {
        notificationHandler = handler != null ? handler : packet -> {
        };
    }

    public static void registerNotificationTypesHandler(Consumer<NotificationTypesSyncToClient> handler) {
        notificationTypesHandler = handler != null ? handler : packet -> {
        };
    }

    public static void handle(ConfigSnapshotToClient packet) {
        configSnapshotHandler.accept(packet);
    }

    public static void handle(NotificationToClient packet) {
        notificationHandler.accept(packet);
    }

    public static void handle(NotificationTypesSyncToClient packet) {
        notificationTypesHandler.accept(packet);
    }
}
