package xin.vanilla.banira.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.ConfigEditPermission;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置同步包：客户端将修改的配置项同步至服务端
 */
public class ConfigSyncToServer implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncToServer> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("config_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(ConfigSyncToServer::toBytes, ConfigSyncToServer::new);


    private static final long NOTIFY_OK_MS = 3000L;
    private static final long NOTIFY_ERR_MS = 4500L;

    private final String configName;
    private final Map<String, String> changes;

    public ConfigSyncToServer(String configName, Map<String, String> changes) {
        this.configName = configName != null ? configName : "";
        this.changes = changes != null ? new HashMap<>(changes) : new HashMap<>();
    }

    public ConfigSyncToServer(FriendlyByteBuf buf) {
        this.configName = buf.readUtf(256);
        int size = buf.readVarInt();
        this.changes = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(256);
            String value = buf.readUtf(32767);
            changes.put(key, value);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configName, 256);
        buf.writeVarInt(changes.size());
        for (Map.Entry<String, String> e : changes.entrySet()) {
            buf.writeUtf(e.getKey(), 256);
            buf.writeUtf(e.getValue() != null ? e.getValue() : "", 32767);
        }
    }

    public static void handle(ConfigSyncToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() != PacketFlow.SERVERBOUND || !(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (packet.changes.isEmpty()) {
                sendNotify(player, "config_editor_sync_server_empty", NOTIFY_ERR_MS);
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
                    ConfigEntryDescriptor pathDesc = holder.getDescriptor(e.getKey());
                    if (!ConfigEditPermission.canModifyEntry(player, pathDesc)) {
                        sendNotify(player, "config_editor_sync_server_no_permission", NOTIFY_ERR_MS);
                        return;
                    }
                }
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
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void sendNotify(ServerPlayer player, String langKey, long durationMs, Object... args) {
        String lang = Translator.getPlayerLanguage(player);
        Component text = args.length > 0
                ? BaniraComponent.get().transAuto(langKey, args).languageCode(lang)
                : BaniraComponent.get().transAuto(langKey).languageCode(lang);
        MessageUtils.sendDefaultNotification(player, text, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, durationMs);
    }

    /**
     * 将配置值编码为网络传输用的字符串（与 {@link #decodeNetworkValue} 成对使用）。
     */
    public static String encodeConfigValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(ConfigSyncToServer::encodeListElement).collect(Collectors.joining(","));
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        return String.valueOf(value);
    }

    private static String encodeListElement(Object o) {
        if (o instanceof Enum) {
            return ((Enum<?>) o).name();
        }
        return String.valueOf(o);
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
                case INTEGER_LIST:
                case LONG_LIST:
                case DOUBLE_LIST:
                case BOOLEAN_LIST:
                case ENUM_LIST:
                    return ConfigListSpecHelper.parseNetworkCsv(value, desc);
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
