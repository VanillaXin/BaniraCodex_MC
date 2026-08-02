package xin.vanilla.banira.client.gui.widget;

import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.CharInputEvent;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.util.GLFWKeyUtils;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/** 点击后直接捕获下一组键盘组合键，不要求玩家输入按键名称。 */
@Accessors(chain = true, fluent = true)
public class KeyCaptureInputWidget extends InputWidget {
    @Setter
    @Nullable
    private Consumer<String> onCaptured;
    private final Set<Integer> capturedKeys = new LinkedHashSet<>();
    private final Set<Integer> pressedKeys = new LinkedHashSet<>();

    public KeyCaptureInputWidget(BaniraScreen screen) {
        super(screen);
        text(BaniraComponent.get().transClientAuto("key_capture_hint"));
        showClearButton(true);
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (!focused() || !enabled() || event == null) {
            return false;
        }
        int keyCode = event.keyCode();
        if (keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
            clearPending();
            screen.unfocusWidget(this);
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_BACKSPACE || keyCode == GLFWKey.GLFW_KEY_DELETE) {
            capture("");
            return true;
        }
        capturedKeys.add(keyCode);
        pressedKeys.add(keyCode);
        value(GLFWKeyUtils.getKeyDisplayString(capturedKeys.stream().mapToInt(Integer::intValue).toArray()));
        return true;
    }

    @Override
    protected boolean onKeyRelease(KeyEvent event) {
        if (!focused() || !capturedKeys.contains(event.keyCode())) {
            return false;
        }
        pressedKeys.remove(event.keyCode());
        // 玩家全部松开后再提交，才能可靠捕获 A+B 及带修饰键的组合。
        if (pressedKeys.isEmpty() && capturedKeys.stream().anyMatch(key -> !isModifierKey(key))) {
            String shortcut = GLFWKeyUtils.getKeyDisplayString(
                    capturedKeys.stream().mapToInt(Integer::intValue).toArray());
            clearPending();
            capture(shortcut);
        }
        return true;
    }

    @Override
    protected boolean onCharTyped(CharInputEvent event) {
        return focused();
    }

    private void capture(String shortcut) {
        value(shortcut);
        if (onCaptured != null) {
            onCaptured.accept(shortcut);
        }
        screen.unfocusWidget(this);
    }

    private void clearPending() {
        capturedKeys.clear();
        pressedKeys.clear();
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == GLFWKey.GLFW_KEY_LEFT_CONTROL || keyCode == GLFWKey.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFWKey.GLFW_KEY_LEFT_SHIFT || keyCode == GLFWKey.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFWKey.GLFW_KEY_LEFT_ALT || keyCode == GLFWKey.GLFW_KEY_RIGHT_ALT
                || keyCode == GLFWKey.GLFW_KEY_LEFT_SUPER || keyCode == GLFWKey.GLFW_KEY_RIGHT_SUPER;
    }
}
