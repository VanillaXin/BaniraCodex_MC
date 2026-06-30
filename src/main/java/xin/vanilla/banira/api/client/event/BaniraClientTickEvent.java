package xin.vanilla.banira.api.client.event;

/**
 * 客户端 tick 事件；当前只在 END 阶段分发。
 */
public final class BaniraClientTickEvent {
    public static final BaniraClientTickEvent END = new BaniraClientTickEvent();

    private BaniraClientTickEvent() {
    }
}
