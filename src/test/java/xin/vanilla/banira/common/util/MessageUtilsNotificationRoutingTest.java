package xin.vanilla.banira.common.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationVanillaFallback;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
import xin.vanilla.banira.common.notification.NotificationBatch;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.NoopNetworkService;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.adapt;
import static xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.repeat;

public class MessageUtilsNotificationRoutingTest {
    @BeforeClass
    public static void bootstrapGame() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
    }

    private RecordingPlayer player;
    private Sink sink;
    private JsonObject originalConfig;
    private boolean originalDirty;

    @Before
    public void setUp() throws Exception {
        originalConfig = new JsonParser().parse(CustomConfig.getCustomConfig().toString()).getAsJsonObject();
        originalDirty = CustomConfig.isDirty();
        sink = new Sink();
        BaniraNetworkService network = (BaniraNetworkService) Proxy.newProxyInstance(
                BaniraNetworkService.class.getClassLoader(), new Class<?>[]{BaniraNetworkService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendToPlayer")) {
                        assertSame(player, args[1]);
                        sink.submit((NotificationToClient) args[0]);
                        return null;
                    }
                    return method.invoke(NoopNetworkService.INSTANCE, args);
                });
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(network));
        // Only the player identity and final transport boundary are substituted.
        // MessageUtils, platform selection, vanilla packets and codecs remain real.
        player = allocate(RecordingPlayer.class);
        player.id = UUID.randomUUID();
        Field chatVisibility = ServerPlayer.class.getDeclaredField("chatVisibility");
        chatVisibility.setAccessible(true);
        chatVisibility.set(player, net.minecraft.world.entity.player.ChatVisiblity.FULL);
        RecordingConnection connection = allocate(RecordingConnection.class);
        connection.sink = sink;
        player.connection = connection;
        CustomConfig.setPlayerLanguage(player.getUUID().toString(), "zh_cn");
    }

    @After
    public void tearDown() throws Exception {
        if (player != null) PlayerUtils.removeRemoteClientDataStatus(player, Banira.MOD_ID);
        Field config = CustomConfig.class.getDeclaredField("customConfig");
        config.setAccessible(true);
        config.set(null, originalConfig);
        CustomConfig.setDirty(originalDirty);
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void absentClientUsesChatAndActionBarNotCustomPackets() {
        assertVanillaSingleRoutes(false);
    }

    @Test
    public void configuredVanillaUsesChatAndActionBarDespiteInstalledClient() {
        assertVanillaSingleRoutes(true);
    }

    @Test
    public void installedNotificationModeUsesCustomPacketAndKeepsMetadata() {
        selectRoute(2);
        for (EnumNotificationVanillaFallback fallback : EnumNotificationVanillaFallback.values()) {
            MessageUtils.sendNotification(player, text("short"), EnumNotificationStyle.SUCCESS, fallback, "test.reward");
        }
        assertTrue(sink.nativePackets.isEmpty());
        assertEquals(EnumNotificationVanillaFallback.values().length, sink.customPackets.size());
        for (NotificationToClient packet : sink.customPackets) {
            assertEquals("test.reward", packet.typeId());
            assertEquals("SUCCESS", packet.styleName());
            assertEquals("TOP_RIGHT", packet.positionName());
            assertEquals(5000, packet.durationTime());
            assertEquals("zh_cn", new JsonParser().parse(packet.componentJson()).getAsJsonObject().get("languageCode").getAsString());
        }
    }

    @Test
    public void singleOptionalModTranslationPreservesRichStructureOnEveryRoute() {
        Component source = new ScopedComponent("absent_optional_mod")
                .trans(EnumI18nType.FORMAT, "reward", text("apple"))
                .translationFallback("Reward: %s").languageCode("zh_cn")
                .color(xin.vanilla.banira.common.data.Color.argb(0xff55ff55)).italic(true)
                .clickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/reward"))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("details")));
        source.getChildren().add(text(" child").underlined(true));
        JsonObject original = source.toJson();
        xin.vanilla.banira.common.notification.NotificationBudget.Payload expected =
                xin.vanilla.banira.common.notification.NotificationBudget.prepare(source, "zh_cn");
        for (int route = 0; route < 3; route++) {
            selectRoute(route);
            sink.clear();
            MessageUtils.sendNotification(player, source, "test.reward");
            if (route == 2) {
                assertEquals(expected.componentJson(), sink.customPackets.get(0).componentJson());
            } else {
                assertEquals(expected.vanillaJson(), net.minecraft.network.chat.Component.Serializer.toJson(
                        sink.nativePackets.get(0).content(), RegistryAccess.EMPTY));
            }
            assertEquals(original, source.toJson());
        }
    }

    @Test
    public void allBatchRoutesPreflightThenSubmitWholeOrderedPages() {
        List<Component> entries = Arrays.asList(text(repeat("a", 10000)), text(repeat("b", 10000)), text(repeat("c", 10000)));
        for (int route = 0; route < 3; route++) {
            selectRoute(route);
            sink.clear();
            MessageUtils.sendNotificationBatch(player, text("prefix:"), entries, text(","),
                    EnumNotificationStyle.SUCCESS, "test.reward");
            assertEquals(3, sink.calls);
            if (route == 2) {
                assertEquals(3, sink.customPackets.size());
                assertTrue(sink.nativePackets.isEmpty());
                for (int i = 0; i < 3; i++) {
                    JsonObject json = new JsonParser().parse(sink.customPackets.get(i).componentJson()).getAsJsonObject();
                    assertEquals("prefix:", json.get("text").getAsString());
                    assertEquals(entries.get(i).toJson(), json.getAsJsonArray("children").get(0));
                }
            } else {
                assertEquals(3, sink.nativePackets.size());
                assertTrue(sink.customPackets.isEmpty());
                for (int i = 0; i < 3; i++)
                    assertEquals("prefix:" + entries.get(i).text(),
                            sink.nativePackets.get(i).content().getString());
            }
        }
    }

    @Test
    public void everyRouteRejectsLateOversizedEntryBeforeAnySubmission() {
        for (int route = 0; route < 3; route++) {
            selectRoute(route);
            sink.clear();
            assertThrows(IllegalArgumentException.class, () -> MessageUtils.sendNotificationBatch(player, text(""),
                    Arrays.asList(text(repeat("a", 10000)), text(repeat("b", 10000)), text(repeat("\u4e2d", 6000))),
                    text(","), EnumNotificationStyle.SUCCESS, "test.reward"));
            assertEquals(0, sink.calls);
        }
    }

    @Test
    public void shortSingleMessagesValidateNativeExpansionAndEmptyTextHover() {
        Component expanding = new ScopedComponent("absent_optional_mod").trans(EnumI18nType.FORMAT, "key", repeat("x", 1900))
                .translationFallback(repeat("%1$s", 10));
        Component emptyWithHover = text("").hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                net.minecraft.network.chat.Component.literal(repeat("\u4e2d", 6000))));
        for (int route = 0; route < 3; route++) {
            selectRoute(route);
            sink.clear();
            for (EnumNotificationVanillaFallback fallback : EnumNotificationVanillaFallback.values()) {
                assertThrows(IllegalArgumentException.class, () -> MessageUtils.sendNotification(player,
                        expanding, EnumNotificationStyle.NORMAL, fallback, "test.reward"));
                assertThrows(IllegalArgumentException.class, () -> MessageUtils.sendNotification(player,
                        emptyWithHover, EnumNotificationStyle.NORMAL, fallback, "test.reward"));
            }
            assertEquals(0, sink.calls);
        }
    }

    @Test
    public void allRoutesPropagateSingleFailuresAndCountBatchPartialSubmission() {
        for (int route = 0; route < 3; route++) {
            selectRoute(route);
            for (EnumNotificationVanillaFallback fallback : EnumNotificationVanillaFallback.values()) {
                sink.clear();
                sink.failAt = 1;
                assertSame(sink.failure, assertThrows(IllegalStateException.class, () -> MessageUtils.sendNotification(player,
                        text("short"), EnumNotificationStyle.NORMAL, fallback, "test.reward")));
                assertEquals(1, sink.calls);
            }
            sink.clear();
            sink.failAt = 2;
            NotificationBatch.SendException failure = assertThrows(NotificationBatch.SendException.class,
                    () -> MessageUtils.sendNotificationBatch(player, text(""),
                            Arrays.asList(text(repeat("a", 10000)), text(repeat("b", 10000)), text(repeat("c", 10000))),
                            text(","), EnumNotificationStyle.NORMAL, "test.reward"));
            assertEquals(1, failure.sentPages());
            assertEquals(3, failure.totalPages());
            assertSame(sink.failure, failure.getCause());
            assertEquals(2, sink.calls);
        }
    }

    private void assertVanillaSingleRoutes(boolean installed) {
        selectRoute(installed ? 1 : 0);
        Component source = text("\u4e2d\ud83c\udf38");
        for (EnumNotificationVanillaFallback fallback : EnumNotificationVanillaFallback.values()) {
            MessageUtils.sendNotification(player, source, EnumNotificationStyle.SUCCESS, fallback, "test.reward");
        }
        assertTrue(sink.customPackets.isEmpty());
        assertEquals(2, sink.nativePackets.size());
        assertFalse(sink.nativePackets.get(0).overlay());
        assertTrue(sink.nativePackets.get(1).overlay());
        for (ClientboundSystemChatPacket packet : sink.nativePackets)
            assertEquals(source.text(), packet.content().getString());
    }

    private void selectRoute(int route) {
        PlayerUtils.removeRemoteClientDataStatus(player, Banira.MOD_ID);
        if (route != 0) PlayerUtils.setRemoteClientModInstalled(player, Banira.MOD_ID, true);
        CustomConfig.setPlayerNotificationReceiveMode(player.getUUID().toString(), route == 1
                ? CustomConfig.notificationReceiveModeVanillaMessage : CustomConfig.notificationReceiveModeNotification);
    }

    private static Component text(String value) {
        return BaniraComponent.get().literal(value).languageCode("zh_cn");
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class RecordingPlayer extends ServerPlayer {
        private UUID id;

        private RecordingPlayer() {
            super(null, null, null, net.minecraft.server.level.ClientInformation.createDefault());
        }

        @Override
        public RegistryAccess registryAccess() {
            return RegistryAccess.EMPTY;
        }

        @Override
        public UUID getUUID() {
            return id;
        }

        @Override
        public boolean isLocalPlayer() {
            return false;
        }
    }

    private static final class RecordingConnection extends ServerGamePacketListenerImpl {
        private Sink sink;

        private RecordingConnection() {
            super(null, null, null, null);
        }

        @Override
        public void send(Packet<?> packet) {
            sink.submit((ClientboundSystemChatPacket) packet);
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {
            sink.submit((ClientboundSystemChatPacket) packet);
        }
    }

    private static final class Sink {
        private final List<ClientboundSystemChatPacket> nativePackets = new ArrayList<>();
        private final List<NotificationToClient> customPackets = new ArrayList<>();
        private final IllegalStateException failure = new IllegalStateException("transport rejected");
        private int calls;
        private int failAt;

        private void submit(Object packet) {
            if (++calls == failAt) throw failure;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                if (packet instanceof NotificationToClient) {
                    ((NotificationToClient) packet).toBytes(adapt(buffer));
                    customPackets.add(new NotificationToClient(adapt(buffer)));
                } else {
                    RegistryFriendlyByteBuf nativeBuffer = new RegistryFriendlyByteBuf(buffer, RegistryAccess.EMPTY);
                    ClientboundSystemChatPacket.STREAM_CODEC.encode(nativeBuffer, (ClientboundSystemChatPacket) packet);
                    nativePackets.add(ClientboundSystemChatPacket.STREAM_CODEC.decode(nativeBuffer));
                }
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }

        private void clear() {
            nativePackets.clear();
            customPackets.clear();
            calls = 0;
            failAt = 0;
        }
    }
}
