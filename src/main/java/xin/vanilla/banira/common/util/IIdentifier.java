package xin.vanilla.banira.common.util;


import net.minecraft.util.ResourceLocation;

public interface IIdentifier {

    String modId();

    IIdentifier instance();

    default ResourceLocation empty() {
        return create("empty");
    }

    default ResourceLocation create(String path) {
        return create(modId(), path);
    }

    default ResourceLocation create(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    default ResourceLocation parse(String identifier) {
        return ResourceLocation.tryParse(identifier);
    }
}
