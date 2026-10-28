package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.internal.client.ui.page.NavigationUiSession.Action;
import xin.vanilla.banira.internal.client.ui.page.QuickActionEditUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiQuickLifecycleTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void inlineIconSelectionUsesOwnedStructuredCallback() throws Exception {
        var session = new QuickActionEditUiSession(xin.vanilla.banira.BaniraComponent.get().literal("Quick"), Collections::emptyList, Collections.emptyList());
        assertTrue("AUI icon source must not open a native-only popup", Arrays.stream(session.getClass().getMethods()).anyMatch(method -> method.getName().equals("icons")));
        AtomicInteger selected = new AtomicInteger();
        call(session, "icons", new Class<?>[]{List.class, Supplier.class, Consumer.class}, Arrays.asList(new DropdownOption("ITEM"), new DropdownOption("EFFECT")),
                (Supplier<String>) () -> "ITEM", (Consumer<String>) value -> selected.incrementAndGet());
        NativeUiView view = new NativeUiView(new Object());
        session.bind(view);
        view.input("icon", UiValue.text("EFFECT"));
        assertEquals(1, selected.get());
        session.detach(UiDetachReason.SWITCH);
        view.input("icon", UiValue.text("ITEM"));
        assertEquals(1, selected.get());
    }

    @Test
    public void refreshRebindsRowsWithoutKeepingOldDeleteCallback() throws Exception {
        List<Action> rows = new ArrayList<>();
        AtomicInteger deletes = new AtomicInteger();
        var session = new QuickActionEditUiSession(xin.vanilla.banira.BaniraComponent.get().literal("Quick"), () -> rows, Collections.emptyList());
        rows.add(new Action("one", xin.vanilla.banira.BaniraComponent.get().literal("One"), () -> {
        }, deletes::incrementAndGet));
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        session.bind(root);
        root.rowProviders.get("fields").bind(row, 0);
        assertTrue("In-place changes must reuse the current session", Arrays.stream(session.getClass().getMethods()).anyMatch(method -> method.getName().equals("refresh")));
        rows.clear();
        call(session, "refresh");
        row.activate("delete");
        assertEquals(0, deletes.get());
        assertEquals(0, root.rowProviders.get("fields").size());
    }

    @Test
    public void absentMenuCommandsAreNotBlankActiveButtons() {
        var session = new QuickActionEditUiSession(xin.vanilla.banira.BaniraComponent.get().literal("Menu"), Collections::emptyList,
                Collections.singletonList(new Action("details", xin.vanilla.banira.BaniraComponent.get().literal("Details"), () -> {
                }, null)));
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        assertEquals(Boolean.FALSE, root.enabled.get("add_menu"));
        assertEquals(Boolean.FALSE, root.enabled.get("icon"));
    }
}
