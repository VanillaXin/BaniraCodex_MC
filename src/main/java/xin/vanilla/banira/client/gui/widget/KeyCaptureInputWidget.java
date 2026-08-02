package xin.vanilla.banira.client.gui.widget;

import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.CharInputEvent;
import xin.vanilla.banira.client.gui.event.KeyEvent;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** 点击后直接捕获下一组键盘组合键，不要求玩家输入按键名称。 */
@Accessors(chain = true, fluent = true)
public class KeyCaptureInputWidget extends InputWidget {
    @Setter
    @Nullable
    private Consumer<String> onCaptured;
    private int pendingKeyCode = GLFWKey.GLFW_KEY_UNKNOWN;
    private String pendingShortcut = "";

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
        if (isModifierKey(keyCode)) {
            return true;
        }
        pendingKeyCode = keyCode;
        pendingShortcut = GLFWKey.formatShortcut(keyCode, event.modifiers());
        return true;
    }

    @Override
    protected boolean onKeyRelease(KeyEvent event) {
        if (!focused() || pendingKeyCode == GLFWKey.GLFW_KEY_UNKNOWN
                || event.keyCode() != pendingKeyCode) {
            return false;
        }
        String shortcut = pendingShortcut;
        clearPending();
        capture(shortcut);
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
        pendingKeyCode = GLFWKey.GLFW_KEY_UNKNOWN;
        pendingShortcut = "";
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == GLFWKey.GLFW_KEY_LEFT_CONTROL || keyCode == GLFWKey.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFWKey.GLFW_KEY_LEFT_SHIFT || keyCode == GLFWKey.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFWKey.GLFW_KEY_LEFT_ALT || keyCode == GLFWKey.GLFW_KEY_RIGHT_ALT
                || keyCode == GLFWKey.GLFW_KEY_LEFT_SUPER || keyCode == GLFWKey.GLFW_KEY_RIGHT_SUPER;
    }
}
