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
    public @Nullable ResourceLocation effectKey(@Nullable Effect effect) {
        return null;
    }

    @Override
    public @Nullable Effect effect(@Nullable ResourceLocation id) {
        return null;
    }

    @Override
    public @Nonnull Collection<Effect> effects() {
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
