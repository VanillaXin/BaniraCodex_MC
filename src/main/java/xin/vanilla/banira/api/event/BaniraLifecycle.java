package xin.vanilla.banira.api.event;

import xin.vanilla.banira.common.util.BaniraEventBus;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 子 mod 推荐使用的生命周期入口，避免直接依赖 Forge/Fabric/NeoForge 事件类型。
 */
public final class BaniraLifecycle {
    private BaniraLifecycle() {
    }

    public static BaniraEventBus.Registration onCommonSetup(@Nonnull Consumer<BaniraCommonSetupEvent> callback) {
        return BaniraEventBus.ModLifecycle.onCommonSetup(callback);
    }
}
