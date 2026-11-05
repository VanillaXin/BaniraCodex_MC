package xin.vanilla.banira.common.notification;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStackTemplate;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.ITranslator;
import xin.vanilla.banira.common.util.NativeComponentJson;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.common.notification.NotificationPreparationFixture.*;

public class NotificationBatchPreparationDifferentialTest {
    @BeforeClass
    public static void installPlatform() {
        xin.vanilla.banira.test.NativeBootstrap.initialize();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void exactGreedyPagesAndBothRealWireEncodingsMatchFor256Entries() {
        for (String name : CASES) {
            Fixture fixture = create(name);
            List<String> before = fixture.inputSnapshot();
            List<Encoding> expected = reference(fixture);
            assertEquals(name + " reference mutated input", before, fixture.inputSnapshot());
            List<Encoding> actual = production(fixture);
            assertEquals(name + " page count", expected.size(), actual.size());
            assertTrue(name + " must exercise page rollover", actual.size() > 1);
            for (int i = 0; i < actual.size(); i++) {
                assertEquals(name + " Banira page " + i, expected.get(i).componentJson, actual.get(i).componentJson);
                assertEquals(name + " native page " + i, expected.get(i).vanillaJson, actual.get(i).vanillaJson);
                assertWireRoundTrip(actual.get(i));
            }
            assertEquals(name + " production mutated input", before, fixture.inputSnapshot());
        }
    }

    @Test
    public void dynamicLanguageSupplierRetainsLegacyOutputWithoutSamplingItForEligibility() {
        Fixture expected = dynamicFixture();
        Fixture actual = dynamicFixture();
        assertEquals(reference(expected), production(actual));
    }

    @Test
    public void anExternalSupplierInAnOtherwiseLiteralBatchUsesAllLegacyEvaluations() {
        for (boolean changing : new boolean[]{false, true}) {
            Fixture expected = create("literal");
            Fixture actual = create("literal");
            AtomicInteger expectedCalls = new AtomicInteger();
            AtomicInteger actualCalls = new AtomicInteger();
            expected.entries.get(255).languageCode(() -> supplierLanguage(expectedCalls, changing));
            actual.entries.get(255).languageCode(() -> supplierLanguage(actualCalls, changing));
            assertEquals(reference(expected), production(actual));
            assertTrue(expectedCalls.get() > 0);
            assertEquals("Eligibility must not evaluate caller code", expectedCalls.get(), actualCalls.get());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void statefulTranslatorInAnOtherwiseLiteralBatchKeepsLegacyResolutionOrder() throws Exception {
        Field field = Translator.class.getDeclaredField("CACHE");
        field.setAccessible(true);
        Map<String, ITranslator> cache = (Map<String, ITranslator>) field.get(null);
        String id = "notification_preparation_changing";
        ITranslator previous = cache.get(id);
        try {
            ChangingTranslator referenceTranslator = new ChangingTranslator(id);
            Fixture expected = translatedPrefix(id);
            List<Encoding> encodings = reference(expected);
            int calls = referenceTranslator.calls.get();
            ChangingTranslator actualTranslator = new ChangingTranslator(id);
            assertEquals(encodings, production(translatedPrefix(id)));
            assertEquals(calls, actualTranslator.calls.get());
        } finally {
            if (previous == null) cache.remove(id);
            else cache.put(id, previous);
        }
    }

    @Test
    public void oneHoverOrNativeOriginalKeepsTheMixedLiteralBatchEquivalent() {
        for (boolean original : new boolean[]{false, true}) {
            Fixture fixture = create("literal");
            Component entry = original
                    ? BaniraComponent.get().object(net.minecraft.network.chat.Component.literal("mixed-original")).languageCode(LANGUAGE)
                    : text("mixed-hover").hoverEvent(new HoverEvent.ShowText(
                            net.minecraft.network.chat.Component.literal("mixed-hover-details")));
            fixture.entries.set(200, entry);
            List<String> before = fixture.inputSnapshot();
            assertEquals(reference(fixture), production(fixture));
            assertEquals(before, fixture.inputSnapshot());
        }
    }

    @Test
    public void literalPreparationDoesNotMutateInputAndDoesNotCacheMutableColorsAcrossCalls() {
        Fixture fixture = create("literal");
        List<String> before = fixture.inputSnapshot();
        List<NotificationBudget.Payload> first = NotificationBatch.prepare(
                fixture.prefix, fixture.entries, fixture.separator, LANGUAGE);
        List<Encoding> frozen = encodings(first);
        assertEquals(before, fixture.inputSnapshot());
        fixture.entries.get(0).color().setArgb(0xff55ff55);
        fixture.prefix.color().setAlpha(0);
        assertEquals(frozen, encodings(first));
        List<Encoding> second = production(fixture);
        assertEquals(reference(fixture), second);
        assertNotEquals(frozen, second);
    }

    @Test
    public void callerListSubclassIsNotTraversedToSelectTheFastPath() {
        Fixture seed = create("literal");
        ObservedList expectedEntries = new ObservedList(seed.entries);
        ObservedList actualEntries = new ObservedList(seed.entries);
        Fixture expected = new Fixture("literal", seed.prefix, expectedEntries, seed.separator);
        Fixture actual = new Fixture("literal", seed.prefix, actualEntries, seed.separator);
        assertEquals(reference(expected), production(actual));
        assertEquals(ENTRY_COUNT, expectedEntries.gets);
        assertEquals(expectedEntries.gets, actualEntries.gets);
        assertEquals(0, actualEntries.iterators);
    }

    @Test
    public void plainUnboundLanguagesInheritTheSameRecipientAndExplicitChildLanguages() {
        Fixture fixture = create("literal");
        fixture.prefix.languageCode((String) null);
        fixture.prefix.getChildren().get(0).languageCode((String) null);
        fixture.separator.languageCode((String) null);
        fixture.entries.get(0).languageCode((String) null);
        fixture.entries.get(1).languageCode("fr_fr");
        List<String> before = fixture.inputSnapshot();
        assertEquals(reference(fixture), production(fixture));
        assertEquals(before, fixture.inputSnapshot());
    }

    @Test
    public void aLaterCallSeesMutatedNativeTextHoverAndPrefixButEarlierPayloadStaysFrozen() {
        Fixture fixture = create("native");
        List<NotificationBudget.Payload> first = NotificationBatch.prepare(
                fixture.prefix, fixture.entries, fixture.separator, LANGUAGE);
        List<Encoding> frozen = encodings(first);
        ((MutableComponent) fixture.entries.get(0).original()).append(net.minecraft.network.chat.Component.literal("later"));
        fixture.entries.get(1).hoverEvent(new HoverEvent.ShowText(
                net.minecraft.network.chat.Component.literal("later-hover")));
        fixture.prefix.text("changed-prefix: ");
        ((MutableComponent) first.get(0).vanillaMessage()).setStyle(
                net.minecraft.network.chat.Style.EMPTY.withObfuscated(true));
        assertEquals(frozen, encodings(first));
        List<Encoding> second = production(fixture);
        assertNotEquals(frozen, second);
        assertEquals(reference(fixture), second);
    }

    @Test
    public void oversizedLastOf256EntriesSubmitsNothingAndRetainsFailureIndex() {
        Fixture fixture = create("literal");
        fixture.entries.set(ENTRY_COUNT - 1, text(repeat("\u4e2d", 6000)));
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> reference(fixture));
        AtomicInteger calls = new AtomicInteger();
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                () -> NotificationBatch.send(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE,
                        page -> calls.incrementAndGet()));
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
        assertEquals(0, calls.get());
    }

    @Test
    public void preparationDoesNotInspectALaterNullPastAnEarlierOversizedEntry() {
        Fixture fixture = create("literal");
        fixture.entries.set(200, text(repeat("\u4e2d", 6000)));
        fixture.entries.set(255, null);
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> reference(fixture));
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> production(fixture));
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
    }

    @Test
    public void invalidNativeOriginalIsNotReclassifiedAsBudgetOverflowOrTransportFailure() {
        Fixture fixture = create("native");
        fixture.entries.set(100, BaniraComponent.get().object(new Object()).languageCode(LANGUAGE));
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> reference(fixture));
        AtomicInteger calls = new AtomicInteger();
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                () -> NotificationBatch.send(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE,
                        page -> calls.incrementAndGet()));
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(0, calls.get());
    }

    @Test
    public void failureOnSecondPageOf256EntriesReportsOnlyOneAcceptedPageAndNeverRetries() {
        Fixture fixture = create("rich");
        List<Encoding> expected = reference(fixture);
        RuntimeException transport = new IllegalStateException("rejected second submission");
        AtomicInteger calls = new AtomicInteger();
        NotificationBatch.SendException failure = assertThrows(NotificationBatch.SendException.class,
                () -> NotificationBatch.send(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE, page -> {
                    if (calls.incrementAndGet() == 2) throw transport;
                    assertEquals(expected.get(0).componentJson, page.componentJson());
                    assertEquals(expected.get(0).vanillaJson, page.vanillaJson());
                }));
        assertEquals(1, failure.sentPages());
        assertEquals(expected.size(), failure.totalPages());
        assertSame(transport, failure.getCause());
        assertEquals(2, calls.get());
    }

    @Test
    public void explicitRegistriesMatchLegacyForPlainPagesAndNestedNativeItemHover() {
        assertNull(xin.vanilla.banira.internal.common.ClientRuntimeBridge.localPlayer());
        assertNull(xin.vanilla.banira.internal.common.BaniraServerRuntime.server());
        var enchantments = new net.minecraft.core.MappedRegistry<net.minecraft.world.item.enchantment.Enchantment>(
                net.minecraft.core.registries.Registries.ENCHANTMENT, com.mojang.serialization.Lifecycle.stable());
        var enchantmentId = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                "banira_codex", "notification_preparation_context");
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
        var original = net.minecraft.network.chat.Component.literal("registry-bound reward")
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withHoverEvent(
                        new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(item))));
        String originalBefore = NativeComponentJson.toJson(original, registries);
        for (boolean nativeHover : new boolean[]{false, true}) {
            Fixture seed = create("literal");
            Fixture fixture = new Fixture("explicit-context", seed.prefix, seed.entries, seed.separator, registries);
            if (nativeHover) {
                Component nested = text("nested-original");
                nested.getChildren().add(BaniraComponent.get().object(original));
                nested.getArgs().add(BaniraComponent.get().object(original));
                fixture.entries.set(200, nested);
            }
            List<String> before = fixture.inputSnapshot();
            List<Encoding> expected = reference(fixture);
            List<Encoding> actual = production(fixture);
            assertEquals(expected, actual);
            assertEquals(before, fixture.inputSnapshot());
            boolean foundEnchantment = false;
            for (Encoding encoding : actual) {
                assertWireRoundTrip(encoding, registries);
                foundEnchantment |= encoding.componentJson.contains(enchantmentId.toString());
            }
            assertEquals(nativeHover, foundEnchantment);
        }
        assertEquals(originalBefore, NativeComponentJson.toJson(original, registries));
    }

    @Test
    public void plainNativeStreamOverflowSubmitsNothingWhenBothJsonBudgetsFit() {
        Component oversized = text(repeat("\ud83c\udf38", 2800));
        assertTrue(oversized.toJson(RegistryAccess.EMPTY).toString().getBytes(StandardCharsets.UTF_8).length < 16384);
        assertTrue(NativeComponentJson.toJson(oversized.toChat(LANGUAGE), RegistryAccess.EMPTY)
                .getBytes(StandardCharsets.UTF_8).length < 16384);
        Fixture fixture = create("literal");
        fixture.entries.set(255, oversized);
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> reference(fixture));
        AtomicInteger calls = new AtomicInteger();
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                () -> NotificationBatch.send(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE,
                        fixture.registries, page -> calls.incrementAndGet()));
        assertEquals(expected.getMessage(), actual.getMessage());
        assertTrue(actual.getMessage().contains("entry 255"));
        assertEquals("Native component encoding exceeds 16384 bytes", expected.getCause().getMessage());
        assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
        assertEquals(0, calls.get());
    }

    @Test
    public void nullRegistryContextRetainsLegacyFailureWithoutSending() {
        for (String name : new String[]{"literal", "native"}) {
            Fixture seed = create(name);
            Fixture fixture = new Fixture(name, seed.prefix, seed.entries, seed.separator, null);
            NullPointerException expected = assertThrows(NullPointerException.class, () -> reference(fixture));
            AtomicInteger calls = new AtomicInteger();
            NullPointerException actual = assertThrows(NullPointerException.class,
                    () -> NotificationBatch.send(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE,
                            fixture.registries, page -> calls.incrementAndGet()));
            assertEquals("registries", expected.getMessage());
            assertEquals(expected.getMessage(), actual.getMessage());
            assertEquals(0, calls.get());
        }
    }

    private static Fixture dynamicFixture() {
        Fixture fixture = create("rich");
        AtomicInteger languageCalls = new AtomicInteger();
        fixture.entries.get(0).languageCode(() -> languageCalls.incrementAndGet() % 2 == 0 ? "fr_fr" : "ja_jp");
        return fixture;
    }

    private static String supplierLanguage(AtomicInteger calls, boolean changing) {
        int value = calls.incrementAndGet();
        return changing && value % 2 == 0 ? "fr_fr" : LANGUAGE;
    }

    private static Fixture translatedPrefix(String id) {
        Fixture fixture = create("literal");
        fixture.prefix.i18nType(EnumI18nType.FORMAT).modId(id).text("prefix");
        return fixture;
    }

    private static final class ChangingTranslator extends Translator {
        final AtomicInteger calls = new AtomicInteger();

        ChangingTranslator(String id) {
            super(id, ChangingTranslator.class);
            registerInCache();
        }

        @Override
        public String getTranslation(String key, String language) {
            return "revision-" + calls.incrementAndGet() + ": ";
        }
    }

    private static final class ObservedList extends ArrayList<Component> {
        int gets;
        int iterators;

        ObservedList(List<Component> values) {
            super(values);
        }

        @Override
        public Component get(int index) {
            gets++;
            return super.get(index);
        }

        @Override
        public Iterator<Component> iterator() {
            iterators++;
            return super.iterator();
        }
    }

    private static void assertWireRoundTrip(Encoding encoding) {
        assertWireRoundTrip(encoding, RegistryAccess.EMPTY);
    }

    private static void assertWireRoundTrip(Encoding encoding, RegistryAccess registries) {
        assertTrue(encoding.componentJson.getBytes(StandardCharsets.UTF_8).length <= 16384);
        assertTrue(encoding.vanillaJson.getBytes(StandardCharsets.UTF_8).length <= 16384);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf(encoding.componentJson, 16384);
            assertEquals(encoding.componentJson, buffer.readUtf(16384));
            assertEquals(0, buffer.readableBytes());
            buffer.clear();
            RegistryFriendlyByteBuf nativeBuffer = new RegistryFriendlyByteBuf(buffer, registries);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(nativeBuffer,
                    NativeComponentJson.fromJson(encoding.vanillaJson, registries));
            assertTrue(nativeBuffer.readableBytes() <= 16384);
            assertEquals(encoding.vanillaJson, NativeComponentJson.toJson(
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(nativeBuffer), registries));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
