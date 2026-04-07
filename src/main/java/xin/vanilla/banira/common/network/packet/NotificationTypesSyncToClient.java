package xin.vanilla.banira.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * 将服务端当前已知的通知类型 id 列表同步给刚登录的玩家客户端
 */
public class NotificationTypesSyncToClient implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NotificationTypesSyncToClient> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("notification_types"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NotificationTypesSyncToClient> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(NotificationTypesSyncToClient::toBytes, NotificationTypesSyncToClient::new);

    private static final int MAX_TYPE_ID_LENGTH = 128;
    private static final int MAX_TYPE_COUNT = 512;

    private final List<String> typeIds;

    public NotificationTypesSyncToClient(List<String> typeIds) {
        this.typeIds = typeIds != null ? new ArrayList<>(typeIds) : new ArrayList<>();
    }

    public NotificationTypesSyncToClient(FriendlyByteBuf buf) {
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

    public void toBytes(FriendlyByteBuf buf) {
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

    public static void handle(NotificationTypesSyncToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.CLIENTBOUND) {
                ClientSide.handle(packet);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        private static void handle(NotificationTypesSyncToClient packet) {
            NotificationTypeRegistry.registerAllFromServer(packet.typeIds());
        }
    }
}
