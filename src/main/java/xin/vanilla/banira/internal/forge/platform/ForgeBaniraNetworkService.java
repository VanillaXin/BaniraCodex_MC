package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.platform.network.BaniraNetworkService;

final class ForgeBaniraNetworkService implements BaniraNetworkService {
    @Override
    public NetworkHandler create(String channelName, IIdentifier identifier) {
        return NetworkHandler.create(channelName, identifier);
    }
}
