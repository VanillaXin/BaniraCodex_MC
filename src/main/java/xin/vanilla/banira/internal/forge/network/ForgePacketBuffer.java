package xin.vanilla.banira.internal.forge.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.internal.network.NativePacketBufferAccess;

import java.util.Objects;
import java.util.UUID;

/**
 * Forge 1.16.5 的 PacketBuffer 适配实现。
 */
final class ForgePacketBuffer implements BaniraPacketBuffer, NativePacketBufferAccess<PacketBuffer> {
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
    public byte readByte() {
        return buffer.readByte();
    }

    @Override
    public void writeByte(int value) {
        buffer.writeByte(value);
    }

    @Override
    public double readDouble() {
        return buffer.readDouble();
    }

    @Override
    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    @Override
    public UUID readUuid() {
        return buffer.readUUID();
    }

    @Override
    public void writeUuid(UUID value) {
        buffer.writeUUID(Objects.requireNonNull(value, "value"));
    }

    @Override
    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return buffer.readEnum(Objects.requireNonNull(enumClass, "enumClass"));
    }

    @Override
    public void writeEnum(Enum<?> value) {
        buffer.writeEnum(Objects.requireNonNull(value, "value"));
    }

    @Override
    public BaniraIdentifier readIdentifier() {
        return BaniraIdentifier.parse(buffer.readResourceLocation().toString());
    }

    @Override
    public void writeIdentifier(BaniraIdentifier value) {
        BaniraIdentifier identifier = Objects.requireNonNull(value, "value");
        buffer.writeResourceLocation(new ResourceLocation(identifier.getNamespace(), identifier.getPath()));
    }

    @Override
    public PacketBuffer nativeBuffer() {
        return buffer;
    }
}
