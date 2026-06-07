package xin.vanilla.banira.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务端下发的配置全量快照，客户端写入 {@link ConfigHolder} 并刷新打开中的配置编辑界面
 */
public class ConfigSnapshotToClient implements NetworkPacket {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String configName;
    private final Map<String, String> snapshot;

    public ConfigSnapshotToClient(String configName, Map<String, String> snapshot) {
        this.configName = configName != null ? configName : "";
        this.snapshot = snapshot != null ? new HashMap<>(snapshot) : new HashMap<>();
    }

    public ConfigSnapshotToClient(BaniraPacketBuffer buf) {
        this.configName = buf.readUtf(256);
        int size = buf.readVarInt();
        this.snapshot = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(256);
            String value = buf.readUtf(32767);
            snapshot.put(key, value);
        }
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(configName, 256);
        buf.writeVarInt(snapshot.size());
        for (Map.Entry<String, String> e : snapshot.entrySet()) {
            buf.writeUtf(e.getKey(), 256);
            buf.writeUtf(e.getValue() != null ? e.getValue() : "", 32767);
        }
    }

    public String configName() {
        return configName;
    }

    public Map<String, String> snapshot() {
        return snapshot;
    }

    public static void handle(ConfigSnapshotToClient packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isClientSide()) {
                return;
            }
            ClientSide.apply(packet);
        });
        ctx.markHandled();
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {

        private ClientSide() {
        }

        private static void apply(ConfigSnapshotToClient packet) {
            ConfigHolder holder = ConfigRegistry.get(packet.configName());
            if (holder == null) {
                return;
            }
            try {
                Map<String, Object> parsedSnapshot = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : packet.snapshot().entrySet()) {
                    Object parsed = ConfigSyncToServer.decodeNetworkValue(holder, e.getKey(), e.getValue());
                    if (!holder.validate(e.getKey(), parsed)) {
                        throw new IllegalArgumentException("Invalid config value: " + e.getKey());
                    }
                    parsedSnapshot.put(e.getKey(), parsed);
                }
                for (Map.Entry<String, Object> e : parsedSnapshot.entrySet()) {
                    holder.set(e.getKey(), e.getValue());
                }
                holder.save();
            } catch (Exception ex) {
                LOGGER.error("Failed to apply config snapshot for {}", packet.configName(), ex);
                Notification err = Notification.ofComponent(
                        BaniraComponent.get().transClientAuto("config_editor_fetch_apply_failed",
                                ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                err.position(EnumPosition.TOP_RIGHT).durationTime(4000);
                NotificationManager.get().addNotification(err);
                return;
            }
            Screen open = Minecraft.getInstance().screen;
            if (open instanceof ConfigEditorScreen screen) {
                screen.refreshUIFromHolderAfterRemoteFetch(packet.configName());
            }
            Notification ok = Notification.ofComponent(
                    BaniraComponent.get().transClientAuto("config_editor_fetch_applied", packet.snapshot().size()));
            ok.position(EnumPosition.TOP_RIGHT).durationTime(3000);
            NotificationManager.get().addNotification(ok);
        }
    }
}
