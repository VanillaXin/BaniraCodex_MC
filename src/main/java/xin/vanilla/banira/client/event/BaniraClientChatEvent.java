package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class BaniraClientChatEvent {
    private String message;
    private boolean canceled;

    public BaniraClientChatEvent(String message) {
        this.message = message;
    }

    public void cancel() {
        this.canceled = true;
    }
}
