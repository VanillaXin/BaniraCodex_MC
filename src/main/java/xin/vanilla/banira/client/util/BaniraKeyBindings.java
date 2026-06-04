package xin.vanilla.banira.client.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.internal.client.BaniraKeyBindingService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 便捷注册客户端键位：静态字段初始化阶段入队，在 Banira 客户端 setup 阶段
 * {@link #flushPendingRegistrations()} 一次性提交；若在 flush 之后调用 {@link #register}，会立即交给当前加载器适配层注册。
 *
 * <p>翻译键格式：{@code key.<modId>.<suffix>}，分类默认 {@code key.<modId>.categories}。{@code modId} 须与 {@code mods.toml} 及语言文件前缀一致，便于子 Mod 复用。</p>
 *
 * <pre>{@code
 * public static final BaniraKeyHandle MY_KEY = BaniraKeyBindings.register(MyMod.MODID, "my_action", GLFWKey.GLFW_KEY_K);
 *
 * public static final BaniraKeyHandle OTHER = BaniraKeyBindings.spec(MyMod.MODID, "other")
 *         .defaultKey(GLFWKey.GLFW_KEY_J)
 *         .register();
 * }</pre>
 */
public final class BaniraKeyBindings {

    private static final List<BaniraKeyHandle> PENDING = new ArrayList<>();
    private static boolean flushCompleted;

    private BaniraKeyBindings() {
    }

    // region 描述与分类

    /**
     * 默认键位分类翻译键 {@code key.<modId>.categories}
     */
    @Nonnull
    public static String defaultCategory(@Nonnull String modId) {
        requireModId(modId);
        return String.format("key.%s.categories", modId);
    }

    /**
     * 键位描述翻译键 {@code key.<modId>.<suffix>}
     */
    @Nonnull
    public static String descriptionId(@Nonnull String modId, @Nonnull String suffix) {
        requireModId(modId);
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must be non-empty");
        }
        return String.format("key.%s.%s", modId, suffix);
    }

    // endregion

    // region 注册

    /**
     * 使用 {@link #defaultCategory(String)} 注册；若在首次 {@link #flushPendingRegistrations()} 之前调用，仅入队。
     */
    @Nonnull
    public static BaniraKeyHandle register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode) {
        return register(modId, suffix, defaultKeyScanCode, defaultCategory(modId));
    }

    /**
     * 指定分类翻译键注册。
     */
    @Nonnull
    public static BaniraKeyHandle register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode, @Nonnull String categoryTranslationKey) {
        requireModId(modId);
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must be non-empty");
        }
        BaniraKeyHandle handle = BaniraKeyBindingService.create(descriptionId(modId, suffix), defaultKeyScanCode, categoryTranslationKey);
        enqueueOrRegister(handle);
        return handle;
    }

    /**
     * 流式配置入口。
     */
    @Nonnull
    public static Spec spec(@Nonnull String modId, @Nonnull String suffix) {
        requireModId(modId);
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must be non-empty");
        }
        return new Spec().modId(modId).suffix(suffix);
    }

    /**
     * 应在客户端 setup 阶段尽早调用（本模组由 {@link xin.vanilla.banira.internal.client.BaniraClientModSetup} 调用）。
     * 子 mod 可使用 {@link BaniraClientEventHub.ModLifecycle#onClientSetup(Runnable)} 安排自己的客户端初始化。
     */
    public static void flushPendingRegistrations() {
        for (BaniraKeyHandle handle : PENDING) {
            BaniraKeyBindingService.register(handle);
        }
        PENDING.clear();
        flushCompleted = true;
    }

    // endregion

    // region Spec

    /**
     * 链式注册配置（{@link Accessors#fluent()} / {@link Accessors#chain()}）
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static final class Spec {
        private @Nonnull String modId = "";
        private @Nonnull String suffix = "";
        private int defaultKey = GLFWKey.GLFW_KEY_UNKNOWN;
        private @Nullable String category;

        /**
         * 创建 Banira 键位句柄并完成入队或立即注册。
         */
        @Nonnull
        public BaniraKeyHandle register() {
            requireModId(modId);
            if (suffix.isEmpty()) {
                throw new IllegalStateException("modId/suffix must be set (use BaniraKeyBindings.spec(modId, suffix))");
            }
            String cat = category != null ? category : defaultCategory(modId);
            BaniraKeyHandle handle = BaniraKeyBindingService.create(descriptionId(modId, suffix), defaultKey, cat);
            enqueueOrRegister(handle);
            return handle;
        }
    }

    // endregion

    private static void requireModId(@Nonnull String modId) {
        if (modId.isEmpty()) {
            throw new IllegalArgumentException("modId must be non-empty");
        }
    }

    private static void enqueueOrRegister(@Nonnull BaniraKeyHandle handle) {
        if (flushCompleted) {
            BaniraKeyBindingService.register(handle);
        } else {
            PENDING.add(handle);
        }
    }
}
