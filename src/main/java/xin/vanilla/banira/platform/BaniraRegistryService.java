package xin.vanilla.banira.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 跨加载器注册表访问入口；具体加载器注册表差异留在 platform 实现。
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
    ResourceLocation effectKey(@Nullable MobEffect effect);

    @Nullable
    MobEffect effect(@Nullable ResourceLocation id);

    @Nonnull
    Collection<MobEffect> effects();

    @Nullable
    Biome biome(@Nullable ResourceLocation id);

    @Nonnull
    Collection<ResourceLocation> biomeIds();
}
