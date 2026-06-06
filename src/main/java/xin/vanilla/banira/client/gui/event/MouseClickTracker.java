package xin.vanilla.banira.client.gui.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;

import java.util.function.LongSupplier;

/**
 * GUI 内部兼容壳；公共语义由 {@link BaniraMouseClickTracker} 提供。
 */
public final class MouseClickTracker {
    public static final long DEFAULT_DOUBLE_CLICK_MS = BaniraMouseClickTracker.DEFAULT_DOUBLE_CLICK_MS;
    public static final double DEFAULT_DOUBLE_CLICK_DISTANCE = BaniraMouseClickTracker.DEFAULT_DOUBLE_CLICK_DISTANCE;

    private final BaniraMouseClickTracker delegate;

    public MouseClickTracker() {
        this(DEFAULT_DOUBLE_CLICK_MS, DEFAULT_DOUBLE_CLICK_DISTANCE, System::currentTimeMillis);
    }

    public MouseClickTracker(long doubleClickMs, double doubleClickDistance, LongSupplier clock) {
        this.delegate = new BaniraMouseClickTracker(doubleClickMs, doubleClickDistance, clock);
    }

    public Result record(double mouseX, double mouseY, int button) {
        return new Result(delegate.record(mouseX, mouseY, button));
    }

    public void reset() {
        delegate.reset();
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Result {
        private final BaniraMouseClickTracker.Result delegate;
        private final int clickCount;

        private Result(BaniraMouseClickTracker.Result delegate) {
            this.delegate = delegate;
            this.clickCount = delegate.clickCount();
        }

        public boolean doubleClick() {
            return delegate.doubleClick();
        }

        public boolean repeatedClick() {
            return delegate.repeatedClick();
        }
    }
}
