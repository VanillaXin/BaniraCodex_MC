package xin.vanilla.banira.internal.neoforge.platform;

import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraPlayerDataService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * NeoForge 1.21.1 的玩家 NBT 数据适配。
 */
public final class NeoForgeBaniraPlayerDataService implements BaniraPlayerDataService {
    public static final NeoForgeBaniraPlayerDataService INSTANCE = new NeoForgeBaniraPlayerDataService();

    private NeoForgeBaniraPlayerDataService() {
    }

    @Nonnull
    @Override
    public Object getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId) {
        return BaniraServerRuntime.playerDataManager().getOrCreate(playerUuid, modId);
    }

    @Override
    public void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data) {
        if (data != null && !(data instanceof CompoundTag)) {
            throw new IllegalArgumentException("NeoForge 1.21 player data must be a CompoundTag");
        }
        BaniraServerRuntime.playerDataManager().put(playerUuid, modId, (CompoundTag) data);
    }
}
