package xin.vanilla.banira.internal.network.data;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.PacketBuffer;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.internal.network.NativePacketBufferAccess;

/**
 * 1.16.5 的进度显示信息仍依赖原版 PacketBuffer 序列化，限制在内部网络数据层使用。
 */
final class AdvancementBufferAccess {
    private AdvancementBufferAccess() {
    }

    static DisplayInfo readDisplayInfo(BaniraPacketBuffer buffer) {
        return DisplayInfo.fromNetwork(nativeBuffer(buffer));
    }

    static void writeDisplayInfo(BaniraPacketBuffer buffer, DisplayInfo displayInfo) {
        displayInfo.serializeToNetwork(nativeBuffer(buffer));
    }

    private static PacketBuffer nativeBuffer(BaniraPacketBuffer buffer) {
        if (buffer instanceof NativePacketBufferAccess) {
            return (PacketBuffer) ((NativePacketBufferAccess<?>) buffer).nativeBuffer();
        }
        throw new IllegalArgumentException("Advancement display info requires PacketBuffer");
    }
}
