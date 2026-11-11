package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.internal.client.ui.page.NavigationUiSession;
import xin.vanilla.banira.internal.client.ui.page.QuickActionEditUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiQuickActionPresentationTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private NavigationUiSession.Action action(String id, String label, Runnable open, Runnable delete) {
        return new NavigationUiSession.Action(id, BaniraComponent.get().literal(label), open, delete);
    }


    @Test
    public void compactRowsKeepHumanLabelsAndSingleRealCallbacks() {
        AtomicInteger open = new AtomicInteger(), delete = new AtomicInteger();
        lombok.var s = new QuickActionEditUiSession(BaniraComponent.get().literal("Shortcuts"), () -> com.google.common.collect.ImmutableList.of(action("custom:alpha", "Alpha", open::incrementAndGet, delete::incrementAndGet)), com.google.common.collect.ImmutableList.of());
        lombok.var root = new NativeUiView(new Object());
        s.bind(root);
        assertTrue(root.rowProviders.get("fields") instanceof UiRowPresentation);
        assertEquals(24, ((UiRowPresentation) root.rowProviders.get("fields")).rowHeight());
        lombok.var row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 0);
        assertEquals("Alpha", row.texts.get("open").toString());
        assertNotNull(row.tooltips.get("open"));
        row.activate("open");
        row.activate("delete");
        assertEquals(1, open.get());
        assertEquals(1, delete.get());
    }

    @Test
    public void searchKeepsOriginalActionIdentityAndRetiresOldRows() {
        AtomicInteger a = new AtomicInteger(), b = new AtomicInteger();
        lombok.var s = new QuickActionEditUiSession(BaniraComponent.get().literal("Shortcuts"), () -> com.google.common.collect.ImmutableList.of(action("a", "Alpha", a::incrementAndGet, null), action("b", "Beta", b::incrementAndGet, null)), com.google.common.collect.ImmutableList.of());
        lombok.var root = new NativeUiView(new Object());
        s.bind(root);
        lombok.var old = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(old, 0);
        assertTrue(root.controls.containsKey("search"));
        root.state().rowPosition(8);
        root.inputDraft("search", "Beta");
        assertEquals(1, root.rowProviders.get("fields").size());
        assertEquals("b", root.rowProviders.get("fields").key(0));
        assertEquals(0, root.state().rowPosition(), 0);
        old.activate("open");
        assertEquals(0, a.get());
        lombok.var filtered = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(filtered, 0);
        filtered.activate("open");
        assertEquals(1, b.get());
        root.activate("clear_search");
        assertEquals(2, root.rowProviders.get("fields").size());
    }

    @Test
    public void emptySearchAndRebindKeepStateAndOnlyCurrentTools() {
        AtomicInteger saves = new AtomicInteger();
        lombok.var s = new QuickActionEditUiSession(BaniraComponent.get().literal("Steps"), () -> com.google.common.collect.ImmutableList.of(action("step:0", "Command", () -> {
        }, null)), com.google.common.collect.ImmutableList.of(action("save", "Save", saves::incrementAndGet, null)));
        lombok.var first = new NativeUiView(new Object());
        s.bind(first);
        assertTrue(first.controls.containsKey("search"));
        first.inputDraft("search", "missing");
        assertEquals(0, first.rowProviders.get("fields").size());
        assertEquals(Boolean.TRUE, first.enabled.get("empty"));
        lombok.var second = new NativeUiView(new Object());
        s.bind(second);
        assertEquals("missing", second.drafts.get("search"));
        first.activate("save");
        second.activate("save");
        assertEquals(1, saves.get());
        s.close(UiCloseReason.DISCONNECT);
        second.activate("save");
        assertEquals(1, saves.get());
    }

    @Test
    public void baseNavigationUsesCompactRowsAndRetainsActionIdentity() {
        lombok.var s = new NavigationUiSession(BaniraComponent.get().literal("Navigation"), com.google.common.collect.ImmutableList.of(action("a", "Alpha", () -> {
        }, null)));
        lombok.var root = new NativeUiView(new Object());
        s.bind(root);
        assertTrue(root.rowProviders.get("fields") instanceof UiRowPresentation);
        assertEquals(24, ((UiRowPresentation) root.rowProviders.get("fields")).rowHeight());
        assertEquals("a", root.rowProviders.get("fields").key(0));
        assertEquals(1, root.rowProviders.get("fields").size());
    }

    @Test
    public void iconSelectionHasOneCurrentCallbackAfterSearchRefresh() {
        AtomicInteger selected = new AtomicInteger();
        lombok.var s = new QuickActionEditUiSession(BaniraComponent.get().literal("Steps"), () -> com.google.common.collect.ImmutableList.of(action("a", "Alpha", () -> {
        }, null)), com.google.common.collect.ImmutableList.of());
        s.icons(com.google.common.collect.ImmutableList.of(new xin.vanilla.banira.client.gui.widget.DropdownOption("ITEM"), new xin.vanilla.banira.client.gui.widget.DropdownOption("RESOURCE")), () -> "ITEM", value -> selected.incrementAndGet());
        lombok.var root = new NativeUiView(new Object());
        s.bind(root);
        assertTrue(root.controls.containsKey("search"));
        root.inputDraft("search", "Alpha");
        root.input("icon", UiValue.text("RESOURCE"));
        assertEquals(1, selected.get());
        lombok.var next = new NativeUiView(new Object());
        s.bind(next);
        root.input("icon", UiValue.text("ITEM"));
        assertEquals(1, selected.get());
        next.input("icon", UiValue.text("RESOURCE"));
        assertEquals(2, selected.get());
    }

    @Test
    public void bothSearchHintsHaveRealBilingualResources() throws Exception {
        for (String locale : com.google.common.collect.ImmutableList.of("zh_cn", "en_us"))
            try (java.io.InputStream in = getClass().getResourceAsStream("/assets/banira_codex/lang/" + locale + ".json")) {
                assertNotNull(in);
                lombok.var language = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : com.google.common.collect.ImmutableList.of("notification_type_config_search_hint", "custom_quick_action_search_hint")) {
                    String value = language.get("word.banira_codex." + key).getAsString();
                    assertFalse(value.trim().isEmpty());
                    assertFalse(value.contains("banira_codex"));
                    assertFalse(value.equals(key));
                }
            }
    }


    @Test
    public void typingSearchDoesNotRestoreThePreviousDomCaret() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var search = dom.new Node("input");
        dom.ids.put("search", search);
        lombok.var s = new QuickActionEditUiSession(BaniraComponent.get().literal("Shortcuts"), () -> com.google.common.collect.ImmutableList.of(action("a", "Alpha", () -> {
        }, null), action("b", "Beta", () -> {
        }, null)), com.google.common.collect.ImmutableList.of());
        lombok.var view = dom.view();
        s.bind(view);
        view.state().selection("search", 0, 0);
        search.focused = true;
        search.value = "B";
        search.start = 1;
        search.end = 1;
        search.emit("input");
        assertEquals("B", s.query());
        assertEquals("B", search.value);
        assertEquals(1, search.start);
        assertEquals(1, search.end);
    }
}
