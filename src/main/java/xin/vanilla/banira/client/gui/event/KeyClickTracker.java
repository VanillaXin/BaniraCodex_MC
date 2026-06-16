package xin.vanilla.banira.client.gui.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;

import java.util.function.LongSupplier;

/**
 * GUI 内部兼容壳；公共语义由 {@link BaniraKeyPressTracker} 提供。
 */
public final class KeyClickTracker {
    public static final long DEFAULT_DOUBLE_PRESS_MS = BaniraKeyPressTracker.DEFAULT_DOUBLE_PRESS_MS;

    private final BaniraKeyPressTracker delegate;

    public KeyClickTracker() {
        this(DEFAULT_DOUBLE_PRESS_MS, System::currentTimeMillis);
    }

    public KeyClickTracker(long doublePressMs, LongSupplier clock) {
        this.delegate = new BaniraKeyPressTracker(doublePressMs, clock);
    }

    public Result recordPress(int keyCode, int scanCode, int modifiers) {
        return new Result(delegate.recordPress(keyCode, scanCode, modifiers));
    }

    public void recordRelease(int keyCode, int scanCode, int modifiers) {
        delegate.recordRelease(keyCode, scanCode);
    }

    public void reset() {
        delegate.reset();
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Result {
        private final BaniraKeyPressTracker.Result delegate;
        private final int pressCount;
        private final boolean heldRepeat;

        private Result(BaniraKeyPressTracker.Result delegate) {
            this.delegate = delegate;
            this.pressCount = delegate.pressCount();
            this.heldRepeat = delegate.heldRepeat();
        }

        public boolean doublePress() {
            return delegate.doublePress();
        }

        public boolean repeatedPress() {
            return delegate.repeatedPress();
        }
    }
}
