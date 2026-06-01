package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.internal.forge.network.ForgeNetworkChannel;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;
import xin.vanilla.banira.platform.network.BaniraNetworkService;

final class ForgeBaniraNetworkService implements BaniraNetworkService {
    @Override
    public BaniraNetworkChannel create(String channelName, IIdentifier identifier) {
        return ForgeNetworkChannel.create(channelName, identifier);
    }
}
