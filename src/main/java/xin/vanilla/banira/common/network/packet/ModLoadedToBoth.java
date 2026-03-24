package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import xin.vanilla.banira.common.util.PlayerUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class ModLoadedToBoth {

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

    public static void handle(ModLoadedToBoth packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && packet.modid() != null && !packet.modid().isEmpty()) {
                    PlayerUtils.setPlayerModInstalled(player, packet.modid(), false);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
