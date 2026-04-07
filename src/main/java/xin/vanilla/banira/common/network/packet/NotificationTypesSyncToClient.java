package xin.vanilla.banira.common.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 将服务端当前已知的通知类型 id 列表同步给刚登录的玩家客户端
 */
public class NotificationTypesSyncToClient {

    private static final int MAX_TYPE_ID_LENGTH = 128;
    private static final int MAX_TYPE_COUNT = 512;

    private final List<String> typeIds;

    public NotificationTypesSyncToClient(List<String> typeIds) {
        this.typeIds = typeIds != null ? new ArrayList<>(typeIds) : new ArrayList<>();
    }

    public NotificationTypesSyncToClient(PacketBuffer buf) {
        int n = buf.readVarInt();
        if (n < 0) {
            n = 0;
        }
        n = Math.min(n, MAX_TYPE_COUNT);
        this.typeIds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            typeIds.add(NotificationTypeKeys.normalizeOrDefault(buf.readUtf(MAX_TYPE_ID_LENGTH)));
        }
    }

    public void toBytes(PacketBuffer buf) {
        int n = Math.min(typeIds.size(), MAX_TYPE_COUNT);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            String id = typeIds.get(i);
            buf.writeUtf(id != null ? NotificationTypeKeys.normalizeOrDefault(id) : NotificationTypeKeys.DEFAULT, MAX_TYPE_ID_LENGTH);
        }
    }

    public List<String> typeIds() {
        return typeIds;
    }

    public static void handle(NotificationTypesSyncToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientSide.handle(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        private static void handle(NotificationTypesSyncToClient packet) {
            NotificationTypeRegistry.registerAllFromServer(packet.typeIds());
        }
    }
}
