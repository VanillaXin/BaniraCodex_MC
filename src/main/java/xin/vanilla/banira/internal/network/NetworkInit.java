package xin.vanilla.banira.internal.network;

import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.network.packet.AdvancementToClient;
import xin.vanilla.banira.internal.network.packet.BiomeToClient;
import xin.vanilla.banira.internal.network.packet.DimensionToClient;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;

import java.util.ArrayList;

public final class NetworkInit {
    public static final NetworkHandler HANDLER = NetworkHandler.create("main_network", Identifier.id());

    public static final int REQUEST_ADVANCEMENT_DATA = 1;
    public static final int REQUEST_DIMENSION_DATA = 2;
    public static final int REQUEST_BIOME_DATA = 3;

    public static void register() {
        HANDLER.registerSplit(AdvancementToClient.class, AdvancementToClient::toBytes, AdvancementToClient::new, AdvancementToClient::handle);
        HANDLER.registerSplit(DimensionToClient.class, DimensionToClient::toBytes, DimensionToClient::new, DimensionToClient::handle);
        HANDLER.registerSplit(BiomeToClient.class, BiomeToClient::toBytes, BiomeToClient::new, BiomeToClient::handle);

        HANDLER.register(RequestToBoth.class, RequestToBoth::toBytes, RequestToBoth::new, RequestToBoth::handle);
        HANDLER.register(ModLoadedToBoth.class, ModLoadedToBoth::toBytes, ModLoadedToBoth::new, ModLoadedToBoth::handle);

        RequestToBoth.registerHandler(REQUEST_ADVANCEMENT_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(HANDLER.getChannel(), new AdvancementToClient(AdvancementUtils.advancementData()), player);
        });
        RequestToBoth.registerHandler(REQUEST_DIMENSION_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(HANDLER.getChannel(), new DimensionToClient(DimensionUtils.getClientDimensionIds()), player);
        });
        RequestToBoth.registerHandler(REQUEST_BIOME_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(HANDLER.getChannel(), new BiomeToClient(new ArrayList<>(BiomeUtils.getAllIds())), player);
        });
    }
}
