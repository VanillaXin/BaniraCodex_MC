package xin.vanilla.banira.common.config.view;

import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import java.util.*;
import java.util.function.Supplier;

/**
 * Resolves a live handle per operation while retained category objects remain reusable.
 */
public final class ConfigViewBinding {
    private final Class<?> configClass;
    private final Supplier<? extends BaniraConfigHandle> handles;
    private final Config.UnboundAccess unbound;
    private final Map<ConfigViewField<?>, Boolean> fields = new IdentityHashMap<>();
    private final ConfigViewDefaults defaults = new ConfigViewDefaults();
    private BaniraConfigHandle checkedHandle;

    public ConfigViewBinding(Class<?> configClass, Supplier<? extends BaniraConfigHandle> handles,
                             Config.UnboundAccess unbound, ConfigViewField<?>... fields) {
        this.configClass = Objects.requireNonNull(configClass, "configClass");
        this.handles = Objects.requireNonNull(handles, "handles");
        this.unbound = Objects.requireNonNull(unbound, "unbound");
        Set<String> paths = new HashSet<>();
        for (ConfigViewField<?> field : fields) {
            Objects.requireNonNull(field, "field");
            if (!paths.add(field.path)) throw new IllegalArgumentException("Duplicate config path: " + field.path);
            this.fields.put(field, Boolean.TRUE);
        }
    }

    public void requireAvailable() {
        handle();
    }

    public BaniraConfigHandle handle() {
        BaniraConfigHandle handle = handles.get();
        check(handle);
        return handle;
    }

    private synchronized void check(BaniraConfigHandle handle) {
        if (handle != checkedHandle) {
            checkedHandle = null;
            defaults.clear();
            if (handle != null) {
                for (ConfigViewField<?> field : fields.keySet()) {
                    // Some backends report the live value's class, not the schema type.
                    Object declaredDefault = handle.defaultValue(field.path);
                    Class<?> type = declaredDefault == null ? handle.valueClass(field.path) : declaredDefault.getClass();
                    if (!handle.hasValue(field.path) || type == null
                            || !field.valueType.isAssignableFrom(ConfigViewValues.box(type))) {
                        throw new IllegalStateException("Config view schema mismatch: "
                                + configClass.getName() + ":" + field.path);
                    }
                }
                checkedHandle = handle;
            }
        }
        if (handle == null && unbound == Config.UnboundAccess.REQUIRE_REGISTERED) {
            throw new IllegalStateException("Config is not registered: " + configClass.getName());
        }
    }

    public <T> T read(ConfigViewField<T> field) {
        requireField(field);
        BaniraConfigHandle handle = handle();
        return ConfigViewValues.read(field, handle == null ? null : handle.get(field.path),
                () -> handle == null ? defaults.read(field) : handle.defaultValue(field.path));
    }

    public <T> void write(ConfigViewField<T> field, T value) {
        requireField(field);
        BaniraConfigHandle handle = handle();
        if (handle == null) return;
        Object copied = ConfigViewValues.write(field, value);
        if (!handle.validate(field.path, copied)) {
            throw new IllegalArgumentException("Invalid config value: " + configClass.getName() + ":" + field.path);
        }
        handle.set(field.path, copied);
    }

    private void requireField(ConfigViewField<?> field) {
        if (!fields.containsKey(field)) {
            throw new IllegalArgumentException("Field does not belong to config view: " + configClass.getName());
        }
    }
}
