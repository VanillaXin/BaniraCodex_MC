package xin.vanilla.banira.common.util;


import net.minecraft.resources.Identifier;

public interface IIdentifier {

    String modId();

    IIdentifier instance();

    default Identifier empty() {
        return create("empty");
    }

    default Identifier create(String path) {
        return create(modId(), path);
    }

    default Identifier create(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    default Identifier parse(String identifier) {
        return Identifier.tryParse(identifier);
    }
}
