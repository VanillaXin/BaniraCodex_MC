package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;

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
            for (Map.Entry<String, String> e : packet.snapshot().entrySet()) {
                Object parsed = ConfigSyncToServer.decodeNetworkValue(holder, e.getKey(), e.getValue());
                if (parsed != null) {
                    holder.set(e.getKey(), parsed);
                }
            }
            holder.save();
        } catch (Exception ex) {
            LOGGER.error("Failed to apply config snapshot for {}", packet.configName(), ex);
            ConfigEditorNotifier.show("config_editor_fetch_apply_failed", 4000,
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            return;
        }
        Screen open = Minecraft.getInstance().screen;
        if (open instanceof ConfigEditorScreen) {
            ((ConfigEditorScreen) open).refreshUIFromHolderAfterRemoteFetch(packet.configName());
        }
        ConfigEditorNotifier.show("config_editor_fetch_applied", 3000, String.valueOf(packet.snapshot().size()));
    }
}
