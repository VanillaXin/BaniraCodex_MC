package xin.vanilla.banira.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;

import javax.annotation.Nonnull;

/**
 * 当前加载器的网络能力适配入口。
 */
public interface BaniraNetworkService {
    @Nonnull
    NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull IIdentifier identifier);

    void sendToServer(@Nonnull INetworkPacket packet);

    void sendToPlayer(@Nonnull INetworkPacket packet, @Nonnull ServerPlayer player);

    boolean hasDefaultChannel();

    boolean hasLocalChannel(@Nonnull ResourceLocation channel);

    boolean hasPlayerChannel(@Nonnull ServerPlayer player, @Nonnull ResourceLocation channel);
}
