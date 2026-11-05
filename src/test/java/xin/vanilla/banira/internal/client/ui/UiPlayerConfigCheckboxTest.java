package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiPresentation;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class UiPlayerConfigCheckboxTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private PlayerConfigUiSession session(AtomicInteger saves, boolean checkboxes) throws Exception {
        var args = new InputFormScreen.Args()
                .setParentScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
                })
                .setTitle(Text.literal("Settings")).setCallback(result -> {
                    assertEquals("false", result.value("enabled"));
                    saves.incrementAndGet();
                }).addWidget(new InputFormScreen.Widget().name("enabled").title(Text.literal("Show result"))
                        .type(InputFormScreen.WidgetType.DROPDOWN).dropdownOptions("true", "false").defaultValue("true"));
        var empty = BaniraComponent.get().empty();
        var presentation = new PlayerConfigUiPresentation("aotake_sweep", Text.literal("Aotake").toComponent(),
                Text.literal("Settings").toComponent(), empty, () -> empty);
        if (checkboxes) {
            try {
                presentation = PlayerConfigUiPresentation.class.getConstructor(String.class, Component.class,
                                Component.class, Component.class, Supplier.class, Set.class)
                        .newInstance("aotake_sweep", Text.literal("Aotake").toComponent(),
                                Text.literal("Settings").toComponent(), empty, (Supplier<Component>) () -> empty, Set.of("enabled"));
            } catch (NoSuchMethodException missing) {
                throw new AssertionError("Player config must reuse typed checkbox binding", missing);
            }
        }
        return new PlayerConfigUiSession(args).presentation(presentation);
    }

    @Test
    public void explicitlySelectedBooleanUsesExistingTypedCheckboxAndSavesOnce() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        var session = session(saves, true);
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        NativeUiView row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 0);
        assertFalse(row.controls.containsKey("input"));
        assertEquals(UiValue.Kind.BOOLEAN, row.values.get("input").kind());
        assertTrue(row.values.get("input").asBoolean());
        row.input("input", UiValue.bool(false));
        assertEquals("false", session.fields().draft("enabled"));
        root.activate("submit");
        root.activate("submit");
        assertEquals(1, saves.get());
    }

    @Test
    public void checkboxRebindKeepsDraftAndRetiresOldListeners() throws Exception {
        var session = session(new AtomicInteger(), true);
        NativeUiView old = new NativeUiView(new Object());
        session.bind(old);
        NativeUiView row = new NativeUiView(new Object());
        old.rowProviders.get("fields").bind(row, 0);
        row.input("input", UiValue.bool(false));
        session.detach(UiDetachReason.SWITCH);
        NativeUiView next = new NativeUiView(new Object());
        session.bind(next);
        NativeUiView rebound = new NativeUiView(new Object());
        next.rowProviders.get("fields").bind(rebound, 0);
        assertFalse(rebound.values.get("input").asBoolean());
        row.input("input", UiValue.bool(true));
        assertEquals("false", session.fields().draft("enabled"));
        session.close(UiCloseReason.CANCEL);
        assertEquals(0, rebound.subscriptionCount());
    }

    @Test
    public void existingSelectPresentationRemainsUnchanged() throws Exception {
        var session = session(new AtomicInteger(), false);
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        NativeUiView row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 0);
        assertEquals(UiControlSpec.Kind.SELECT, row.controls.get("input").kind());
        assertEquals(UiValue.Kind.TEXT, row.values.get("input").kind());
    }
}
