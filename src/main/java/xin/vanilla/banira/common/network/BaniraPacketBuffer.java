package xin.vanilla.banira.common.network;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * Small loader-neutral facade around the active branch packet buffer.
 */
public final class BaniraPacketBuffer {
    private final PacketBuffer forgeBuffer;

    private BaniraPacketBuffer(PacketBuffer forgeBuffer) {
        this.forgeBuffer = forgeBuffer;
    }

    static BaniraPacketBuffer forge(PacketBuffer forgeBuffer) {
        return new BaniraPacketBuffer(forgeBuffer);
    }

    public int readVarInt() {
        return forgeBuffer.readVarInt();
    }

    public BaniraPacketBuffer writeVarInt(int value) {
        forgeBuffer.writeVarInt(value);
        return this;
    }

    public String readUtf() {
        return forgeBuffer.readUtf();
    }

    public String readUtf(int maxLength) {
        return forgeBuffer.readUtf(maxLength);
    }

    public BaniraPacketBuffer writeUtf(String value) {
        forgeBuffer.writeUtf(value);
        return this;
    }

    public BaniraPacketBuffer writeUtf(String value, int maxLength) {
        forgeBuffer.writeUtf(value, maxLength);
        return this;
    }

    public int readInt() {
        return forgeBuffer.readInt();
    }

    public BaniraPacketBuffer writeInt(int value) {
        forgeBuffer.writeInt(value);
        return this;
    }

    public long readLong() {
        return forgeBuffer.readLong();
    }

    public BaniraPacketBuffer writeLong(long value) {
        forgeBuffer.writeLong(value);
        return this;
    }

    public boolean readBoolean() {
        return forgeBuffer.readBoolean();
    }

    public BaniraPacketBuffer writeBoolean(boolean value) {
        forgeBuffer.writeBoolean(value);
        return this;
    }

    public ResourceLocation readResourceLocation() {
        return forgeBuffer.readResourceLocation();
    }

    public BaniraPacketBuffer writeResourceLocation(ResourceLocation value) {
        forgeBuffer.writeResourceLocation(value);
        return this;
    }

    public DisplayInfo readDisplayInfo() {
        return DisplayInfo.fromNetwork(forgeBuffer);
    }

    public BaniraPacketBuffer writeDisplayInfo(DisplayInfo value) {
        value.serializeToNetwork(forgeBuffer);
        return this;
    }
}
