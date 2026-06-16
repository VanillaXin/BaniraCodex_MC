package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 跨版本注册表访问入口。
 * <p>
 * MC 1.17 起很多注册表类型包名发生迁移，所以公开 platform 层只暴露对象和字符串 id。
 */
public interface BaniraRegistryService {
    @Nullable
    String blockKey(@Nullable Object block);

    @Nullable
    Object block(@Nullable String id);

    @Nonnull
    Collection<?> blocks();

    @Nullable
    String itemKey(@Nullable Object item);

    @Nullable
    Object item(@Nullable String id);

    @Nonnull
    Collection<?> items();

    @Nonnull
    Collection<String> itemTagIds(@Nullable Object item);

    @Nullable
    String entityTypeKey(@Nullable Object entityType);

    @Nullable
    Object entityType(@Nullable String id);

    @Nonnull
    Collection<?> entityTypes();

    @Nullable
    String effectKey(@Nullable Object effect);

    @Nullable
    Object effect(@Nullable String id);

    @Nonnull
    Collection<?> effects();

    @Nullable
    Object biome(@Nullable String id);

    @Nonnull
    Collection<String> biomeIds();
}
