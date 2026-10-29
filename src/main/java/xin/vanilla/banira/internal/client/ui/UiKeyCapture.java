package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.internal.client.GLFWKeyUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UiKeyCapture {
    private final Set<Integer> captured = new LinkedHashSet<>();
    private final Set<Integer> pressed = new LinkedHashSet<>();

    public List<String> press(int key) {
        if (key == GLFWKey.GLFW_KEY_ESCAPE) {
            clear();
            return null;
        }
        if (key == GLFWKey.GLFW_KEY_BACKSPACE || key == GLFWKey.GLFW_KEY_DELETE) {
            clear();
            return Collections.emptyList();
        }
        captured.add(key);
        pressed.add(key);
        return null;
    }

    public List<String> release(int key) {
        if (!captured.contains(key)) return null;
        pressed.remove(key);
        if (!pressed.isEmpty()) return null;
        if (captured.stream().allMatch(UiKeyCapture::modifier)) {
            clear();
            return null;
        }
        List<String> result = GLFWKeyUtils.getKeyDisplayNames(true, captured.stream().mapToInt(Integer::intValue).toArray());
        clear();
        return result;
    }

    public String preview() {
        return GLFWKeyUtils.getKeyDisplayString(captured.stream().mapToInt(Integer::intValue).toArray());
    }

    public void clear() {
        captured.clear();
        pressed.clear();
    }

    private static boolean modifier(int key) {
        return key == GLFWKey.GLFW_KEY_LEFT_CONTROL || key == GLFWKey.GLFW_KEY_RIGHT_CONTROL
                || key == GLFWKey.GLFW_KEY_LEFT_SHIFT || key == GLFWKey.GLFW_KEY_RIGHT_SHIFT
                || key == GLFWKey.GLFW_KEY_LEFT_ALT || key == GLFWKey.GLFW_KEY_RIGHT_ALT
                || key == GLFWKey.GLFW_KEY_LEFT_SUPER || key == GLFWKey.GLFW_KEY_RIGHT_SUPER;
    }
}
