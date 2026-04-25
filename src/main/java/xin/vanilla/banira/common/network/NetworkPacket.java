package xin.vanilla.banira.common.network;

import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.util.function.Supplier;

public interface NetworkPacket {
    /**
     * 获取当前网络包使用的通道供应器
     */
    default Supplier<SimpleChannel> channel() {
        return NetworkInit.HANDLER::getChannel;
    }
}
