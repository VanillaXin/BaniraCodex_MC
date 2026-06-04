package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 键盘事件数据，用于承载按键码与 Banira 统一后的连击语义。
 */
@Data
@Accessors(chain = true, fluent = true)
public class KeyEvent {
    private int keyCode;
    private int scanCode;
    private int modifiers;
    private int pressCount = 1;
    private boolean doublePress;
    private boolean repeatedPress;
    private boolean heldRepeat;
    private boolean pressTracked;

    public static KeyEvent of(int keyCode, int scanCode, int modifiers) {
        return new KeyEvent().keyCode(keyCode).scanCode(scanCode).modifiers(modifiers);
    }

    public static KeyEvent of(int keyCode, int scanCode, int modifiers, KeyClickTracker.Result press) {
        int count = press != null ? press.pressCount() : 1;
        return of(keyCode, scanCode, modifiers)
                .pressCount(Math.max(1, count))
                .doublePress(press != null && press.doublePress())
                .repeatedPress(press != null && press.repeatedPress())
                .heldRepeat(press != null && press.heldRepeat())
                .pressTracked(press != null);
    }

    /**
     * 等同于 keyCode，便于语义化调用。
     */
    public int key() {
        return keyCode;
    }
}
