package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.platform.BaniraRegistryService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Fabric 1.19.2 注册表适配层；公开 API 只暴露 Object 与字符串 id。
 */
public enum FabricBaniraRegistryService implements BaniraRegistryService {
    INSTANCE;

    @Override
    public @Nullable String blockKey(@Nullable Object block) {
        ResourceLocation key = block instanceof Block ? Registry.BLOCK.getKey((Block) block) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object block(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? Registry.BLOCK.get(location) : null;
    }

    @Override
    public @Nonnull Collection<?> blocks() {
        return values(Registry.BLOCK);
    }

    @Override
    public @Nullable String itemKey(@Nullable Object item) {
        ResourceLocation key = item instanceof Item ? Registry.ITEM.getKey((Item) item) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object item(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? Registry.ITEM.get(location) : null;
    }

    @Override
    public @Nonnull Collection<?> items() {
        return values(Registry.ITEM);
    }

    @Override
    public @Nonnull Collection<String> itemTagIds(@Nullable Object item) {
        if (!(item instanceof Item)) {
            return Collections.emptyList();
        }
        return ((Item) item).builtInRegistryHolder().tags()
                .map(TagKey::location)
                .sorted(ResourceLocation::compareTo)
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable String entityTypeKey(@Nullable Object entityType) {
        ResourceLocation key = entityType instanceof EntityType ? Registry.ENTITY_TYPE.getKey((EntityType<?>) entityType) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object entityType(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? Registry.ENTITY_TYPE.get(location) : null;
    }

    @Override
    public @Nonnull Collection<?> entityTypes() {
        return values(Registry.ENTITY_TYPE);
    }

    @Override
    public @Nullable String effectKey(@Nullable Object effect) {
        ResourceLocation key = effect instanceof MobEffect ? Registry.MOB_EFFECT.getKey((MobEffect) effect) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object effect(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? Registry.MOB_EFFECT.get(location) : null;
    }

    @Override
    public @Nonnull Collection<?> effects() {
        return values(Registry.MOB_EFFECT);
    }

    @Override
    public @Nullable Object biome(@Nullable String id) {
        return BiomeUtils.getBiome(id);
    }

    @Override
    public @Nonnull Collection<String> biomeIds() {
        return BiomeUtils.getAllIds();
    }

    private static <T> Collection<T> values(Iterable<T> registry) {
        return StreamSupport.stream(registry.spliterator(), false).collect(Collectors.toList());
    }

    private static ResourceLocation parse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }
}
