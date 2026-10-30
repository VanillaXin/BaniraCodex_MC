package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.BaniraUi;
import xin.vanilla.banira.api.client.ui.UiCloseReason;

import static org.junit.Assert.*;

public class UiPublicPageContractTest {
    @Test
    public void atlasSpriteNodesKeepTheTemplateGeometry() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.java"), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("Atlas sprites must not inherit the compact tool icon dimensions", source.contains(
                "value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.SPRITE"));
    }

    @Test
    public void removedCalendarIconsCanBeClearedWithoutLeavingOldMonthSprites() {
        NativeUiView view = new NativeUiView(new Object());
        try {
            view.gameElement("status39", null);
        } catch (RuntimeException rejected) {
            fail("An absent calendar icon must clear its retained DOM element: " + rejected);
        }
        assertTrue(view.gameElements.containsKey("status39"));
        assertNull(view.gameElements.get("status39"));
    }

    @Test
    public void publicOnlySourceRetainsParentAndStateAcrossHandoff() {
        class Source implements xin.vanilla.banira.api.client.ui.UiPageSource, xin.vanilla.banira.api.client.ui.UiPageSession {
            int captures, prepares, closes;
            final xin.vanilla.banira.api.client.ui.UiPageSpec spec = new xin.vanilla.banira.api.client.ui.UiPageSpec(
                    "sakura_sign_in:test", "sakura_sign_in/signin.html", this, parent -> this);

            public String uiSurfaceId() {
                return spec.id();
            }

            public xin.vanilla.banira.api.client.ui.UiPageSpec uiPage() {
                return spec;
            }

            public xin.vanilla.banira.api.client.ui.UiPageSession pageSession() {
                return this;
            }

            public void capturePageState(xin.vanilla.banira.api.client.ui.UiPageState state) {
                captures++;
                state.selectedId("birthday");
            }

            public void preparePageState(xin.vanilla.banira.api.client.ui.UiPageState state) {
                prepares++;
            }

            public void bind(xin.vanilla.banira.api.client.ui.UiView view) {
            }

            public void detach(xin.vanilla.banira.api.client.ui.UiDetachReason reason) {
            }

            public void close(UiCloseReason reason) {
                closes++;
            }
        }
        Source source = new Source();
        UiSessionHost host = new UiSessionHost(new UiBackendController(() -> false, () -> false),
                (page, parent) -> new NativeUiView(new Object()), screen -> {
        }, screen -> true);
        assertTrue(host.open(source.spec, null));
        host.screenRemoved(source);
        host.observeScreen(source);
        host.tick();
        assertSame(source.spec, host.page());
        assertEquals("birthday", host.view().state().selectedId());
        assertEquals(1, source.captures);
        assertEquals(2, source.prepares);
        host.disconnect();
        assertEquals(1, source.closes);
    }

    @Test
    public void legacyStateMethodsRemainResolvedThroughPublicState() {
        UiViewState state = new UiViewState();
        state.rowPosition(7);
        state.selection("date", 1, 4);
        state.detailScroll(Double.NaN);
        assertEquals(7, ((xin.vanilla.banira.api.client.ui.UiPageState) state).rowPosition(), 0);
        assertEquals(4, state.selectionEnd());
        assertEquals(0, state.detailScroll(), 0);
    }

    @Test
    public void downstreamNativePageAndStateArePublicWithoutInternalSignatures() throws Exception {
        try {
            Class<?> page = Class.forName("xin.vanilla.banira.api.client.ui.UiPageSource");
            Class<?> state = Class.forName("xin.vanilla.banira.api.client.ui.UiPageState");
            assertTrue(page.isAssignableFrom(UiNativePage.class));
            assertTrue(state.isAssignableFrom(UiViewState.class));
            page.getMethod("capturePageState", state);
            page.getMethod("preparePageState", state);
            for (java.lang.reflect.Method method : page.getMethods()) {
                for (Class<?> parameter : method.getParameterTypes())
                    assertFalse(parameter.getName().contains(".internal."));
                assertFalse(method.getReturnType().getName().contains(".internal."));
            }
        } catch (ReflectiveOperationException missing) {
            fail("Missing public page contract: " + missing);
        }
    }

    @Test
    public void publicLifecycleRetainsOptionalDriverCompatibility() throws Exception {
        try {
            assertTrue(BaniraUi.Driver.class.getMethod("screenRemoved", Object.class).isDefault());
            assertTrue(BaniraUi.Driver.class.getMethod("closePage", Object.class, UiCloseReason.class).isDefault());
            BaniraUi.class.getMethod("screenRemoved", Object.class);
            BaniraUi.class.getMethod("closePage", Object.class, UiCloseReason.class);
        } catch (ReflectiveOperationException missing) {
            fail("Missing public lifecycle: " + missing);
        }
    }

    @Test
    public void calendarCanUseAtlasSpritesAndOriginalKeyBindings() {
        try {
            xin.vanilla.banira.api.client.ui.UiGameElement.class.getMethod("sprite", xin.vanilla.banira.client.data.Texture.class);
            assertTrue(xin.vanilla.banira.api.client.ui.UiPageSession.class.getMethod("key", int.class, int.class, int.class, boolean.class).isDefault());
        } catch (ReflectiveOperationException missing) {
            fail("Missing calendar presentation contract: " + missing);
        }
    }
}
