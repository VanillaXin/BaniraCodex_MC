package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.NetworkContext;
import xin.vanilla.banira.common.network.NetworkPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 请求数据同步包
 */
@Getter
public class RequestToBoth implements NetworkPacket {
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
    public static void handle(RequestToBoth packet, NetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isServerSide()) {
                ServerPlayer player = ctx.sender();
                if (player != null) {
                    BiConsumer<RequestToBoth, ServerPlayer> handler = handlers.get(packet.getRequestType());
                    if (handler != null) {
                        handler.accept(packet, player);
                    }
                }
            }
        });
            }
}
