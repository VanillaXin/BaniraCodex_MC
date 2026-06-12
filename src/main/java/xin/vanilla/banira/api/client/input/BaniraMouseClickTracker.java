package xin.vanilla.banira.api.client.input;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.LongSupplier;

/**
 * 统一的鼠标连击判定器，不依赖 Minecraft、Forge 或 Fabric 事件类型。
 */
public final class BaniraMouseClickTracker {
    public static final long DEFAULT_DOUBLE_CLICK_MS = 300L;
    public static final double DEFAULT_DOUBLE_CLICK_DISTANCE = 5.0D;

    private final long doubleClickMs;
    private final double doubleClickDistance;
    private final LongSupplier clock;

    private long lastClickTime = Long.MIN_VALUE;
    private double lastClickX;
    private double lastClickY;
    private int lastButton = -1;
    private int clickCount;

    public BaniraMouseClickTracker() {
        this(DEFAULT_DOUBLE_CLICK_MS, DEFAULT_DOUBLE_CLICK_DISTANCE, System::currentTimeMillis);
    }

    public BaniraMouseClickTracker(long doubleClickMs, double doubleClickDistance, LongSupplier clock) {
        this.doubleClickMs = Math.max(1L, doubleClickMs);
        this.doubleClickDistance = Math.max(0.0D, doubleClickDistance);
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    public Result record(double mouseX, double mouseY, int button) {
        long now = clock.getAsLong();
        boolean repeated = lastButton == button
                && now - lastClickTime <= doubleClickMs
                && Math.abs(mouseX - lastClickX) <= doubleClickDistance
                && Math.abs(mouseY - lastClickY) <= doubleClickDistance;
        clickCount = repeated ? Math.min(clickCount + 1, 3) : 1;
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;
        lastButton = button;
        return new Result(clickCount);
    }

    public void reset() {
        lastClickTime = Long.MIN_VALUE;
        lastClickX = 0.0D;
        lastClickY = 0.0D;
        lastButton = -1;
        clickCount = 0;
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Result {
        private final int clickCount;

        private Result(int clickCount) {
            this.clickCount = Math.max(1, clickCount);
        }

        public boolean doubleClick() {
            return clickCount == 2;
        }

        public boolean repeatedClick() {
            return clickCount > 1;
        }
    }
}
