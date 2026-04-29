package xin.vanilla.banira.editable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已注册、可由 Banira 配置编辑器与网络包解析使用的配置持有者。
 */
public final class EditableConfigRegistry {

    private static final Map<String, EditableConfigHolder> BY_CONFIG_NAME = new LinkedHashMap<>();
    private static final Map<Class<?>, EditableConfigHolder> BY_CONFIG_CLASS = new LinkedHashMap<>();

    private EditableConfigRegistry() {
    }

    /**
     * 从 AutoConfig 注册一份可由 Banira 编辑器打开的配置。
     *
     * @param modId        用于翻译键等作用域
     * @param autoHolder   {@link me.shedaniel.autoconfig.AutoConfig#register} 的返回值
     * @param syncToServer 是否允许「同步至服务端」「从服务端拉取」（与单端 client 配置一般为 false）
     */
    public static <T extends ConfigData> void registerAutoConfig(String modId, ConfigHolder<T> autoHolder, boolean syncToServer) {
        Class<T> configClass = autoHolder.getConfigClass();
        me.shedaniel.autoconfig.annotation.Config ann = configClass.getAnnotation(me.shedaniel.autoconfig.annotation.Config.class);
        if (ann == null) {
            throw new IllegalArgumentException("ConfigData class missing @Config: " + configClass.getName());
        }
        String name = ann.name();
        ConfigFieldStructure.Result structure = ConfigFieldStructure.build(configClass);
        EditableConfigHolder editable = new AutoConfigEditableHolder(modId, name, syncToServer, autoHolder, structure);
        register(editable, configClass);
    }

    public static void register(EditableConfigHolder holder, Class<?> configClass) {
        BY_CONFIG_NAME.put(holder.getConfigName(), holder);
        BY_CONFIG_CLASS.put(configClass, holder);
    }

    @Nullable
    public static EditableConfigHolder get(String configName) {
        return BY_CONFIG_NAME.get(configName);
    }

    @Nullable
    public static EditableConfigHolder getFor(Class<? extends ConfigData> configClass) {
        if (configClass == null) {
            return null;
        }
        ensureConfigDataInitialized(configClass);
        return BY_CONFIG_CLASS.get(configClass);
    }

    public static EditableConfigHolder getRequired(Class<? extends ConfigData> configClass) {
        ensureConfigDataInitialized(configClass);
        EditableConfigHolder h = BY_CONFIG_CLASS.get(configClass);
        if (h == null) {
            throw new IllegalStateException("Editable config not registered: " + configClass.getName());
        }
        return h;
    }

    /**
     * 仅加载 {@link ConfigData} 类不会执行其 {@code static} 块；若未先访问任意静态成员，则
     * {@code registerAutoConfig(...)} 尚未向本注册表登记。通过 {@link Class#forName(String, boolean, ClassLoader)} 触发类初始化。
     */
    private static void ensureConfigDataInitialized(Class<? extends ConfigData> configClass) {
        try {
            Class.forName(configClass.getName(), true, configClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load ConfigData class: " + configClass.getName(), e);
        }
    }

    public static Map<String, EditableConfigHolder> getAll() {
        return new LinkedHashMap<>(BY_CONFIG_NAME);
    }
}
