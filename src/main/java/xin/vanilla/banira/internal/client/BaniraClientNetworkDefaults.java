package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import java.util.List;

public final class BaniraClientNetworkDefaults {

    private BaniraClientNetworkDefaults() {
    }

    public static void register() {
        ModLoadedToBoth.registerClientHandler(packet -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            for (String modid : packet.modids()) {
                PlayerUtils.setRemoteServerModInstalled(minecraft.player, modid, false);
            }
        });
        BaniraClientEventHub.Player.onClientLoggedIn(player -> {
            List<String> ids = ModLoadedPresence.announcedModIds();
            if (!ids.isEmpty()) {
                PacketUtils.sendPacketToServer(new ModLoadedToBoth(ids));
            }
        });
        BaniraClientEventHub.Player.onClientLoggedOut(player -> {
            if (player == null) {
                return;
            }
            AdvancementUtils.clearAdvancementData();
            PlayerUtils.removeRemoteServerDataStatus(player);
        });
    }
}
