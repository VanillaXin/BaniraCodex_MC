package xin.vanilla.banira.common.network;

import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.util.function.Supplier;

public interface NetworkPacket extends INetworkPacket {
    default Supplier<SimpleChannel> channel() {
        return NetworkInit.HANDLER::getChannel;
    }
}
