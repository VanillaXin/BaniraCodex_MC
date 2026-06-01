package xin.vanilla.banira.common.api;

import xin.vanilla.banira.common.network.NetworkHandler;

public interface INetworkPacket {
    /**
     * Returns the loader-neutral channel handler used by this packet.
     */
    NetworkHandler networkHandler();
}
