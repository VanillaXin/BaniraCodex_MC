package xin.vanilla.banira.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置注册表，管理多个 ConfigHolder 的注册与加载
 */
public final class ConfigRegistry {

    private static final Map<String, ConfigHolder> HOLDERS = new LinkedHashMap<>();

    /**
     * 注册 ConfigHolder（供加载器配置服务调用）。
     */
    public static void registerHolder(ConfigHolder holder) {
        HOLDERS.put(holder.getConfigName(), holder);
    }

    /**
     * 获取配置持有者
     *
     * @param configKey 配置键，即注册时的 configName（如 "banira_codex-server"）
     */
    public static ConfigHolder get(String configKey) {
        return HOLDERS.get(configKey);
    }

    /**
     * 获取配置持有者
     */
    public static ConfigHolder get(String configName, ConfigScope scope) {
        String key = configName + "-" + scope.extension();
        return HOLDERS.get(key);
    }

    /**
     * 获取所有配置持有者
     */
    public static Map<String, ConfigHolder> getAll() {
        return new LinkedHashMap<>(HOLDERS);
    }
}
