package xin.vanilla.banira.client.gui.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraPressGestureState;

import java.util.function.LongSupplier;

/**
 * GUI 内部兼容壳；公共语义由 {@link BaniraPressGestureState} 提供。
 */
@Getter
@Accessors(fluent = true)
public final class PressGestureState {
    private final BaniraPressGestureState delegate;

    public PressGestureState() {
        this(System::currentTimeMillis);
    }

    public PressGestureState(LongSupplier clock) {
        this.delegate = new BaniraPressGestureState(clock);
    }

    public boolean pressing() {
        return delegate.pressing();
    }

    public boolean fired() {
        return delegate.fired();
    }

    public int button() {
        return delegate.button();
    }

    public void press(int button) {
        delegate.press(button);
    }

    public void release() {
        delegate.release();
    }

    public boolean pressing(int button) {
        return delegate.pressing(button);
    }

    public boolean ready(long durationMs) {
        return delegate.ready(durationMs);
    }

    public void fire() {
        delegate.fire();
    }

    public long elapsedMillis() {
        return delegate.elapsedMillis();
    }

    public float progress(long durationMs) {
        return delegate.progress(durationMs);
    }
}
