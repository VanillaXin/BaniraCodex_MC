package xin.vanilla.banira.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.NetworkContext;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.editable.EditableConfigHolder;
import xin.vanilla.banira.editable.EditableConfigRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端下发的配置全量快照，客户端写入 {@link EditableConfigHolder} 并刷新打开中的配置编辑界面
 */
public class ConfigSnapshotToClient implements NetworkPacket {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String configName;
    private final Map<String, String> snapshot;

    public ConfigSnapshotToClient(String configName, Map<String, String> snapshot) {
        this.configName = configName != null ? configName : "";
        this.snapshot = snapshot != null ? new HashMap<>(snapshot) : new HashMap<>();
    }

    public ConfigSnapshotToClient(FriendlyByteBuf buf) {
        this.configName = buf.readUtf(256);
        int size = buf.readVarInt();
        this.snapshot = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(256);
            String value = buf.readUtf(32767);
            snapshot.put(key, value);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
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

    public static void handle(ConfigSnapshotToClient packet, NetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isClientSide()) {
                return;
            }
            apply(packet);
        });
    }

    private static void apply(ConfigSnapshotToClient packet) {
        EditableConfigHolder holder = EditableConfigRegistry.get(packet.configName());
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
            holder.validateAfterChanges();
            holder.save();
        } catch (Exception ex) {
            LOGGER.error("Failed to apply config snapshot for {}", packet.configName(), ex);
            Notification err = Notification.ofComponent(
                    BaniraComponent.get().transClient("text.autoconfig.banira_codex.editor.message.fetch_apply_failed",
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
                BaniraComponent.get().transClient("text.autoconfig.banira_codex.editor.message.fetch_applied", packet.snapshot().size()));
        ok.position(EnumPosition.TOP_RIGHT).durationTime(3000);
        NotificationManager.get().addNotification(ok);
    }
}
