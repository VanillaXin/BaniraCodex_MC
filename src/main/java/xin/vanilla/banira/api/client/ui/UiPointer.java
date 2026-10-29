package xin.vanilla.banira.api.client.ui;

public final class UiPointer {
    public enum Kind {DOWN, MOVE, UP}

    private final Kind kind;
    private final double x, y;
    private final int button;

    public UiPointer(Kind kind, double x, double y, int button) {
        this.kind = java.util.Objects.requireNonNull(kind);
        this.x = x;
        this.y = y;
        this.button = button;
    }

    public Kind kind() {
        return kind;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public int button() {
        return button;
    }

    public boolean finite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }
}
