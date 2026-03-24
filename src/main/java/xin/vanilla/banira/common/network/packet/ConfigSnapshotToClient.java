package xin.vanilla.banira.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.enums.EnumPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端下发的配置全量快照，客户端写入 {@link ConfigHolder} 并刷新打开中的配置编辑界面
 */
public class ConfigSnapshotToClient {

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

    public static void handle(ConfigSnapshotToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isClient()) {
                return;
            }
            ClientSide.apply(packet);
        });
        ctx.get().setPacketHandled(true);
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
                for (Map.Entry<String, String> e : packet.snapshot().entrySet()) {
                    Object parsed = ConfigSyncToServer.decodeNetworkValue(holder, e.getKey(), e.getValue());
                    if (parsed != null) {
                        holder.set(e.getKey(), parsed);
                    }
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
            if (open instanceof ConfigEditorScreen) {
                ((ConfigEditorScreen) open).refreshUIFromHolderAfterRemoteFetch(packet.configName());
            }
            Notification ok = Notification.ofComponent(
                    BaniraComponent.get().transClientAuto("config_editor_fetch_applied", packet.snapshot().size()));
            ok.position(EnumPosition.TOP_RIGHT).durationTime(3000);
            NotificationManager.get().addNotification(ok);
        }
    }
}
