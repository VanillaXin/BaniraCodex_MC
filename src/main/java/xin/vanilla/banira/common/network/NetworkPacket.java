package xin.vanilla.banira.common.network;

import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.internal.network.NetworkInit;

public interface NetworkPacket extends INetworkPacket {
    @Override
    default NetworkHandler networkHandler() {
        return NetworkInit.HANDLER;
    }
}
