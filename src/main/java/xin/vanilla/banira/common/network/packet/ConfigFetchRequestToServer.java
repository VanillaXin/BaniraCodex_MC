package xin.vanilla.banira.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.NetworkContext;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.ConfigEditPermission;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.editable.ConfigEntryDescriptor;
import xin.vanilla.banira.editable.EditableConfigHolder;
import xin.vanilla.banira.editable.EditableConfigRegistry;

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

    public ConfigFetchRequestToServer(FriendlyByteBuf buf) {
        this.configName = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configName, 256);
    }

    public String configName() {
        return configName;
    }

    public static void handle(ConfigFetchRequestToServer packet, NetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (!ConfigEditPermission.canAccessServerConfigEditor(player)) {
                sendErr(player, "text.autoconfig.banira_codex.editor.sync_server_no_permission");
                return;
            }
            EditableConfigHolder holder = EditableConfigRegistry.get(packet.configName);
            if (holder == null) {
                sendErr(player, "text.autoconfig.banira_codex.editor.message.sync_server_unknown_config", packet.configName);
                return;
            }
            if (!holder.canSyncToServer()) {
                sendErr(player, "text.autoconfig.banira_codex.editor.sync_server_not_applicable");
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
    }

    private static void sendErr(ServerPlayer player, String translationKey, Object... args) {
        String lang = Translator.getPlayerLanguage(player);
        Component text = args.length > 0
                ? BaniraComponent.get().transLang(lang, translationKey, args)
                : BaniraComponent.get().transLang(lang, translationKey);
        MessageUtils.sendDefaultNotification(player, text, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_ERR_MS);
    }
}
