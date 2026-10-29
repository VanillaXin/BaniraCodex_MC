package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiPressActionsTest {
    @Test
    public void primaryCommandsIgnoreSecondaryClicksButRetainKeyboardActivation() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        var view = dom.view();
        AtomicInteger count = new AtomicInteger();
        view.onAction("submit", count::incrementAndGet);
        dom.ids.get("submit").emit("click", 1);
        assertEquals("A secondary click must not send a primary business command", 0, count.get());
        dom.ids.get("submit").emit("click", 0);
        dom.ids.get("submit").emit("click", -1);
        assertEquals(2, count.get());
        view.close();
    }

    @Test
    public void dateRowCommandWithoutInputIsARealSingleActionButton() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        var view = dom.view();
        AtomicInteger count = new AtomicInteger();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "birthday:0";
            }

            public void bind(UiView row, int index) {
                row.text("apply", xin.vanilla.banira.BaniraComponent.get().literal("10/9"));
                row.onAction("apply", count::incrementAndGet);
            }
        }, 120);
        var button = dom.created.stream().filter(node -> node.tag.equals("button") && node.text.equals("10/9")).findFirst().orElseThrow();
        assertEquals("row-action", button.attributes.get("class"));
        assertNull(button.gameElement);
        button.emit("click", 0);
        assertEquals(1, count.get());
        view.close();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private UiDomView view(UiDomControlsTest.Dom dom, AtomicLong clock) throws Exception {
        assertTrue("AUI press actions need the existing gesture clock boundary", Arrays.stream(UiDomView.class.getConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 3));
        return UiDomView.class.getConstructor(Object.class, UiDomAccess.class, LongSupplier.class)
                .newInstance(new Object(), dom.access, (LongSupplier) clock::get);
    }

    @Test
    public void longPressFiresOnceAndSuppressesReleaseClick() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        AtomicLong clock = new AtomicLong();
        UiDomView view = view(dom, clock);
        AtomicInteger clicks = new AtomicInteger(), holds = new AtomicInteger();
        view.onAction("submit", clicks::incrementAndGet);
        call(view, "onLongAction", new Class<?>[]{String.class, Runnable.class}, "submit", (Runnable) holds::incrementAndGet);
        dom.ids.get("submit").emit("mousedown");
        clock.set(1999);
        view.afterLayout();
        assertEquals(0, holds.get());
        clock.set(2000);
        view.afterLayout();
        view.afterLayout();
        assertEquals(1, holds.get());
        dom.ids.get("submit").emit("mouseup");
        dom.ids.get("submit").emit("click");
        assertEquals(0, clicks.get());
        dom.ids.get("submit").emit("mousedown");
        clock.set(2100);
        dom.ids.get("submit").emit("mouseup");
        dom.ids.get("submit").emit("click");
        assertEquals(1, clicks.get());
    }

    @Test
    public void disabledLeaveSecondaryButtonAndDisposedViewNeverHold() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        AtomicLong clock = new AtomicLong();
        UiDomView view = view(dom, clock);
        AtomicInteger holds = new AtomicInteger();
        call(view, "onLongAction", new Class<?>[]{String.class, Runnable.class}, "submit", (Runnable) holds::incrementAndGet);
        dom.ids.get("submit").emit("mousedown", 1);
        clock.addAndGet(3000);
        view.afterLayout();
        dom.ids.get("submit").emit("mousedown");
        dom.ids.get("submit").emit("mouseleave");
        clock.addAndGet(3000);
        view.afterLayout();
        dom.ids.get("submit").emit("mousedown");
        view.enabled("submit", false);
        clock.addAndGet(3000);
        view.afterLayout();
        view.enabled("submit", true);
        dom.ids.get("submit").emit("mousedown");
        view.close();
        clock.addAndGet(3000);
        view.afterLayout();
        assertEquals(0, holds.get());
    }

    @Test
    public void rowCommandsAreActualButtonsBesideTheEditor() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        var view = dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "config:name";
            }

            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 64, "", false));
                row.draft("input", "name");
                row.text("reset", xin.vanilla.banira.BaniraComponent.get().literal("R"));
                row.onAction("reset", () -> {
                });
            }
        }, 120);
        assertTrue("Reset must be a named, keyboard-accessible icon button", dom.created.stream().anyMatch(node -> node.tag.equals("button")
                && "R".equals(node.attributes.get("aria-label")) && node.text.isEmpty() && node.gameElement != null));
        view.close();
    }
}
