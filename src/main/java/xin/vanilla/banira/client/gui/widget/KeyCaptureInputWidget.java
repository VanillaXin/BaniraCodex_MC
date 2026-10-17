package xin.vanilla.banira.client.gui.widget;

import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.internal.client.ui.UiKeyCapture;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 点击后直接捕获下一组键盘组合键，不要求玩家输入按键名称。
 */
@Accessors(chain = true, fluent = true)
public class KeyCaptureInputWidget extends InputWidget {
    @Setter
    @Nullable
    private Consumer<String> onCaptured;
    private final UiKeyCapture pending = new UiKeyCapture();

    public KeyCaptureInputWidget(BaniraScreen screen) {
        super(screen);
        text(BaniraComponent.get().transClientAuto("key_capture_hint"));
        showClearButton(true);
    }

    @Override
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) {
            return false;
        }
        if (keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
            pending.clear();
            screen.unfocusWidget(this);
            return true;
        }
        List<String> completed = pending.press(keyCode);
        if (completed != null) capture(String.join("+", completed));
        else value(pending.preview());
        return true;
    }

    @Override
    protected boolean onKeyRelease(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) {
            return false;
        }
        List<String> completed = pending.release(keyCode);
        if (completed != null) capture(String.join("+", completed));
        return true;
    }

    @Override
    protected boolean onCharTyped(char codePoint, int modifiers) {
        return focused();
    }

    private void capture(String shortcut) {
        value(shortcut);
        if (onCaptured != null) {
            onCaptured.accept(shortcut);
        }
        screen.unfocusWidget(this);
    }

    @Override public void focused(boolean focused) {
        if (!focused) pending.clear();
        super.focused(focused);
    }

    @Override public void enabled(boolean enabled) {
        if (!enabled) pending.clear();
        super.enabled(enabled);
    }
}
