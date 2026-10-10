package xin.vanilla.banira.platform;

import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;

public enum UnavailableCostService implements BaniraCostService {
    INSTANCE;
    @Override public CostAccess access(Object payer, InventoryScope scope) {
        throw new IllegalStateException("Cost resources are unavailable on this platform");
    }
}
