package xin.vanilla.banira.platform.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.SplitPacket;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Loader-neutral network channel contract exposed to Banira Codex users.
 */
public interface BaniraNetworkChannel {
    ResourceLocation channelName();

    String modId();

    void sendToServer(INetworkPacket packet);

    void sendToPlayer(ServerPlayerEntity player, INetworkPacket packet);

    <MSG extends INetworkPacket> void register(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler);

    <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler);
}
