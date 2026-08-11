package xin.vanilla.banira.api.permission;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 汇总各模组声明的虚拟权限，供命令建议与完整键校验使用。
 */
public final class BaniraVirtualPermissionRegistry {
    private static final Map<String, BaniraVirtualPermission> PERMISSIONS = new LinkedHashMap<>();

    private BaniraVirtualPermissionRegistry() {
    }

    @Nonnull
    public static synchronized BaniraVirtualPermission register(@Nonnull BaniraVirtualPermission permission) {
        Objects.requireNonNull(permission, "permission");
        String key = validate(permission);
        String lookupKey = normalize(key);
        if (PERMISSIONS.containsKey(lookupKey)) {
            throw new IllegalStateException("Virtual permission is already registered: " + key);
        }
        PERMISSIONS.put(lookupKey, permission);
        return permission;
    }

    public static synchronized void register(BaniraVirtualPermission... permissions) {
        if (permissions == null) {
            return;
        }
        for (BaniraVirtualPermission permission : permissions) {
            register(permission);
        }
    }

    @Nonnull
    public static synchronized Optional<BaniraVirtualPermission> find(String key) {
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PERMISSIONS.get(normalize(key)));
    }

    @Nonnull
    public static synchronized List<BaniraVirtualPermission> all() {
        List<BaniraVirtualPermission> result = new ArrayList<>(PERMISSIONS.values());
        result.sort(Comparator.comparingInt(BaniraVirtualPermission::sort)
                .thenComparing(BaniraVirtualPermission::key));
        return Collections.unmodifiableList(result);
    }

    static synchronized void clearForTests() {
        PERMISSIONS.clear();
    }

    private static String validate(BaniraVirtualPermission permission) {
        String modId = permission.modId() == null ? "" : permission.modId().trim();
        String id = permission.id() == null ? "" : permission.id().trim();
        if (modId.isEmpty() || id.isEmpty() || modId.indexOf(':') >= 0 || id.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Virtual permission must define a non-empty modId and id");
        }
        if (!permission.op()) {
            throw new IllegalArgumentException("Disabled virtual permission cannot be registered: " + modId + ":" + id);
        }
        return modId + ":" + id;
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
