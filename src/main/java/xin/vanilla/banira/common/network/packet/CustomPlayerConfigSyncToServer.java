package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.server.ServerSenderAccess;

/**
 * 将 CustomConfig 中当前玩家的配置同步至服务端。
 */
@Getter
@Accessors(fluent = true)
public class CustomPlayerConfigSyncToServer implements NetworkPacket {

    private static final long NOTIFY_OK_MS = 3000L;
    private static final long NOTIFY_ERR_MS = 4500L;

    private final String language;
    private final String notificationReceiveMode;

    public CustomPlayerConfigSyncToServer(String language, String notificationReceiveMode) {
        this.language = language != null ? language : "";
        this.notificationReceiveMode = notificationReceiveMode != null ? notificationReceiveMode : "";
    }

    public CustomPlayerConfigSyncToServer(BaniraPacketBuffer buf) {
        this.language = buf.readUtf(256);
        this.notificationReceiveMode = buf.readUtf(256);
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(language, 256);
        buf.writeUtf(notificationReceiveMode, 256);
    }

    public static void handle(CustomPlayerConfigSyncToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            Object sender = ctx.sender();
            if (sender == null) {
                return;
            }
            String lang = packet.language.trim();
            Translator translator = (Translator) Translator.of(Banira.MOD_ID);
            boolean langOk = "client".equalsIgnoreCase(lang) || "server".equalsIgnoreCase(lang)
                    || translator.getI18nFiles().contains(lang);
            if (!langOk) {
                ServerSenderAccess.sendDefaultNotification(sender,
                        BaniraComponent.get().transAuto("custom_player_config_sync_invalid_language").languageCode(ServerSenderAccess.language(sender)),
                        EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_ERR_MS);
                return;
            }
            String uuid = ServerSenderAccess.uuidString(sender);
            if (uuid == null) {
                return;
            }
            CustomConfig.setPlayerLanguage(uuid, lang);
            CustomConfig.setPlayerNotificationReceiveMode(uuid, packet.notificationReceiveMode);
            CustomConfig.saveCustomConfig();
            ServerSenderAccess.sendDefaultNotification(sender,
                    BaniraComponent.get().transAuto("custom_player_config_sync_ok").languageCode(ServerSenderAccess.language(sender)),
                    EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_OK_MS);
        });
        ctx.markHandled();
    }
}
