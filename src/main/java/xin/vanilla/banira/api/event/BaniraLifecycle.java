package xin.vanilla.banira.api.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 子 mod 推荐使用的生命周期入口，避免直接依赖 Forge/Fabric/NeoForge 事件类型。
 */
public final class BaniraLifecycle {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Consumer<BaniraCommonSetupEvent>> COMMON_SETUP_CALLBACKS = new CopyOnWriteArrayList<>();

    private BaniraLifecycle() {
    }

    public static BaniraEventRegistration onCommonSetup(@Nonnull Consumer<BaniraCommonSetupEvent> callback) {
        COMMON_SETUP_CALLBACKS.add(callback);
        return () -> COMMON_SETUP_CALLBACKS.remove(callback);
    }

    /**
     * 供加载器 adapter 派发生命周期事件；子 mod 通常只应注册回调。
     */
    public static void dispatchCommonSetup(@Nonnull BaniraCommonSetupEvent event) {
        for (Consumer<BaniraCommonSetupEvent> callback : COMMON_SETUP_CALLBACKS) {
            try {
                callback.accept(event);
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for mod common setup event", t);
            }
        }
    }
}
