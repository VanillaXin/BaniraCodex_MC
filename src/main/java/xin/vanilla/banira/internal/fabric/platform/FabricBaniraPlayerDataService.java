package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.platform.BaniraPlayerDataService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Fabric 1.16.5 的玩家 NBT 数据适配。
 */
public final class FabricBaniraPlayerDataService implements BaniraPlayerDataService {
    public static final FabricBaniraPlayerDataService INSTANCE = new FabricBaniraPlayerDataService();

    private FabricBaniraPlayerDataService() {
    }

    @Nonnull
    @Override
    public Object getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId) {
        return BaniraCodex.playerDataManager.getOrCreate(playerUuid, modId);
    }

    @Override
    public void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data) {
        if (data != null && !(data instanceof CompoundTag)) {
            throw new IllegalArgumentException("Fabric 1.16 player data must be a CompoundTag");
        }
        BaniraCodex.playerDataManager.put(playerUuid, modId, (CompoundTag) data);
    }

    @Override
    public void flush(@Nonnull UUID playerUuid) {
        BaniraCodex.playerDataManager.saveToDisk(playerUuid);
    }
}
