package xin.vanilla.banira.common.network;

import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

public interface NetworkPacket extends INetworkPacket {
    @Override
    default BaniraNetworkChannel networkChannel() {
        return NetworkInit.HANDLER;
    }
}
