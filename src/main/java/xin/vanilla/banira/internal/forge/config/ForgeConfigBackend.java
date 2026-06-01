package xin.vanilla.banira.internal.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueBackend;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

final class ForgeConfigBackend implements ConfigValueBackend {
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    @Nullable
    private ModConfig modConfig;

    ForgeConfigBackend(Map<String, ForgeConfigSpec.ConfigValue<?>> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    void setModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    @Override
    public Set<String> getValuePaths() {
        return values.keySet();
    }

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
    public void save() {
        if (modConfig != null) {
            modConfig.save();
        }
    }
}
