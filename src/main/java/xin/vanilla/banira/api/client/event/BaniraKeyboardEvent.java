package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;

import javax.annotation.Nonnull;

/**
 * GUI 内键盘/字符输入事件；Pre 阶段回调可取消后续原生处理。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraKeyboardEvent {
    private final @Nonnull Action action;
    private final @Nonnull BaniraScreenInfo screen;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;
    private final int codePoint;
    private int pressCount = 1;
    private boolean doublePress;
    private boolean repeatedPress;
    private boolean heldRepeat;
    private boolean pressTracked;
    private boolean canceled;

    private BaniraKeyboardEvent(@Nonnull Action action, @Nonnull BaniraScreenInfo screen, int keyCode, int scanCode,
                                int modifiers, int codePoint) {
        this.action = action;
        this.screen = screen;
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.codePoint = codePoint;
    }

    public static BaniraKeyboardEvent pressed(@Nonnull BaniraScreenInfo screen, int keyCode, int scanCode, int modifiers) {
        return new BaniraKeyboardEvent(Action.KEY_PRESS, screen, keyCode, scanCode, modifiers, -1);
    }

    public static BaniraKeyboardEvent released(@Nonnull BaniraScreenInfo screen, int keyCode, int scanCode, int modifiers) {
        return new BaniraKeyboardEvent(Action.KEY_RELEASE, screen, keyCode, scanCode, modifiers, -1);
    }

    public static BaniraKeyboardEvent charTyped(@Nonnull BaniraScreenInfo screen, int codePoint, int modifiers) {
        return new BaniraKeyboardEvent(Action.CHAR_TYPED, screen, -1, -1, modifiers, codePoint);
    }

    public char character() {
        return codePoint >= 0 ? (char) codePoint : '\0';
    }

    public void cancel() {
        canceled = true;
    }

    /**
     * 由事件 Hub 写入统一按键语义；adapter 不需要自行处理原生 key-repeat。
     */
    public BaniraKeyboardEvent withPressMetadata(BaniraKeyPressTracker.Result press) {
        if (press == null) {
            return this;
        }
        this.pressCount = press.pressCount();
        this.doublePress = press.doublePress();
        this.repeatedPress = press.repeatedPress();
        this.heldRepeat = press.heldRepeat();
        this.pressTracked = true;
        return this;
    }

    public enum Action {
        KEY_PRESS,
        KEY_RELEASE,
        CHAR_TYPED
    }
}
