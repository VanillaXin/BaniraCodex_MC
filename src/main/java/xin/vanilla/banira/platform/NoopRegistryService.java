package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * 测试和早期启动兜底实现；真实加载器需要提供自己的注册表访问。
 */
public enum NoopRegistryService implements BaniraRegistryService {
    INSTANCE;

    @Override
    public @Nullable String blockKey(@Nullable Object block) {
        return null;
    }

    @Override
    public @Nullable Object block(@Nullable String id) {
        return null;
    }

    @Override
    public @Nonnull Collection<?> blocks() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable String itemKey(@Nullable Object item) {
        return null;
    }

    @Override
    public @Nullable Object item(@Nullable String id) {
        return null;
    }

    @Override
    public @Nonnull Collection<?> items() {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Collection<String> itemTagIds(@Nullable Object item) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable String entityTypeKey(@Nullable Object entityType) {
        return null;
    }

    @Override
    public @Nullable Object entityType(@Nullable String id) {
        return null;
    }

    @Override
    public @Nonnull Collection<?> entityTypes() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable String effectKey(@Nullable Object effect) {
        return null;
    }

    @Override
    public @Nullable Object effect(@Nullable String id) {
        return null;
    }

    @Override
    public @Nonnull Collection<?> effects() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Object biome(@Nullable String id) {
        return null;
    }

    @Override
    public @Nonnull Collection<String> biomeIds() {
        return Collections.emptyList();
    }
}
