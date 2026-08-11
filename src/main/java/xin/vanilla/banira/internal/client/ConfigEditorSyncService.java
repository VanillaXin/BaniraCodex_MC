package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
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

    public static void applyEncodedValues(ConfigHolder holder, Map<String, String> encodedPayload) {
        for (Map.Entry<String, String> entry : encodedPayload.entrySet()) {
            holder.set(entry.getKey(), decodeValue(holder, entry.getKey(), entry.getValue()));
        }
    }

    private static Object decodeValue(ConfigHolder holder, String path, String value) {
        Object decoded = ConfigSyncToServer.decodeNetworkValue(holder, path, value);
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc != null && desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE && decoded instanceof Double) {
            double d = (Double) decoded;
            int dp = desc.getDecimalPlaces();
            double factor = Math.pow(10, dp);
            return Math.round(d * factor) / factor;
        }
        return decoded;
    }
}
