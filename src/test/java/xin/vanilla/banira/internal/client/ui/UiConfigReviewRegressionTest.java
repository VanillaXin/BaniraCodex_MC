package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigReviewRegressionTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void displayedShortTitlesAreSearchable() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.modId = "banira_codex";
        f.field("guiNightMode", ConfigValueType.STRING, "DARK");
        f.open();
        lombok.var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        lombok.var row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 0);
        String label = row.texts.get("label").getString(xin.vanilla.banira.common.util.Translator.getClientLanguage());
        assertFalse(label.contains("guiNightMode"));
        assertEquals(com.google.common.collect.ImmutableList.of("guiNightMode"), call(f.session, "search", new Class<?>[]{String.class}, label));
    }

    @Test
    public void selectingLowerCategoriesKeepsIndependentSidebarScrollAfterLayout() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        for (int i = 0; i < 160; i++) f.field("group" + i + ".value", ConfigValueType.STRING, "value");
        f.open();
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("categories", dom.new Node("categories"));
        dom.ids.put("category_content", dom.new Node("category_content"));
        lombok.var view = dom.view();
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        call(view, "afterLayout");
        dom.ids.get("category_content").scroll = 2200;
        dom.ids.get("category_content").emit("scroll");
        dom.created.stream().filter(n -> "group110".equals(n.text)).findFirst().orElseThrow(java.util.NoSuchElementException::new).emit("click");
        call(view, "afterLayout");
        assertEquals(2200, dom.ids.get("category_content").scroll, 0);
        assertEquals(0, view.state().rowPosition(), 0);
    }

    @Test
    public void selectingNestedCategoryExpandsNativeAncestors() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.field("group.parent", ConfigValueType.STRING, "value");
        f.field("group.child.value", ConfigValueType.STRING, "value");
        f.open();
        lombok.var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, view);
        UiRowProvider categories = view.rowProviders.get("categories");
        int index = -1;
        for (int i = 0; i < categories.size(); i++) if (categories.key(i).equals("category:group.child")) index = i;
        assertTrue(index >= 0);
        lombok.var row = new NativeUiView(new Object());
        categories.bind(row, index);
        row.activate("open");
        Map<?, ?> expansion = (Map<?, ?>) call(f.session, "categoryExpansion");
        assertEquals(Boolean.TRUE, expansion.get("group.child"));
        assertEquals(Boolean.TRUE, expansion.get("group"));
    }
}
