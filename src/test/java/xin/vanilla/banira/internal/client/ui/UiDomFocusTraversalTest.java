package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiValue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class UiDomFocusTraversalTest {
    @BeforeClass public static void bootstrap() { UiDomControlsTest.bootstrap(); }
    @Before public void platform() { new UiDomControlsTest().platform(); }

    private static NativeUiView view(UiDomControlsTest.Dom dom, List<Boolean> directions) throws Exception {
        UiDomAccess access = (UiDomAccess) Proxy.newProxyInstance(UiDomAccess.class.getClassLoader(),
                new Class<?>[]{UiDomAccess.class}, (proxy, method, args) -> {
                    if (method.getName().equals("nextFocus")) {
                        directions.add((Boolean) args[0]);
                        return dom.ids.get((Boolean) args[0] ? "amount" : "cancel").proxy;
                    }
                    return method.invoke(dom.access, args);
                });
        return new UiDomView(new Object(), access);
    }

    private static void key(UiDomControlsTest.Dom dom, String id, int code, int modifiers) {
        Object event = Proxy.newProxyInstance(dom.eventType.getClassLoader(), new Class<?>[]{dom.eventType},
                (proxy, method, args) -> {
                    if (method.getName().equals("keyCode")) return code;
                    if (method.getName().equals("modifiers")) return modifiers;
                    if (method.getReturnType() == int.class) return -1;
                    if (method.getReturnType() == double.class) return 0d;
                    return null;
                });
        for (var listener : new ArrayList<>(dom.ids.get(id).events.getOrDefault("keydown", List.of()))) listener.accept(event);
    }

    @Test public void tabAndShiftTabTraverseWithoutRestoringOldInputFocusOrChangingDraft() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        List<Boolean> directions = new ArrayList<>();
        NativeUiView view = view(dom, directions);
        view.control("amount", new UiControlSpec(UiControlSpec.Kind.TEXT, 64, "", false));
        view.draft("amount", "draft");
        dom.ids.get("amount").focused = true;
        view.state().selection("amount", 3, 1);
        key(dom, "body", 258, 0);
        assertEquals(List.of(false), directions);
        assertTrue(dom.ids.get("cancel").focused);
        assertEquals("", view.state().focusedField());
        assertEquals("draft", dom.ids.get("amount").value);
        key(dom, "body", 258, 1);
        assertEquals(List.of(false, true), directions);
        view.close();
        key(dom, "body", 258, 0);
        assertEquals(2, directions.size());
    }

    @Test public void inactiveDocumentAndOtherKeysCannotNavigate() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        List<Boolean> directions = new ArrayList<>();
        NativeUiView view = view(dom, directions);
        key(dom, "body", 257, 0);
        dom.current = false;
        key(dom, "body", 258, 0);
        assertEquals(List.of(), directions);
        view.close();
    }

    @Test public void persistentHudCannotOwnPageTabEvents() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        NativeUiView hud = new UiDomView(null, (UiDomAccess) dom.access);
        assertFalse(dom.ids.get("body").events.containsKey("keydown"));
        hud.close();
    }

    @Test public void openPopupBlocksPageNavigationUntilItIsClosed() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        List<Boolean> directions = new ArrayList<>();
        NativeUiView view = view(dom, directions);
        view.control("amount", new UiControlSpec(UiControlSpec.Kind.SELECT, 64, "", false,
                List.of(new xin.vanilla.banira.client.gui.widget.DropdownOption("one")),
                xin.vanilla.banira.client.gui.widget.DropdownInputMode.SELECTION_ONLY));
        view.value("amount", UiValue.text("one"));
        dom.ids.get("amount").emit("click");
        key(dom, "body", 258, 0);
        assertEquals(List.of(), directions);
        dom.created.stream().filter(node -> "popup-search".equals(node.attributes.get("class")))
                .findFirst().orElseThrow().key("keydown", 258);
        key(dom, "body", 258, 0);
        assertEquals(List.of(false), directions);
        view.close();
    }

    @Test public void traversalWrapsAndSkipsDisabledHiddenAndOccludedCandidates() throws Exception {
        List<Integer> order = List.of(0, 1, 2, 3, 4);
        Predicate<Integer> eligible = candidate -> candidate == 0 || candidate == 4;
        assertEquals(4, next(order, 0, eligible, false));
        assertEquals(0, next(order, 4, eligible, false));
        assertEquals(4, next(order, 0, eligible, true));
        assertEquals(0, next(order, 4, eligible, true));
        assertEquals(4, next(order, 2, eligible, false));
        assertEquals(0, next(order, null, eligible, false));
        assertEquals(4, next(order, null, eligible, true));
        assertNull(next(order, 0, candidate -> false, false));
        assertNull(next(List.of(), null, eligible, false));
    }

    private static Object next(List<Integer> order, Integer current, Predicate<Integer> eligible, boolean reverse) throws Exception {
        Class<?> type;
        try { type = Class.forName("xin.vanilla.banira.internal.client.ui.UiFocusTraversal"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Missing DOM focus traversal policy", missing); }
        return type.getMethod("next", List.class, Object.class, Predicate.class, boolean.class)
                .invoke(null, order, current, eligible, reverse);
    }
}
