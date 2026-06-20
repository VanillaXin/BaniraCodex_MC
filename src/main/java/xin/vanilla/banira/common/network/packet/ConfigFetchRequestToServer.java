package xin.vanilla.banira.common.network.packet;

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
import xin.vanilla.banira.internal.server.ServerSenderAccess;

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
            Object sender = ctx.sender();
            if (sender == null) {
                return;
            }
            if (!ServerSenderAccess.canAccessServerConfigEditor(sender)) {
                sendErr(sender, "config_editor_sync_server_no_permission");
                return;
            }
            ConfigHolder holder = ConfigRegistry.get(packet.configName);
            if (holder == null) {
                sendErr(sender, "config_editor_sync_server_unknown_config", packet.configName);
                return;
            }
            if (!holder.canSyncToServer()) {
                sendErr(sender, "config_editor_sync_server_not_applicable");
                return;
            }
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (ConfigEntryDescriptor d : holder.getDescriptors()) {
                String path = d.getPath();
                Object v = holder.get(path);
                snapshot.put(path, v != null ? ConfigSyncToServer.encodeConfigValue(v) : "");
            }
            ServerSenderAccess.sendPacket(sender, new ConfigSnapshotToClient(packet.configName, snapshot));
        });
        ctx.markHandled();
    }

    private static void sendErr(Object sender, String langKey, Object... args) {
        String lang = ServerSenderAccess.language(sender);
        Component text = args.length > 0
                ? BaniraComponent.get().transAuto(langKey, args).languageCode(lang)
                : BaniraComponent.get().transAuto(langKey).languageCode(lang);
        ServerSenderAccess.sendDefaultNotification(sender, text, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_ERR_MS);
    }
}
