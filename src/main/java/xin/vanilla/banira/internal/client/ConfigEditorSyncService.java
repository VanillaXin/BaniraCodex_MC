package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.network.packet.ConfigFetchRequestToServer;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;
import xin.vanilla.banira.common.util.PacketUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Network encoding and sending for the config editor.
 */
public final class ConfigEditorSyncService {
    private ConfigEditorSyncService() {
    }

    public static boolean hasServerConnection() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static Map<String, String> encodePayload(Map<String, Object> payload) {
        Map<String, String> encoded = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            encoded.put(entry.getKey(), ConfigSyncToServer.encodeConfigValue(entry.getValue()));
        }
        return encoded;
    }

    public static void sendSync(ConfigHolder holder, Map<String, String> encodedPayload) {
        PacketUtils.sendPacketToServer(new ConfigSyncToServer(holder.getConfigName(), encodedPayload));
    }

    public static void requestSnapshot(ConfigHolder holder) {
        PacketUtils.sendPacketToServer(new ConfigFetchRequestToServer(holder.getConfigName()));
    }

}
