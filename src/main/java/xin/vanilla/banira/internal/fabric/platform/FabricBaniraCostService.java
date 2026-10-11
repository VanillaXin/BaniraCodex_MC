package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;
import xin.vanilla.banira.internal.fabric.cost.FabricCostResources;
import xin.vanilla.banira.platform.BaniraCostService;

public final class FabricBaniraCostService implements BaniraCostService {
    @Override public CostAccess access(Object payer, InventoryScope scope) {
        if (!(payer instanceof ServerPlayer)) throw new IllegalArgumentException("Server player required");
        return FabricCostResources.access((ServerPlayer) payer, scope);
    }
}
