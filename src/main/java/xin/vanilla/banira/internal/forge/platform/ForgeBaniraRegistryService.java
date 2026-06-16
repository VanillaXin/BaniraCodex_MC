package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.platform.BaniraRegistryService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Forge 1.16.5 注册表访问实现；映射类差异只留在 internal.forge。
 */
public final class ForgeBaniraRegistryService implements BaniraRegistryService {
    @Override
    public String blockKey(Object block) {
        ResourceLocation key = block instanceof Block && ForgeRegistries.BLOCKS != null
                ? ForgeRegistries.BLOCKS.getKey((Block) block)
                : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public Object block(String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.BLOCKS.getValue(location) : null;
    }

    @Override
    public Collection<?> blocks() {
        if (ForgeRegistries.BLOCKS == null) return Collections.emptyList();
        List<Block> blocks = new ArrayList<>();
        ForgeRegistries.BLOCKS.forEach(blocks::add);
        return blocks;
    }

    @Override
    public String itemKey(Object item) {
        ResourceLocation key = item instanceof Item && ForgeRegistries.ITEMS != null
                ? ForgeRegistries.ITEMS.getKey((Item) item)
                : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public Object item(String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.ITEMS.getValue(location) : null;
    }

    @Override
    public Collection<?> items() {
        if (ForgeRegistries.ITEMS == null) return Collections.emptyList();
        List<Item> items = new ArrayList<>();
        ForgeRegistries.ITEMS.forEach(items::add);
        return items;
    }

    @Override
    public Collection<String> itemTagIds(Object item) {
        return Collections.emptyList();
    }

    @Override
    public String entityTypeKey(Object entityType) {
        ResourceLocation key = entityType instanceof EntityType && ForgeRegistries.ENTITIES != null
                ? ForgeRegistries.ENTITIES.getKey((EntityType<?>) entityType)
                : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public Object entityType(String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.ENTITIES.getValue(location) : null;
    }

    @Override
    public Collection<?> entityTypes() {
        if (ForgeRegistries.ENTITIES == null) return Collections.emptyList();
        List<EntityType<?>> entityTypes = new ArrayList<>();
        ForgeRegistries.ENTITIES.forEach(entityTypes::add);
        return entityTypes;
    }

    @Override
    public String effectKey(Object effect) {
        ResourceLocation key = effect instanceof Effect && ForgeRegistries.POTIONS != null
                ? ForgeRegistries.POTIONS.getKey((Effect) effect)
                : null;
        return key != null ? key.toString() : null;
    }

    @Override
    public Object effect(String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.POTIONS.getValue(location) : null;
    }

    @Override
    public Collection<?> effects() {
        if (ForgeRegistries.POTIONS == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        ForgeRegistries.POTIONS.forEach(effects::add);
        return effects;
    }

    @Override
    public Object biome(String id) {
        ResourceLocation location = parse(id);
        return location != null ? ForgeRegistries.BIOMES.getValue(location) : null;
    }

    @Override
    public Collection<String> biomeIds() {
        Set<ResourceLocation> keys = ForgeRegistries.BIOMES != null ? ForgeRegistries.BIOMES.getKeys() : Collections.emptySet();
        return keys.stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    private static ResourceLocation parse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }
}
