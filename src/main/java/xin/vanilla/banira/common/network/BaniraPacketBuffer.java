package xin.vanilla.banira.common.network;

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

    public static BaniraPacketBuffer forge(PacketBuffer forgeBuffer) {
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

    public BaniraPacketBuffer writeUtf(String value) {
        forgeBuffer.writeUtf(value);
        return this;
    }

    public ResourceLocation readResourceLocation() {
        return forgeBuffer.readResourceLocation();
    }

    public BaniraPacketBuffer writeResourceLocation(ResourceLocation value) {
        forgeBuffer.writeResourceLocation(value);
        return this;
    }

    /**
     * @deprecated Loader-specific access; avoid in dependent mods.
     */
    @Deprecated
    public PacketBuffer forgeBuffer() {
        return forgeBuffer;
    }
}
