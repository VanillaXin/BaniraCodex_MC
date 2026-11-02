package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.network.packet.ConfigFetchRequestToServer;
import xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.config.CustomConfig;

public final class ClientPlayerPreferences {
    private static Object requestedConnection;
    private static Object activeConnection;

    private ClientPlayerPreferences() {
    }

    private static Object currentConnection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return null;
        net.minecraft.network.Connection connection = mc.getConnection().getConnection();
        return connection != null && connection.isConnected() ? connection : null;
    }

    public static boolean isCurrentConnection(Object origin) {
        return origin != null && origin == currentConnection();
    }

    private static Object bindCurrent() {
        Minecraft mc = Minecraft.getInstance();
        Object connection = currentConnection();
        if (connection == null) {
            disconnect();
            return null;
        }
        if (CustomConfig.beginPlayerPreferencesClient(connection, PlayerUtils.getPlayerUUIDString(mc.player))) {
            if (activeConnection != null) xin.vanilla.banira.internal.client.ui.CommonUiPages.disconnect();
            activeConnection = connection;
            requestedConnection = null;
        }
        return connection;
    }

    public static void tick() {
        Object connection = bindCurrent();
        Minecraft mc = Minecraft.getInstance();
        if (connection == null || requestedConnection == connection
                || !PlayerUtils.isRemoteServerModInstalled(mc.player, BaniraCodex.MODID)) return;
        requestedConnection = connection;
        try {
            PacketUtils.sendPacketToServer(new ConfigFetchRequestToServer(CustomConfig.PLAYER_PREFERENCES_CONFIG));
        } catch (RuntimeException failure) {
            LogManager.getLogger().warn("Failed to request player preferences", failure);
        }
    }

    public static void apply(ConfigSnapshotToClient packet, Object origin) {
        Object current = currentConnection();
        if (origin == null || origin != current) return;
        if (bindCurrent() == null) return;
        CustomConfig.applyPlayerPreferencesClient(origin,
                PlayerUtils.getPlayerUUIDString(Minecraft.getInstance().player), packet.snapshot());
    }

    public static void disconnect() {
        requestedConnection = null;
        activeConnection = null;
        CustomConfig.clearPlayerPreferencesClient();
    }
}
