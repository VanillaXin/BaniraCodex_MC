package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 请求数据同步包
 */
@Getter
public class RequestToBoth {
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
    public static void handle(RequestToBoth packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    BiConsumer<RequestToBoth, ServerPlayer> handler = handlers.get(packet.getRequestType());
                    if (handler != null) {
                        handler.accept(packet, player);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
