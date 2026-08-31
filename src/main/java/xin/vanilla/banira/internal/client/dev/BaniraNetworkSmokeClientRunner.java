package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

/** 自动加入独立专服，确保通用服务端能力经过真实远端连接。 */
public final class BaniraNetworkSmokeClientRunner {
    private static int ticks;
    private static boolean connected;
    private static boolean finished;

    private BaniraNetworkSmokeClientRunner() {
    }

    public static void tick(Minecraft client) {
        if (!BaniraNetworkSmokeStatus.enabled() || finished) return;
        if (!connected && ++ticks >= 20) {
            String host = System.getProperty("banira.networkSmoke.host", "127.0.0.1");
            int port = Integer.getInteger("banira.networkSmoke.port", 25579);
            ServerData server = new ServerData("Banira Network Smoke", host + ':' + port, false);
            client.setScreen(new ConnectScreen(client.screen, client, server));
            connected = true;
            ticks = 0;
            return;
        }
        if (client.player == null || client.getSingleplayerServer() != null) {
            if (ticks > 1400) fail(client, "remote login timed out");
            else ticks++;
            return;
        }
        if (ticks == 0) BaniraNetworkSmokeStatus.append("PASS remote-login");
        if (++ticks >= 500) {
            finished = true;
            BaniraNetworkSmokeStatus.append("FINISHED " + BaniraNetworkSmokeStatus.phase());
            client.stop();
        }
    }

    private static void fail(Minecraft client, String reason) {
        finished = true;
        BaniraNetworkSmokeStatus.append("FAIL client " + reason);
        client.stop();
    }
}
