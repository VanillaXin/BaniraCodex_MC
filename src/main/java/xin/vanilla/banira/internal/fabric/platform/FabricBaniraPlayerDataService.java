package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraPlayerDataService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** Fabric 1.18.2 的玩家 NBT 数据适配。 */
public final class FabricBaniraPlayerDataService implements BaniraPlayerDataService {
    public static final FabricBaniraPlayerDataService INSTANCE = new FabricBaniraPlayerDataService();

    private FabricBaniraPlayerDataService() {
    }

    @Nonnull
    @Override
    public Object getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId) {
        return BaniraServerRuntime.playerDataManager().getOrCreate(playerUuid, modId);
    }

    @Override
    public void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data) {
        if (data != null && !(data instanceof CompoundTag)) {
            throw new IllegalArgumentException("Fabric 1.18 player data must be a CompoundTag");
        }
        BaniraServerRuntime.playerDataManager().put(playerUuid, modId, (CompoundTag) data);
    }
}
