package xin.vanilla.banira.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Banira 公共 API 使用的加载器无关资源标识符。
 */
@Getter
@EqualsAndHashCode
public final class BaniraIdentifier {
    public static final String DEFAULT_NAMESPACE = "minecraft";

    private final String namespace;
    private final String path;

    private BaniraIdentifier(@Nonnull String namespace, @Nonnull String path) {
        this.namespace = requirePart(namespace, "namespace");
        this.path = requirePart(path, "path");
    }

    @Nonnull
    public static BaniraIdentifier of(@Nonnull String namespace, @Nonnull String path) {
        return new BaniraIdentifier(namespace, path);
    }

    @Nonnull
    public static BaniraIdentifier parse(@Nonnull String identifier) {
        String value = Objects.requireNonNull(identifier, "identifier");
        int separator = value.indexOf(':');
        if (separator < 0) {
            return of(DEFAULT_NAMESPACE, value);
        }
        return of(value.substring(0, separator), value.substring(separator + 1));
    }

    @Nonnull
    public String asString() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return asString();
    }

    private static String requirePart(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return value;
    }
}
