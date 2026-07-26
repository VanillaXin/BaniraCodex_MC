package xin.vanilla.banira.common.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置注册表，管理多个 ConfigHolder 的注册与加载
 */
public final class ConfigRegistry {

    private static final Map<String, ConfigHolder> HOLDERS = new LinkedHashMap<>();

    /**
     * 注册配置
     *
     * @param holder       配置持有者
     * @param modContainer Mod 容器
     */
    public static void register(ConfigHolder holder, ModContainer modContainer) {
        String fileName = holder.getConfigName().endsWith(".toml") ? holder.getConfigName() : holder.getConfigName() + ".toml";
        ModConfig modConfig = ConfigTracker.INSTANCE.registerConfig(ForgeConfigAdapter.toForgeType(holder.getConfigScope()), holder.getSpec(), modContainer, fileName);
        holder.setModConfig(modConfig);
        HOLDERS.put(holder.getConfigName(), holder);
    }

    /**
     * 注册 ConfigHolder（供 ForgeConfigAdapter 等调用）
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
     * 获取配置持有者（兼容旧 API）
     */
    public static ConfigHolder get(String configName, ConfigScope scope) {
        String key = configName + "-" + ForgeConfigAdapter.toForgeType(scope).extension();
        return HOLDERS.get(key);
    }

    /**
     * 获取所有配置持有者
     */
    public static Map<String, ConfigHolder> getAll() {
        return new LinkedHashMap<>(HOLDERS);
    }
}
