package xin.vanilla.banira.common.api;

import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

/**
 * Banira 网络包标记接口；不暴露具体加载器的 channel/context 类型。
 */
public interface INetworkPacket extends BaniraNetworkPacket {
    /**
     * 1.16.5 发送链路仍需要通道名；真实 channel 对象留在加载器内部。
     */
    default ResourceLocation networkChannelName() {
        return NetworkInit.DEFAULT_CHANNEL_NAME;
    }
}
