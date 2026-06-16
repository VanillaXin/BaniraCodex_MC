package xin.vanilla.banira.common.network.packet;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.ConfigEditPermission;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.Translator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端请求服务端返回指定配置的全量快照
 */
public class ConfigFetchRequestToServer implements NetworkPacket {

    private static final long NOTIFY_ERR_MS = 4500L;

    private final String configName;

    public ConfigFetchRequestToServer(String configName) {
        this.configName = configName != null ? configName : "";
    }

    public ConfigFetchRequestToServer(BaniraPacketBuffer buf) {
        this.configName = buf.readUtf(256);
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(configName, 256);
    }

    public String configName() {
        return configName;
    }

    public static void handle(ConfigFetchRequestToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (!ConfigEditPermission.canAccessServerConfigEditor(player)) {
                sendErr(player, "config_editor_sync_server_no_permission");
                return;
            }
            ConfigHolder holder = ConfigRegistry.get(packet.configName);
            if (holder == null) {
                sendErr(player, "config_editor_sync_server_unknown_config", packet.configName);
                return;
            }
            if (!holder.canSyncToServer()) {
                sendErr(player, "config_editor_sync_server_not_applicable");
                return;
            }
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (ConfigEntryDescriptor d : holder.getDescriptors()) {
                String path = d.getPath();
                Object v = holder.get(path);
                snapshot.put(path, v != null ? ConfigSyncToServer.encodeConfigValue(v) : "");
            }
            PacketUtils.sendPacketToPlayer(new ConfigSnapshotToClient(packet.configName, snapshot), player);
        });
        ctx.markHandled();
    }

    private static void sendErr(ServerPlayer player, String langKey, Object... args) {
        String lang = Translator.getPlayerLanguage(player);
        Component text = args.length > 0
                ? BaniraComponent.get().transAuto(langKey, args).languageCode(lang)
                : BaniraComponent.get().transAuto(langKey).languageCode(lang);
        MessageUtils.sendDefaultNotification(player, text, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_ERR_MS);
    }
}
