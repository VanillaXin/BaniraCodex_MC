package xin.vanilla.banira.api.cost;

import java.util.Objects;

public final class CostAmount {
    private final double raw;
    private final int amount;
    private final CostFailure failure;

    private CostAmount(double raw, int amount, CostFailure failure) {
        this.raw = raw;
        this.amount = amount;
        this.failure = failure;
    }

    public static CostAmount success(double raw, int amount) {
        if (!Double.isFinite(raw) || raw < 0 || amount < 0) throw new IllegalArgumentException("Invalid cost amount");
        return new CostAmount(raw, amount, CostFailure.NONE);
    }

    public static CostAmount failed(CostFailure failure) {
        if (!Objects.requireNonNull(failure, "failure").isFailure()) throw new IllegalArgumentException("Failure required");
        return new CostAmount(0, 0, failure);
    }

    public boolean isSuccess() { return !failure.isFailure(); }
    public CostFailure failure() { return failure; }
    public double rawAmount() { requireSuccess(); return raw; }
    public int amount() { requireSuccess(); return amount; }
    private void requireSuccess() {
        if (!isSuccess()) throw new IllegalStateException("Cost calculation failed: " + failure);
    }
}
