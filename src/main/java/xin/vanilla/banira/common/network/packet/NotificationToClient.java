package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class NotificationToClient {

    private static final int MAX_COMPONENT_JSON_LENGTH = 16384;
    private static final String DEFAULT_POSITION = "TOP_RIGHT";
    private static final String DEFAULT_ANIMATION = "AUTO";

    private final String componentJson;
    private final String positionName;
    private final String animationName;
    private final long durationTime;

    public NotificationToClient(Component component, EnumPosition position, EnumMoveType animation, long durationTime) {
        this.componentJson = JsonUtils.toString(Component.serialize(component));
        this.positionName = position != null ? position.name() : DEFAULT_POSITION;
        this.animationName = animation != null ? animation.name() : DEFAULT_ANIMATION;
        this.durationTime = durationTime > 0 ? durationTime : 5000L;
    }

    public NotificationToClient(NotificationData data) {
        this(data.component(), data.position(), data.animation(), data.durationTime());
    }

    public NotificationToClient(Component component) {
        this(component, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000L);
    }

    public NotificationToClient(PacketBuffer buf) {
        this.componentJson = buf.readUtf(MAX_COMPONENT_JSON_LENGTH);
        this.positionName = buf.readUtf(64);
        this.animationName = buf.readUtf(64);
        this.durationTime = buf.readLong();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUtf(this.componentJson != null ? this.componentJson : "{}", MAX_COMPONENT_JSON_LENGTH);
        buf.writeUtf(this.positionName != null ? this.positionName : DEFAULT_POSITION, 64);
        buf.writeUtf(this.animationName != null ? this.animationName : DEFAULT_ANIMATION, 64);
        buf.writeLong(this.durationTime);
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
                Component component = Component.deserialize(JsonUtils.parseObject(packet.componentJson()));
                EnumPosition position = EnumPosition.valueOfEx(packet.positionName());
                if (position == null) position = EnumPosition.TOP_RIGHT;
                EnumMoveType animation;
                try {
                    animation = EnumMoveType.valueOf(packet.animationName());
                } catch (Exception ignored) {
                    animation = EnumMoveType.AUTO;
                }
                NotificationData data = NotificationData.of(component, position, animation, packet.durationTime());
                Notification n = Notification.fromData(data);
                NotificationManager.get().addNotification(n, true);
            } catch (Exception e) {
                LOGGER.error("Failed to handle notification packet", e);
            }
        }
    }
}
