package xin.vanilla.banira.api.permission;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 虚拟权限键的通用工具，避免子 mod 自己拼接 {@code modId:id}。
 */
public final class BaniraVirtualPermissions {

    private BaniraVirtualPermissions() {
    }

    @Nonnull
    public static String key(@Nonnull BaniraVirtualPermission permission) {
        return permission.modId() + ":" + permission.id();
    }

    @Nonnull
    public static Set<String> keys(BaniraVirtualPermission... permissions) {
        if (permissions == null || permissions.length == 0) {
            return Collections.emptySet();
        }
        return keys(Arrays.asList(permissions));
    }

    @Nonnull
    public static Set<String> keys(Collection<? extends BaniraVirtualPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptySet();
        }
        return permissions.stream()
                .filter(Objects::nonNull)
                .filter(BaniraVirtualPermission::op)
                .map(BaniraVirtualPermissions::key)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Nonnull
    public static String format(Collection<? extends BaniraVirtualPermission> permissions) {
        Set<String> keys = keys(permissions);
        if (keys.isEmpty()) {
            return "(empty)";
        }
        return keys.stream().sorted().collect(Collectors.joining(", "));
    }
}
