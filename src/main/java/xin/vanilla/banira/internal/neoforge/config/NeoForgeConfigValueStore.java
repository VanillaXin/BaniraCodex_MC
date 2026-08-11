package xin.vanilla.banira.internal.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueStore;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ModConfigSpec 后端；仅供当前 Forge 分支的配置适配器使用。
 */
final class NeoForgeConfigValueStore implements ConfigValueStore {
    private final ModConfigSpec spec;
    private final Map<String, ModConfigSpec.ConfigValue<?>> values;
    @Nullable
    private ModConfig modConfig;

    NeoForgeConfigValueStore(ModConfigSpec spec, Map<String, ModConfigSpec.ConfigValue<?>> values) {
        this.spec = spec;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    ModConfigSpec spec() {
        return spec;
    }

    void bindModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    @Override
    public Set<String> paths() {
        return values.keySet();
    }

    @Nullable
    @Override
    public Object get(String path) {
        ModConfigSpec.ConfigValue<?> cv = values.get(path);
        return cv != null ? cv.get() : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(String path, Object value) {
        ModConfigSpec.ConfigValue cv = values.get(path);
        if (cv != null) {
            cv.set(value);
        }
    }

    @Override
    public Class<?> valueClass(String path) {
        try {
            Object current = get(path);
            return current != null ? current.getClass() : Object.class;
        } catch (Throwable ignored) {
            return Object.class;
        }
    }

    @Nullable
    @Override
    public Object defaultValue(String path) {
        ModConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null ? valueSpec.getDefault() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        ModConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null && valueSpec.test(value);
    }

    @Override
    public void save() {
        if (modConfig != null && modConfig.getLoadedConfig() != null) {
            modConfig.getLoadedConfig().save();
        }
    }

    @Nullable
    private ModConfigSpec.ValueSpec valueSpec(String path) {
        ModConfigSpec.ConfigValue<?> cv = values.get(path);
        if (cv == null) {
            return null;
        }
        return spec.getSpec().get(cv.getPath());
    }
}
