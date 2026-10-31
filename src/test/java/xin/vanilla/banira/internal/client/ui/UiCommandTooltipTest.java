package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiCommandTooltipTest {
    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void iconCommandsHaveFullLiteralTooltipAndClosedViewCannotChangeIt() throws Exception {
        assertTrue("Named command tooltips must use the shared view", Arrays.stream(NativeUiView.class.getMethods())
                .anyMatch(method -> method.getName().equals("tooltip")));
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var view = dom.view();
        Component text = xin.vanilla.banira.BaniraComponent.get().literal("Full reset description <b>literal</b>");
        call(view, "tooltip", new Class<?>[]{String.class, Component.class}, "submit", text);
        assertEquals(text.getString("en_us"), dom.ids.get("submit").tooltip);
        view.close();
        call(view, "tooltip", new Class<?>[]{String.class, Component.class}, "submit", xin.vanilla.banira.BaniraComponent.get().literal("stale"));
        assertEquals(text.getString("en_us"), dom.ids.get("submit").tooltip);
    }
}
