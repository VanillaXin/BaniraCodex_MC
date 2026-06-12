package xin.vanilla.banira.platform;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 跨加载器注册表访问入口；具体注册表差异留在 platform 实现。
 */
public interface BaniraRegistryService {
    @Nullable
    ResourceLocation blockKey(@Nullable Block block);

    @Nullable
    Block block(@Nullable ResourceLocation id);

    @Nonnull
    Collection<Block> blocks();

    @Nullable
    ResourceLocation itemKey(@Nullable Item item);

    @Nullable
    Item item(@Nullable ResourceLocation id);

    @Nonnull
    Collection<Item> items();

    @Nonnull
    Collection<ResourceLocation> itemTagIds(@Nullable Item item);

    @Nullable
    ResourceLocation entityTypeKey(@Nullable EntityType<?> entityType);

    @Nullable
    EntityType<?> entityType(@Nullable ResourceLocation id);

    @Nonnull
    Collection<EntityType<?>> entityTypes();

    @Nullable
    ResourceLocation effectKey(@Nullable Effect effect);

    @Nullable
    Effect effect(@Nullable ResourceLocation id);

    @Nonnull
    Collection<Effect> effects();

    @Nullable
    Biome biome(@Nullable ResourceLocation id);

    @Nonnull
    Collection<ResourceLocation> biomeIds();
}
