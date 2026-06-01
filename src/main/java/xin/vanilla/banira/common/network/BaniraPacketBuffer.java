package xin.vanilla.banira.common.network;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.util.ResourceLocation;

/**
 * Small loader-neutral facade around the active branch packet buffer.
 */
public final class BaniraPacketBuffer {
    private final BaniraPacketBufferDelegate delegate;

    public BaniraPacketBuffer(BaniraPacketBufferDelegate delegate) {
        this.delegate = delegate;
    }

    public int readVarInt() {
        return delegate.readVarInt();
    }

    public BaniraPacketBuffer writeVarInt(int value) {
        delegate.writeVarInt(value);
        return this;
    }

    public String readUtf() {
        return delegate.readUtf();
    }

    public String readUtf(int maxLength) {
        return delegate.readUtf(maxLength);
    }

    public BaniraPacketBuffer writeUtf(String value) {
        delegate.writeUtf(value);
        return this;
    }

    public BaniraPacketBuffer writeUtf(String value, int maxLength) {
        delegate.writeUtf(value, maxLength);
        return this;
    }

    public int readInt() {
        return delegate.readInt();
    }

    public BaniraPacketBuffer writeInt(int value) {
        delegate.writeInt(value);
        return this;
    }

    public long readLong() {
        return delegate.readLong();
    }

    public BaniraPacketBuffer writeLong(long value) {
        delegate.writeLong(value);
        return this;
    }

    public boolean readBoolean() {
        return delegate.readBoolean();
    }

    public BaniraPacketBuffer writeBoolean(boolean value) {
        delegate.writeBoolean(value);
        return this;
    }

    public ResourceLocation readResourceLocation() {
        return delegate.readResourceLocation();
    }

    public BaniraPacketBuffer writeResourceLocation(ResourceLocation value) {
        delegate.writeResourceLocation(value);
        return this;
    }

    public DisplayInfo readDisplayInfo() {
        return delegate.readDisplayInfo();
    }

    public BaniraPacketBuffer writeDisplayInfo(DisplayInfo value) {
        delegate.writeDisplayInfo(value);
        return this;
    }
}
