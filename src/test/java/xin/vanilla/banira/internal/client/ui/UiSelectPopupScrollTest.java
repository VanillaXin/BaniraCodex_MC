package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class UiSelectPopupScrollTest {
    @BeforeClass public static void bootstrap() { UiDomControlsTest.bootstrap(); }
    @Before public void platform() { new UiDomControlsTest().platform(); }

    private static final class Fixture {
        final UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        final UiDomView view = (UiDomView) dom.view();
        final AtomicReference<UiValue> selected = new AtomicReference<>();
        final AtomicInteger callbacks = new AtomicInteger();

        Fixture() throws Exception {
            List<DropdownOption> options = new ArrayList<>();
            for (int index = 0; index < 8; index++) options.add(new DropdownOption("value:" + index));
            view.control("amount", new UiControlSpec(UiControlSpec.Kind.SELECT, 256, "", false,
                    options, DropdownInputMode.SELECTION_ONLY));
            view.onValue("amount", value -> { selected.set(value); callbacks.incrementAndGet(); });
            dom.ids.get("amount").emit("click");
            assertNotNull(node("popup-options"));
        }

        UiDomControlsTest.Dom.Node node(String css) {
            return dom.created.stream().filter(node -> css.equals(node.attributes.get("class"))).findFirst().orElse(null);
        }

        List<UiDomControlsTest.Dom.Node> rows() {
            return dom.created.stream().filter(node -> "popup-option".equals(node.attributes.get("class"))
                    && !node.removed && !"display:none".equals(node.attributes.get("style"))).toList();
        }

        int writes() { return dom.created.stream().mapToInt(node -> node.writes).sum(); }
    }

    @Test public void postLayoutRealizesTailAfterSilentSmoothScrollWithoutReallocatingRows() throws Exception {
        Fixture fixture = new Fixture();
        assertFalse(fixture.rows().stream().anyMatch(node -> "value:7".equals(node.text)));
        int allocated = fixture.dom.created.size();
        fixture.node("popup-options").scroll = 71.2;
        fixture.view.afterLayout();
        UiDomControlsTest.Dom.Node tail = fixture.rows().stream().filter(node -> "value:7".equals(node.text)).findFirst().orElse(null);
        assertNotNull("A completed scroll must realize the last option without a second input event", tail);
        assertEquals(allocated, fixture.dom.created.size());
        int writes = fixture.writes();
        fixture.view.afterLayout(); fixture.view.afterLayout();
        assertEquals("Unchanged viewport must not rewrite option DOM each frame", writes, fixture.writes());
        tail.emit("click"); tail.emit("click");
        assertEquals("value:7", fixture.selected.get().asText());
        assertEquals(1, fixture.callbacks.get());
        fixture.view.afterLayout(); fixture.view.close();
        assertTrue(fixture.node("popup-options").removed);
    }

    @Test public void postLayoutRefreshesInitialAndResizedViewportWithoutScrollEvents() throws Exception {
        Fixture fixture = new Fixture();
        UiDomControlsTest.Dom.Node viewport = fixture.node("popup-options");
        viewport.height = 0;
        fixture.node("popup-search").emit("input");
        assertEquals(2, fixture.rows().size());
        fixture.view.afterLayout();
        assertEquals(2, fixture.rows().size());
        viewport.height = 44;
        fixture.view.afterLayout();
        assertEquals("First measurable viewport must realize its visible rows", 4, fixture.rows().size());
        viewport.height = 110;
        fixture.view.afterLayout();
        assertEquals("Growing viewport must refresh its row window", 7, fixture.rows().size());
        fixture.view.close();
        int writes = fixture.writes();
        fixture.view.afterLayout();
        assertEquals(writes, fixture.writes());
        assertEquals(0, fixture.callbacks.get());
    }
}
