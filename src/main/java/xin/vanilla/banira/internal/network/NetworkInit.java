package xin.vanilla.banira.internal.network;

import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.network.packet.AdvancementToClient;

public final class NetworkInit {
    public static final NetworkHandler HANDLER = NetworkHandler.create("main_network", BaniraCodex.resourceFactory());

    /**
     * RequestToBoth 请求进度数据
     */
    public static final int REQUEST_ADVANCEMENT_DATA = 1;

    public static void register() {
        HANDLER.registerSplit(
                AdvancementToClient.class,
                AdvancementToClient::toBytes,
                AdvancementToClient::new,
                AdvancementToClient::handle
        );

        HANDLER.register(
                RequestToBoth.class,
                RequestToBoth::toBytes,
                RequestToBoth::new,
                RequestToBoth::handle
        );

        // 注册服务端进度数据请求处理器
        RequestToBoth.registerHandler(REQUEST_ADVANCEMENT_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(HANDLER.getChannel(), new AdvancementToClient(AdvancementUtils.advancementData()), player);
        });

    }
}
