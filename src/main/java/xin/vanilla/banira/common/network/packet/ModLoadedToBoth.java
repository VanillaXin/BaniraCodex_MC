package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;

@Getter
@Accessors(fluent = true)
public class ModLoadedToBoth implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ModLoadedToBoth> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("mod_loaded"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ModLoadedToBoth> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(ModLoadedToBoth::toBytes, ModLoadedToBoth::new);


    private final String modid;

    public ModLoadedToBoth(String modid) {
        this.modid = modid;
    }

    public ModLoadedToBoth(FriendlyByteBuf buf) {
        this.modid = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.modid != null ? this.modid : "", 256);
    }

    public static void handle(ModLoadedToBoth packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.SERVERBOUND && ctx.player() instanceof ServerPlayer player
                    && packet.modid() != null && !packet.modid().isEmpty()) {
                PlayerUtils.setPlayerModInstalled(player, packet.modid(), false);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
