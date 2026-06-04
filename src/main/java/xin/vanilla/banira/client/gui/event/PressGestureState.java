package xin.vanilla.banira.client.gui.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.LongSupplier;

/**
 * Reusable press/long-press state for widgets that need progress feedback.
 */
@Getter
@Accessors(fluent = true)
public final class PressGestureState {
    private final LongSupplier clock;
    private boolean pressing;
    private boolean fired;
    private int button = -1;
    private long startedAt;

    public PressGestureState() {
        this(System::currentTimeMillis);
    }

    public PressGestureState(LongSupplier clock) {
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    public void press(int button) {
        this.pressing = true;
        this.fired = false;
        this.button = button;
        this.startedAt = clock.getAsLong();
    }

    public void release() {
        this.pressing = false;
        this.button = -1;
        this.startedAt = 0L;
    }

    public boolean pressing(int button) {
        return pressing && this.button == button;
    }

    public boolean ready(long durationMs) {
        return pressing && !fired && elapsedMillis() >= Math.max(1L, durationMs);
    }

    public void fire() {
        this.fired = true;
    }

    public long elapsedMillis() {
        return pressing ? Math.max(0L, clock.getAsLong() - startedAt) : 0L;
    }

    public float progress(long durationMs) {
        if (!pressing) {
            return 0.0F;
        }
        if (fired) {
            return 1.0F;
        }
        return Math.min(1.0F, elapsedMillis() / (float) Math.max(1L, durationMs));
    }
}
