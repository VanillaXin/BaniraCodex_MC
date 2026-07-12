package xin.vanilla.banira.common.network;

import xin.vanilla.banira.api.BaniraIdentifier;

import java.util.UUID;

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

    byte readByte();

    void writeByte(int value);

    double readDouble();

    void writeDouble(double value);

    UUID readUuid();

    void writeUuid(UUID value);

    <T extends Enum<T>> T readEnum(Class<T> enumClass);

    void writeEnum(Enum<?> value);

    BaniraIdentifier readIdentifier();

    void writeIdentifier(BaniraIdentifier value);
}
