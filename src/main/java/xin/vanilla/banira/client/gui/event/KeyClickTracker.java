package xin.vanilla.banira.client.gui.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Tracks repeated key presses while ignoring native key-repeat while a key is held.
 */
public final class KeyClickTracker {
    public static final long DEFAULT_DOUBLE_PRESS_MS = 300L;

    private final long doublePressMs;
    private final LongSupplier clock;
    private final Set<DownIdentity> pressedKeys = new HashSet<>();

    private long lastPressTime = Long.MIN_VALUE;
    private KeyIdentity lastKey;
    private int pressCount;

    public KeyClickTracker() {
        this(DEFAULT_DOUBLE_PRESS_MS, System::currentTimeMillis);
    }

    public KeyClickTracker(long doublePressMs, LongSupplier clock) {
        this.doublePressMs = Math.max(1L, doublePressMs);
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    public Result recordPress(int keyCode, int scanCode, int modifiers) {
        KeyIdentity key = new KeyIdentity(keyCode, scanCode, modifiers);
        if (!pressedKeys.add(new DownIdentity(keyCode, scanCode))) {
            return Result.heldRepeatResult();
        }

        long now = clock.getAsLong();
        boolean repeated = key.equals(lastKey) && now - lastPressTime <= doublePressMs;
        pressCount = repeated ? Math.min(pressCount + 1, 3) : 1;
        lastPressTime = now;
        lastKey = key;
        return Result.of(pressCount);
    }

    public void recordRelease(int keyCode, int scanCode, int modifiers) {
        pressedKeys.remove(new DownIdentity(keyCode, scanCode));
    }

    public void reset() {
        pressedKeys.clear();
        lastPressTime = Long.MIN_VALUE;
        lastKey = null;
        pressCount = 0;
    }

    private static final class KeyIdentity {
        private final int keyCode;
        private final int scanCode;
        private final int modifiers;

        private KeyIdentity(int keyCode, int scanCode, int modifiers) {
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof KeyIdentity)) {
                return false;
            }
            KeyIdentity that = (KeyIdentity) o;
            return keyCode == that.keyCode && scanCode == that.scanCode && modifiers == that.modifiers;
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyCode, scanCode, modifiers);
        }
    }

    private static final class DownIdentity {
        private final int keyCode;
        private final int scanCode;

        private DownIdentity(int keyCode, int scanCode) {
            this.keyCode = keyCode;
            this.scanCode = scanCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DownIdentity)) {
                return false;
            }
            DownIdentity that = (DownIdentity) o;
            return keyCode == that.keyCode && scanCode == that.scanCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyCode, scanCode);
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Result {
        private final int pressCount;
        private final boolean heldRepeat;

        private Result(int pressCount, boolean heldRepeat) {
            this.pressCount = Math.max(1, pressCount);
            this.heldRepeat = heldRepeat;
        }

        private static Result of(int pressCount) {
            return new Result(pressCount, false);
        }

        private static Result heldRepeatResult() {
            return new Result(1, true);
        }

        public boolean doublePress() {
            return !heldRepeat && pressCount == 2;
        }

        public boolean repeatedPress() {
            return !heldRepeat && pressCount > 1;
        }
    }
}
