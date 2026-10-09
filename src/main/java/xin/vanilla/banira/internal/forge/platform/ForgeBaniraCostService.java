package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;
import xin.vanilla.banira.internal.forge.cost.ForgeCostResources;
import xin.vanilla.banira.platform.BaniraCostService;

public final class ForgeBaniraCostService implements BaniraCostService {
    @Override public CostAccess access(Object payer, InventoryScope scope) {
        if (!(payer instanceof ServerPlayerEntity)) throw new IllegalArgumentException("Server player required");
        return ForgeCostResources.access((ServerPlayerEntity) payer, scope);
    }
}
