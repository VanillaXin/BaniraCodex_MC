package xin.vanilla.banira.internal.neoforge.platform;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.cost.CostAccess;
import xin.vanilla.banira.api.cost.InventoryScope;
import xin.vanilla.banira.internal.neoforge.cost.NeoForgeCostResources;
import xin.vanilla.banira.platform.BaniraCostService;

public final class NeoForgeBaniraCostService implements BaniraCostService {
    @Override public CostAccess access(Object payer, InventoryScope scope) {
        if (!(payer instanceof ServerPlayer)) throw new IllegalArgumentException("Server player required");
        return NeoForgeCostResources.access((ServerPlayer) payer, scope);
    }
}
