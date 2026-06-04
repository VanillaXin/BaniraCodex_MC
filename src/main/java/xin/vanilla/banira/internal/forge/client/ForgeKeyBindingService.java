package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Forge 按键注册服务：静态初始化阶段先入队，client setup 时统一提交。
 */
public final class ForgeKeyBindingService {
    private static final List<ForgeBaniraKeyHandle> PENDING = new ArrayList<>();
    private static boolean flushCompleted;

    private ForgeKeyBindingService() {
    }

    @Nonnull
    public static BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
        String category = spec.category() != null ? spec.category() : BaniraInput.defaultCategory(spec.modId());
        KeyMapping binding = new KeyMapping(BaniraInput.descriptionId(spec.modId(), spec.suffix()), spec.defaultKey(), category);
        ForgeBaniraKeyHandle handle = new ForgeBaniraKeyHandle(binding, category, spec.defaultKey());
        enqueueOrRegister(handle);
        return handle;
    }

    public static void flushPendingRegistrations() {
        for (ForgeBaniraKeyHandle handle : PENDING) {
            ClientRegistry.registerKeyBinding(handle.binding());
        }
        PENDING.clear();
        flushCompleted = true;
    }

    private static void enqueueOrRegister(@Nonnull ForgeBaniraKeyHandle handle) {
        if (flushCompleted) {
            ClientRegistry.registerKeyBinding(handle.binding());
        } else {
            PENDING.add(handle);
        }
    }
}
