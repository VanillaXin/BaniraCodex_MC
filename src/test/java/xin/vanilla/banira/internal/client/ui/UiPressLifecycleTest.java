package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.platform.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiPressLifecycleTest {
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }

    @Test public void longActionCanCloseDocumentWithoutExecutingAnotherPendingAction() throws Exception {
        var dom = new UiDomControlsTest.Dom(); AtomicLong clock = new AtomicLong();
        var view = UiDomView.class.getConstructor(Object.class, UiDomAccess.class, LongSupplier.class)
                .newInstance(new Object(), dom.access, (LongSupplier) clock::get);
        AtomicInteger later = new AtomicInteger();
        call(view, "onLongAction", new Class<?>[]{String.class, Runnable.class}, "submit", (Runnable) view::close);
        call(view, "onLongAction", new Class<?>[]{String.class, Runnable.class}, "cancel", (Runnable) later::incrementAndGet);
        dom.ids.get("submit").emit("mousedown"); dom.ids.get("cancel").emit("mousedown");
        clock.set(2000);
        Throwable escaped = null;
        try { view.afterLayout(); } catch (RuntimeException failure) { escaped = failure; }
        assertNull("Closing a page from its long action must not invalidate the gesture iteration", escaped);
        assertTrue(view.closed()); assertEquals(0, later.get());
    }

    @Test public void recycledCategoryRowDoesNotKeepAnInvisibleEditorHitTarget() throws Exception {
        var dom = new UiDomControlsTest.Dom(); var view = (UiDomView) dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() { return 2; }
            public String key(int index) { return "row:" + index; }
            public void bind(UiView row, int index) {
                if (index == 0) {
                    row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 64, "", false));
                    row.draft("input", "old");
                } else {
                    row.text("open", xin.vanilla.banira.BaniraComponent.get().literal("Group"));
                    row.onAction("open", () -> { });
                }
            }
        }, 1);
        view.afterLayout();
        var editor = dom.created.stream().filter(node -> "editor".equals(node.attributes.get("class"))).findFirst().orElse(null);
        assertNotNull(editor);
        dom.ids.get("content").scroll = 48; dom.ids.get("content").emit("scroll");
        assertEquals("display:none", editor.attributes.get("style"));
        view.close();
    }
}
