package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BaniraClientScreenEvent {
    private final BaniraClientScreenEventType type;
    private final Object nativeScreen;
    private final BaniraDrawContext draw;
    private final double mouseX;
    private final double mouseY;
    private final float partialTicks;

    public BaniraClientScreenEvent(BaniraClientScreenEventType type, Object nativeScreen, BaniraDrawContext draw,
                                   double mouseX, double mouseY, float partialTicks) {
        this.type = type != null ? type : BaniraClientScreenEventType.UNKNOWN;
        this.nativeScreen = nativeScreen;
        this.draw = draw;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTicks = partialTicks;
    }

    public <T> T nativeScreen(Class<T> type) {
        return type.isInstance(nativeScreen) ? type.cast(nativeScreen) : null;
    }
}
