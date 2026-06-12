package xin.vanilla.banira.common.api;

import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

/**
 * Banira 网络包标记接口；不暴露具体加载器的 channel/context 类型。
 */
public interface INetworkPacket extends BaniraNetworkPacket {
    /**
     * 1.16.5 的旧发送链路仍需要 channel，默认指向 Banira 主通道；自定义通道包可以覆盖。
     */
    default BaniraNetworkChannel networkChannel() {
        return NetworkInit.DEFAULT_CHANNEL;
    }
}
