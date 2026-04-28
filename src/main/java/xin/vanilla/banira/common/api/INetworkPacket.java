package xin.vanilla.banira.common.api;


import net.minecraft.resources.ResourceLocation;

public interface INetworkPacket {
    /**
     * 获取当前网络包使用的通道。
     */
    ResourceLocation channel();
}
