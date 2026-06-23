package xin.vanilla.banira.internal.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.internal.network.NativePacketBufferAccess;

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
    public ResourceLocation readResourceLocation() {
        return delegate.readResourceLocation();
    }

    @Override
    public void writeResourceLocation(ResourceLocation value) {
        delegate.writeResourceLocation(value);
    }

    @Override
    public FriendlyByteBuf nativeBuffer() {
        return delegate;
    }
}
