package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.internal.client.ConfigEditorViewportModel;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiViewportInputTest {
    @Test
    public void virtualRowsKeepAllDataAndAtMost120RealizedRows() throws Exception {
        Class<?> window = type("internal.client.ui.UiRowWindow");
        UiRowProvider rows = new UiRowProvider() {
            public int size() { return 5000; }
            public String key(int index) { return "row:" + index; }
            public void bind(UiView row, int index) { }
        };
        Object instance = window.getConstructor(UiRowProvider.class, int.class).newInstance(rows, 120);
        call(instance, "moveTo", new Class<?>[]{int.class}, 4800);
        assertEquals(5000, call(instance, "total"));
        assertEquals(120, call(instance, "realizedCount"));
        assertEquals("row:4800", call(instance, "key", new Class<?>[]{int.class}, 0));
    }

    @Test
    public void viewportHasSeparateFixedHeaderContentAndOverlayHitRegions() throws Exception {
        for (int[] size : new int[][]{{320, 180}, {427, 240}, {640, 360}}) {
            ConfigEditorViewportModel viewport = new ConfigEditorViewportModel(10, 10, 6, 2, 22);
            viewport.resize(size[0], size[1]);
            viewport.layoutContent(6000, size[1] - 85, new ScrollbarWidget(null));
            viewport.applyScrollbarValue(1000);
            assertFalse(viewport.containsListPoint(25, 25));
            assertFalse(viewport.containsListPoint(-1, 60));
            assertTrue(viewport.containsListPoint(25, 60));
            assertFalse(viewport.containsListPoint(25, size[1] - 10));
        }
        try (java.io.InputStream input = getClass().getResourceAsStream(
                "/assets/apricityui/apricity/banira_codex/ui/form.html")) {
            assertNotNull("Missing real AUI form template", input);
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(input);
            javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            assertEquals("0", xpath.evaluate("count(//*[@id='content']//*[@id='header'])", document));
            assertEquals("1", xpath.evaluate("count(//*[@id='header'])", document));
            assertEquals("1", xpath.evaluate("count(//*[@id='content'])", document));
        }
    }

    @Test
    public void hiddenOrDisabledViewsDoNotDispatchOrUpdate() {
        class Probe extends NativeUiView {
            int updates;
            Probe() { super(new Object()); }
            @Override protected void changed(String id) { updates++; }
        }
        Probe view = new Probe();
        java.util.concurrent.atomic.AtomicInteger actions = new java.util.concurrent.atomic.AtomicInteger();
        view.onAction("go", actions::incrementAndGet);
        view.enabled("go", false);
        view.activate("go");
        assertEquals(0, actions.get());
        view.close();
        int updates = view.updates;
        view.value("field", UiValue.text("late reply"));
        view.activate("go");
        assertEquals("Closed view must not update", updates, view.updates);
        assertEquals(0, actions.get());
    }

    @Test
    public void foreignPickerReturnResumesOnlyOnSafeTick() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        Object previous = call(f.host, "view");
        Object screen = call(previous, "screen");
        Object state = call(previous, "state");
        call(state, "scrollAnchor", new Class<?>[]{String.class}, "field:tags");
        assertTrue("Session host must support native child detachment",
                java.util.Arrays.stream(f.host.getClass().getMethods()).anyMatch(method -> method.getName().equals("suspendScreen")));
        call(f.host, "suspendScreen", new Class<?>[]{NativeUiView.class}, previous);
        assertNull(call(f.host, "page"));
        assertEquals(0, call(previous, "subscriptionCount"));
        call(f.host, "resumeScreen", new Class<?>[]{Object.class}, screen);
        assertNull(call(f.host, "page"));
        call(f.host, "tick");
        assertSame(f.page, call(f.host, "page"));
        assertSame(state, call(call(f.host, "view"), "state"));
        assertEquals(0, f.closed.get());
        call(previous, "activate", new Class<?>[]{String.class}, "submit");
        assertEquals(0, f.submitted.get());
    }

    @Test
    public void disconnectClearsSuspendedAndPendingReturnsExactlyOnce() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        NativeUiView previous = (NativeUiView) call(f.host, "view");
        call(f.host, "suspendScreen", new Class<?>[]{NativeUiView.class}, previous);
        call(f.host, "resumeScreen", new Class<?>[]{Object.class}, previous.screen());
        call(f.host, "disconnect");
        call(f.host, "tick");
        assertNull(call(f.host, "page"));
        assertEquals(1, f.closed.get());
        call(f.host, "disconnect");
        assertEquals(1, f.closed.get());
    }

    @Test
    public void rawDraftChannelDoesNotOverwriteTypedDecimal() throws Exception {
        NativeUiView view = new NativeUiView(new Object());
        assertTrue("Missing finite control configuration",
                java.util.Arrays.stream(view.getClass().getMethods()).anyMatch(method -> method.getName().equals("control")));
        call(view, "control", new Class<?>[]{String.class, xin.vanilla.banira.api.client.ui.UiControlSpec.class},
                "amount", new xin.vanilla.banira.api.client.ui.UiControlSpec(
                        xin.vanilla.banira.api.client.ui.UiControlSpec.Kind.DECIMAL, 64, "", false));
        view.value("amount", UiValue.decimal(new java.math.BigDecimal("2.50")));
        java.util.concurrent.atomic.AtomicReference<String> draft = new java.util.concurrent.atomic.AtomicReference<>();
        call(view, "onDraft", new Class<?>[]{String.class, java.util.function.Consumer.class},
                "amount", (java.util.function.Consumer<String>) draft::set);
        call(view, "inputDraft", new Class<?>[]{String.class, String.class}, "amount", "-");
        assertEquals("-", draft.get());
        assertEquals(new java.math.BigDecimal("2.50"), view.values.get("amount").asDecimal());
        view.close();
        call(view, "inputDraft", new Class<?>[]{String.class, String.class}, "amount", "77");
        assertEquals("-", draft.get());
    }

    @Test
    public void supersededPickerReturnDoesNotReopenBehindAnotherScreen() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        NativeUiView previous = (NativeUiView) call(f.host, "view");
        call(f.host, "suspendScreen", new Class<?>[]{NativeUiView.class}, previous);
        call(f.host, "resumeScreen", new Class<?>[]{Object.class}, previous.screen());
        assertTrue("Host must observe the actual current screen before resuming",
                java.util.Arrays.stream(f.host.getClass().getMethods()).anyMatch(method -> method.getName().equals("observeScreen")));
        call(f.host, "observeScreen", new Class<?>[]{Object.class}, new Object());
        call(f.host, "tick");
        assertNull(call(f.host, "page"));
    }
}
