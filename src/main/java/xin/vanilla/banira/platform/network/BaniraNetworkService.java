package xin.vanilla.banira.platform.network;

import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.util.IIdentifier;

/**
 * Loader-neutral network channel creation surface.
 */
public interface BaniraNetworkService {
    NetworkHandler create(String channelName, IIdentifier identifier);
}
