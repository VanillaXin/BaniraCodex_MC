package xin.vanilla.banira.internal.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueStore;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ForgeConfigBackend implements ConfigValueStore {
    private final ForgeConfigSpec spec;
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    @Nullable
    private ModConfig modConfig;

    ForgeConfigBackend(ForgeConfigSpec spec, Map<String, ForgeConfigSpec.ConfigValue<?>> values) {
        this.spec = spec;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    void setModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    @Override
    public Set<String> paths() {
        return values.keySet();
    }

    @Nullable
    @Override
    public Object get(String path) {
        ForgeConfigSpec.ConfigValue<?> value = values.get(path);
        return value != null ? value.get() : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(String path, Object value) {
        ForgeConfigSpec.ConfigValue configValue = values.get(path);
        if (configValue != null) {
            configValue.set(value);
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
        ForgeConfigSpec.ConfigValue<?> value = values.get(path);
        if (value == null) {
            return null;
        }
        return spec.getSpec().get(value.getPath());
    }
}
