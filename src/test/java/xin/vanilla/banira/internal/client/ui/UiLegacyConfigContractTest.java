package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.Test;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.event.CharInputEvent;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.widget.KeyCaptureInputWidget;
import xin.vanilla.banira.internal.client.BaniraClientPacketHandlers;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

import static org.junit.Assert.*;

public class UiLegacyConfigContractTest {
    @Test
    public void nativeConfigAndNavigationUseTheRealLegacyRenderer() throws Exception {
        assertNotNull(ConfigEditorScreen.class.getDeclaredMethod("renderWidgets", PoseStack.class, float.class));
        assertNotNull(ConfigEditorScreen.class.getDeclaredMethod("onRender", PoseStack.class, float.class));
        assertNotNull(CodexNavigationScreen.class.getDeclaredMethod("onRender", PoseStack.class, float.class));
        assertTrue(UiNativePage.class.isAssignableFrom(ConfigEditorScreen.class));
        assertTrue(UiNativePage.class.isAssignableFrom(CodexNavigationScreen.class));
    }

    @Test
    public void nativeKeyCaptureKeepsLegacyEventDispatch() throws Exception {
        assertNotNull(KeyCaptureInputWidget.class.getDeclaredMethod("onKeyPress", KeyEvent.class));
        assertNotNull(KeyCaptureInputWidget.class.getDeclaredMethod("onKeyRelease", KeyEvent.class));
        assertNotNull(KeyCaptureInputWidget.class.getDeclaredMethod("onCharTyped", CharInputEvent.class));
    }

    @Test
    public void configReplyConnectionChecksRemainClientBound() throws Exception {
        assertEquals(boolean.class, BaniraClientRuntime.class.getDeclaredMethod("isCurrentConnection", Object.class).getReturnType());
        assertEquals(boolean.class, BaniraClientPacketHandlers.class.getDeclaredMethod("isCurrentConfigConnection", Object.class).getReturnType());
    }
}
