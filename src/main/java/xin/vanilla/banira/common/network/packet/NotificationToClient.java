package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.JsonUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class NotificationToClient {

    private static final int MAX_COMPONENT_JSON_LENGTH = 16384;
    private static final String DEFAULT_POSITION = "TOP_RIGHT";
    private static final String DEFAULT_ANIMATION = "AUTO";
    private static final String DEFAULT_STYLE = "NORMAL";
    private static final int MAX_TYPE_ID_LENGTH = 128;

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
        this.componentJson = JsonUtils.toString(AbstractComponent.serialize(component));
        this.positionName = position != null ? position.name() : DEFAULT_POSITION;
        this.animationName = animation != null ? animation.name() : DEFAULT_ANIMATION;
        this.durationTime = durationTime > 0 ? durationTime : 5000L;
        this.styleName = style != null ? style.name() : DEFAULT_STYLE;
        this.typeId = NotificationTypeKeys.normalizeOrDefault(notificationType);
    }

    public NotificationToClient(NotificationData data) {
        this(data.component(), data.position(), data.animation(), data.durationTime(), data.style(),
                data.notificationType() != null ? data.notificationType() : NotificationTypeKeys.DEFAULT);
    }

    public NotificationToClient(Component component) {
        this(component, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000L);
    }

    public NotificationToClient(FriendlyByteBuf buf) {
        this.componentJson = buf.readUtf(MAX_COMPONENT_JSON_LENGTH);
        this.positionName = buf.readUtf(64);
        this.animationName = buf.readUtf(64);
        this.durationTime = buf.readLong();
        this.styleName = buf.readUtf(32);
        this.typeId = NotificationTypeKeys.normalizeOrDefault(buf.readUtf(MAX_TYPE_ID_LENGTH));
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.componentJson != null ? this.componentJson : "{}", MAX_COMPONENT_JSON_LENGTH);
        buf.writeUtf(this.positionName != null ? this.positionName : DEFAULT_POSITION, 64);
        buf.writeUtf(this.animationName != null ? this.animationName : DEFAULT_ANIMATION, 64);
        buf.writeLong(this.durationTime);
        buf.writeUtf(this.styleName != null ? this.styleName : DEFAULT_STYLE, 32);
        buf.writeUtf(this.typeId != null ? this.typeId : NotificationTypeKeys.DEFAULT, MAX_TYPE_ID_LENGTH);
    }

    public static void handle(NotificationToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientSide.handle(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }


    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        private static final Logger LOGGER = LogManager.getLogger();

        private static void handle(NotificationToClient packet) {
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
    }
}
