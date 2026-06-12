package xin.vanilla.banira.platform;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;

import javax.annotation.Nonnull;

/**
 * 当前加载器的网络能力适配入口。
 */
public interface BaniraNetworkService {
    @Nonnull
    NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull IIdentifier identifier);

    void sendToServer(@Nonnull BaniraNetworkPacket packet);

    void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull ServerPlayerEntity player);

    boolean hasDefaultChannel();

    boolean hasLocalChannel(@Nonnull ResourceLocation channel);

    boolean hasPlayerChannel(@Nonnull ServerPlayerEntity player, @Nonnull ResourceLocation channel);
}
