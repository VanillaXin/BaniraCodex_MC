package xin.vanilla.banira.api.cost;

import java.util.Objects;

public final class CostFailure {
    public static final CostFailure NONE = of("banira:none");
    public static final CostFailure INVALID_RESULT = of("banira:invalid_result");
    public static final CostFailure AMOUNT_OVERFLOW = of("banira:amount_overflow");
    public static final CostFailure INVALID_OPERATION = of("banira:invalid_operation");
    public static final CostFailure CONFIGURATION_UNAVAILABLE = of("banira:configuration_unavailable");
    public static final CostFailure ACTOR_UNAVAILABLE = of("banira:actor_unavailable");
    public static final CostFailure INSUFFICIENT_RESOURCE = of("banira:insufficient_resource");
    public static final CostFailure INVALID_PAYMENT = of("banira:invalid_payment");
    public static final CostFailure PAYMENT_FAILED = of("banira:payment_failed");
    public static final CostFailure UNCERTAIN = of("banira:uncertain");
    public static final CostFailure RATE_LIMITED = of("banira:rate_limited");
    private final String code;

    private CostFailure(String code) { this.code = code; }

    public static CostFailure of(String code) {
        Objects.requireNonNull(code, "code");
        if (code.length() > 256 || !code.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid cost failure code");
        return new CostFailure(code);
    }

    public String code() { return code; }
    public boolean isFailure() { return !equals(NONE); }
    @Override public boolean equals(Object other) {
        return other instanceof CostFailure && code.equals(((CostFailure) other).code);
    }
    @Override public int hashCode() { return code.hashCode(); }
    @Override public String toString() { return code; }
}
