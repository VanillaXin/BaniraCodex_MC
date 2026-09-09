package xin.vanilla.banira.common.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
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
                            new StringTextComponent(repeat("\u60ac\u6d6e\ud83c\udf38", 12) + i)));
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
        assertTrue(ITextComponent.Serializer.toJson(expanding.clone().toChat("zh_cn"))
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
        int nativeSize = ITextComponent.Serializer.toJson(exact.clone().toChat("zh_cn"))
                .getBytes(StandardCharsets.UTF_8).length;
        assertTrue(nativeSize < 16384);
        exact.translationFallback(exact.translationFallback() + repeat("x", 16384 - nativeSize));
        assertEquals(16384, ITextComponent.Serializer.toJson(exact.clone().toChat("zh_cn"))
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
        Component source = BaniraComponent.get().object(new StringTextComponent("original"));
        source.getChildren().add(text("child"));
        String originalJson = ITextComponent.Serializer.toJson((ITextComponent) source.original());
        JsonObject sourceJson = source.toJson();
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals(originalJson, ITextComponent.Serializer.toJson((ITextComponent) source.original()));
        assertEquals(sourceJson, source.toJson());
        String frozen = payload.vanillaJson();
        payload.vanillaMessage().getSiblings().clear();
        assertEquals(frozen, payload.vanillaJson());
    }

    private static Component expansion(int copies) {
        return new ScopedComponent("absent_optional_mod").trans(EnumI18nType.FORMAT, "repeated", repeat("x", 1900))
                .translationFallback(repeat("%1$s", copies)).languageCode("zh_cn");
    }

    private static Component text(String value) {
        return BaniraComponent.get().literal(value).languageCode("zh_cn");
    }

    @Test
    public void originalRichTextSurvivesCustomNotificationRoundTrip() {
        net.minecraft.util.text.IFormattableTextComponent original = new StringTextComponent("original")
                .withStyle(net.minecraft.util.text.Style.EMPTY.withColor(net.minecraft.util.text.Color.fromRgb(0x55ff55))
                        .withBold(true).setUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("details"))))
                .append(new StringTextComponent(" sibling").withStyle(net.minecraft.util.text.Style.EMPTY
                        .withColor(net.minecraft.util.text.Color.fromRgb(0xff5555)).withItalic(true)));
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        xin.vanilla.banira.common.network.packet.NotificationToClient packet =
                new xin.vanilla.banira.common.network.packet.NotificationToClient(payload,
                        xin.vanilla.banira.common.enums.EnumPosition.TOP_RIGHT,
                        xin.vanilla.banira.common.enums.EnumMoveType.AUTO, 5000,
                        xin.vanilla.banira.common.enums.EnumNotificationStyle.NORMAL, "default");
        PacketBuffer nativeBuffer = new PacketBuffer(Unpooled.buffer());
        try {
            xin.vanilla.banira.common.network.BaniraPacketBuffer buffer =
                    xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.adapt(nativeBuffer);
            packet.toBytes(buffer);
            xin.vanilla.banira.common.network.packet.NotificationToClient decodedPacket =
                    new xin.vanilla.banira.common.network.packet.NotificationToClient(buffer);
            Component decoded = BaniraComponent.get().deserialize(new JsonParser().parse(decodedPacket.componentJson()).getAsJsonObject());
            assertNotNull(decoded.original());
            assertEquals(ITextComponent.Serializer.toJson(original),
                    ITextComponent.Serializer.toJson((ITextComponent) decoded.original()));
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
        net.minecraft.util.text.IFormattableTextComponent original = new StringTextComponent("root")
                .append(new StringTextComponent(" sibling"));
        Component source = BaniraComponent.get().object(original).languageCode("zh_cn");
        source.getChildren().add(text(" child"));
        NotificationBudget.Payload payload = NotificationBudget.prepare(source, "zh_cn");
        assertEquals("root sibling child", payload.vanillaMessage().getString());
        assertEquals("root sibling", original.getString());
    }

    private static void assertLegalNativeEncoding(NotificationBudget.Payload page) {
        assertTrue(page.componentJson().getBytes(StandardCharsets.UTF_8).length <= 16384);
        assertTrue(page.vanillaJson().getBytes(StandardCharsets.UTF_8).length <= 16384);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeUtf(page.componentJson(), 16384);
            assertEquals(page.componentJson(), buffer.readUtf(16384));
            buffer.clear();
            buffer.writeComponent(page.vanillaMessage());
            assertEquals(page.vanillaJson(), ITextComponent.Serializer.toJson(buffer.readComponent()));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
