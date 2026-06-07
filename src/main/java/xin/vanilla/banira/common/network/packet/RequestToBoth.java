package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.*;

import java.util.function.BiConsumer;

/**
 * 请求数据同步包
 */
@Getter
@Accessors(fluent = true)
public class RequestToBoth implements NetworkPacket {
    /**
     * 请求类型ID到处理器的注册表。
     */
    private static final RequestPacketHandlers HANDLERS = new RequestPacketHandlers();

    /**
     * 请求包的类型ID
     */
    private int requestType;

    public RequestToBoth() {
    }

    public RequestToBoth(int requestType) {
        this.requestType = requestType;
    }

    public RequestToBoth(BaniraPacketBuffer buf) {
        this.requestType = buf.readVarInt();
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeVarInt(this.requestType);
    }

    /**
     * 注册请求类型处理器
     *
     * @param requestType 请求类型ID
     * @param handler     处理器
     */
    public static RequestHandlerRegistration registerHandler(int requestType, BiConsumer<RequestToBoth, ServerPlayer> handler) {
        return HANDLERS.register(requestType, handler);
    }

    /**
     * 注销指定请求类型的当前处理器。
     */
    public static boolean unregisterHandler(int requestType) {
        return HANDLERS.unregister(requestType);
    }

    /**
     * 处理请求包
     *
     * @param packet 请求包
     * @param ctx    网络事件上下文
     */
    public static void handle(RequestToBoth packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isServerSide()) {
                ServerPlayer player = ctx.sender();
                HANDLERS.dispatch(packet, player);
            }
        });
        ctx.markHandled();
    }

}
