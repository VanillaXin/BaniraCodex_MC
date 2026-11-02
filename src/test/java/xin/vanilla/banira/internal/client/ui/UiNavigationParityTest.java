package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.type;

public class UiNavigationParityTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private Object action(String id, Runnable opened, Runnable deleted) throws Exception {
        return type("internal.client.ui.page.NavigationUiSession$Action")
                .getConstructor(String.class, Component.class, Runnable.class, Runnable.class)
                .newInstance(id, xin.vanilla.banira.BaniraComponent.get().literal(id), opened, deleted);
    }

    @Test
    public void navigationCommandsAreBoundOnceAndDetachedRowsCannotOpenScreens() throws Exception {
        AtomicInteger opened = new AtomicInteger();
        Object session = type("internal.client.ui.page.NavigationUiSession")
                .getConstructor(Component.class, List.class).newInstance(xin.vanilla.banira.BaniraComponent.get().literal("Navigation"),
                        Collections.singletonList(action("settings", opened::incrementAndGet, null)));
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        assertEquals(1, root.rowProviders.get("fields").size());
        root.rowProviders.get("fields").bind(row, 0);
        row.activate("open");
        assertEquals(1, opened.get());
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.RELOAD);
        row.activate("open");
        assertEquals(1, opened.get());
        assertEquals(0, row.subscriptionCount());
    }

    @Test
    public void quickRowsUseCurrentStableKeysAndNeverRetainPreviousDeleteHandler() throws Exception {
        AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
        List<Object> actions = new ArrayList<>();
        actions.add(action("first", () -> {
        }, first::incrementAndGet));
        Object session = type("internal.client.ui.page.QuickActionEditUiSession")
                .getConstructor(Component.class, Supplier.class, List.class)
                .newInstance(xin.vanilla.banira.BaniraComponent.get().literal("Quick actions"),
                        (Supplier<List<Object>>) () -> actions, Collections.emptyList());
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        root.rowProviders.get("fields").bind(row, 0);
        row.activate("delete");
        assertEquals(1, first.get());
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        actions.clear();
        actions.add(action("second", () -> {
        }, second::incrementAndGet));
        NativeUiView next = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, next);
        assertEquals("second", next.rowProviders.get("fields").key(0));
        next.rowProviders.get("fields").bind(row, 0);
        row.activate("delete");
        assertEquals(1, first.get());
        assertEquals(1, second.get());
    }
}
