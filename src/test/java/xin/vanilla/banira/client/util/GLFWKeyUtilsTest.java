package xin.vanilla.banira.client.util;

import org.junit.Test;
import xin.vanilla.banira.client.data.GLFWKey;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class GLFWKeyUtilsTest {

    @Test
    public void sortsModifierKeysBeforeMainKey() {
        String display = GLFWKeyUtils.getKeyDisplayString(
                GLFWKey.GLFW_KEY_LEFT_ALT,
                GLFWKey.GLFW_KEY_K,
                GLFWKey.GLFW_KEY_LEFT_CONTROL,
                GLFWKey.GLFW_KEY_LEFT_SHIFT
        );

        assertEquals("LeftControl+LeftShift+LeftAlt+K", display);
    }

    @Test
    public void parsesDisplayNamesCaseInsensitively() {
        assertEquals(Collections.singletonList(GLFWKey.GLFW_KEY_K), GLFWKeyUtils.getKeyCodes("k"));
    }

    @Test
    public void delegatesShortcutDisplayToStableApi() {
        int modifiers = GLFWKey.GLFW_MOD_ALT | GLFWKey.GLFW_MOD_CONTROL | GLFWKey.GLFW_MOD_SHIFT;

        assertEquals("Ctrl + Shift + Alt + K", GLFWKey.formatShortcut(GLFWKey.GLFW_KEY_K, modifiers));
        assertEquals("Ctrl + Shift + Alt + K", GLFWKeyUtils.getShortcutDisplayString(GLFWKey.GLFW_KEY_K, modifiers));
    }
}
