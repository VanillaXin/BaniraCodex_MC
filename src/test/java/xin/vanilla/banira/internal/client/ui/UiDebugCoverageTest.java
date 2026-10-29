package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiDebugCoverageTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
    }

    static Class<?> type() {
        try {
            return Class.forName("xin.vanilla.banira.internal.client.ui.page.DebugUiSession");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Debug needs a shared session", missing);
        }
    }

    @Test
    public void allExistingEntrancesHaveAFiniteActionAndStaleActionsAreReleased() throws Exception {
        List<String> expected = Arrays.asList("notification", "input", "item", "effect", "advancement", "notification_log", "config_client", "config_common", "quick_plus", "quick_minus", "config_test", "input_large");
        AtomicInteger count = new AtomicInteger();
        Object s = type().getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<String>) id -> count.incrementAndGet());
        assertTrue(((Set<?>) call(s, "actions")).containsAll(expected));
        NativeUiView root = new NativeUiView(new Object());
        call(s, "bind", new Class<?>[]{UiView.class}, root);
        for (String id : expected) {
            if (id.equals("config_test")) {
                root.activate(id);
                root.activateLong(id);
            } else root.activate(id);
        }
        assertEquals(expected.size(), count.get());
        call(s, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        root.activate("item");
        assertEquals(expected.size(), count.get());
        assertEquals(0, root.subscriptionCount());
        assertTrue(UiNativePage.class.isAssignableFrom(xin.vanilla.banira.internal.DebugScreen.class));
    }

    @Test
    public void diagnosticSettingsSurviveRebindAndRejectNonFiniteOrUnboundedValues() throws Exception {
        Object s = type().getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<String>) id -> {
        });
        call(s, "setting", new Class<?>[]{String.class, String.class}, "lines", "4");
        call(s, "setting", new Class<?>[]{String.class, String.class}, "font", "NaN");
        call(s, "setting", new Class<?>[]{String.class, String.class}, "length", "999999999");
        call(s, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
        assertEquals("4", call(s, "setting", new Class<?>[]{String.class}, "lines"));
        assertEquals("9", call(s, "setting", new Class<?>[]{String.class}, "font"));
        assertEquals("20", call(s, "setting", new Class<?>[]{String.class}, "length"));
    }

    @Test
    public void originalDebugLongPressFiresAtOneSecondAndOnlyOnce() throws Exception {
        AtomicInteger count = new AtomicInteger();
        var clock = new java.util.concurrent.atomic.AtomicLong();
        var s = (xin.vanilla.banira.internal.client.ui.page.DebugUiSession) type().getConstructor(java.util.function.Consumer.class)
                .newInstance((java.util.function.Consumer<String>) id -> count.incrementAndGet());
        var dom = new UiDomControlsTest.Dom();
        dom.ids.put("config_test", dom.new Node("button"));
        UiDomView view = new UiDomView(new Object(), (UiDomAccess) dom.access, clock::get);
        s.bind(view);
        dom.ids.get("config_test").emit("mousedown");
        clock.set(999);
        view.afterLayout();
        assertEquals(0, count.get());
        clock.set(1000);
        view.afterLayout();
        assertEquals(1, count.get());
        clock.set(5000);
        view.afterLayout();
        dom.ids.get("config_test").emit("mouseup");
        dom.ids.get("config_test").emit("click");
        assertEquals(1, count.get());
        s.detach(UiDetachReason.SWITCH);
        view.close();
    }

    @Test
    public void dropdownDemosAndPresetGameElementsRetainTheirTypedState() throws Exception {
        List<String> actions = new ArrayList<>();
        var s = (xin.vanilla.banira.internal.client.ui.page.DebugUiSession) type().getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<String>) actions::add);
        NativeUiView first = new NativeUiView(new Object());
        s.bind(first);
        first.input("season", UiValue.text("WINTER"));
        first.input("fruit", UiValue.strings(Arrays.asList("a", "b")));
        assertEquals(100, first.controls.get("vegetable").options().size());
        assertEquals(8, first.controls.get("popup").options().size());
        assertEquals(UiGameElement.Kind.PRESET, first.gameElements.get("preset_CLOSE").kind());
        first.input("popup", UiValue.text("item"));
        first.input("popup", UiValue.text("item"));
        assertEquals(Arrays.asList("item", "item"), actions);
        s.detach(UiDetachReason.SWITCH);
        NativeUiView second = new NativeUiView(new Object());
        s.bind(second);
        assertEquals("WINTER", second.values.get("season").asText());
        assertEquals(Arrays.asList("a", "b"), second.values.get("fruit").asStrings());
        s.close(UiCloseReason.DISCONNECT);
        first.input("popup", UiValue.text("effect"));
        assertEquals(2, actions.size());
    }

    @Test
    public void secondaryPointerCanReachTheSeasonTextureDiagnosticAndReleasesOnExit() throws Exception {
        var s = (xin.vanilla.banira.internal.client.ui.page.DebugUiSession) type().getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<String>) id -> {
        });
        var dom = new UiDomControlsTest.Dom();
        dom.ids.put("diagnostic", dom.new Node("div"));
        UiDomView view = (UiDomView) dom.view();
        s.bind(view);
        dom.ids.get("diagnostic").emit("mousedown", 1);
        assertEquals(1, view.gameElements.get("diagnostic").pressed());
        dom.ids.get("body").emit("mouseup", 1);
        assertEquals(-1, view.gameElements.get("diagnostic").pressed());
        s.detach(UiDetachReason.SWITCH);
        dom.ids.get("diagnostic").emit("mousedown", 1);
        assertEquals(-1, view.gameElements.get("diagnostic").pressed());
        view.close();
    }
}
