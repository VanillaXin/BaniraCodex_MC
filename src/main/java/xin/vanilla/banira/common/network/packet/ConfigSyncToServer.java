package xin.vanilla.banira.common.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.Translator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 配置同步包：客户端将修改的配置项同步至服务端
 */
public class ConfigSyncToServer {

    private static final long NOTIFY_OK_MS = 3000L;
    private static final long NOTIFY_ERR_MS = 4500L;

    private final String configName;
    private final Map<String, String> changes;

    public ConfigSyncToServer(String configName, Map<String, String> changes) {
        this.configName = configName != null ? configName : "";
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
            if (!ctx.get().getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (packet.changes.isEmpty()) {
                sendNotify(player, "config_editor_sync_server_empty", NOTIFY_ERR_MS);
                return;
            }
            if (!player.hasPermissions(2)) {
                sendNotify(player, "config_editor_sync_server_no_permission", NOTIFY_ERR_MS);
                return;
            }
            ConfigHolder holder = ConfigRegistry.get(packet.configName);
            if (holder == null) {
                sendNotify(player, "config_editor_sync_server_unknown_config", NOTIFY_ERR_MS, packet.configName);
                return;
            }
            if (!holder.canSyncToServer()) {
                sendNotify(player, "config_editor_sync_server_not_applicable", NOTIFY_ERR_MS);
                return;
            }
            try {
                for (Map.Entry<String, String> e : packet.changes.entrySet()) {
                    Object parsed = decodeNetworkValue(holder, e.getKey(), e.getValue());
                    if (parsed != null) {
                        holder.set(e.getKey(), parsed);
                    }
                }
                saveConfig(holder);
                sendNotify(player, "config_editor_sync_server_ok", NOTIFY_OK_MS,
                        String.valueOf(packet.changes.size()));
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                sendNotify(player, "config_editor_sync_server_save_failed", NOTIFY_ERR_MS, msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void sendNotify(ServerPlayerEntity player, String langKey, long durationMs, Object... args) {
        String lang = Translator.getPlayerLanguage(player);
        Component text = args.length > 0
                ? Component.transAuto(BaniraCodex.MODID, langKey, args).languageCode(lang)
                : Component.transAuto(BaniraCodex.MODID, langKey).languageCode(lang);
        MessageUtils.sendNotification(player, text, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, durationMs);
    }

    /**
     * 将配置值编码为网络传输用的字符串（与 {@link #decodeNetworkValue} 成对使用）。
     */
    public static String encodeConfigValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(value);
    }

    /**
     * 将网络字符串解析为可写入 {@link ConfigHolder} 的对象；无法解析时回退为原始字符串。
     */
    public static Object decodeNetworkValue(ConfigHolder holder, String path, String value) {
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc == null) {
            return value;
        }
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
                    if (value == null || value.isEmpty()) {
                        return Collections.emptyList();
                    }
                    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
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
