package xin.vanilla.banira.client.gui.tooltip;

/**
 * 每帧只保留最后提交、也就是视觉层级最高的 Tooltip 请求。
 */
public final class TooltipRequestCollector<T> {
    private Object screenToken;
    private T winner;
    private boolean hasWinner;

    public void beginFrame(Object screenToken) {
        this.screenToken = screenToken;
        winner = null;
        hasWinner = false;
    }

    public void submit(T request) {
        winner = request;
        hasWinner = true;
    }

    public boolean hasWinner() {
        return hasWinner;
    }

    public T winner() {
        return winner;
    }

    public Object screenToken() {
        return screenToken;
    }
}
