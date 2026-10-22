package xin.vanilla.banira.internal.client.ui;

public final class UiPointerOwnership {
    private Object owner;
    public void acquire(Object token, Runnable hide) {
        if (token == null) throw new IllegalArgumentException("Cursor owner");
        if (owner == token) return;
        hide.run();
        owner = token;
    }
    public void release(Object token, Runnable restore) {
        if (owner != token || token == null) return;
        owner = null;
        restore.run();
    }
}
