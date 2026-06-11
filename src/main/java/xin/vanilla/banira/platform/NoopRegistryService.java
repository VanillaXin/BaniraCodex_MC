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
import java.util.Collections;

/**
 * 测试和早期启动兜底实现；真实加载器需提供自己的注册表访问。
 */
public enum NoopRegistryService implements BaniraRegistryService {
    INSTANCE;

    @Override
    public @Nullable ResourceLocation blockKey(@Nullable Block block) {
        return null;
    }

    @Override
    public @Nullable Block block(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<Block> blocks() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ResourceLocation itemKey(@Nullable Item item) {
        return null;
    }

    @Override
    public @Nullable Item item(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<Item> items() {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Collection<ResourceLocation> itemTagIds(@Nullable Item item) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ResourceLocation entityTypeKey(@Nullable EntityType<?> entityType) {
        return null;
    }

    @Override
    public @Nullable EntityType<?> entityType(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<EntityType<?>> entityTypes() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ResourceLocation effectKey(@Nullable MobEffect effect) {
        return null;
    }

    @Override
    public @Nullable MobEffect effect(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<MobEffect> effects() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Biome biome(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<ResourceLocation> biomeIds() {
        return Collections.emptyList();
    }
}
