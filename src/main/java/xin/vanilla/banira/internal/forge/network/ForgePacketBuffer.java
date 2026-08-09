package xin.vanilla.banira.internal.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.internal.network.NativePacketBufferAccess;

import java.util.Objects;
import java.util.UUID;

/**
 * Forge FriendlyByteBuf 适配实现。
 */
public final class ForgePacketBuffer implements BaniraPacketBuffer, NativePacketBufferAccess<FriendlyByteBuf> {
    private final FriendlyByteBuf delegate;

    public ForgePacketBuffer(FriendlyByteBuf delegate) {
        this.delegate = delegate;
    }

    @Override
    public String readUtf() {
        return delegate.readUtf();
    }

    @Override
    public String readUtf(int maxLength) {
        return delegate.readUtf(maxLength);
    }

    @Override
    public void writeUtf(String value) {
        delegate.writeUtf(value);
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        delegate.writeUtf(value, maxLength);
    }

    @Override
    public int readInt() {
        return delegate.readInt();
    }

    @Override
    public void writeInt(int value) {
        delegate.writeInt(value);
    }

    @Override
    public int readVarInt() {
        return delegate.readVarInt();
    }

    @Override
    public void writeVarInt(int value) {
        delegate.writeVarInt(value);
    }

    @Override
    public long readLong() {
        return delegate.readLong();
    }

    @Override
    public void writeLong(long value) {
        delegate.writeLong(value);
    }

    @Override
    public boolean readBoolean() {
        return delegate.readBoolean();
    }

    @Override
    public void writeBoolean(boolean value) {
        delegate.writeBoolean(value);
    }

    @Override
    public byte readByte() {
        return delegate.readByte();
    }

    @Override
    public void writeByte(int value) {
        delegate.writeByte(value);
    }

    @Override
    public double readDouble() {
        return delegate.readDouble();
    }

    @Override
    public void writeDouble(double value) {
        delegate.writeDouble(value);
    }

    @Override
    public UUID readUuid() {
        return delegate.readUUID();
    }

    @Override
    public void writeUuid(UUID value) {
        delegate.writeUUID(Objects.requireNonNull(value, "value"));
    }

    @Override
    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return delegate.readEnum(Objects.requireNonNull(enumClass, "enumClass"));
    }

    @Override
    public void writeEnum(Enum<?> value) {
        delegate.writeEnum(Objects.requireNonNull(value, "value"));
    }

    @Override
    public BaniraIdentifier readIdentifier() {
        return BaniraIdentifier.parse(delegate.readResourceLocation().toString());
    }

    @Override
    public void writeIdentifier(BaniraIdentifier value) {
        BaniraIdentifier identifier = Objects.requireNonNull(value, "value");
        delegate.writeResourceLocation(ResourceLocation.fromNamespaceAndPath(identifier.getNamespace(), identifier.getPath()));
    }

    @Override
    public FriendlyByteBuf nativeBuffer() {
        return delegate;
    }
}
