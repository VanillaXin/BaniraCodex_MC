package xin.vanilla.banira.common.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.repeat;

public class NotificationBatchTest {
    @BeforeClass
    public static void installPlatform() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void packs256RichEntriesWithoutLossOrInputMutation() {
        Component prefix = text("\u5956\u52b1: ").color(Color.argb(0xffff5555));
        Component separator = text(" | ").italic(true);
        List<Component> entries = new ArrayList<>();
        List<JsonObject> originals = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            Component entry = new ScopedComponent("absent_optional_mod")
                    .trans(EnumI18nType.FORMAT, "reward", i)
                    .translationFallback("\u4e2d\u6587\ud83c\udf38\"\\\n %s")
                    .languageCode("zh_cn").bold(true).color(Color.argb(0xff55ff55))
                    .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reward " + i))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            net.minecraft.network.chat.Component.literal(repeat("\u60ac\u6d6e\ud83c\udf38", 12) + i)));
            entries.add(entry);
            entry.getArgs().get(0).languageCode("zh_cn");
            originals.add(entry.toJson());
        }
        JsonObject prefixBefore = prefix.toJson();
        JsonObject separatorBefore = separator.toJson();
        List<NotificationBudget.Payload> pages = NotificationBatch.prepare(prefix, entries, separator, "zh_cn");
        assertTrue(pages.size() > 1);
        assertTrue(pages.size() < entries.size());
        List<JsonObject> decoded = new ArrayList<>();
        for (NotificationBudget.Payload page : pages) {
            assertLegalNativeEncoding(page);
            JsonObject pageJson = new JsonParser().parse(page.componentJson()).getAsJsonObject();
            assertEquals(prefixBefore.get("text"), pageJson.get("text"));
            assertEquals(prefixBefore.get("color"), pageJson.get("color"));
            JsonArray children = pageJson.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                if (i % 2 == 0) decoded.add(children.get(i).getAsJsonObject());
                else assertEquals(separatorBefore, children.get(i));
            }
        }
        assertEquals(originals, decoded);
        for (int i = 0; i < entries.size(); i++) assertEquals(originals.get(i), entries.get(i).toJson());
        assertEquals(prefixBefore, prefix.toJson());
        assertEquals(separatorBefore, separator.toJson());
    }

    @Test
    public void unboundDescendantsUseRecipientLanguageBeforeFallbackSerialization() {
        new RecipientTranslator();
        Component entry = new ScopedComponent("notification_batch_language_test")
                .trans(EnumI18nType.FORMAT, "reward", "argument");
        entry.getChildren().add(BaniraComponent.get().literal("explicit").languageCode("ja_jp"));
        JsonObject before = entry.toJson();
        NotificationBudget.Payload payload = NotificationBatch.prepare(BaniraComponent.get().literal(""),
                Collections.singletonList(entry), BaniraComponent.get().literal(","), "fr_fr").get(0);
        JsonObject child = new JsonParser().parse(payload.componentJson()).getAsJsonObject()
                .getAsJsonArray("children").get(0).getAsJsonObject();
        assertEquals("fr_fr %s", child.get("translationFallback").getAsString());
        assertEquals("fr_fr", child.get("languageCode").getAsString());
        assertEquals("fr_fr", child.getAsJsonArray("args").get(0).getAsJsonObject().get("languageCode").getAsString());
        assertEquals("ja_jp", child.getAsJsonArray("children").get(0).getAsJsonObject().get("languageCode").getAsString());
        assertEquals(before, entry.toJson());
    }

    @Test
    public void nullLanguageSupplierFallsBackBeforeBindingDescendants() {
        Component source = BaniraComponent.get().literal("parent").languageCode(() -> null);
        source.getChildren().add(BaniraComponent.get().literal("child"));
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "fr_fr");
        JsonObject restored = new JsonParser().parse(payload.componentJson()).getAsJsonObject();
        assertEquals("fr_fr", restored.get("languageCode").getAsString());
        assertEquals("fr_fr", restored.getAsJsonArray("children").get(0).getAsJsonObject()
                .get("languageCode").getAsString());
        assertNull(source.languageCode().get());
        assertNull(source.getChildren().get(0).languageCode());
    }

    private static final class RecipientTranslator extends xin.vanilla.banira.common.util.Translator {
        private RecipientTranslator() {
            super("notification_batch_language_test");
            registerInCache();
        }

        @Override
        public String getTranslation(String key, String language) {
            return language + " %s";
        }
    }

    @Test
    public void oversizedLateEntrySendsNothing() {
        List<Component> entries = Arrays.asList(text(repeat("a", 10000)), text(repeat("b", 10000)),
                text(repeat("\u4e2d", 6000)));
        AtomicInteger sent = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> NotificationBatch.send(text(""), entries,
                text(","), "zh_cn", page -> sent.incrementAndGet()));
        assertEquals(0, sent.get());
    }

    @Test
    public void nativeTranslationExpansionHasIndependentBudget() {
        Component expanding = expansion(10);
        assertTrue(expanding.toJson().toString().getBytes(StandardCharsets.UTF_8).length < 16384);
        assertTrue(net.minecraft.network.chat.Component.Serializer.toJson(expanding.clone().toChat("zh_cn"), RegistryAccess.EMPTY)
                .getBytes(StandardCharsets.UTF_8).length > 16384);
        assertThrows(IllegalArgumentException.class, () -> NotificationBudget.prepare(expanding, "zh_cn"));
        List<NotificationBudget.Payload> pages = NotificationBatch.prepare(text(""),
                Arrays.asList(expansion(3), expansion(3), expansion(3)), text(","), "zh_cn");
        assertTrue(pages.size() > 1);
        for (NotificationBudget.Payload page : pages) assertLegalNativeEncoding(page);
    }

    @Test
    public void nativeBoundaryIsInclusiveAndRejectsOneByteOver() {
        // Repeated format arguments keep Banira JSON small at the native boundary.
        Component exact = expansion(7);
        exact.translationFallback(exact.translationFallback() + "x");
        int nativeSize = net.minecraft.network.chat.Component.Serializer.toJson(exact.clone().toChat("zh_cn"), RegistryAccess.EMPTY)
                .getBytes(StandardCharsets.UTF_8).length;
        assertTrue(nativeSize < 16384);
        exact.translationFallback(exact.translationFallback() + repeat("x", 16384 - nativeSize));
        assertEquals(16384, net.minecraft.network.chat.Component.Serializer.toJson(exact.clone().toChat("zh_cn"), RegistryAccess.EMPTY)
                .getBytes(StandardCharsets.UTF_8).length);
        NotificationBudget.Payload payload = NotificationBudget.prepare(exact, "zh_cn");
        assertEquals(16384, payload.vanillaJson().getBytes(StandardCharsets.UTF_8).length);
        exact.translationFallback(exact.translationFallback() + "x");
        assertThrows(IllegalArgumentException.class, () -> NotificationBudget.prepare(exact, "zh_cn"));
    }

    @Test
    public void midSendFailureReportsAcceptedPagesAndDoesNotRetry() {
        List<Component> entries = Arrays.asList(text(repeat("a", 10000)), text(repeat("b", 10000)),
                text(repeat("c", 10000)));
        AtomicInteger calls = new AtomicInteger();
        RuntimeException transport = new IllegalStateException("transport rejected");
        NotificationBatch.SendException failure = assertThrows(NotificationBatch.SendException.class,
                () -> NotificationBatch.send(text(""), entries, text(","), "zh_cn", page -> {
                    if (calls.incrementAndGet() == 2) throw transport;
                }));
        assertEquals(1, failure.sentPages());
        assertEquals(3, failure.totalPages());
        assertSame(transport, failure.getCause());
        assertEquals(2, calls.get());
    }

    @Test
    public void shortAndEmptyListsKeepOnePrefixPageAndSnapshotNativeOriginals() {
        assertEquals(1, NotificationBatch.prepare(text("prefix"), Collections.emptyList(), text(","), "zh_cn").size());
        Component source = BaniraComponent.get().object(net.minecraft.network.chat.Component.literal("original"));
        source.getChildren().add(text("child"));
        String originalJson = net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) source.original(), RegistryAccess.EMPTY);
        JsonObject sourceJson = source.toJson();
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals(originalJson, net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) source.original(), RegistryAccess.EMPTY));
        assertEquals(sourceJson, source.toJson());
        String frozen = payload.vanillaJson();
        net.minecraft.network.chat.MutableComponent decoded = (net.minecraft.network.chat.MutableComponent) payload.vanillaMessage();
        decoded.setStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
        assertNotEquals(frozen, net.minecraft.network.chat.Component.Serializer.toJson(decoded, RegistryAccess.EMPTY));
        assertEquals(frozen, payload.vanillaJson());
        assertEquals(frozen, net.minecraft.network.chat.Component.Serializer.toJson(payload.vanillaMessage(), RegistryAccess.EMPTY));
    }

    @Test
    public void originalRichTextSurvivesCustomNotificationRoundTrip() {
        net.minecraft.network.chat.MutableComponent original = net.minecraft.network.chat.Component.literal("original")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x55ff55).withBold(true).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                net.minecraft.network.chat.Component.literal("details"))))
                .append(net.minecraft.network.chat.Component.literal(" sibling").withStyle(
                        net.minecraft.network.chat.Style.EMPTY.withColor(0xff5555).withItalic(true)));
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        xin.vanilla.banira.common.network.packet.NotificationToClient packet =
                new xin.vanilla.banira.common.network.packet.NotificationToClient(payload,
                        xin.vanilla.banira.common.enums.EnumPosition.TOP_RIGHT,
                        xin.vanilla.banira.common.enums.EnumMoveType.AUTO, 5000,
                        xin.vanilla.banira.common.enums.EnumNotificationStyle.NORMAL, "default");
        FriendlyByteBuf nativeBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var buffer = xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.adapt(nativeBuffer);
            packet.toBytes(buffer);
            var decodedPacket = new xin.vanilla.banira.common.network.packet.NotificationToClient(buffer);
            Component decoded = BaniraComponent.get().deserialize(JsonParser.parseString(decodedPacket.componentJson()).getAsJsonObject());
            assertNotNull(decoded.original());
            assertEquals(net.minecraft.network.chat.Component.Serializer.toJson(original, RegistryAccess.EMPTY),
                    net.minecraft.network.chat.Component.Serializer.toJson(
                            (net.minecraft.network.chat.Component) decoded.original(), RegistryAccess.EMPTY));
            assertEquals("original sibling", decoded.toChat("zh_cn").getString());
            assertTrue(decoded.toChat("zh_cn").getStyle().isBold());
            assertTrue(decoded.toChat("zh_cn").getStyle().isUnderlined());
            source.bold(false);
            Component explicitlyDisabled = BaniraComponent.get().deserialize(source.toJson());
            assertFalse(explicitlyDisabled.toChat("zh_cn").getStyle().isBold());
            assertTrue(explicitlyDisabled.toChat("zh_cn").getStyle().isUnderlined());
            assertEquals(0, nativeBuffer.readableBytes());
        } finally {
            nativeBuffer.release();
        }
        Component parent = text("parent");
        parent.getChildren().add(source);
        parent.getArgs().add(source);
        Component nested = BaniraComponent.get().deserialize(parent.toJson());
        assertNotNull(nested.getChildren().get(0).original());
        assertNotNull(nested.getArgs().get(0).original());
    }

    @Test
    public void decodedNativeSiblingsAllowBaniraChildrenWithoutMutatingOriginal() {
        var original = net.minecraft.network.chat.Component.literal("root")
                .append(net.minecraft.network.chat.Component.literal(" sibling"));
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        source.getChildren().add(text(" child"));
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals("root sibling child", payload.vanillaMessage().getString());
        assertEquals("root sibling", original.getString());
    }

    @Test
    public void nativeModifiedUtfExpansionIsBoundedIndependentlyOfJson() {
        Component source = text(repeat("\ud83c\udf38", 2800));
        assertTrue(source.toJson().toString().getBytes(StandardCharsets.UTF_8).length < 16384);
        assertTrue(net.minecraft.network.chat.Component.Serializer.toJson(source.toChat(), RegistryAccess.EMPTY)
                .getBytes(StandardCharsets.UTF_8).length < 16384);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, source.toChat());
            assertTrue(buffer.readableBytes() > 16384);
        } finally {
            buffer.release();
        }
        assertThrows(NotificationBudget.LimitExceededException.class, () -> NotificationBudget.prepare(source, "zh_cn"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void smokeListenerCollectsAllPagesFromActualNeoForgeSystemMessageHook() throws Exception {
        Class<?> runner = xin.vanilla.banira.internal.client.dev.BaniraNetworkSmokeClientRunner.class;
        var listener = Arrays.stream(runner.getDeclaredMethods())
                .filter(method -> method.getName().equals("receiveVanillaNotification")).findFirst().orElseThrow();
        listener.setAccessible(true);
        var detailsField = runner.getDeclaredField("vanillaDetails");
        var pagesField = runner.getDeclaredField("vanillaPages");
        detailsField.setAccessible(true);
        pagesField.setAccessible(true);
        List<String> details = (List<String>) detailsField.get(null);
        List<String> before = new ArrayList<>(details);
        int pagesBefore = pagesField.getInt(null);
        java.util.function.Consumer handler = event -> {
            try {
                listener.invoke(null, event);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        var shutdownField = bus.getClass().getDeclaredField("shutdown");
        shutdownField.setAccessible(true);
        boolean wasShutdown = shutdownField.getBoolean(bus);
        // Register the real listener's event type, just as its method-reference registration does.
        bus.addListener(net.neoforged.bus.api.EventPriority.NORMAL, false,
                (Class) listener.getParameterTypes()[0], handler);
        try {
            bus.start();
            details.clear();
            pagesField.setInt(null, 0);
            List<Component> entries = new ArrayList<>();
            List<String> expected = new ArrayList<>();
            for (int index = 0; index < 256; index++) {
                String value = index + "-abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789";
                expected.add(value);
                entries.add(text(value).color(0xFF00FF00));
            }
            List<NotificationBudget.Payload> pages = NotificationBatch.prepare(
                    text("banira-smoke-native:"), entries, text("|"), "zh_cn");
            assertTrue(pages.size() > 1);
            for (NotificationBudget.Payload page : pages) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                try {
                    var packet = new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(page.vanillaMessage(), false);
                    net.minecraft.network.protocol.game.ClientboundSystemChatPacket.STREAM_CODEC.encode(buffer, packet);
                    var decoded = net.minecraft.network.protocol.game.ClientboundSystemChatPacket.STREAM_CODEC.decode(buffer);
                    assertSame(decoded.content(), net.neoforged.neoforge.client.ClientHooks.onClientSystemChat(
                            decoded.content(), decoded.overlay()));
                    assertEquals(0, buffer.readableBytes());
                } finally {
                    buffer.release();
                }
            }
            assertEquals(expected, details);
            assertEquals(pages.size(), pagesField.getInt(null));
        } finally {
            bus.unregister(handler);
            shutdownField.setBoolean(bus, wasShutdown);
            details.clear();
            details.addAll(before);
            pagesField.setInt(null, pagesBefore);
        }
    }

    private static Component expansion(int copies) {
        return new ScopedComponent("absent_optional_mod").trans(EnumI18nType.FORMAT, "repeated", repeat("x", 1900))
                .translationFallback(repeat("%1$s", copies)).languageCode("zh_cn");
    }

    private static Component text(String value) {
        return BaniraComponent.get().literal(value).languageCode("zh_cn");
    }

    private static void assertLegalNativeEncoding(NotificationBudget.Payload page) {
        assertTrue(page.componentJson().getBytes(StandardCharsets.UTF_8).length <= 16384);
        assertTrue(page.vanillaJson().getBytes(StandardCharsets.UTF_8).length <= 16384);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf(page.componentJson(), 16384);
            assertEquals(page.componentJson(), buffer.readUtf(16384));
            buffer.clear();
            RegistryFriendlyByteBuf nativeBuffer = new RegistryFriendlyByteBuf(buffer, RegistryAccess.EMPTY);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(nativeBuffer, page.vanillaMessage());
            assertTrue(nativeBuffer.readableBytes() <= 16384);
            assertEquals(page.vanillaJson(), net.minecraft.network.chat.Component.Serializer.toJson(
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(nativeBuffer), RegistryAccess.EMPTY));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void explicitRegistriesPreserveEnchantedItemHoverWithoutGlobalRuntime() {
        assertNull(xin.vanilla.banira.internal.common.ClientRuntimeBridge.localPlayer());
        assertNull(xin.vanilla.banira.internal.common.BaniraServerRuntime.server());
        var enchantments = new net.minecraft.core.MappedRegistry<net.minecraft.world.item.enchantment.Enchantment>(
                net.minecraft.core.registries.Registries.ENCHANTMENT, com.mojang.serialization.Lifecycle.stable());
        var enchantmentId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                "banira_codex", "notification_context_test");
        var definition = net.minecraft.world.item.enchantment.Enchantment.definition(
                net.minecraft.core.HolderSet.direct(net.minecraft.world.item.Items.DIAMOND_SWORD.builtInRegistryHolder()),
                1, 3, net.minecraft.world.item.enchantment.Enchantment.constantCost(1),
                net.minecraft.world.item.enchantment.Enchantment.constantCost(20), 1,
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        var enchantment = net.minecraft.core.Registry.registerForHolder(enchantments, enchantmentId,
                net.minecraft.world.item.enchantment.Enchantment.enchantment(definition).build(enchantmentId));
        RegistryAccess registries = new RegistryAccess.ImmutableRegistryAccess(
                List.of(net.minecraft.core.registries.BuiltInRegistries.ITEM, enchantments)).freeze();
        var item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        item.enchant(enchantment, 2);
        assertEquals(2, item.getEnchantments().getLevel(enchantment));
        var original = net.minecraft.network.chat.Component.literal("enchanted reward")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item))))
                .append(net.minecraft.network.chat.Component.literal(" sibling")
                        .withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(true)));
        var ops = registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var expected = ComponentSerialization.CODEC.encodeStart(ops, original).getOrThrow();
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");

        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn", registries);
        var storedOriginal = JsonParser.parseString(payload.componentJson()).getAsJsonObject().get("original");
        assertEquals(expected, storedOriginal);
        var decoded = ComponentSerialization.CODEC.parse(ops, storedOriginal).getOrThrow();
        var hover = decoded.getStyle().getHoverEvent();
        assertNotNull(hover);
        var itemInfo = hover.getValue(HoverEvent.Action.SHOW_ITEM);
        assertNotNull(itemInfo);
        assertSame(net.minecraft.world.item.Items.DIAMOND_SWORD, itemInfo.getItemStack().getItem());
        assertEquals(item.getCount(), itemInfo.getItemStack().getCount());
        assertEquals(2, itemInfo.getItemStack().getEnchantments().getLevel(enchantment));
        assertEquals(expected, ComponentSerialization.CODEC.encodeStart(ops, decoded).getOrThrow());
        assertEquals(expected, ComponentSerialization.CODEC.encodeStart(ops, payload.vanillaMessage()).getOrThrow());
        assertEquals(expected, ComponentSerialization.CODEC.encodeStart(ops, original).getOrThrow());

        // The same explicit context must reach original components in both recursive collections.
        Component parent = text("parent");
        parent.getChildren().add(source);
        parent.getArgs().add(source);
        var nested = JsonParser.parseString(NotificationBudget.prepare(parent, "zh_cn", registries).componentJson())
                .getAsJsonObject();
        assertEquals(expected, nested.getAsJsonArray("children").get(0).getAsJsonObject().get("original"));
        assertEquals(expected, nested.getAsJsonArray("args").get(0).getAsJsonObject().get("original"));
    }
}
