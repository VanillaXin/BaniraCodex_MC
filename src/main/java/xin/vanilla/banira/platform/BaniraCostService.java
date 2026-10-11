package xin.vanilla.banira.platform;

import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;

public interface BaniraCostService {
    CostAccess access(Object payer, InventoryScope scope);
}
