package xin.vanilla.banira.common.network;

import net.minecraft.util.ResourceLocation;

/**
 * Banira 网络缓冲区抽象；只暴露公共协议当前需要的稳定读写操作。
 */
public interface BaniraPacketBuffer {
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
}
