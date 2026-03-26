package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 请求数据同步包
 */
@Getter
public class RequestToBoth implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestToBoth> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("request_to_both"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestToBoth> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(RequestToBoth::toBytes, RequestToBoth::new);

    /**
     * 请求类型ID到处理器的映射
     */
    private static final Map<Integer, BiConsumer<RequestToBoth, ServerPlayer>> handlers = new HashMap<>();

    /**
     * 请求包的类型ID
     */
    private int requestType;

    public RequestToBoth() {
    }

    public RequestToBoth(int requestType) {
        this.requestType = requestType;
    }

    public RequestToBoth(FriendlyByteBuf buf) {
        this.requestType = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.requestType);
    }

    /**
     * 注册请求类型处理器
     *
     * @param requestType 请求类型ID
     * @param handler     处理器
     */
    public static void registerHandler(int requestType, BiConsumer<RequestToBoth, ServerPlayer> handler) {
        handlers.put(requestType, handler);
    }

    /**
     * 处理请求包
     *
     * @param packet 请求包
     * @param ctx    网络事件上下文
     */
    public static void handle(RequestToBoth packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.SERVERBOUND && ctx.player() instanceof ServerPlayer player) {
                BiConsumer<RequestToBoth, ServerPlayer> handler = handlers.get(packet.getRequestType());
                if (handler != null) {
                    handler.accept(packet, player);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
