package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class BaniraHudRenderEvent {
    private final BaniraHudOverlayElement element;
    private final BaniraDrawContext draw;
    private final boolean beforeVanilla;
    private boolean canceled;

    public BaniraHudRenderEvent(BaniraHudOverlayElement element, BaniraDrawContext draw, boolean beforeVanilla) {
        this.element = element != null ? element : BaniraHudOverlayElement.UNKNOWN;
        this.draw = draw;
        this.beforeVanilla = beforeVanilla;
    }

    public boolean isBeforeVanilla() {
        return beforeVanilla;
    }

    public boolean isAfterVanilla() {
        return !beforeVanilla;
    }

    public void cancel() {
        cancelVanilla();
    }

    /**
     * Suppresses vanilla rendering for this HUD element when the current phase is cancelable.
     */
    public void cancelVanilla() {
        this.canceled = true;
    }

    public boolean isVanillaCanceled() {
        return canceled;
    }
}
