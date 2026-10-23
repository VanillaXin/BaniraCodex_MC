package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.platform.*;

import java.util.*;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiConfigPresentationTest {
    @org.junit.BeforeClass public static void bootstrap() { UiDomControlsTest.bootstrap(); }
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }

    private UiConfigSessionTest.Fixture fixture() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("enabled", ConfigValueType.BOOLEAN, true);
        f.field("group.number", ConfigValueType.INTEGER, 2);
        f.field("group.child.name", ConfigValueType.STRING, "initial");
        f.field("other.names", ConfigValueType.STRING_LIST, List.of("first", "second"));
        f.open(); return f;
    }

    @Test public void categoryNavigationUsesTheActualTreeAndRetainsDrafts() throws Exception {
        var f = fixture(); var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        UiRowProvider categories = view.rowProviders.get("categories");
        assertNotNull("The approved sidebar must be backed by the holder tree", categories);
        assertEquals(5, categories.size());
        assertEquals("category:group", categories.key(2));
        f.draft("group.number", "-");
        NativeUiView category = new NativeUiView(new Object()); categories.bind(category, 2); category.activate("open");
        UiRowProvider fields = view.rowProviders.get("fields");
        assertEquals(2, fields.size());
        assertEquals("config:group.number", fields.key(0));
        assertEquals("-", call(f.session, "draft", new Class<?>[]{String.class}, "group.number"));
        assertEquals(false, call(f.session, "save"));
    }

    @Test public void searchSpansCategoriesAndClearReturnsToTheSelectedCategory() throws Exception {
        var f = fixture(); var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        assertNotNull("Search must be paired with the real category navigation", view.rowProviders.get("categories"));
        NativeUiView category = new NativeUiView(new Object()); view.rowProviders.get("categories").bind(category, 2); category.activate("open");
        view.inputDraft("search", "other.names");
        assertEquals("config:other.names", view.rowProviders.get("fields").key(0));
        view.activate("clear_search");
        assertEquals("", call(f.session, "query"));
        assertEquals(2, view.rowProviders.get("fields").size());
        assertEquals("config:group.number", view.rowProviders.get("fields").key(0));
    }

    @Test public void sidebarScrollCannotChangeTheFieldWindowOrPagePosition() throws Exception {
        var f = fixture(); var dom = new UiDomControlsTest.Dom();
        dom.ids.put("categories", dom.new Node("categories"));
        dom.ids.put("category_content", dom.new Node("category_content"));
        NativeUiView view = dom.view(); call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        assertNotNull(view.rowProviders.get("categories"));
        view.state().rowPosition(3);
        dom.ids.get("category_content").scroll = 40;
        dom.ids.get("category_content").emit("scroll");
        assertEquals(3, view.state().rowPosition(), 0);
        assertEquals(0, dom.ids.get("content").scroll, 0);
        assertTrue(dom.created.stream().anyMatch(n -> "height:28px;flex-shrink:0".equals(n.attributes.get("style"))));
        assertTrue(dom.created.stream().anyMatch(n -> "height:20px;flex-shrink:0".equals(n.attributes.get("style"))));
    }

    @Test public void explicitPullAndFullSyncUseTheExistingServicesWithoutCleaningQueuedDrafts() throws Exception {
        var f = fixture(); var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        view.activate("fetch"); assertEquals(1, f.fetches.get());
        f.draft("group.number", "5"); view.activate("sync_all");
        assertEquals(1, f.sent.size()); assertEquals(4, f.sent.get(0).size());
        assertEquals(1, ((xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state")).pendingChangeCount());
    }
}
