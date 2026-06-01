package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.internal.forge.network.ForgeNetworkChannel;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;
import xin.vanilla.banira.platform.network.BaniraNetworkService;

final class ForgeBaniraNetworkService implements BaniraNetworkService {
    private NetworkRegistryAccessor networkRegistry;

    @Override
    public BaniraNetworkChannel create(String channelName, IIdentifier identifier) {
        return ForgeNetworkChannel.create(channelName, identifier);
    }

    @Override
    public boolean hasChannel(ResourceLocation channelName) {
        return registry().banira$instances().containsKey(channelName);
    }

    @Override
    public boolean hasChannel(ServerPlayerEntity player, ResourceLocation channelName) {
        return hasChannel(channelName);
    }

    private NetworkRegistryAccessor registry() {
        if (networkRegistry == null) {
            networkRegistry = (NetworkRegistryAccessor) new NetworkRegistry();
        }
        return networkRegistry;
    }
}
