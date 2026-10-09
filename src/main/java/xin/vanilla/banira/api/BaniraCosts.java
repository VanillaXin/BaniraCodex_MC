package xin.vanilla.banira.api;

import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;

import java.util.Objects;

public final class BaniraCosts {
    private BaniraCosts() { }
    public static xin.vanilla.banira.api.cost.CostRequirement parse(String expression) {
        return xin.vanilla.banira.common.cost.CostExpressionParser.parse(expression);
    }
    public static CostAccess access(Object payer, InventoryScope scope) {
        return Banira.platform().costService().access(Objects.requireNonNull(payer, "payer"), Objects.requireNonNull(scope, "scope"));
    }
}
