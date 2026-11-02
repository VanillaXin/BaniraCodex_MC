package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.type;

public class UiDomControlsTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @org.junit.Before
    public void platform() {
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
    }

    static final class Dom {
        final Map<String, Node> ids = new LinkedHashMap<>();
        final List<Node> created = new ArrayList<>();
        boolean current = true;
        Class<?> nodeType, eventType;
        Object access;
        Consumer<String> colorResult;
        int colorCloses;
        boolean failingTooltips;
        boolean rejectUnsupportedId;

        Dom() throws Exception {
            Class<?> api = type("internal.client.ui.UiDomAccess");
            nodeType = type("internal.client.ui.UiDomAccess$Node");
            eventType = type("internal.client.ui.UiDomAccess$Event");
            for (String id : Arrays.asList("body", "fields", "content", "submit", "cancel", "amount", "text", "header")) {
                ids.put(id, new Node(id));
            }
            access = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "find":
                        return ids.containsKey(args[0]) ? ids.get(args[0]).proxy : null;
                    case "create":
                        Node node = new Node((String) args[0]);
                        created.add(node);
                        return node.proxy;
                    case "overlayRoot":
                        return ids.get("body").proxy;
                    case "current":
                        return current;
                    case "color":
                        colorResult = (Consumer<String>) args[2];
                        return (AutoCloseable) () -> colorCloses++;
                    default:
                        return null;
                }
            });
        }

        class Node {
            final String tag;
            final Object proxy;
            final Map<String, String> attributes = new HashMap<>();
            final Map<String, String> styleProperties = new HashMap<>();
            final Map<String, List<Consumer<Object>>> events = new HashMap<>();
            final List<Object> children = new ArrayList<>();
            String text = "", value = "", tooltip = "";
            xin.vanilla.banira.common.data.Component component;
            UiGameElement gameElement;
            int writes, start, end;
            boolean removed, focused;
            double scroll, height = 100;
            ScreenCoordinate rectangle = new ScreenCoordinate(10, 40, 200, 100);

            @SuppressWarnings("unchecked")
            Node(String tag) {
                this.tag = tag;
                proxy = Proxy.newProxyInstance(nodeType.getClassLoader(), new Class<?>[]{nodeType}, (self, method, args) -> {
                    switch (method.getName()) {
                        case "toString":
                            return tag;
                        case "hashCode":
                            return System.identityHashCode(self);
                        case "equals":
                            return self == args[0];
                        case "text":
                            if (args == null || args.length == 0) return text;
                            text = (String) args[0];
                            writes++;
                            return null;
                        case "component":
                            component = (xin.vanilla.banira.common.data.Component) args[0];
                            return null;
                        case "gameElement":
                            gameElement = (UiGameElement) args[0];
                            return null;
                        case "tooltip":
                            if (failingTooltips) throw new IllegalStateException("Tooltip adapter failure");
                            tooltip = (String) args[0];
                            return null;
                        case "value":
                            if (args == null || args.length == 0) return value;
                            value = (String) args[0];
                            writes++;
                            return null;
                        case "attribute":
                            if (rejectUnsupportedId && args[0].equals("id"))
                                throw new IllegalArgumentException("Unsupported UI attribute");
                            attributes.put((String) args[0], (String) args[1]);
                            writes++;
                            return null;
                        case "hudId":
                            attributes.put("id", (String) args[0]);
                            writes++;
                            return null;
                        case "styleProperty":
                            styleProperties.put((String) args[0], (String) args[1]);
                            writes++;
                            return null;
                        case "removeAttribute":
                            attributes.remove(args[0]);
                            writes++;
                            return null;
                        case "append":
                            children.remove(args[0]);
                            children.add(args[0]);
                            return null;
                        case "remove":
                            removed = true;
                            return null;
                        case "bounds":
                            return rectangle;
                        case "scrollTop":
                            if (args == null || args.length == 0) return scroll;
                            scroll = height == 0 ? 0 : (double) args[0];
                            return null;
                        case "clientHeight":
                            return height;
                        case "focused":
                            return focused;
                        case "focus":
                            focused = true;
                            return null;
                        case "blur":
                            focused = false;
                            return null;
                        case "selectionStart":
                            return start;
                        case "selectionEnd":
                            return end;
                        case "selection":
                            start = (int) args[0];
                            end = (int) args[1];
                            return null;
                        case "on":
                            List<Consumer<Object>> listeners = events.computeIfAbsent((String) args[0], key -> new ArrayList<>());
                            Consumer<Object> listener = (Consumer<Object>) args[1];
                            listeners.add(listener);
                            return (AutoCloseable) () -> listeners.remove(listener);
                        default:
                            return null;
                    }
                });
            }

            void emit(String event) {
                emit(event, 0);
            }

            void key(String event, int code) {
                Object input = Proxy.newProxyInstance(eventType.getClassLoader(), new Class<?>[]{eventType}, (p, m, a) -> {
                    if (m.getReturnType() == int.class) return m.getName().equals("keyCode") ? code : -1;
                    if (m.getReturnType() == double.class) return 0d;
                    return null;
                });
                for (Consumer<Object> listener : new ArrayList<>(events.getOrDefault(event, Collections.emptyList())))
                    listener.accept(input);
            }

            void emit(String event, int button) {
                Object input = Proxy.newProxyInstance(eventType.getClassLoader(), new Class<?>[]{eventType},
                        (p, m, a) -> m.getReturnType() == int.class ? (m.getName().equals("button") ? button : 0)
                                : m.getReturnType() == double.class ? (m.getName().equals("x") ? 20.0 : 50.0)
                                : m.getReturnType() == boolean.class ? false : null);
                for (Consumer<Object> listener : new ArrayList<>(events.getOrDefault(event, Collections.emptyList())))
                    listener.accept(input);
            }
        }

        NativeUiView view() throws Exception {
            return (NativeUiView) type("internal.client.ui.UiDomView")
                    .getConstructor(Object.class, type("internal.client.ui.UiDomAccess")).newInstance(new Object(), access);
        }
    }

    @Test
    public void actualBindingUsesLiteralTextAndRawDraftEvents() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.control("amount", new UiControlSpec(UiControlSpec.Kind.DECIMAL, 64, "", false));
        List<String> drafts = new ArrayList<>();
        view.onDraft("amount", drafts::add);
        dom.ids.get("amount").value = "-";
        dom.ids.get("amount").emit("input");
        assertEquals(Collections.singletonList("-"), drafts);
        view.value("text", UiValue.text("<script>not executable</script>"));
        assertEquals("<script>not executable</script>", dom.ids.get("text").value);
        view.close();
        dom.ids.get("amount").value = "late";
        dom.ids.get("amount").emit("input");
        assertEquals(1, drafts.size());
    }

    @Test
    public void navigationRowsHaveContentAfterDeferredDocumentAttachment() throws Exception {
        Dom dom = new Dom();
        UiDomView view = new UiDomView(new Object(), null);
        lombok.var session = new xin.vanilla.banira.internal.client.ui.page.NavigationUiSession(
                xin.vanilla.banira.BaniraComponent.get().literal("Navigation"), com.google.common.collect.ImmutableList.of(
                new xin.vanilla.banira.internal.client.ui.page.NavigationUiSession.Action("first",
                        xin.vanilla.banira.BaniraComponent.get().literal("First"), () -> {
                }, null),
                new xin.vanilla.banira.internal.client.ui.page.NavigationUiSession.Action("second",
                        xin.vanilla.banira.BaniraComponent.get().literal("Second"), () -> {
                }, null)));
        session.bind(view);
        view.attach((UiDomAccess) dom.access);
        view.afterLayout();
        assertEquals(2, dom.created.stream().filter(node -> node.tag.equals("button") && !node.text.isEmpty()).count());
        view.close();
    }

    @Test
    public void rowActionsDoNotInheritParagraphMargins() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "first";
            }

            public void bind(UiView row, int index) {
                row.text("open", xin.vanilla.banira.BaniraComponent.get().literal("Action"));
            }
        }, 120);
        assertEquals("row-action", dom.created.stream().filter(node -> node.tag.equals("button") && node.text.equals("Action"))
                .findFirst().orElseThrow(java.util.NoSuchElementException::new).attributes.get("class"));
        view.close();
    }

    @Test
    public void scopedToolsUsePresetIconsAndRetainTheirActions() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        int[] resets = {0};
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "first";
            }

            public void bind(UiView row, int index) {
                row.text("reset", xin.vanilla.banira.BaniraComponent.get().literal("Reset"));
                row.tooltip("reset", xin.vanilla.banira.BaniraComponent.get().literal("Reset value"));
                row.onAction("reset", () -> resets[0]++);
                row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, 64, "", false));
            }
        }, 120);
        Dom.Node reset = dom.created.stream().filter(node -> node.gameElement != null
                && node.gameElement.kind() == UiGameElement.Kind.PRESET
                && node.gameElement.preset() == xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.RESET).findFirst().orElse(null);
        assertNotNull("Reset needs a centered icon instead of a font glyph", reset);
        assertEquals("", reset.text);
        assertEquals("Reset value", reset.attributes.get("aria-label"));
        assertEquals("Reset value", reset.tooltip);
        reset.emit("click");
        assertEquals(1, resets[0]);
        assertTrue(dom.created.stream().anyMatch(node -> node.gameElement != null && node.gameElement.kind() == UiGameElement.Kind.PRESET
                && node.gameElement.preset() == xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.ARROW_DOWN));
        view.close();
    }

    @Test
    public void longListHasBoundedNodesAndScrollPreservesFullData() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        List<Integer> bound = new ArrayList<>();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "key:" + index;
            }

            public void bind(UiView row, int index) {
                bound.add(index);
            }
        }, 120);
        assertTrue(bound.size() > 0 && bound.size() <= 120);
        dom.ids.get("content").scroll = 4800 * 48d;
        dom.ids.get("content").emit("scroll");
        assertTrue(bound.contains(4800));
        assertTrue(dom.created.stream().filter(node -> !node.removed).count() < 2000);
    }

    @Test
    public void engineScrollWithoutAnEventRefreshesTheVirtualWindowAfterLayout() throws Exception {
        Dom dom = new Dom();
        UiDomView view = (UiDomView) dom.view();
        List<Integer> bound = new ArrayList<>();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "key:" + index;
            }

            public void bind(UiView row, int index) {
                bound.add(index);
            }
        }, 120);
        view.afterLayout();
        view.afterLayout();
        bound.clear();
        dom.ids.get("content").scroll = 4800d * 48;
        view.afterLayout();
        assertTrue("Silent scroll must realize the current viewport", bound.contains(4800));
        assertEquals(4800, view.state().rowPosition(), .001);
        assertTrue(dom.created.stream().anyMatch(node -> "height:230400px;flex-shrink:0".equals(node.attributes.get("style"))));
        view.close();
    }

    @Test
    public void keyCapturePreviewsWithoutPublishingAndEscapeRestoresPreviousChord() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.KEY_CHORD, 64, "", false));
        view.value("text", UiValue.keyChords(com.google.common.collect.ImmutableList.of("Tab")));
        List<UiValue> changes = new ArrayList<>();
        view.onValue("text", changes::add);
        Dom.Node input = dom.ids.get("text");
        input.focused = true;
        UiKeyCapture reference = new UiKeyCapture();
        reference.press(341);
        input.key("keydown", 341);
        assertEquals(reference.preview(), input.value);
        assertTrue(changes.isEmpty());
        input.key("keydown", 256);
        assertEquals("Tab", input.value);
        assertFalse(input.focused);
        assertTrue(changes.isEmpty());
        input.key("keyup", 341);
        assertTrue(changes.isEmpty());
        view.close();
    }

    @Test
    public void keyCaptureBlurRestoresPreviewAndCompletionPublishesOnce() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.KEY_CHORD, 64, "", false));
        view.value("text", UiValue.keyChords(com.google.common.collect.ImmutableList.of("Tab")));
        List<UiValue> changes = new ArrayList<>();
        view.onValue("text", changes::add);
        Dom.Node input = dom.ids.get("text");
        input.focused = true;
        input.key("keydown", 341);
        input.emit("blur");
        assertEquals("Tab", input.value);
        assertTrue(changes.isEmpty());
        input.key("keydown", 341);
        input.key("keydown", 75);
        input.key("keyup", 75);
        assertTrue(changes.isEmpty());
        input.key("keyup", 341);
        input.key("keyup", 341);
        assertEquals(1, changes.size());
        assertEquals(String.join("+", changes.get(0).asKeyChords()), input.value);
        view.close();
    }

    @Test
    public void releasingOnlyModifiersDoesNotLatchThemIntoTheNextShortcut() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.KEY_CHORD, 64, "", false));
        view.value("text", UiValue.keyChords(com.google.common.collect.ImmutableList.of("Tab")));
        List<UiValue> changes = new ArrayList<>();
        view.onValue("text", changes::add);
        Dom.Node input = dom.ids.get("text");
        input.focused = true;
        input.key("keydown", 341);
        input.key("keyup", 341);
        assertEquals("Tab", input.value);
        assertTrue(changes.isEmpty());
        input.key("keydown", 75);
        input.key("keyup", 75);
        assertEquals(1, changes.size());
        assertEquals(com.google.common.collect.ImmutableList.of("K"), changes.get(0).asKeyChords());
        view.close();
    }

    @Test
    public void restoredFocusedRowIsFullyVisibleWithoutFightingLaterManualScroll() throws Exception {
        Dom dom = new Dom();
        UiDomView view = (UiDomView) dom.view();
        UiViewState state = new UiViewState();
        state.selection("retained/input", 1, 2);
        view.restoreState(state);
        dom.ids.get("content").rectangle = new ScreenCoordinate(0, 0, 200, 100);
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "retained";
            }

            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 64, "", false));
                row.draft("input", "draft");
            }
        }, 120);
        Dom.Node input = dom.created.stream().filter(node -> node.tag.equals("input")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        input.rectangle = new ScreenCoordinate(2, 90, 150, 22);
        for (int frame = 0; frame < 3; frame++) view.afterLayout();
        assertTrue(input.focused);
        assertEquals(12, dom.ids.get("content").scroll, 0);
        assertEquals(12, state.scrollOffset(), 0);
        assertEquals(.25, state.rowPosition(), 0);
        assertEquals("draft", input.value);
        assertEquals(1, input.start);
        assertEquals(2, input.end);
        dom.ids.get("content").scroll = 50;
        dom.ids.get("content").emit("scroll");
        view.afterLayout();
        assertEquals("The restored focus must not override later user scrolling", 50, dom.ids.get("content").scroll, 0);
        view.close();
    }

    @Test
    public void invisibleDocumentDoesNotReceiveDomMutations() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.value("text", UiValue.text("before"));
        int count = dom.ids.get("text").writes;
        dom.current = false;
        view.value("text", UiValue.text("hidden"));
        assertEquals(count, dom.ids.get("text").writes);
    }

    @Test
    public void selectionAndUnicodePasteSurviveViewRebind() throws Exception {
        Dom first = new Dom();
        NativeUiView view = first.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.TEXT, 256, "", false));
        view.draft("text", "生日🌸 pasted text");
        first.ids.get("text").focused = true;
        first.ids.get("text").start = 2;
        first.ids.get("text").end = 6;
        first.ids.get("text").emit("select");
        UiViewState state = view.state();
        view.close();
        Dom second = new Dom();
        NativeUiView restored = second.view();
        restored.restoreState(state);
        restored.control("text", new UiControlSpec(UiControlSpec.Kind.TEXT, 256, "", false));
        restored.draft("text", "生日🌸 pasted text");
        assertEquals(2, second.ids.get("text").start);
        assertEquals(6, second.ids.get("text").end);
    }

    @Test
    public void callerRowLimitIsRespected() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        AtomicIntegerCounter count = new AtomicIntegerCounter();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "key:" + index;
            }

            public void bind(UiView row, int index) {
                count.value++;
            }
        }, 2);
        assertEquals(2, count.value);
    }

    @Test
    public void scrollingReusesDomRowsInsteadOfRecreatingThem() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "key:" + index;
            }

            public void bind(UiView row, int index) {
                row.text("label", xin.vanilla.banira.client.gui.component.Text.literal("Row " + index).toComponent());
                row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 256, "", false));
                row.draft("input", "value:" + index);
            }
        }, 120);
        int allocated = dom.created.size();
        for (int index = 1; index <= 100; index++) {
            dom.ids.get("content").scroll = index * 48d;
            dom.ids.get("content").emit("scroll");
        }
        assertEquals("Scrolling must reuse row nodes", allocated, dom.created.size());
    }

    @Test
    public void decimalControlRejectsTextTypedValue() throws Exception {
        NativeUiView view = new NativeUiView(new Object());
        view.control("amount", new UiControlSpec(UiControlSpec.Kind.DECIMAL, 64, "", false));
        try {
            view.input("amount", UiValue.text("1"));
            fail("Decimal control accepted text value");
        } catch (IllegalArgumentException expected) {
        }
        assertFalse(view.values.containsKey("amount"));
    }

    private static final class AtomicIntegerCounter {
        int value;
    }

    @Test
    public void colorPickerBlocksUnderlyingActionsAndClosesOnDisable() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        java.util.concurrent.atomic.AtomicInteger submits = new java.util.concurrent.atomic.AtomicInteger();
        view.onAction("submit", submits::incrementAndGet);
        java.util.concurrent.atomic.AtomicReference<UiView> field = new java.util.concurrent.atomic.AtomicReference<>();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "color";
            }

            public void bind(UiView row, int index) {
                field.set(row);
                row.control("input", new UiControlSpec(UiControlSpec.Kind.COLOR, 64, "", false));
                row.draft("input", "#ff112233");
            }
        }, 1);
        Dom.Node pick = dom.created.stream().filter(node -> node.tag.equals("button")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        pick.emit("click");
        dom.ids.get("submit").emit("click");
        assertEquals("Modal color picker must block underlying controls", 0, submits.get());
        field.get().enabled("input", false);
        assertEquals(1, dom.colorCloses);
        dom.ids.get("submit").emit("click");
        assertEquals(1, submits.get());
        view.close();
        assertEquals(1, dom.colorCloses);
    }

    @Test
    public void selectPopupKeepsCommaValuesAndReusesOptionNodes() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        List<xin.vanilla.banira.client.gui.widget.DropdownOption> options = new ArrayList<>();
        options.add(new xin.vanilla.banira.client.gui.widget.DropdownOption("a,b"));
        for (int i = 1; i < 5000; i++)
            options.add(new xin.vanilla.banira.client.gui.widget.DropdownOption("value:" + i));
        java.util.concurrent.atomic.AtomicReference<UiValue> selected = new java.util.concurrent.atomic.AtomicReference<>();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "select";
            }

            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, 256, "", true, options,
                        xin.vanilla.banira.client.gui.widget.DropdownInputMode.SELECTION_ONLY));
                row.value("input", UiValue.strings(Collections.emptyList()));
                row.onValue("input", selected::set);
            }
        }, 1);
        dom.created.stream().filter(node -> "input".equals(node.tag)).findFirst().orElseThrow(java.util.NoSuchElementException::new).emit("click");
        Dom.Node scrolling = dom.created.stream().filter(node -> "popup-options".equals(node.attributes.get("class"))).findFirst().orElse(null);
        assertNotNull("Clicking the selection input must open its popup", scrolling);
        dom.created.stream().filter(node -> "popup-option".equals(node.attributes.get("class"))).findFirst().orElseThrow(java.util.NoSuchElementException::new).emit("click");
        assertEquals(Collections.singletonList("a,b"), selected.get().asStrings());
        int allocated = dom.created.size();
        for (int i = 1; i <= 100; i++) {
            scrolling.scroll = i * 22d;
            scrolling.emit("scroll");
        }
        assertEquals("Popup must reuse option nodes", allocated, dom.created.size());
        assertTrue(dom.created.stream().filter(node -> !node.removed).count() < 2000);
    }

    @Test
    public void textPortRetainsStyledComponentsWithoutHtml() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        xin.vanilla.banira.common.data.Component text = xin.vanilla.banira.client.gui.component.Text.literal("<script>literal</script>").toComponent();
        view.text("text", text);
        assertSame("Styled component must reach game-element bridge", text, dom.ids.get("text").component);
    }

    @Test
    public void themeUsesExistingPaletteTokens() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        assertTrue("Missing shared theme binding", Arrays.stream(view.getClass().getMethods()).anyMatch(method -> method.getName().equals("theme")));
        xin.vanilla.banira.client.data.BaniraColorConfig theme = new xin.vanilla.banira.client.data.BaniraColorConfig()
                .textPrimary(0xff123456).bgSurface(0xff102030).accent(0xffabcdef);
        call(view, "theme", new Class<?>[]{xin.vanilla.banira.client.data.BaniraColorConfig.class}, theme);
        String style = dom.ids.get("body").attributes.get("style");
        assertNotNull(style);
        assertTrue(style.contains("--text:#123456ff"));
        assertTrue(style.contains("--surface:#102030ff"));
    }

    @Test
    public void recyclingFocusedRowCannotEditAnotherField() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "field:" + index;
            }

            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 64, "", false));
                row.draft("input", "value:" + index);
            }
        }, 2);
        Dom.Node input = dom.created.stream().filter(node -> node.tag.equals("input")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        input.focused = true;
        input.start = 2;
        input.end = 4;
        dom.ids.get("content").scroll = 4000 * 48d;
        dom.ids.get("content").emit("scroll");
        assertFalse("A recycled input must not retain focus for a different field", input.focused);
        assertEquals("field:0/input", view.state().focusedField());
    }

    @Test
    public void initialScrollWaitsForLayoutInsteadOfLosingItsAnchor() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        dom.ids.get("content").height = 0;
        view.state().rowPosition(4800);
        List<Integer> bound = new ArrayList<>();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 5000;
            }

            public String key(int index) {
                return "field:" + index;
            }

            public void bind(UiView row, int index) {
                bound.add(index);
            }
        }, 120);
        assertTrue("Missing post-layout restore hook", Arrays.stream(view.getClass().getMethods()).anyMatch(method -> method.getName().equals("afterLayout")));
        dom.ids.get("content").height = 100;
        call(view, "afterLayout");
        assertTrue(bound.contains(4800));
        assertEquals(4800d, view.state().rowPosition(), .001);
        view.close();
    }

    @Test
    public void textScrollStateIsCapturedAndRestored() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.state().scrollOffset(300);
        assertTrue("Missing post-layout restore hook", Arrays.stream(view.getClass().getMethods()).anyMatch(method -> method.getName().equals("afterLayout")));
        call(view, "afterLayout");
        assertEquals(300d, dom.ids.get("content").scroll, .001);
        dom.ids.get("content").scroll = 500;
        dom.ids.get("content").emit("scroll");
        assertEquals(500d, view.state().scrollOffset(), .001);
    }

    @Test
    public void popupAdapterFailureFallsBackOnceAndReleasesOverlay() throws Exception {
        Dom dom = new Dom();
        UiDomView view = (UiDomView) dom.view();
        java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
        view.failureHandler(failure -> failures.incrementAndGet());
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 1;
            }

            public String key(int index) {
                return "select";
            }

            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, 64, "", false,
                        Collections.singletonList(new xin.vanilla.banira.client.gui.widget.DropdownOption("a")),
                        xin.vanilla.banira.client.gui.widget.DropdownInputMode.SELECTION_ONLY));
                row.value("input", UiValue.text(""));
            }
        }, 1);
        dom.created.stream().filter(node -> node.tag.equals("input")).findFirst().orElseThrow(java.util.NoSuchElementException::new).emit("click");
        Dom.Node search = dom.created.stream().filter(node -> "popup-search".equals(node.attributes.get("class"))).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        dom.failingTooltips = true;
        try {
            search.emit("input");
        } catch (RuntimeException failure) {
            fail("Recoverable popup failure escaped: " + failure);
        }
        assertEquals(1, failures.get());
        assertTrue(dom.created.stream().filter(node -> "popup-layer".equals(node.attributes.get("class"))).allMatch(node -> node.removed));
        search.emit("input");
        assertEquals(1, failures.get());
    }

    @Test
    public void recycledSelectionReadonlyDoesNotLeakIntoCheckbox() throws Exception {
        Dom dom = new Dom();
        NativeUiView view = dom.view();
        view.rows("fields", new UiRowProvider() {
            public int size() {
                return 2;
            }

            public String key(int index) {
                return "field:" + index;
            }

            public void bind(UiView row, int index) {
                if (index == 0) {
                    row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, 64, "", false));
                    row.value("input", UiValue.text("a"));
                } else row.value("input", UiValue.bool(true));
            }
        }, 1);
        Dom.Node input = dom.created.stream().filter(node -> node.tag.equals("input")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        dom.ids.get("content").scroll = 48;
        dom.ids.get("content").emit("scroll");
        assertFalse("Readonly from a prior row must be cleared", input.attributes.containsKey("readonly"));
    }
}
