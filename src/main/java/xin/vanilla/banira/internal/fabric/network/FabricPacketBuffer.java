package xin.vanilla.banira.internal.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.internal.network.NativePacketBufferAccess;

import java.util.Objects;

/**
 * Fabric 1.19.2 的 FriendlyByteBuf 适配；公共协议只依赖 BaniraPacketBuffer。
 */
public final class FabricPacketBuffer implements BaniraPacketBuffer, NativePacketBufferAccess<FriendlyByteBuf> {
    private final FriendlyByteBuf delegate;

    public FabricPacketBuffer(FriendlyByteBuf delegate) {
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
    public BaniraIdentifier readIdentifier() {
        return BaniraIdentifier.parse(delegate.readResourceLocation().toString());
    }

    @Override
    public void writeIdentifier(BaniraIdentifier value) {
        BaniraIdentifier identifier = Objects.requireNonNull(value, "value");
        delegate.writeResourceLocation(new ResourceLocation(identifier.getNamespace(), identifier.getPath()));
    }

    @Override
    public FriendlyByteBuf nativeBuffer() {
        return delegate;
    }
}
