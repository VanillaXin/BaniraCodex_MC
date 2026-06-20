package xin.vanilla.banira.internal.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigSnapshotClientHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private ConfigSnapshotClientHandler() {
    }

    public static void apply(ConfigSnapshotToClient packet) {
        ConfigHolder holder = ConfigRegistry.get(packet.configName());
        if (holder == null) {
            return;
        }
        try {
            applyValidatedSnapshot(holder, packet.snapshot());
            holder.save();
        } catch (Exception ex) {
            LOGGER.error("Failed to apply config snapshot for {}", packet.configName(), ex);
            ConfigEditorNotifier.show("config_editor_fetch_apply_failed", 4000,
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            return;
        }
        BaniraClientScreenService.refreshOpenConfigEditor(packet.configName());
        ConfigEditorNotifier.show("config_editor_fetch_applied", 3000, String.valueOf(packet.snapshot().size()));
    }

    static void applyValidatedSnapshot(ConfigHolder holder, Map<String, String> snapshot) {
        Map<String, Object> parsedSnapshot = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : snapshot.entrySet()) {
            Object parsed = ConfigSyncToServer.decodeNetworkValue(holder, e.getKey(), e.getValue());
            if (!holder.validate(e.getKey(), parsed)) {
                throw new IllegalArgumentException("Invalid config value: " + e.getKey());
            }
            parsedSnapshot.put(e.getKey(), parsed);
        }
        for (Map.Entry<String, Object> e : parsedSnapshot.entrySet()) {
            holder.set(e.getKey(), e.getValue());
        }
    }
}
