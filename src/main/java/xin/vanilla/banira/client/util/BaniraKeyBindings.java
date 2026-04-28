package xin.vanilla.banira.client.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.GLFWKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 便捷注册 {@link KeyMapping}：静态字段初始化阶段入队，在客户端初始化中一次性提交。
 *
 * <p>翻译键格式：{@code key.<modId>.<suffix>}，分类默认 {@code key.<modId>.categories}。{@code modId} 须与 fabric.mod.json 及语言文件前缀一致，便于子 Mod 复用。</p>
 *
 * <pre>{@code
 * public static final KeyMapping MY_KEY = BaniraKeyBindings.register(MyMod.MODID, "my_action", GLFWKey.GLFW_KEY_K);
 *
 * public static final KeyMapping OTHER = BaniraKeyBindings.spec(MyMod.MODID, "other")
 *         .defaultKey(GLFWKey.GLFW_KEY_J)
 *         .register();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class BaniraKeyBindings {
    private static final Logger LOGGER = LogManager.getLogger();


    private static final List<KeyMapping> PENDING = new ArrayList<>();
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
     * 使用 {@link #defaultCategory(String)} 注册；若在首次 {@link #flushPendingRegistrations(Consumer)} 之前调用，仅入队。
     */
    @Nonnull
    public static KeyMapping register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode) {
        return register(modId, suffix, defaultKeyScanCode, defaultCategory(modId));
    }

    /**
     * 指定分类翻译键注册。
     */
    @Nonnull
    public static KeyMapping register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode, @Nonnull String categoryTranslationKey) {
        requireModId(modId);
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must be non-empty");
        }
        KeyMapping binding = new KeyMapping(descriptionId(modId, suffix), defaultKeyScanCode, categoryTranslationKey);
        enqueueOrRegister(binding);
        return binding;
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
     * 应在客户端初始化中调用。
     */
    public static void flushPendingRegistrations(@Nonnull Consumer<KeyMapping> registrar) {
        for (KeyMapping binding : PENDING) {
            registrar.accept(binding);
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
         * 创建 {@link KeyMapping} 并完成入队或立即注册。
         */
        @Nonnull
        public KeyMapping register() {
            requireModId(modId);
            if (suffix.isEmpty()) {
                throw new IllegalStateException("modId/suffix must be set (use BaniraKeyBindings.spec(modId, suffix))");
            }
            String cat = category != null ? category : defaultCategory(modId);
            KeyMapping binding = new KeyMapping(descriptionId(modId, suffix), defaultKey, cat);
            enqueueOrRegister(binding);
            return binding;
        }
    }

    // endregion

    private static void requireModId(@Nonnull String modId) {
        if (modId.isEmpty()) {
            throw new IllegalArgumentException("modId must be non-empty");
        }
    }

    private static void enqueueOrRegister(@Nonnull KeyMapping binding) {
        if (flushCompleted) {
            LOGGER.warn("Key mapping registered after Fabric key binding initialization: {}", binding.getName());
        } else {
            PENDING.add(binding);
        }
    }
}
