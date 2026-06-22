package xin.vanilla.banira.internal.network.data;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.PacketBuffer;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;

/**
 * 1.16.5 的进度显示信息仍依赖原版 PacketBuffer 序列化，限制在内部网络数据层使用。
 */
final class AdvancementBufferAccess {
    private AdvancementBufferAccess() {
    }

    static DisplayInfo readDisplayInfo(BaniraPacketBuffer buffer) {
        Object nativeBuffer = buffer.nativeBuffer();
        if (!(nativeBuffer instanceof PacketBuffer)) {
            throw new IllegalArgumentException("Advancement display info requires PacketBuffer");
        }
        return DisplayInfo.fromNetwork((PacketBuffer) nativeBuffer);
    }

    static void writeDisplayInfo(BaniraPacketBuffer buffer, DisplayInfo displayInfo) {
        Object nativeBuffer = buffer.nativeBuffer();
        if (!(nativeBuffer instanceof PacketBuffer)) {
            throw new IllegalArgumentException("Advancement display info requires PacketBuffer");
        }
        displayInfo.serializeToNetwork((PacketBuffer) nativeBuffer);
    }
}
