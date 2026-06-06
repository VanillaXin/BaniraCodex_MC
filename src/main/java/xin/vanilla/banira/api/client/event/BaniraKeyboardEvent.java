package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * GUI 内键盘/字符输入事件；Pre 阶段回调可取消后续原生处理。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraKeyboardEvent {
    private final @Nonnull Action action;
    private final @Nonnull Object screen;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;
    private final int codePoint;
    private final @Nonnull Object nativeEvent;
    private boolean canceled;

    private BaniraKeyboardEvent(@Nonnull Action action, @Nonnull Object screen, int keyCode, int scanCode,
                                int modifiers, int codePoint, @Nonnull Object nativeEvent) {
        this.action = action;
        this.screen = screen;
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.codePoint = codePoint;
        this.nativeEvent = nativeEvent;
    }

    public static BaniraKeyboardEvent pressed(@Nonnull Object screen, int keyCode, int scanCode, int modifiers,
                                              @Nonnull Object nativeEvent) {
        return new BaniraKeyboardEvent(Action.KEY_PRESS, screen, keyCode, scanCode, modifiers, -1, nativeEvent);
    }

    public static BaniraKeyboardEvent released(@Nonnull Object screen, int keyCode, int scanCode, int modifiers,
                                               @Nonnull Object nativeEvent) {
        return new BaniraKeyboardEvent(Action.KEY_RELEASE, screen, keyCode, scanCode, modifiers, -1, nativeEvent);
    }

    public static BaniraKeyboardEvent charTyped(@Nonnull Object screen, int codePoint, int modifiers,
                                                @Nonnull Object nativeEvent) {
        return new BaniraKeyboardEvent(Action.CHAR_TYPED, screen, -1, -1, modifiers, codePoint, nativeEvent);
    }

    public char character() {
        return codePoint >= 0 ? (char) codePoint : '\0';
    }

    public void cancel() {
        canceled = true;
    }

    public enum Action {
        KEY_PRESS,
        KEY_RELEASE,
        CHAR_TYPED
    }
}
