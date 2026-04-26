package xin.vanilla.banira.common.api;

import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

public interface INetworkPacket {
    /**
     * 获取当前网络包使用的通道供应器
     */
    Supplier<SimpleChannel> channel();
}
