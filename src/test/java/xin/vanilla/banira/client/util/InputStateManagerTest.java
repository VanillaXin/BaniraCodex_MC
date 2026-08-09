package xin.vanilla.banira.client.util;

import org.junit.Test;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.common.data.KeyValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InputStateManagerTest {

    @Test
    public void inputQueriesAreSafeBeforePlatformInstall() {
        assertFalse(InputStateManager.isKeyPressing(65));
        assertFalse(InputStateManager.isMousePressing(0));

        KeyValue<Double, Double> raw = InputStateManager.getRawCursorPos();
        assertEquals(0.0D, raw.key(), 0.0D);
        assertEquals(0.0D, raw.val(), 0.0D);

        KeyValue<Integer, Integer> gui = InputStateManager.rawToGui(10.0D, 20.0D);
        assertEquals(10, gui.key().intValue());
        assertEquals(20, gui.val().intValue());
    }

    @Test
    public void unknownKeyEventsAreIgnored() {
        InputStateManager.instance().handleScreenClosed();

        InputStateManager.instance().handleKeyPressed(GLFWKey.GLFW_KEY_UNKNOWN);

        assertFalse(InputStateManager.instance().isKeyPressed(GLFWKey.GLFW_KEY_UNKNOWN));
        assertFalse(InputStateManager.instance().pressedKeyCodes().contains(
                GLFWKey.GLFW_KEY_UNKNOWN));
    }
}
