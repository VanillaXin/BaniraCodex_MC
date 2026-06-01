package xin.vanilla.banira.common.network;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.util.ResourceLocation;

/**
 * Loader adapter for {@link BaniraPacketBuffer}.
 */
public interface BaniraPacketBufferDelegate {
    int readVarInt();

    void writeVarInt(int value);

    String readUtf();

    String readUtf(int maxLength);

    void writeUtf(String value);

    void writeUtf(String value, int maxLength);

    int readInt();

    void writeInt(int value);

    long readLong();

    void writeLong(long value);

    boolean readBoolean();

    void writeBoolean(boolean value);

    ResourceLocation readResourceLocation();

    void writeResourceLocation(ResourceLocation value);

    DisplayInfo readDisplayInfo();

    void writeDisplayInfo(DisplayInfo value);
}
