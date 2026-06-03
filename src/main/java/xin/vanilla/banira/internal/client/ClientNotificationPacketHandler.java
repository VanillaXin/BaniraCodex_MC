package xin.vanilla.banira.internal.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.NotificationTypeSyncEntry;
import xin.vanilla.banira.common.util.JsonUtils;

/**
 * Client-only handlers for notification packets decoded by the common network layer.
 */
public final class ClientNotificationPacketHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private ClientNotificationPacketHandler() {
    }

    public static void handle(NotificationToClient packet) {
        try {
            Component component = BaniraComponent.get().deserialize(JsonUtils.parseObject(packet.componentJson()));
            EnumPosition position = EnumPosition.valueOfEx(packet.positionName());
            if (position == null) position = EnumPosition.TOP_RIGHT;
            EnumMoveType animation;
            try {
                animation = EnumMoveType.valueOf(packet.animationName());
            } catch (Exception ignored) {
                animation = EnumMoveType.AUTO;
            }
            EnumNotificationStyle style = EnumNotificationStyle.valueOfEx(packet.styleName());
            NotificationData data = NotificationData.of(component, position, animation, packet.durationTime(), style, packet.typeId());
            Notification n = Notification.fromData(data, true);
            NotificationManager.get().addNotification(n, true);
        } catch (Exception e) {
            LOGGER.error("Failed to handle notification packet", e);
        }
    }

    public static void handleTypes(NotificationTypesSyncToClient packet) {
        for (NotificationTypeSyncEntry e : packet.entries()) {
            if (e == null) {
                continue;
            }
            NotificationTypeRegistry.ensureKnown(e.typeId());
            NotificationTypeRegistry.acceptServerSyncedDisplayDefault(e.typeId(), e.defaultDisplayIfAbsent());
            NotificationTypeSettingsStore.get().applyResolvedDisplayDefaultIfNoSavedEntry(e.typeId());
        }
    }
}
