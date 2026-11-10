package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.internal.client.ui.page.FormUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiSelectorParityTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(xin.vanilla.banira.internal.forge.platform.ForgeBaniraRegistryService.INSTANCE));
        xin.vanilla.banira.common.util.ItemUtils.clearCache();
    }

    @After
    public void cleanup() {
        xin.vanilla.banira.common.util.ItemUtils.clearCache();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    static Screen parent() {
        return new Screen(net.minecraft.network.chat.Component.empty()) {
        };
    }

    static Class<?> type() {
        try {
            return Class.forName("xin.vanilla.banira.internal.client.ui.page.SelectorUiSession");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Selectors need a shared session", missing);
        }
    }

    static Object session(Object args) throws Exception {
        return type().getConstructor(args.getClass()).newInstance(args);
    }

    static FormUiSession editor(Object session, String name) throws Exception {
        return (FormUiSession) call(session, "editor", new Class<?>[]{String.class}, name);
    }

    static void draft(FormUiSession form, String text) {
        form.draft(0, text);
    }

    @Test
    public void selectorToolsReuseNativeGameIconsWithoutFontGlyphs() throws Exception {
        Object effect = session(new EffectSelectScreen.Args().parentScreen(parent()).defaultEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0)).onDataReceived1(value -> {
        }));
        NativeUiView view = new NativeUiView(new Object());
        call(effect, "bind", new Class<?>[]{UiView.class}, view);
        assertNotNull("Duration needs a real clock icon", view.gameElements.get("duration"));
        assertEquals(Items.CLOCK, view.gameElements.get("duration").option().icon().getItem());
        assertEquals(Items.ANVIL, view.gameElements.get("amplifier").option().icon().getItem());
        assertEquals(UiGameElement.Kind.EFFECT, view.gameElements.get("effect").kind());
        assertEquals("", view.texts.get("duration").toVanilla().getString());
        assertEquals("", view.texts.get("mode").toVanilla().getString());
        assertFalse(view.tooltips.get("duration").toVanilla().getString().isEmpty());
        call(effect, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.CANCEL);
    }

    @Test
    public void itemDefaultAndCallbackAreCopiesAndSubmitIsSingleShot() throws Exception {
        ItemStack original = new ItemStack(Items.DIAMOND, 7);
        AtomicInteger calls = new AtomicInteger();
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(original).onDataReceived1(stack -> {
            calls.incrementAndGet();
            stack.setCount(30);
        }));
        original.setCount(20);
        ItemStack read = (ItemStack) call(s, "item");
        assertEquals(7, read.getCount());
        read.setCount(1);
        assertEquals(7, ((ItemStack) call(s, "item")).getCount());
        assertEquals(true, call(s, "submit"));
        assertEquals(false, call(s, "submit"));
        assertEquals(1, calls.get());
        assertEquals(7, ((ItemStack) call(s, "item")).getCount());
    }

    @Test
    public void failedCallbackCanRetryAndDoesNotReplaceCallbackNavigation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(new ItemStack(Items.STONE)).closeAfterSubmit(false)
                .onDataReceived2(stack -> calls.incrementAndGet() == 1 ? "rejected" : ""));
        assertEquals(false, call(s, "submit"));
        assertEquals("rejected", call(s, "error"));
        assertEquals(true, call(s, "submit"));
        assertEquals(false, call(s, "closesAfterSubmit"));
        assertEquals(2, calls.get());
    }

    @Test
    public void countAndNbtUseTheSameValidatedForms() throws Exception {
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(new ItemStack(Items.DIAMOND, 3)).onDataReceived1(stack -> {
        }));
        FormUiSession count = editor(s, "count");
        draft(count, "2881");
        assertFalse(count.submit());
        draft(count, "23");
        assertTrue(count.submit());
        assertEquals(23, ((ItemStack) call(s, "item")).getCount());
        FormUiSession nbt = editor(s, "nbt");
        assertTrue(nbt.args().getWidgets().get(0).wideInput());
        var nbtView = new NativeUiView(new Object());
        nbt.bind(nbtView);
        var nbtRows = (UiRowPresentation) nbtView.rowProviders.get("fields");
        assertEquals(48, nbtRows.rowHeight());
        assertEquals("player-config-field wide-input-field", nbtRows.rowClass(0));
        draft(nbt, "{broken");
        assertFalse(nbt.submit());
        draft(nbt, "{Damage:4}");
        assertTrue(nbt.submit());
        ItemStack item = (ItemStack) call(s, "item");
        assertEquals(23, item.getCount());
        assertEquals(4, item.getTag().getInt("Damage"));
        FormUiSession id = editor(s, "item");
        draft(id, "minecraft:iron_sword");
        assertTrue(id.submit());
        item = (ItemStack) call(s, "item");
        assertEquals(Items.IRON_SWORD, item.getItem());
        assertEquals(23, item.getCount());
        assertEquals(4, item.getTag().getInt("Damage"));
    }

    @Test
    public void effectDurationUsesClockInputAndPreservesStoredTickPrecision() throws Exception {
        MobEffectInstance original = new MobEffectInstance(MobEffects.LUCK, 601, 2);
        Object s = session(new EffectSelectScreen.Args().parentScreen(parent()).defaultEffect(original).onDataReceived1(effect -> {
        }));
        FormUiSession duration = editor(s, "duration");
        assertEquals("0:00:30.05", duration.draft(0));
        draft(duration, "0");
        assertFalse(duration.submit());
        draft(duration, "0:01:00.05");
        assertTrue(duration.submit());
        FormUiSession level = editor(s, "amplifier");
        draft(level, "101");
        assertFalse(level.submit());
        draft(level, "4");
        assertTrue(level.submit());
        MobEffectInstance effect = (MobEffectInstance) call(s, "effect");
        assertEquals(1201, effect.getDuration());
        assertEquals(3, effect.getAmplifier());
        FormUiSession id = editor(s, "effect");
        draft(id, "minecraft:strength");
        assertTrue(id.submit());
        effect = (MobEffectInstance) call(s, "effect");
        assertEquals(MobEffects.DAMAGE_BOOST, effect.getEffect());
        assertEquals(1201, effect.getDuration());
        assertEquals(3, effect.getAmplifier());
        assertEquals(601, original.getDuration());
        assertEquals(2, original.getAmplifier());
    }

    @Test
    public void advancementIdIsValidatedAndCallbackRunsOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Object s = session(new AdvancementSelectScreen.Args().parentScreen(parent()).onDataReceived1(id -> calls.incrementAndGet()));
        FormUiSession id = editor(s, "advancement");
        draft(id, "Bad ID");
        assertFalse(id.submit());
        draft(id, "minecraft:story/mine_stone");
        assertTrue(id.submit());
        assertEquals("minecraft:story/mine_stone", call(s, "advancement").toString());
        assertEquals(true, call(s, "submit"));
        call(s, "submit");
        assertEquals(1, calls.get());
    }

    @Test
    public void formDraftAndSelectionSurviveBackendSwitchWithoutCallingResult() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(new ItemStack(Items.STONE, 8)).onDataReceived1(stack -> calls.incrementAndGet()));
        FormUiSession count = editor(s, "count");
        draft(count, "");
        call(s, "query", new Class<?>[]{String.class}, "stone");
        for (int i = 0; i < 4; i++) {
            call(s, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
            call(s, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        }
        assertSame(count, editor(s, "count"));
        assertEquals("", count.draft(0));
        assertEquals("stone", call(s, "query"));
        assertEquals(8, ((ItemStack) call(s, "item")).getCount());
        assertEquals(0, calls.get());
        call(s, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.CANCEL);
        assertEquals(false, call(s, "submit"));
        assertFalse(count.submit());
        assertEquals(0, calls.get());
    }

    @Test
    public void recycledRowsAndDetachedViewsCannotSelectOrSubmit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).onDataReceived1(stack -> calls.incrementAndGet()));
        call(s, "query", new Class<?>[]{String.class}, "stone");
        NativeUiView root = new NativeUiView(new Object());
        call(s, "bind", new Class<?>[]{UiView.class}, root);
        var rows = root.rowProviders.get("fields");
        assertTrue(rows.size() > 1);
        NativeUiView row = new NativeUiView(new Object());
        rows.bind(row, 0);
        String first = rows.key(0);
        rows.bind(row, 1);
        row.activate("open");
        assertNotEquals(first, call(s, "selectedKey"));
        assertEquals(row.texts.get("label").toString(), call(s, "selectedKey"));
        assertFalse(row.tooltips.get("open").toString().isEmpty());
        call(s, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        String selected = (String) call(s, "selectedKey");
        row.activate("open");
        root.activate("submit");
        assertEquals(selected, call(s, "selectedKey"));
        assertEquals(0, calls.get());
        assertEquals(0, root.subscriptionCount());
    }

    @Test
    public void nativeEntriesUseTheSharedRoute() {
        assertTrue(UiNativePage.class.isAssignableFrom(ItemSelectScreen.class));
        assertTrue(UiNativePage.class.isAssignableFrom(EffectSelectScreen.class));
        assertTrue(UiNativePage.class.isAssignableFrom(AdvancementSelectScreen.class));
    }

    @Test
    public void actualNativeFactoryAndChildFormRetainSessionIdentity() throws Exception {
        ItemSelectScreen screen = new ItemSelectScreen(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(new ItemStack(Items.DIAMOND, 9)).onDataReceived1(stack -> {
        }));
        UiPageSpec page = screen.uiPage();
        assertSame(screen.uiSession(), page.session());
        assertSame(screen, page.nativeFactory().create(null));
        screen.uiSession().select("minecraft:iron_sword");
        assertEquals(screen.uiSession().selectedKey(), screen.getSelectedItemId());
        FormUiSession form = screen.uiSession().editor("count");
        form.draft(0, "");
        InputFormScreen child = new InputFormScreen(form);
        assertSame(form, child.uiSession());
        assertSame(screen, child.previousScreen());
        assertEquals("", child.uiSession().draft(0));
        for (BaniraScreen other : new BaniraScreen[]{new EffectSelectScreen(new EffectSelectScreen.Args().parentScreen(parent()).onDataReceived1(effect -> {
        })),
                new AdvancementSelectScreen(new AdvancementSelectScreen.Args().parentScreen(parent()).onDataReceived1(id -> {
                }))}) {
            UiNativePage routed = (UiNativePage) other;
            assertSame(routed.uiSession(), routed.uiPage().session());
            assertSame(other, routed.uiPage().nativeFactory().create(null));
        }
    }

    @Test
    public void realHostReloadAndSwitchKeepParentAndEditorDraftWithoutResult() throws Exception {
        AtomicInteger count = new AtomicInteger();
        var s = (xin.vanilla.banira.internal.client.ui.page.SelectorUiSession) session(new ItemSelectScreen.Args().parentScreen(parent()).defaultItem(new ItemStack(Items.DIAMOND, 8)).onDataReceived1(stack -> count.incrementAndGet()));
        s.query("diamond");
        FormUiSession form = s.editor("count");
        form.draft(0, "9999");
        UiSessionHost host = new UiSessionHost(new UiBackendController(() -> true, () -> true), (page, previous) -> new NativeUiView(new Object()), value -> {
        }, value -> true);
        assertTrue(host.open(new UiPageSpec("banira_codex:item-select", "banira_codex/ui/item-select.html", s, previous -> new Object()), null));
        host.view().state().rowPosition(1.25);
        host.view().state().selection("search", 5, 2);
        UiViewState state = host.view().state();
        for (int i = 0; i < 6; i++) {
            NativeUiView previous = host.view();
            host.switchBackend(i % 2 == 0);
            host.reload();
            host.tick();
            assertEquals(0, previous.subscriptionCount());
            assertSame(state, host.view().state());
        }
        assertEquals("diamond", s.query());
        assertSame(form, s.editor("count"));
        assertEquals("9999", form.draft(0));
        assertEquals(1.25, state.rowPosition(), 0);
        assertEquals(0, count.get());
        host.disconnect();
        assertFalse(form.submit());
        assertEquals(0, count.get());
    }

    @Test
    public void nativeInitializationCannotOverwriteNewBackendQueryWithOldWidget() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object minecraft = unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), net.minecraft.client.Minecraft.class);
        java.lang.reflect.Field instance = null;
        Object previous = null;
        for (var candidate : net.minecraft.client.Minecraft.class.getDeclaredFields())
            if (candidate.getType() == net.minecraft.client.Minecraft.class && java.lang.reflect.Modifier.isStatic(candidate.getModifiers())) {
                instance = candidate;
                instance.setAccessible(true);
                previous = instance.get(null);
                instance.set(null, minecraft);
                break;
            }
        assertNotNull(instance);
        try {
            ItemSelectScreen screen = new ItemSelectScreen(new ItemSelectScreen.Args().parentScreen(parent()).onDataReceived1(stack -> {
            }));
            var input = new xin.vanilla.banira.client.gui.widget.InputWidget(screen);
            input.value("stale");
            var search = ItemSelectScreen.class.getDeclaredField("searchInputWidget");
            search.setAccessible(true);
            search.set(screen, input);
            screen.uiSession().query("diamond");
            screen.prepareUiState(new UiViewState());
            var init = ItemSelectScreen.class.getDeclaredMethod("onInit");
            init.setAccessible(true);
            init.invoke(screen);
            assertEquals("diamond", screen.uiSession().query());
        } finally {
            instance.set(null, previous);
        }
    }

    @Test
    public void actualDomSearchRowsAndIconBindingsKeepFullTooltipAndRejectStaleSubmit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var s = (xin.vanilla.banira.internal.client.ui.page.SelectorUiSession) session(new ItemSelectScreen.Args().parentScreen(parent()).onDataReceived1(stack -> calls.incrementAndGet()));
        var dom = new UiDomControlsTest.Dom();
        for (String id : new String[]{"search", "title", "mode", "selection", "selected_icon", "item", "count", "nbt", "error"})
            dom.ids.put(id, dom.new Node(id));
        UiDomView view = (UiDomView) dom.view();
        s.query("diamond");
        s.bind(view);
        view.afterLayout();
        assertTrue(dom.created.stream().anyMatch(node -> node.gameElement != null && node.gameElement.kind() == UiGameElement.Kind.OPTION));
        assertTrue(dom.created.stream().anyMatch(node -> node.tag.equals("button") && node.tooltip.contains("minecraft:")));
        dom.ids.get("search").value = "iron";
        dom.ids.get("search").emit("input");
        assertEquals("iron", s.query());
        s.detach(UiDetachReason.SWITCH);
        dom.ids.get("submit").emit("click");
        assertEquals(0, calls.get());
        view.close();
        assertEquals(0, view.subscriptionCount());
    }

    @Test
    public void emptySelectionDoesNotExposeTheGlobalEmptyStackForMutation() throws Exception {
        Object s = session(new ItemSelectScreen.Args().parentScreen(parent()).onDataReceived1(stack -> {
        }));
        ItemStack copy = (ItemStack) call(s, "item");
        assertNotSame(ItemStack.EMPTY, copy);
        copy.setCount(10);
        assertTrue(((ItemStack) call(s, "item")).isEmpty());
    }

    @Test
    public void selectorLabelsUseWordTranslationsWithoutFormatArguments() throws Exception {
        var translate = xin.vanilla.banira.internal.client.ui.page.SelectorUiSession.class
                .getDeclaredMethod("tr", String.class, Object[].class);
        translate.setAccessible(true);
        for (String key : new String[]{"search_item", "search_effect", "search_advancement", "submit", "cancel", "enter_item_id"}) {
            var label = (xin.vanilla.banira.common.data.Component) translate.invoke(null, key, new Object[0]);
            assertEquals(key, xin.vanilla.banira.common.enums.EnumI18nType.WORD, label.i18nType());
        }
    }

    @Test
    public void selectorTooltipsKeepFormatTranslationsWithArguments() throws Exception {
        var translate = xin.vanilla.banira.internal.client.ui.page.SelectorUiSession.class
                .getDeclaredMethod("tr", String.class, Object[].class);
        translate.setAccessible(true);
        var label = (xin.vanilla.banira.common.data.Component) translate.invoke(null, "set_quantity", new Object[]{9});
        assertEquals(xin.vanilla.banira.common.enums.EnumI18nType.FORMAT, label.i18nType());
    }
}
