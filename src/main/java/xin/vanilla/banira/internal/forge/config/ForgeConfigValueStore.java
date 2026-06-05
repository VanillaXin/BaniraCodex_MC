package xin.vanilla.banira.internal.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueStore;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ForgeConfigSpec 后端；仅供当前 Forge 分支的配置适配器使用。
 */
final class ForgeConfigValueStore implements ConfigValueStore {
    private final ForgeConfigSpec spec;
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> values;
    @Nullable
    private ModConfig modConfig;

    ForgeConfigValueStore(ForgeConfigSpec spec, Map<String, ForgeConfigSpec.ConfigValue<?>> values) {
        this.spec = spec;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    ForgeConfigSpec spec() {
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
        ForgeConfigSpec.ConfigValue<?> cv = values.get(path);
        return cv != null ? cv.get() : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(String path, Object value) {
        ForgeConfigSpec.ConfigValue cv = values.get(path);
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
        ForgeConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null ? valueSpec.getDefault() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        ForgeConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null && valueSpec.test(value);
    }

    @Override
    public void save() {
        if (modConfig != null) {
            modConfig.save();
        }
    }

    @Nullable
    private ForgeConfigSpec.ValueSpec valueSpec(String path) {
        ForgeConfigSpec.ConfigValue<?> cv = values.get(path);
        if (cv == null) {
            return null;
        }
        return spec.getSpec().get(cv.getPath());
    }
}
