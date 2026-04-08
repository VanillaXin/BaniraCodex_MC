package xin.vanilla.banira.common.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.util.function.Supplier;

/**
 * 将 CustomConfig 中当前玩家的配置同步至服务端。
 */
public class CustomPlayerConfigSyncToServer {

    private static final long NOTIFY_OK_MS = 3000L;
    private static final long NOTIFY_ERR_MS = 4500L;

    private final String language;
    private final String notificationReceiveMode;

    public CustomPlayerConfigSyncToServer(String language, String notificationReceiveMode) {
        this.language = language != null ? language : "";
        this.notificationReceiveMode = notificationReceiveMode != null ? notificationReceiveMode : "";
    }

    public CustomPlayerConfigSyncToServer(PacketBuffer buf) {
        this.language = buf.readUtf(256);
        this.notificationReceiveMode = buf.readUtf(256);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUtf(language, 256);
        buf.writeUtf(notificationReceiveMode, 256);
    }

    public static void handle(CustomPlayerConfigSyncToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            String lang = packet.language.trim();
            Translator translator = (Translator) Translator.of(BaniraCodex.MODID);
            boolean langOk = "client".equalsIgnoreCase(lang) || "server".equalsIgnoreCase(lang)
                    || translator.getI18nFiles().contains(lang);
            if (!langOk) {
                MessageUtils.sendDefaultNotification(player,
                        BaniraComponent.get().transAuto("custom_player_config_sync_invalid_language").languageCode(Translator.getPlayerLanguage(player)),
                        EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_ERR_MS);
                return;
            }
            String uuid = PlayerUtils.getPlayerUUIDString(player);
            CustomConfig.setPlayerLanguage(uuid, lang);
            CustomConfig.setPlayerNotificationReceiveMode(uuid, packet.notificationReceiveMode);
            CustomConfig.saveCustomConfig();
            MessageUtils.sendDefaultNotification(player,
                    BaniraComponent.get().transAuto("custom_player_config_sync_ok").languageCode(Translator.getPlayerLanguage(player)),
                    EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, NOTIFY_OK_MS);
        });
        ctx.get().setPacketHandled(true);
    }
}
