package xin.vanilla.banira.common.network.packet;

import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.internal.network.BaniraClientPacketDispatch;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端下发的配置全量快照，客户端写入 {@link ConfigHolder} 并刷新打开中的配置编辑界面
 */
public class ConfigSnapshotToClient implements NetworkPacket {

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
            BaniraClientPacketDispatch.handle(packet);
        });
        ctx.markHandled();
    }
}
