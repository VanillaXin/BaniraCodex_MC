package xin.vanilla.banira.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置注册表，管理多个 ConfigHolder 的注册与加载。
 */
public final class ConfigRegistry {
    private static final Map<String, ConfigHolder> HOLDERS = new LinkedHashMap<>();

    private ConfigRegistry() {
    }

    public static synchronized void registerHolder(ConfigHolder holder) {
        HOLDERS.put(holder.getConfigName(), holder);
    }

    public static synchronized ConfigHolder get(String configKey) {
        return HOLDERS.get(configKey);
    }

    public static synchronized ConfigHolder get(String configName, ConfigScope scope) {
        String key = configName + "-" + scope.extension();
        return HOLDERS.get(key);
    }

    public static synchronized Map<String, ConfigHolder> getAll() {
        return new LinkedHashMap<>(HOLDERS);
    }
}
