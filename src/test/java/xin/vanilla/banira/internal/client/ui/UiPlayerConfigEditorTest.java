package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiSession;
import xin.vanilla.banira.platform.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiPlayerConfigEditorTest {
    @BeforeClass public static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }
    private InputFormScreen.Args args(AtomicInteger saves) {
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) { })
                .setTitle(Text.literal("Player config"))
                .setCallback(result -> saves.incrementAndGet());
        for (int i = 0; i < 16; i++) args.addWidget(new InputFormScreen.Widget().name("FIELD_" + i)
                .title(Text.literal("Teleport type " + i)).tooltip(Text.literal("Complete description " + i))
                .type(InputFormScreen.WidgetType.NUMERIC).defaultValue("0").validator(result -> {
                    try { int n = Integer.parseInt(result.value(result.curName())); return n >= 0 && n <= 300 ? "" : "Invalid integer"; }
                    catch (NumberFormatException invalid) { return "Invalid integer"; }
                }));
        return args;
    }
    private PlayerConfigUiSession styled(AtomicInteger saves) throws Exception {
        return styled(args(saves));
    }
    private PlayerConfigUiSession styled(InputFormScreen.Args args) throws Exception {
        PlayerConfigUiSession session = new PlayerConfigUiSession(args);
        Class<?> presentation = type("internal.client.ui.page.PlayerConfigUiPresentation");
        Object metadata = presentation.getConstructor(String.class, Component.class, Component.class, Component.class, Supplier.class)
                .newInstance("narcissus_farewell", BaniraComponent.get().literal("NARCISSUS FAREWELL"), BaniraComponent.get().literal("Countdown"), BaniraComponent.get().literal("s"),
                        (Supplier<Component>) () -> BaniraComponent.get().literal("0-300 s"));
        call(session, "presentation", new Class<?>[]{presentation}, metadata);
        return session;
    }
    @Test public void singleRealSectionAndFilteredCountsKeepRawDrafts() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger());
        NativeUiView root = new NativeUiView(new Object()); session.bind(root);
        assertEquals(1, root.rowProviders.get("categories").size());
        assertEquals(16, root.rowProviders.get("fields").size());
        assertEquals("16 / 16", root.texts.get("entry_count").text());
        session.fields().draft("FIELD_0", "01"); root.inputDraft("search", "FIELD_15");
        assertEquals(1, root.rowProviders.get("fields").size()); assertEquals("01", session.fields().draft("FIELD_0"));
        root.activate("clear_search"); assertEquals("", session.query()); assertEquals(16, root.rowProviders.get("fields").size());
    }
    @Test public void recycledRowsUseOriginalFieldIdentityAndReleaseOldSubscriptions() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); NativeUiView root = new NativeUiView(new Object()); session.bind(root);
        NativeUiView old = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(old, 0);
        root.inputDraft("search", "FIELD_15"); NativeUiView row = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(row, 0);
        assertEquals("FIELD_15", row.texts.get("key").text()); assertEquals("s", row.texts.get("field_unit").text());
        old.inputDraft("input", "8"); row.inputDraft("input", "09");
        assertEquals("0", session.fields().draft("FIELD_0")); assertEquals("09", session.fields().draft("FIELD_15"));
        session.close(UiCloseReason.DISCONNECT); assertEquals(0, row.subscriptionCount()); assertEquals(0, root.subscriptionCount());
    }
    @Test public void categoryReturnRetainsQueryAndDraftButClearsStaleFocusAndScroll() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); NativeUiView root = new NativeUiView(new Object()); session.bind(root);
        root.inputDraft("search", "FIELD_1"); session.fields().draft("FIELD_15", "8"); root.state().rowPosition(9); root.state().selection("field:15:input", 1, 1);
        NativeUiView category = new NativeUiView(new Object()); root.rowProviders.get("categories").bind(category, 0); category.activate("open");
        assertEquals("FIELD_1", session.query()); assertEquals("8", session.fields().draft("FIELD_15"));
        assertEquals(0, root.state().rowPosition(), 0); assertEquals("", root.state().focusedField());
    }
    @Test public void hiddenInvalidDraftCannotSaveOrBeRoundedAndSuccessSendsOnce() throws Exception {
        AtomicInteger saves = new AtomicInteger(); PlayerConfigUiSession session = styled(saves); NativeUiView root = new NativeUiView(new Object()); session.bind(root);
        session.fields().draft("FIELD_0", "1.5"); root.inputDraft("search", "FIELD_15"); root.activate("submit");
        assertEquals(0, saves.get()); assertEquals("1.5", session.fields().draft("FIELD_0"));
        session.fields().draft("FIELD_0", "01"); session.tick(); root.activate("submit"); root.activate("submit"); assertEquals(1, saves.get());
    }
    @Test public void basicNumericFailureHasFieldErrorWithoutChangingRawDraft() {
        PlayerConfigUiSession session = new PlayerConfigUiSession(args(new AtomicInteger()));
        session.fields().draft("FIELD_0", "-"); assertFalse(session.fields().canSubmit());
        assertTrue("Invalid base numeric input must have field error feedback", session.fields().errors().containsKey(0));
        assertEquals("-", session.fields().draft("FIELD_0"));
    }
    @Test public void unchangedAndRestoredDraftStatusDoesNotCountEditEvents() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); NativeUiView root = new NativeUiView(new Object()); session.bind(root);
        String clean = root.texts.get("status").text(); NativeUiView row = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(row, 0);
        row.inputDraft("input", "7"); session.tick(); assertNotEquals(clean, root.texts.get("status").text());
        row.inputDraft("input", "0"); session.tick(); assertEquals(clean, root.texts.get("status").text()); assertTrue(session.fields().canSubmit());
    }
    @Test public void detachedRootCannotClearQueryOrCloseTheReplacement() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); NativeUiView old = new NativeUiView(new Object()); session.bind(old);
        session.detach(UiDetachReason.SWITCH); NativeUiView next = new NativeUiView(new Object()); AtomicInteger closes = new AtomicInteger(); next.onClose(closes::incrementAndGet); session.bind(next);
        next.inputDraft("search", "FIELD_1"); old.activate("clear_search"); old.activate("close");
        assertEquals("FIELD_1", session.query()); assertEquals(0, closes.get()); next.activate("close"); assertEquals(1, closes.get());
    }
    @Test public void optionalPresentationCannotBeReplacedAfterBinding() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); session.bind(new NativeUiView(new Object()));
        try { call(session, "presentation", new Class<?>[]{type("internal.client.ui.page.PlayerConfigUiPresentation")}, (Object) null); fail("Bound presentation changed"); }
        catch (java.lang.reflect.InvocationTargetException expected) { assertTrue(expected.getCause() instanceof IllegalStateException); }
    }
    @Test public void sendFailureShowsFeedbackAndRetryClosesExactlyOnce() throws Exception {
        var failing = new java.util.concurrent.atomic.AtomicBoolean(true); AtomicInteger sends = new AtomicInteger(), closes = new AtomicInteger();
        PlayerConfigUiSession session = styled(args(sends).setCallback(result -> {
            if (failing.get()) throw new IllegalStateException("send failed"); sends.incrementAndGet();
        }));
        NativeUiView root = new NativeUiView(new Object()); root.onClose(closes::incrementAndGet); session.bind(root);
        session.fields().draft("FIELD_0", "07"); session.tick(); root.activate("submit"); session.tick();
        assertEquals("07", session.fields().draft("FIELD_0")); assertEquals(0, sends.get()); assertEquals(0, closes.get());
        assertFalse(root.texts.get("error").text().isEmpty()); assertTrue("Failed send feedback remains hidden", root.enabled.get("error"));
        failing.set(false); root.activate("submit"); root.activate("submit"); assertEquals(1, sends.get()); assertEquals(1, closes.get());
    }
    @Test public void actualDomDeletionAndRecycledInputsRetainRawIdentityAndUnits() throws Exception {
        PlayerConfigUiSession session = styled(new AtomicInteger()); UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        for (String id : new String[]{"search", "categories", "category_content", "close", "clear_search", "error", "status"}) dom.ids.put(id, dom.new Node(id));
        UiDomView root = (UiDomView) dom.view(); session.bind(root); root.afterLayout();
        UiDomControlsTest.Dom.Node input = dom.created.stream().filter(n -> !n.removed && n.tag.equals("input") && n.value.equals("0")).findFirst().orElseThrow();
        input.focused = true; input.start = input.end = 2; input.value = "01"; input.emit("input");
        assertEquals("01", session.fields().draft("FIELD_0")); assertTrue(input.focused);
        input.value = "0"; input.key("keyup", 259); assertEquals("0", session.fields().draft("FIELD_0"));
        assertTrue(dom.created.stream().anyMatch(n -> "paragraph field-unit".equals(n.attributes.get("class")) && n.text.equals("s")));
        dom.ids.get("search").value = "FIELD_15"; dom.ids.get("search").emit("input"); root.afterLayout();
        UiDomControlsTest.Dom.Node next = dom.created.stream().filter(n -> !n.removed && n.tag.equals("input") && n.value.equals("0")).findFirst().orElseThrow();
        input.value = "8"; input.emit("input"); next.value = "09"; next.emit("input");
        assertEquals("0", session.fields().draft("FIELD_0")); assertEquals("09", session.fields().draft("FIELD_15"));
        session.close(UiCloseReason.DISCONNECT); root.close(); assertEquals(0, root.subscriptionCount());
    }
}
