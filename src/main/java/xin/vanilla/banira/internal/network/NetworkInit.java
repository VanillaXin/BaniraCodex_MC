package xin.vanilla.banira.internal.network;

import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.*;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.network.packet.AdvancementToClient;
import xin.vanilla.banira.internal.network.packet.BiomeToClient;
import xin.vanilla.banira.internal.network.packet.DimensionToClient;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class NetworkInit {
    public static final BaniraNetworkChannel HANDLER = BaniraPlatforms.get().network().create("main_network", Identifier.id());

    public static final int REQUEST_ADVANCEMENT_DATA = 1;
    public static final int REQUEST_DIMENSION_DATA = 2;
    public static final int REQUEST_BIOME_DATA = 3;

    public static void register() {
        registerSplitPacket(AdvancementToClient.class, AdvancementToClient::toBytes, AdvancementToClient::new, AdvancementToClient::handle);
        registerSplitPacket(DimensionToClient.class, DimensionToClient::toBytes, DimensionToClient::new, DimensionToClient::handle);
        registerSplitPacket(BiomeToClient.class, BiomeToClient::toBytes, BiomeToClient::new, BiomeToClient::handle);

        registerPacket(RequestToBoth.class, RequestToBoth::toBytes, RequestToBoth::new, RequestToBoth::handle);
        registerPacket(ModLoadedToBoth.class, ModLoadedToBoth::toBytes, ModLoadedToBoth::new, ModLoadedToBoth::handle);
        registerPacket(NotificationToClient.class, NotificationToClient::toBytes, NotificationToClient::new, NotificationToClient::handle);
        registerPacket(NotificationTypesSyncToClient.class, NotificationTypesSyncToClient::toBytes, NotificationTypesSyncToClient::new, NotificationTypesSyncToClient::handle);
        registerPacket(ConfigSyncToServer.class, ConfigSyncToServer::toBytes, ConfigSyncToServer::new, ConfigSyncToServer::handle);
        registerPacket(ConfigFetchRequestToServer.class, ConfigFetchRequestToServer::toBytes, ConfigFetchRequestToServer::new, ConfigFetchRequestToServer::handle);
        registerPacket(ConfigSnapshotToClient.class, ConfigSnapshotToClient::toBytes, ConfigSnapshotToClient::new, ConfigSnapshotToClient::handle);
        registerPacket(CustomPlayerConfigSyncToServer.class, CustomPlayerConfigSyncToServer::toBytes, CustomPlayerConfigSyncToServer::new, CustomPlayerConfigSyncToServer::handle);

        RequestToBoth.registerHandler(REQUEST_ADVANCEMENT_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(new AdvancementToClient(AdvancementUtils.advancementData()), player);
        });
        RequestToBoth.registerHandler(REQUEST_DIMENSION_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(new DimensionToClient(DimensionUtils.getClientDimensionIds()), player);
        });
        RequestToBoth.registerHandler(REQUEST_BIOME_DATA, (packet, player) -> {
            PacketUtils.sendSplitPacketToPlayer(new BiomeToClient(new ArrayList<>(BiomeUtils.getAllIds())), player);
        });
    }

    private static <MSG extends NetworkPacket> void registerPacket(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        HANDLER.register(packetClass, encoder, decoder, handler);
    }

    private static <MSG extends SplitPacket & NetworkPacket> void registerSplitPacket(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        HANDLER.registerSplit(packetClass, encoder, decoder, handler);
    }
}
