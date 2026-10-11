package xin.vanilla.banira.common.cost;

import xin.vanilla.banira.api.cost.CostAmount;
import xin.vanilla.banira.api.cost.CostFailure;

public final class CostAmounts {
    private CostAmounts() { }

    public static CostAmount normalize(double raw, int min, int max) {
        if (min < 0 || max < -1 || max >= 0 && max < min) throw new IllegalArgumentException("Invalid cost limits");
        if (!Double.isFinite(raw) || raw < 0) return CostAmount.failed(CostFailure.INVALID_RESULT);
        double bounded = Math.max(min, raw);
        if (max >= 0) bounded = Math.min(max, bounded);
        double rounded = Math.ceil(bounded);
        return rounded > Integer.MAX_VALUE ? CostAmount.failed(CostFailure.AMOUNT_OVERFLOW)
                : CostAmount.success(raw, (int) rounded);
    }
}
