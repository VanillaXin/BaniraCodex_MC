package xin.vanilla.banira.internal.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.network.packet.*;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.network.packet.AdvancementToClient;
import xin.vanilla.banira.internal.network.packet.BiomeToClient;
import xin.vanilla.banira.internal.network.packet.DimensionToClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = BaniraCodex.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkInit {

    public static final int REQUEST_ADVANCEMENT_DATA = 1;
    public static final int REQUEST_DIMENSION_DATA = 2;
    public static final int REQUEST_BIOME_DATA = 3;

    private NetworkInit() {
    }

    /**
     * 在模组主类中调用：注册 {@link RequestToBoth} 等请求的分发逻辑。
     */
    public static void register() {
        RequestToBoth.registerHandler(REQUEST_ADVANCEMENT_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(player, new AdvancementToClient(AdvancementUtils.advancementData())));
        RequestToBoth.registerHandler(REQUEST_DIMENSION_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(player, new DimensionToClient(DimensionUtils.getClientDimensionIds())));
        RequestToBoth.registerHandler(REQUEST_BIOME_DATA, (packet, player) ->
                PacketUtils.sendSplitPacketToPlayer(player, new BiomeToClient(new ArrayList<>(BiomeUtils.getAllIds()))));
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").optional();

        reg.playBidirectional(RequestToBoth.TYPE, RequestToBoth.STREAM_CODEC, RequestToBoth::handle);
        reg.playBidirectional(ModLoadedToBoth.TYPE, ModLoadedToBoth.STREAM_CODEC, ModLoadedToBoth::handle);

        reg.playToServer(ConfigSyncToServer.TYPE, ConfigSyncToServer.STREAM_CODEC, ConfigSyncToServer::handle);
        reg.playToServer(ConfigFetchRequestToServer.TYPE, ConfigFetchRequestToServer.STREAM_CODEC, ConfigFetchRequestToServer::handle);
        reg.playToServer(CustomPlayerConfigSyncToServer.TYPE, CustomPlayerConfigSyncToServer.STREAM_CODEC, CustomPlayerConfigSyncToServer::handle);

        reg.playToClient(NotificationToClient.TYPE, NotificationToClient.STREAM_CODEC, NotificationToClient::handle);
        reg.playToClient(NotificationTypesSyncToClient.TYPE, NotificationTypesSyncToClient.STREAM_CODEC, NotificationTypesSyncToClient::handle);
        reg.playToClient(ConfigSnapshotToClient.TYPE, ConfigSnapshotToClient.STREAM_CODEC, ConfigSnapshotToClient::handle);

        reg.playToClient(AdvancementToClient.TYPE, AdvancementToClient.STREAM_CODEC, NetworkInit::onAdvancementSplitFragment);
        reg.playToClient(DimensionToClient.TYPE, DimensionToClient.STREAM_CODEC, NetworkInit::onDimensionSplitFragment);
        reg.playToClient(BiomeToClient.TYPE, BiomeToClient.STREAM_CODEC, NetworkInit::onBiomeSplitFragment);
    }

    private static void onAdvancementSplitFragment(AdvancementToClient payload, IPayloadContext ctx) {
        dispatchSplitClientPayload(payload, ctx, AdvancementToClient::handle);
    }

    private static void onDimensionSplitFragment(DimensionToClient payload, IPayloadContext ctx) {
        dispatchSplitClientPayload(payload, ctx, DimensionToClient::handle);
    }

    private static void onBiomeSplitFragment(BiomeToClient payload, IPayloadContext ctx) {
        dispatchSplitClientPayload(payload, ctx, BiomeToClient::handle);
    }

    private static <T extends SplitPacket & CustomPacketPayload> void dispatchSplitClientPayload(
            T payload,
            IPayloadContext ctx,
            BiConsumer<T, IPayloadContext> onMerged) {
        List<T> complete = SplitPacket.handle(payload);
        if (complete != null && !complete.isEmpty()) {
            T merged = SplitPacket.merge(complete);
            if (merged != null) {
                ctx.enqueueWork(() -> onMerged.accept(merged, ctx));
            }
        }
    }
}
