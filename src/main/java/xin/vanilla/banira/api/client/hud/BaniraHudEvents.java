package xin.vanilla.banira.api.client.hud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 子 mod 注册 HUD 回调的稳定入口。
 */
public final class BaniraHudEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Consumer<BaniraHudRenderEvent>> PRE_RENDER = new ArrayList<>();
    private static final List<Consumer<BaniraHudRenderEvent>> POST_RENDER = new ArrayList<>();

    private BaniraHudEvents() {
    }

    public static void onPreRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
        PRE_RENDER.add(callback);
    }

    public static void onPostRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
        POST_RENDER.add(callback);
    }

    public static void dispatchPre(@Nonnull BaniraHudRenderEvent event) {
        fire(PRE_RENDER, event, "hud pre render");
    }

    public static void dispatchPost(@Nonnull BaniraHudRenderEvent event) {
        fire(POST_RENDER, event, "hud post render");
    }

    private static void fire(List<Consumer<BaniraHudRenderEvent>> callbacks, BaniraHudRenderEvent event, String name) {
        for (Consumer<BaniraHudRenderEvent> callback : callbacks) {
            try {
                callback.accept(event);
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", name, t);
            }
        }
    }
}
