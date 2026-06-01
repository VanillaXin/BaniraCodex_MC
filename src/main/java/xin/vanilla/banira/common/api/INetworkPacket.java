package xin.vanilla.banira.common.api;

import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

public interface INetworkPacket {
    /**
     * Returns the loader-neutral channel handler used by this packet.
     */
    BaniraNetworkChannel networkChannel();
}
