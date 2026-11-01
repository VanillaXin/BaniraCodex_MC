package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.ConfirmDialogScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.ReadOnlyTextScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.internal.client.ui.page.DialogUiSession;
import xin.vanilla.banira.internal.client.ui.page.NavigationUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiFabric20CommonPagePortTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private Screen parent() {
        return new Screen(net.minecraft.network.chat.Component.empty()) {
        };
    }

    private InputFormScreen.Args args() {
        return new InputFormScreen.Args().setParentScreen(parent()).setCallback(result -> {
        }).addWidget(new InputFormScreen.Widget().name("value").title(Text.literal("Value")));
    }

    @Test
    public void allFourPagesKeepTheirNativeScreenAndSessionIdentity() {
        for (UiNativePage screen : List.of(new CodexNavigationScreen(null),
                new ConfirmDialogScreen(new ConfirmDialogScreen.Args().parentScreen(parent())),
                new InputFormScreen(args()),
                new ReadOnlyTextScreen(new ReadOnlyTextScreen.Args().parentScreen(parent())))) {
            assertSame(screen.uiSession(), screen.uiPage().session());
            assertSame(screen, screen.uiPage().nativeFactory().create(parent()));
            assertTrue(screen.uiPage().template().startsWith("banira_codex/ui/"));
        }
    }

    @Test
    public void navigationUsesTheFiveRealActionsWithoutInvokingThemOnOpen() {
        CodexNavigationScreen screen = new CodexNavigationScreen(null);
        assertEquals(List.of("codex_navigation_notification_log", "codex_navigation_client_config",
                        "codex_navigation_common_config", "custom_player_config_title", "custom_quick_action_title"),
                screen.uiSession().actions().stream().map(NavigationUiSession.Action::id).toList());
        assertEquals("banira_codex:navigation", screen.uiSurfaceId());
    }

    @Test
    public void navigationRebindRetiresRowsAndLongCloseWithoutLosingActions() {
        AtomicInteger opens = new AtomicInteger(), longCloses = new AtomicInteger();
        NavigationUiSession session = new NavigationUiSession(BaniraComponent.get().literal("Navigation"),
                List.of(new NavigationUiSession.Action("settings", BaniraComponent.get().literal("Settings"),
                        opens::incrementAndGet, null)));
        session.longClose(longCloses::incrementAndGet);
        NativeUiView old = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        session.bind(old);
        old.rowProviders.get("fields").bind(row, 0);
        row.activate("open");
        NativeUiView current = new NativeUiView(new Object());
        session.bind(current);
        row.activate("open");
        old.activateLong("close");
        assertEquals(1, opens.get());
        assertEquals(0, longCloses.get());
        NativeUiView currentRow = new NativeUiView(new Object());
        current.rowProviders.get("fields").bind(currentRow, 0);
        currentRow.activate("open");
        current.activateLong("close");
        assertEquals(2, opens.get());
        assertEquals(1, longCloses.get());
        session.close(UiCloseReason.USER_CLOSE);
        currentRow.activate("open");
        assertEquals(2, opens.get());
        assertEquals(0, row.subscriptionCount());
    }

    @Test
    public void customNativeFormsRemainNative() {
        InputFormScreen subclass = new InputFormScreen(args()) {
        };
        assertNull(subclass.uiPage());
        InputFormScreen.Args changed = args();
        changed.getWidgets().get(0).changed(input -> {
        });
        assertNull(new InputFormScreen(changed).uiPage());
    }

    @Test
    public void confirmationRebindAndDisconnectCannotConfirmOrCancel() {
        AtomicInteger confirms = new AtomicInteger(), cancels = new AtomicInteger();
        DialogUiSession session = new DialogUiSession(new ConfirmDialogScreen.Args()
                .onConfirm(confirms::incrementAndGet).onCancel(cancels::incrementAndGet));
        NativeUiView old = new NativeUiView(new Object()), current = new NativeUiView(new Object());
        session.bind(old);
        session.detach(UiDetachReason.RELOAD);
        session.bind(current);
        old.activate("confirm");
        session.close(UiCloseReason.DISCONNECT);
        current.activate("cancel");
        assertEquals(0, confirms.get());
        assertEquals(0, cancels.get());
        assertTrue(session.resolved());
    }

    @Test
    public void nativeTextCapturePreservesItsScrollOffset() throws Exception {
        ReadOnlyTextScreen screen = new ReadOnlyTextScreen(new ReadOnlyTextScreen.Args().parentScreen(parent()));
        var scroll = ReadOnlyTextScreen.class.getDeclaredField("scroll");
        scroll.setAccessible(true);
        scroll.setInt(screen, 64);
        UiViewState state = new UiViewState();
        screen.captureUiState(state);
        assertEquals(64d, state.scrollOffset(), 0d);
        assertSame(screen.uiSession(), screen.pageSession());
    }

    @Test
    public void sharedKeyParserRetainsOrderAndSeparatorWhitespace() {
        assertEquals(List.of("LeftControl", "K"),
                xin.vanilla.banira.internal.client.GLFWKeyUtils.parseDisplayString(" LeftControl + K "));
        assertEquals(List.of(), xin.vanilla.banira.internal.client.GLFWKeyUtils.parseDisplayString(null));
        assertEquals(List.of(), xin.vanilla.banira.internal.client.GLFWKeyUtils.parseDisplayString(" + "));
    }
}
