package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

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

    public enum Action {
        CLICK,
        RELEASE,
        SCROLL
    }
}
