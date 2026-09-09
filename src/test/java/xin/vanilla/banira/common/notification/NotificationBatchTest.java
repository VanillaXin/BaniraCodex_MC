package xin.vanilla.banira.common.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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
    public static void installPlatform() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
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
            super("notification_batch_language_test", RecipientTranslator.class);
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
        assertTrue(net.minecraft.network.chat.Component.Serializer.toJson(expanding.clone().toChat("zh_cn"))
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
        int nativeSize = net.minecraft.network.chat.Component.Serializer.toJson(exact.clone().toChat("zh_cn"))
                .getBytes(StandardCharsets.UTF_8).length;
        assertTrue(nativeSize < 16384);
        exact.translationFallback(exact.translationFallback() + repeat("x", 16384 - nativeSize));
        assertEquals(16384, net.minecraft.network.chat.Component.Serializer.toJson(exact.clone().toChat("zh_cn"))
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
        String originalJson = net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) source.original());
        JsonObject sourceJson = source.toJson();
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals(originalJson, net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) source.original()));
        assertEquals(sourceJson, source.toJson());
        String frozen = payload.vanillaJson();
        payload.vanillaMessage().getSiblings().clear();
        assertEquals(frozen, payload.vanillaJson());
    }

    @Test
    public void nativeOriginalRoundTripsThroughActualNotificationPacket() {
        var original = net.minecraft.network.chat.Component.literal("original")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x55ff55)
                        .withBold(true).withItalic(true).withUnderlined(true)
                        .withStrikethrough(true).withObfuscated(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("details"))))
                .append(net.minecraft.network.chat.Component.literal(" sibling").withStyle(
                        net.minecraft.network.chat.Style.EMPTY.withColor(0xff5555).withItalic(true)));
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        String originalJson = net.minecraft.network.chat.Component.Serializer.toJson(original);
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        var packet = new xin.vanilla.banira.common.network.packet.NotificationToClient(payload,
                xin.vanilla.banira.common.enums.EnumPosition.TOP_RIGHT,
                xin.vanilla.banira.common.enums.EnumMoveType.AUTO, 5000,
                xin.vanilla.banira.common.enums.EnumNotificationStyle.NORMAL, "default");
        FriendlyByteBuf nativeBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var buffer = xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.adapt(nativeBuffer);
            packet.toBytes(buffer);
            var decodedPacket = new xin.vanilla.banira.common.network.packet.NotificationToClient(buffer);
            Component decoded = BaniraComponent.get().deserialize(
                    JsonParser.parseString(decodedPacket.componentJson()).getAsJsonObject());
            assertNotNull(decoded.original());
            assertEquals(originalJson, net.minecraft.network.chat.Component.Serializer.toJson(
                    (net.minecraft.network.chat.Component) decoded.original()));
            assertEquals("original sibling", decoded.toChat("zh_cn").getString());
            var style = decoded.toChat("zh_cn").getStyle();
            assertTrue(style.isBold());
            assertTrue(style.isItalic());
            assertTrue(style.isUnderlined());
            assertTrue(style.isStrikethrough());
            assertTrue(style.isObfuscated());
            assertEquals(original.getStyle().getClickEvent(), style.getClickEvent());
            assertEquals(original.getStyle().getHoverEvent(), style.getHoverEvent());
            source.bold(false);
            Component disabledBold = BaniraComponent.get().deserialize(source.toJson());
            assertFalse(disabledBold.toChat("zh_cn").getStyle().isBold());
            assertTrue(disabledBold.toChat("zh_cn").getStyle().isUnderlined());
            source.italic(false).underlined(false).strikethrough(false).obfuscated(false);
            var disabled = BaniraComponent.get().deserialize(source.toJson()).toChat("zh_cn").getStyle();
            assertFalse(disabled.isBold());
            assertFalse(disabled.isItalic());
            assertFalse(disabled.isUnderlined());
            assertFalse(disabled.isStrikethrough());
            assertFalse(disabled.isObfuscated());
            assertEquals(0, nativeBuffer.readableBytes());
        } finally {
            nativeBuffer.release();
        }
        Component parent = text("parent");
        parent.getChildren().add(source);
        parent.getArgs().add(source);
        Component nested = BaniraComponent.get().deserialize(parent.toJson());
        assertEquals(originalJson, net.minecraft.network.chat.Component.Serializer.toJson(
                (net.minecraft.network.chat.Component) nested.getChildren().get(0).original()));
        assertEquals(originalJson, net.minecraft.network.chat.Component.Serializer.toJson(
                (net.minecraft.network.chat.Component) nested.getArgs().get(0).original()));
        Component plain = text("plain");
        JsonObject plainBefore = plain.toJson();
        plain.bold(false).italic(false).underlined(false).strikethrough(false).obfuscated(false);
        assertEquals(plainBefore, plain.toJson());
        assertEquals(net.minecraft.network.chat.Style.EMPTY.withBold(false).withItalic(false)
                .withUnderlined(false).withStrikethrough(false).withObfuscated(false), plain.getStyle());
    }

    @Test
    public void decodedNativeSiblingsAllowBaniraChildrenWithoutMutatingOriginal() {
        var original = net.minecraft.network.chat.Component.Serializer.fromJson(
                "{\"text\":\"root\",\"extra\":[{\"text\":\" sibling\"}]}");
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        source.getChildren().add(text(" child"));
        assertEquals("root sibling child", source.toVanilla("zh_cn").getString());
        assertEquals("root sibling", original.getString());
        assertEquals("root sibling child", source.toVanilla("zh_cn").getString());
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals("root sibling child", payload.vanillaMessage().getString());
        assertEquals("root sibling", original.getString());
    }

    @Test
    public void nativeEnchantedItemHoverSurvivesNestedOriginalRoundTrip() {
        var item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        item.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2);
        var original = net.minecraft.network.chat.Component.literal("enchanted reward")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item))));
        String expected = net.minecraft.network.chat.Component.Serializer.toJson(original);
        Component parent = text("parent");
        parent.getChildren().add(BaniraComponent.get().object(original));
        parent.getArgs().add(BaniraComponent.get().object(original));
        NotificationBudget.Payload payload = NotificationBudget.prepare(parent, "zh_cn");
        Component decoded = BaniraComponent.get().deserialize(
                JsonParser.parseString(payload.componentJson()).getAsJsonObject());
        for (Component nested : Arrays.asList(decoded.getChildren().get(0), decoded.getArgs().get(0))) {
            assertNotNull(nested.original());
            var restored = (net.minecraft.network.chat.Component) nested.original();
            assertEquals(expected, net.minecraft.network.chat.Component.Serializer.toJson(restored));
            var restoredItem = nested.toChat("zh_cn").getStyle().getHoverEvent()
                    .getValue(HoverEvent.Action.SHOW_ITEM).getItemStack();
            assertEquals(net.minecraft.world.item.Items.DIAMOND_SWORD, restoredItem.getItem());
            assertEquals(2, net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, restoredItem));
        }
    }

    @Test
    public void originalStyleFlagsKeepUnsetAndExplicitFalseDistinct() throws Exception {
        String[] flags = {"bold", "italic", "underlined", "strikethrough", "obfuscated"};
        String[] getters = {"isBold", "isItalic", "isUnderlined", "isStrikethrough", "isObfuscated"};
        net.minecraft.network.chat.Component original = net.minecraft.network.chat.Component.Serializer.fromJson(
                "{\"text\":\"native\",\"bold\":true,\"italic\":true,\"underlined\":true,\"strikethrough\":true,\"obfuscated\":true}");
        for (int disabled = 0; disabled < flags.length; disabled++) {
            Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
            for (String flag : flags) assertFalse(source.toJson().has(flag));
            Component.class.getMethod(flags[disabled], Boolean.class).invoke(source, Boolean.FALSE);
            JsonObject encoded = source.toJson();
            assertTrue(encoded.has(flags[disabled]));
            assertFalse(encoded.get(flags[disabled]).getAsBoolean());
            Component restored = BaniraComponent.get().deserialize(encoded);
            net.minecraft.network.chat.Style style = restored.toChat("zh_cn").getStyle();
            for (int flag = 0; flag < flags.length; flag++) {
                assertEquals(flags[flag], flag != disabled,
                        net.minecraft.network.chat.Style.class.getMethod(getters[flag]).invoke(style));
            }
            assertEquals("native", original.getString());
        }
        Component plain = text("plain").bold(false).italic(false).underlined(false)
                .strikethrough(false).obfuscated(false);
        for (String flag : flags) assertFalse(plain.toJson().has(flag));
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
            buffer.writeComponent(page.vanillaMessage());
            assertEquals(page.vanillaJson(), net.minecraft.network.chat.Component.Serializer.toJson(buffer.readComponent()));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
