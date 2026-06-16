package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.platform.BaniraRegistryService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Forge 1.18.2 注册表适配层；具体 MC 类型不进入公开 platform API。
 */
public enum ForgeBaniraRegistryService implements BaniraRegistryService {
    INSTANCE;

    @Override
    public @Nullable String blockKey(@Nullable Object block) {
        ResourceLocation key = block instanceof Block ? ForgeRegistries.BLOCKS.getKey((Block) block) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object block(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.BLOCKS.getValue(location) : null;
    }

    @Override
    public @Nonnull Collection<?> blocks() {
        return ForgeRegistries.BLOCKS.getValues();
    }

    @Override
    public @Nullable String itemKey(@Nullable Object item) {
        ResourceLocation key = item instanceof Item ? ForgeRegistries.ITEMS.getKey((Item) item) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object item(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.ITEMS.getValue(location) : null;
    }

    @Override
    public @Nonnull Collection<?> items() {
        return ForgeRegistries.ITEMS.getValues();
    }

    @Override
    public @Nonnull Collection<String> itemTagIds(@Nullable Object item) {
        if (!(item instanceof Item) || ForgeRegistries.ITEMS.tags() == null) {
            return Collections.emptyList();
        }
        return ForgeRegistries.ITEMS.tags().getReverseTag((Item) item)
                .map(reverseTag -> reverseTag.getTagKeys()
                        .map(TagKey::location)
                        .sorted(ResourceLocation::compareTo)
                        .map(ResourceLocation::toString)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public @Nullable String entityTypeKey(@Nullable Object entityType) {
        ResourceLocation key = entityType instanceof EntityType ? ForgeRegistries.ENTITIES.getKey((EntityType<?>) entityType) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object entityType(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.ENTITIES.getValue(location) : null;
    }

    @Override
    public @Nonnull Collection<?> entityTypes() {
        return ForgeRegistries.ENTITIES.getValues();
    }

    @Override
    public @Nullable String effectKey(@Nullable Object effect) {
        ResourceLocation key = effect instanceof MobEffect ? ForgeRegistries.MOB_EFFECTS.getKey((MobEffect) effect) : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable Object effect(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.MOB_EFFECTS.getValue(location) : null;
    }

    @Override
    public @Nonnull Collection<?> effects() {
        return ForgeRegistries.MOB_EFFECTS.getValues();
    }

    @Override
    public @Nullable Object biome(@Nullable String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.BIOMES.getValue(location) : null;
    }

    @Override
    public @Nonnull Collection<String> biomeIds() {
        return ForgeRegistries.BIOMES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }

    private static ResourceLocation parse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }
}
