package xin.vanilla.banira.common.network;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.internal.network.NetworkInit;

public interface NetworkPacket extends INetworkPacket {
    default ResourceLocation channel() {
        return NetworkInit.HANDLER.channel();
    }
}
