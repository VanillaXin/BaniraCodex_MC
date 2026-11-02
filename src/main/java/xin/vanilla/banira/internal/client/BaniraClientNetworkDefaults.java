package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.network.BaniraClientPacketDispatch;

import java.util.List;

public final class BaniraClientNetworkDefaults {

    private BaniraClientNetworkDefaults() {
    }

    public static void register() {
        BaniraClientPacketDispatch.registerConfigConnectionGuard(ClientPlayerPreferences::isCurrentConnection);
        BaniraClientPacketDispatch.registerConfigSnapshotHandler(ConfigSnapshotClientHandler::apply);
        BaniraClientPacketDispatch.registerPlayerPreferencesHandler(ClientPlayerPreferences::apply);
        BaniraClientEventHub.Client.onClientTick(event -> {
            if (event == xin.vanilla.banira.api.client.event.BaniraClientTickEvent.END)
                ClientPlayerPreferences.tick();
        });
        BaniraClientPacketDispatch.registerNotificationHandler(ClientNotificationPacketHandler::handle);
        BaniraClientPacketDispatch.registerNotificationTypesHandler(ClientNotificationPacketHandler::handleTypes);
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
            ClientPlayerPreferences.disconnect();
            if (player == null) {
                return;
            }
            AdvancementUtils.clearAdvancementData();
            PlayerUtils.removeRemoteServerDataStatus(player);
        });
    }
}
