package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;

import javax.annotation.Nonnull;

/**
 * GUI 内鼠标事件；Pre 阶段回调可取消后续原生处理。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraMouseEvent {
    private final @Nonnull Action action;
    private final @Nonnull Object screen;
    private final double mouseX;
    private final double mouseY;
    private final int button;
    private final double scrollDelta;
    private final @Nonnull Object nativeEvent;
    private int clickCount = 1;
    private boolean doubleClick;
    private boolean repeatedClick;
    private boolean clickTracked;
    private boolean canceled;

    private BaniraMouseEvent(@Nonnull Action action, @Nonnull Object screen, double mouseX, double mouseY,
                             int button, double scrollDelta, @Nonnull Object nativeEvent) {
        this.action = action;
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        this.scrollDelta = scrollDelta;
        this.nativeEvent = nativeEvent;
    }

    public static BaniraMouseEvent clicked(@Nonnull Object screen, double mouseX, double mouseY, int button,
                                           @Nonnull Object nativeEvent) {
        return new BaniraMouseEvent(Action.CLICK, screen, mouseX, mouseY, button, 0.0D, nativeEvent);
    }

    public static BaniraMouseEvent released(@Nonnull Object screen, double mouseX, double mouseY, int button,
                                            @Nonnull Object nativeEvent) {
        return new BaniraMouseEvent(Action.RELEASE, screen, mouseX, mouseY, button, 0.0D, nativeEvent);
    }

    public static BaniraMouseEvent scrolled(@Nonnull Object screen, double mouseX, double mouseY, double scrollDelta,
                                            @Nonnull Object nativeEvent) {
        return new BaniraMouseEvent(Action.SCROLL, screen, mouseX, mouseY, -1, scrollDelta, nativeEvent);
    }

    public void cancel() {
        canceled = true;
    }

    /**
     * 由事件 Hub 写入统一点击语义；adapter 不需要自行计算双击。
     */
    public BaniraMouseEvent withClickMetadata(BaniraMouseClickTracker.Result click) {
        if (click == null) {
            return this;
        }
        this.clickCount = click.clickCount();
        this.doubleClick = click.doubleClick();
        this.repeatedClick = click.repeatedClick();
        this.clickTracked = true;
        return this;
    }

    public enum Action {
        CLICK,
        RELEASE,
        SCROLL
    }
}
