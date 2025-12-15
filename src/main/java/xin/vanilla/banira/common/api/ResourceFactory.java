package xin.vanilla.banira.common.api;


import net.minecraft.util.ResourceLocation;

/**
 * 资源创建器接口
 */
@FunctionalInterface
public interface ResourceFactory {

    String modId();

    default ResourceLocation empty() {
        return create("", "");
    }

    default ResourceLocation create(String path) {
        return create(modId(), path);
    }

    default ResourceLocation create(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    default ResourceLocation parse(String location) {
        return ResourceLocation.tryParse(location);
    }
}
