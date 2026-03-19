package xin.vanilla.banira.common.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 配置同步包：客户端将修改的配置项同步至服务端
 */
public class ConfigSyncToServer {

    private final String configName;
    private final Map<String, String> changes;

    public ConfigSyncToServer(String configName, Map<String, String> changes) {
        this.configName = configName = configName != null ? configName : "";
        this.changes = changes != null ? new HashMap<>(changes) : new HashMap<>();
    }

    public ConfigSyncToServer(PacketBuffer buf) {
        this.configName = buf.readUtf(256);
        int size = buf.readVarInt();
        this.changes = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(256);
            String value = buf.readUtf(32767);
            changes.put(key, value);
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUtf(configName, 256);
        buf.writeVarInt(changes.size());
        for (Map.Entry<String, String> e : changes.entrySet()) {
            buf.writeUtf(e.getKey(), 256);
            buf.writeUtf(e.getValue() != null ? e.getValue() : "", 32767);
        }
    }

    public static void handle(ConfigSyncToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayerEntity player = ctx.get().getSender();
                if (player == null || packet.changes.isEmpty()) return;

                // 权限检查：需要 OP 2 级以上才能同步服务端配置
                if (player.hasPermissions(2)) {
                    ConfigHolder holder = ConfigRegistry.get(packet.configName);
                    if (holder != null && holder.canSyncToServer()) {
                        for (Map.Entry<String, String> e : packet.changes.entrySet()) {
                            Object parsed = parseValue(holder, e.getKey(), e.getValue());
                            if (parsed != null) {
                                holder.set(e.getKey(), parsed);
                            }
                        }
                        // 触发配置保存（若 Mod 注册了保存逻辑）
                        saveConfig(holder);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static Object parseValue(ConfigHolder holder, String path, String value) {
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc == null) return value;
        try {
            switch (desc.getValueType()) {
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case INTEGER:
                    return Integer.parseInt(value);
                case LONG:
                    return Long.parseLong(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case ENUM:
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), value);
                    return e;
                case STRING_LIST:
                    if (value == null || value.isEmpty()) return java.util.Collections.emptyList();
                    return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }

    private static void saveConfig(ConfigHolder holder) {
        holder.save();
    }
}
