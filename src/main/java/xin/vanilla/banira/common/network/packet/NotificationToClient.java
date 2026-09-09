package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.internal.client.BaniraClientPacketHandlers;
import xin.vanilla.banira.common.notification.NotificationBudget;

@Getter
@Accessors(fluent = true)
public class NotificationToClient implements NetworkPacket {

    private static final String DEFAULT_POSITION = "TOP_RIGHT";
    private static final String DEFAULT_ANIMATION = "AUTO";
    private static final String DEFAULT_STYLE = "NORMAL";

    private final String componentJson;
    private final String positionName;
    private final String animationName;
    private final long durationTime;
    private final String styleName;
    private final String typeId;

    public NotificationToClient(Component component, EnumPosition position, EnumMoveType animation, long durationTime) {
        this(component, position, animation, durationTime, EnumNotificationStyle.NORMAL);
    }

    public NotificationToClient(Component component, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style) {
        this(component, position, animation, durationTime, style, NotificationTypeKeys.DEFAULT);
    }

    public NotificationToClient(Component component, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style, String notificationType) {
        this(NotificationBudget.componentJson(component), position, animation, durationTime, style, notificationType);
    }

    public NotificationToClient(NotificationBudget.Payload payload, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style, String notificationType) {
        this(payload.componentJson(), position, animation, durationTime, style, notificationType);
    }

    private NotificationToClient(String componentJson, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style, String notificationType) {
        this.componentJson = componentJson;
        this.positionName = position != null ? position.name() : DEFAULT_POSITION;
        this.animationName = animation != null ? animation.name() : DEFAULT_ANIMATION;
        this.durationTime = durationTime > 0 ? durationTime : 5000L;
        this.styleName = style != null ? style.name() : DEFAULT_STYLE;
        this.typeId = NotificationBudget.notificationType(notificationType);
    }

    public NotificationToClient(NotificationData data) {
        this(data.component(), data.position(), data.animation(), data.durationTime(), data.style(),
                data.notificationType() != null ? data.notificationType() : NotificationTypeKeys.DEFAULT);
    }

    public NotificationToClient(Component component) {
        this(component, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000L);
    }

    public NotificationToClient(BaniraPacketBuffer buf) {
        this.componentJson = buf.readUtf(NotificationBudget.MAX_COMPONENT_JSON_BYTES);
        this.positionName = buf.readUtf(64);
        this.animationName = buf.readUtf(64);
        this.durationTime = buf.readLong();
        this.styleName = buf.readUtf(32);
        this.typeId = NotificationTypeKeys.normalizeOrDefault(buf.readUtf(NotificationBudget.MAX_TYPE_ID_BYTES));
    }

    public void toBytes(BaniraPacketBuffer buf) {
        NotificationBudget.validateUtf8(this.componentJson != null ? this.componentJson : "{}",
                NotificationBudget.MAX_COMPONENT_JSON_BYTES, "Banira component JSON");
        NotificationBudget.notificationType(this.typeId);
        buf.writeUtf(this.componentJson != null ? this.componentJson : "{}", NotificationBudget.MAX_COMPONENT_JSON_BYTES);
        buf.writeUtf(this.positionName != null ? this.positionName : DEFAULT_POSITION, 64);
        buf.writeUtf(this.animationName != null ? this.animationName : DEFAULT_ANIMATION, 64);
        buf.writeLong(this.durationTime);
        buf.writeUtf(this.styleName != null ? this.styleName : DEFAULT_STYLE, 32);
        buf.writeUtf(this.typeId != null ? this.typeId : NotificationTypeKeys.DEFAULT, NotificationBudget.MAX_TYPE_ID_BYTES);
    }

    public static void handle(NotificationToClient packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientSide()) {
                BaniraClientPacketHandlers.showNotification(packet);
            }
        });
        ctx.markHandled();
    }
}
