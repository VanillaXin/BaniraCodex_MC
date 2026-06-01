package xin.vanilla.banira.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loader-neutral registry for config holders.
 */
public final class ConfigRegistry {
    private static final Map<String, ConfigHolder> HOLDERS = new LinkedHashMap<>();

    private ConfigRegistry() {
    }

    public static void registerHolder(ConfigHolder holder) {
        HOLDERS.put(holder.getConfigName(), holder);
    }

    public static ConfigHolder get(String configKey) {
        return HOLDERS.get(configKey);
    }

    public static ConfigHolder get(String configName, ConfigScope scope) {
        String key = configName + "-" + extension(scope);
        return HOLDERS.get(key);
    }

    public static Map<String, ConfigHolder> getAll() {
        return new LinkedHashMap<>(HOLDERS);
    }

    private static String extension(ConfigScope scope) {
        if (scope == null) {
            return "toml";
        }
        switch (scope) {
            case CLIENT:
                return "toml";
            case SERVER:
                return "toml";
            case COMMON:
            default:
                return "toml";
        }
    }
}
