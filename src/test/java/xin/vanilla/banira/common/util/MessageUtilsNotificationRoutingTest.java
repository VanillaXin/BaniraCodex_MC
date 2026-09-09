package xin.vanilla.banira.common.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.HoverEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
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
        RecordingConnection connection = allocate(RecordingConnection.class);
        connection.sink = sink;
        player.connection = connection;
        CustomConfig.setPlayerLanguage(player.getUUID().toString(), "zh_cn");
    }

    @After
    public void tearDown() throws Exception {
        if (player != null) PlayerUtils.removeRemoteClientDataStatus(player, BaniraCodex.MODID);
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
                for (int i = 0; i < 3; i++) assertEquals("prefix:" + entries.get(i).text(),
                        sink.nativePackets.get(i).getMessage().getString());
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
                new TextComponent(repeat("\u4e2d", 6000))));
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
        assertEquals(ChatType.SYSTEM, sink.nativePackets.get(0).getType());
        assertEquals(ChatType.GAME_INFO, sink.nativePackets.get(1).getType());
        for (ClientboundChatPacket packet : sink.nativePackets) assertEquals(source.text(), packet.getMessage().getString());
    }

    private void selectRoute(int route) {
        PlayerUtils.removeRemoteClientDataStatus(player, BaniraCodex.MODID);
        if (route != 0) PlayerUtils.setRemoteClientModInstalled(player, BaniraCodex.MODID, true);
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
            super(null, null, null, null);
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
            super(null, null, null);
        }

        @Override
        public void send(Packet<?> packet) {
            sink.submit((ClientboundChatPacket) packet);
        }

        @Override
        public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener) {
            sink.submit((ClientboundChatPacket) packet);
        }
    }

    private static final class Sink {
        private final List<ClientboundChatPacket> nativePackets = new ArrayList<>();
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
                    ((ClientboundChatPacket) packet).write(buffer);
                    ClientboundChatPacket decoded = new ClientboundChatPacket();
                    decoded.read(buffer);
                    nativePackets.add(decoded);
                }
                assertEquals(0, buffer.readableBytes());
            } catch (java.io.IOException error) {
                throw new AssertionError("In-memory packet codec failed", error);
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
