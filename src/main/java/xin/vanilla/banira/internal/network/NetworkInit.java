package xin.vanilla.banira.internal.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.network.packet.*;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.neoforge.platform.NeoForgeBaniraPlatform;
import xin.vanilla.banira.internal.network.packet.AdvancementToClient;
import xin.vanilla.banira.internal.network.packet.BiomeToClient;
import xin.vanilla.banira.internal.network.packet.DimensionToClient;

import java.util.ArrayList;

public final class NetworkInit {
    public static final NetworkHandler HANDLER = NetworkHandler.create("main_network", BaniraIdentifier.of(Banira.MOD_ID, "main_network"));

    public static final int REQUEST_ADVANCEMENT_DATA = 1;
    public static final int REQUEST_DIMENSION_DATA = 2;
    public static final int REQUEST_BIOME_DATA = 3;

    private NetworkInit() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, NetworkInit::registerPayloadHandlers);

        HANDLER.registerSplit(AdvancementToClient.class, AdvancementToClient::toBytes, AdvancementToClient::new, AdvancementToClient::handle);
        HANDLER.registerSplit(DimensionToClient.class, DimensionToClient::toBytes, DimensionToClient::new, DimensionToClient::handle);
        HANDLER.registerSplit(BiomeToClient.class, BiomeToClient::toBytes, BiomeToClient::new, BiomeToClient::handle);

        HANDLER.register(RequestToBoth.class, RequestToBoth::toBytes, RequestToBoth::new, RequestToBoth::handle);
        HANDLER.register(ModLoadedToBoth.class, ModLoadedToBoth::toBytes, ModLoadedToBoth::new, ModLoadedToBoth::handle);
        HANDLER.register(NotificationToClient.class, NotificationToClient::toBytes, NotificationToClient::new, NotificationToClient::handle);
        HANDLER.register(NotificationTypesSyncToClient.class, NotificationTypesSyncToClient::toBytes, NotificationTypesSyncToClient::new, NotificationTypesSyncToClient::handle);
        HANDLER.register(ConfigSyncToServer.class, ConfigSyncToServer::toBytes, ConfigSyncToServer::new, ConfigSyncToServer::handle);
        HANDLER.register(ConfigFetchRequestToServer.class, ConfigFetchRequestToServer::toBytes, ConfigFetchRequestToServer::new, ConfigFetchRequestToServer::handle);
        HANDLER.register(ConfigSnapshotToClient.class, ConfigSnapshotToClient::toBytes, ConfigSnapshotToClient::new, ConfigSnapshotToClient::handle);
        HANDLER.register(CustomPlayerConfigSyncToServer.class, CustomPlayerConfigSyncToServer::toBytes, CustomPlayerConfigSyncToServer::new, CustomPlayerConfigSyncToServer::handle);

        RequestToBoth.registerHandler(REQUEST_ADVANCEMENT_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(new AdvancementToClient(AdvancementUtils.advancementData()), player));
        RequestToBoth.registerHandler(REQUEST_DIMENSION_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(new DimensionToClient(DimensionUtils.getClientDimensionIds()), player));
        RequestToBoth.registerHandler(REQUEST_BIOME_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(new BiomeToClient(new ArrayList<>(BiomeUtils.getAllIds())), player));

        HANDLER.completeRegistration();
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        NeoForgeBaniraPlatform.registerPendingPayloads(event);
    }
}
