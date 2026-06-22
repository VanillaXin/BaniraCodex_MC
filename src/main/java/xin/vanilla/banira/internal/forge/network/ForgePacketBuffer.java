package xin.vanilla.banira.internal.forge.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;

/**
 * Forge 1.16.5 的 PacketBuffer 适配实现。
 */
final class ForgePacketBuffer implements BaniraPacketBuffer {
    private final PacketBuffer buffer;

    ForgePacketBuffer(PacketBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int readVarInt() {
        return buffer.readVarInt();
    }

    @Override
    public void writeVarInt(int value) {
        buffer.writeVarInt(value);
    }

    @Override
    public String readUtf() {
        return buffer.readUtf();
    }

    @Override
    public String readUtf(int maxLength) {
        return buffer.readUtf(maxLength);
    }

    @Override
    public void writeUtf(String value) {
        buffer.writeUtf(value);
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        buffer.writeUtf(value, maxLength);
    }

    @Override
    public int readInt() {
        return buffer.readInt();
    }

    @Override
    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    @Override
    public long readLong() {
        return buffer.readLong();
    }

    @Override
    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    @Override
    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    @Override
    public void writeBoolean(boolean value) {
        buffer.writeBoolean(value);
    }

    @Override
    public ResourceLocation readResourceLocation() {
        return buffer.readResourceLocation();
    }

    @Override
    public void writeResourceLocation(ResourceLocation value) {
        buffer.writeResourceLocation(value);
    }

    @Override
    public Object nativeBuffer() {
        return buffer;
    }
}
