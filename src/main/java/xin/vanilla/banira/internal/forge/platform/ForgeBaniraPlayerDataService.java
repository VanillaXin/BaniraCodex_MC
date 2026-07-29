package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.nbt.CompoundNBT;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.platform.BaniraPlayerDataService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Forge 1.16.5 的玩家 NBT 数据适配。
 */
public final class ForgeBaniraPlayerDataService implements BaniraPlayerDataService {
    public static final ForgeBaniraPlayerDataService INSTANCE = new ForgeBaniraPlayerDataService();

    private ForgeBaniraPlayerDataService() {
    }

    @Nonnull
    @Override
    public Object getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId) {
        return BaniraCodex.playerDataManager.getOrCreate(playerUuid, modId);
    }

    @Override
    public void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data) {
        if (data != null && !(data instanceof CompoundNBT)) {
            throw new IllegalArgumentException("Forge 1.16 player data must be a CompoundNBT");
        }
        BaniraCodex.playerDataManager.put(playerUuid, modId, (CompoundNBT) data);
    }

    @Override
    public void flush(@Nonnull UUID playerUuid) {
        BaniraCodex.playerDataManager.saveToDisk(playerUuid);
    }
}
