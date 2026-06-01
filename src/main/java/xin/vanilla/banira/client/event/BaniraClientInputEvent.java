package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class BaniraClientInputEvent {
    private final BaniraClientInputEventType type;
    private final Object nativeScreen;
    private final double mouseX;
    private final double mouseY;
    private final int button;
    private final double scrollDelta;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;
    private boolean canceled;

    private BaniraClientInputEvent(Builder builder) {
        this.type = builder.type != null ? builder.type : BaniraClientInputEventType.UNKNOWN;
        this.nativeScreen = builder.nativeScreen;
        this.mouseX = builder.mouseX;
        this.mouseY = builder.mouseY;
        this.button = builder.button;
        this.scrollDelta = builder.scrollDelta;
        this.keyCode = builder.keyCode;
        this.scanCode = builder.scanCode;
        this.modifiers = builder.modifiers;
    }

    public static Builder builder(BaniraClientInputEventType type) {
        return new Builder().type(type);
    }

    public <T> T nativeScreen(Class<T> type) {
        return type.isInstance(nativeScreen) ? type.cast(nativeScreen) : null;
    }

    public void cancel() {
        this.canceled = true;
    }

    @Getter
    @Setter
    @Accessors(fluent = true, chain = true)
    public static final class Builder {
        private BaniraClientInputEventType type;
        private Object nativeScreen;
        private double mouseX;
        private double mouseY;
        private int button = -1;
        private double scrollDelta;
        private int keyCode = -1;
        private int scanCode = -1;
        private int modifiers;

        private Builder() {
        }

        public BaniraClientInputEvent build() {
            return new BaniraClientInputEvent(this);
        }
    }
}
