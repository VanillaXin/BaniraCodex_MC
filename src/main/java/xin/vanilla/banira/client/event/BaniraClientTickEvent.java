package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BaniraClientTickEvent {
    private final BaniraTickPhase phase;

    public BaniraClientTickEvent(BaniraTickPhase phase) {
        this.phase = phase != null ? phase : BaniraTickPhase.END;
    }

    public boolean end() {
        return phase == BaniraTickPhase.END;
    }
}
