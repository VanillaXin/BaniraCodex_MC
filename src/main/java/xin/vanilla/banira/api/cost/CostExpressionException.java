package xin.vanilla.banira.api.cost;

public final class CostExpressionException extends IllegalArgumentException {
    private final int line, column;
    public CostExpressionException(String message, int line, int column) {
        super(message + " at " + line + ":" + column);
        this.line = line;
        this.column = column;
    }
    public int line() { return line; }
    public int column() { return column; }
}
