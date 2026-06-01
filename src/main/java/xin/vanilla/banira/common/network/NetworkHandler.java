package xin.vanilla.banira.common.network;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.util.IIdentifier;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    private final SimpleChannel channel;
    @Getter
    private final ResourceLocation channelName;
    @Getter
    private final String modId;
    private int nextPacketId = 0;

    /**
     * 创建网络处理器实例
     *
     * @param channelName 通道名称
     * @param IIdentifier 资源工厂
     * @return NetworkHandler 实例
     */
    public static NetworkHandler create(String channelName, IIdentifier IIdentifier) {
        ResourceLocation id = IIdentifier.create(channelName);
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                id,
                () -> PROTOCOL_VERSION,
                clientVersion -> true,      // 客户端版本始终有效
                serverVersion -> true       // 服务端版本始终有效
        );
        return new NetworkHandler(channel, id);
    }

    private NetworkHandler(SimpleChannel channel, ResourceLocation channelName) {
        this.channel = channel;
        this.channelName = channelName;
        this.modId = channelName.getNamespace();
    }

    public void sendToServer(INetworkPacket packet) {
        channel.sendToServer(packet);
    }

    public void sendToPlayer(ServerPlayerEntity player, INetworkPacket packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 注册网络包
     *
     * @param packetClass 包类
     * @param encoder     编码器
     * @param decoder     解码器
     * @param handler     处理器
     * @param <MSG>       包类型
     */
    private <MSG extends INetworkPacket> void registerForge(Class<MSG> packetClass,
                                                            BiConsumer<MSG, PacketBuffer> encoder,
                                                            Function<PacketBuffer, MSG> decoder,
                                                            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        channel.registerMessage(
                nextPacketId++,
                packetClass,
                encoder,
                decoder,
                handler
        );
    }

    /**
     * Loader-neutral registration overload for new public API users.
     */
    public <MSG extends INetworkPacket> void registerNeutral(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerForge(packetClass,
                (msg, buffer) -> encoder.accept(msg, BaniraPacketBuffer.forge(buffer)),
                buffer -> decoder.apply(BaniraPacketBuffer.forge(buffer)),
                (msg, ctx) -> handler.accept(msg, BaniraNetworkContext.forge(ctx)));
    }

    /**
     * 注册网络包
     *
     * @param packetClass 包类
     * @param encoder     编码器
     * @param decoder     解码器
     * @param handler     处理器
     * @param <MSG>       包类型
     */
    private <MSG extends SplitPacket & INetworkPacket> void registerSplitForge(
            Class<MSG> packetClass,
            BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        BiConsumer<MSG, Supplier<NetworkEvent.Context>> wrappedHandler = (packet, ctx) -> {
            // 保存原始上下文
            final Supplier<NetworkEvent.Context> contextSupplier = ctx;
            // 处理分包逻辑
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                // 所有分包已接收完成，合并并调用处理器
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    ctx.get().enqueueWork(() -> handler.accept(mergedPacket, contextSupplier));
                }
            }
            ctx.get().setPacketHandled(true);
        };
        registerForge(packetClass, encoder, decoder, wrappedHandler);
    }

    /**
     * Loader-neutral split-packet registration overload for new public API users.
     */
    public <MSG extends SplitPacket & INetworkPacket> void registerSplitNeutral(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerSplitForge(packetClass,
                (msg, buffer) -> encoder.accept(msg, BaniraPacketBuffer.forge(buffer)),
                buffer -> decoder.apply(BaniraPacketBuffer.forge(buffer)),
                (msg, ctx) -> handler.accept(msg, BaniraNetworkContext.forge(ctx)));
    }
}
