package xin.vanilla.banira.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.lang.reflect.*;
import java.util.*;

/**
 * 从注解配置类构建 ForgeConfigSpec。
 * 配置类结构需与 Fabric Cloth Config 兼容，便于跨平台迁移。
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 1. 定义配置类（与 Fabric 相同结构）
 * &#64;Getter &#64;Setter &#64;Accessors(fluent = true)
 * &#64;Config(name = "mymod-server")
 * public class CommonConfig implements ConfigData {
 *     &#64;ConfigEntry.Gui.Tooltip("帮助头部")
 *     private String helpHeader = "-----==== Help ====-----";
 *     &#64;ConfigEntry.Gui.Tooltip("每页数量")
 *     &#64;ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
 *     private int helpInfoNumPerPage = 5;
 * }
 *
 * // 2. 注册（Forge）
 * ForgeConfigAdapter.register(CommonConfig.class, BaniraCodex.MODID);
 *
 * // 3. 使用
 * CommonConfig config = CommonConfig.get();
 * String h = config.helpHeader();
 * config.helpHeader("new").helpInfoNumPerPage(10);
 * }</pre>
 */
public final class ForgeConfigAdapter {

    private static final Map<Class<?>, ConfigHolder> HOLDER_MAP = new LinkedHashMap<>();

    /**
     * 从注解配置类构建并注册 Forge 配置
     *
     * @param configClass 配置类（需有 @Config、@Getter、@Setter、@Accessors(fluent=true)）
     * @param modId       Mod ID
     */
    public static <T> void register(Class<T> configClass, String modId) {
        Config configAnn = configClass.getAnnotation(Config.class);
        if (configAnn == null) {
            throw new IllegalArgumentException("Config class must be annotated with @Config: " + configClass.getName());
        }
        String configName = configAnn.name();
        ModConfig.Type configType = configAnn.type();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
        Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap = new LinkedHashMap<>();
        Map<String, String> categoryTooltips = new LinkedHashMap<>();

        buildFromClass(builder, configClass, "", descriptors, valueMap, categoryTooltips);

        ForgeConfigSpec spec = builder.build();
        ConfigHolder holder = new ConfigHolder(configName, configType, spec, descriptors, valueMap, categoryTooltips);

        String fileName = configName.endsWith(".toml") ? configName : configName + ".toml";
        ModList.get().getModContainerById(modId).ifPresent(container -> {
            ModConfig modConfig = new ModConfig(configType, spec, container, fileName);
            container.addConfig(modConfig);
            holder.setModConfig(modConfig);
        });

        HOLDER_MAP.put(configClass, holder);
        ConfigRegistry.registerHolder(holder);
    }

    /**
     * 获取配置的 fluent 代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> configClass) {
        ConfigHolder holder = HOLDER_MAP.get(configClass);
        if (holder == null) {
            throw new IllegalStateException("Config not registered: " + configClass.getName());
        }
        Class<?>[] ifaces = configClass.getInterfaces();
        if (ifaces.length == 0) {
            throw new IllegalArgumentException("Config class must implement an interface for fluent API: " + configClass.getName());
        }
        return (T) createProxy(ifaces, configClass, holder, "");
    }

    /**
     * 获取 ConfigHolder（用于 GUI 编辑等）
     */
    public static ConfigHolder getHolder(Class<?> configClass) {
        return HOLDER_MAP.get(configClass);
    }

    private static void buildFromClass(ForgeConfigSpec.Builder builder, Class<?> clazz, String prefix,
                                       List<ConfigEntryDescriptor> descriptors,
                                       Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap,
                                       Map<String, String> categoryTooltips) {
        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            field.setAccessible(true);
            String key = field.getName();
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            ConfigEntry.Gui.CollapsibleObject collapsible = field.getAnnotation(ConfigEntry.Gui.CollapsibleObject.class);
            ConfigEntry.CollapsibleObject collapsibleAlt = field.getAnnotation(ConfigEntry.CollapsibleObject.class);
            boolean isNested = collapsible != null || collapsibleAlt != null;
            if (isNested) {
                String[] tooltip = getTooltip(field);
                categoryTooltips.put(path, tooltip != null && tooltip.length > 0 ? tooltip[0] : key);
                builder.comment(tooltip).push(key);
                buildFromClass(builder, field.getType(), path, descriptors, valueMap, categoryTooltips);
                builder.pop();
                continue;
            }

            String[] comments = getTooltip(field);
            Class<?> type = field.getType();

            try {
                Object defaultValue = getDefaultValue(field);

                List<String> tooltipList = comments != null && comments.length > 0 ? Arrays.asList(comments) : Collections.emptyList();
                if (type == String.class) {
                    ForgeConfigSpec.ConfigValue<String> cv = builder.comment(comments).define(key, (String) defaultValue);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.STRING, defaultValue, null, null, null, tooltipList);
                } else if (type == boolean.class || type == Boolean.class) {
                    ForgeConfigSpec.ConfigValue<Boolean> cv = builder.comment(comments).define(key, (Boolean) defaultValue);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.BOOLEAN, defaultValue, null, null, null, tooltipList);
                } else if (type == int.class || type == Integer.class) {
                    ConfigEntry.BoundedDiscrete bd = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                    int min = bd != null ? bd.min() : Integer.MIN_VALUE;
                    int max = bd != null ? bd.max() : Integer.MAX_VALUE;
                    ForgeConfigSpec.IntValue cv = builder.comment(comments).defineInRange(key, (Integer) defaultValue, min, max);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.INTEGER, defaultValue, min, max, null, tooltipList);
                } else if (type == long.class || type == Long.class) {
                    ConfigEntry.BoundedLong bl = field.getAnnotation(ConfigEntry.BoundedLong.class);
                    long min = bl != null ? bl.min() : Long.MIN_VALUE;
                    long max = bl != null ? bl.max() : Long.MAX_VALUE;
                    ForgeConfigSpec.LongValue cv = builder.comment(comments).defineInRange(key, (Long) defaultValue, min, max);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.LONG, defaultValue, min, max, null, tooltipList);
                } else if (type == double.class || type == Double.class) {
                    ConfigEntry.BoundedDouble bd = field.getAnnotation(ConfigEntry.BoundedDouble.class);
                    double min = bd != null ? bd.min() : Double.MIN_VALUE;
                    double max = bd != null ? bd.max() : Double.MAX_VALUE;
                    ForgeConfigSpec.DoubleValue cv = builder.comment(comments).defineInRange(key, (Double) defaultValue, min, max);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.DOUBLE, defaultValue, min, max, null, tooltipList);
                } else if (List.class.isAssignableFrom(type)) {
                    @SuppressWarnings("unchecked")
                    List<String> defList = (List<String>) defaultValue;
                    ForgeConfigSpec.ConfigValue<List<? extends String>> cv = builder.comment(comments)
                            .defineList(key, defList != null ? defList : new ArrayList<>(), o -> o instanceof String);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.STRING_LIST, defList, null, null, null, tooltipList);
                } else if (type.isEnum()) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    ForgeConfigSpec.EnumValue cv = builder.comment(comments).defineEnum(key, (Enum) defaultValue);
                    addDescriptor(path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.ENUM, defaultValue, null, null, (Class<? extends Enum<?>>) type, tooltipList);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to build config for field: " + path, e);
            }
        }
    }

    private static String[] getTooltip(Field field) {
        try {
            java.lang.annotation.Annotation a = field.getAnnotation(ConfigEntry.Gui.Tooltip.class);
            if (a != null) {
                Method m = a.getClass().getMethod("value");
                String[] v = (String[]) m.invoke(a);
                if (v != null && v.length > 0) return v;
            }
        } catch (Exception ignored) {
        }
        ConfigEntry ce = field.getAnnotation(ConfigEntry.class);
        if (ce != null && ce.tooltip().length > 0) {
            return ce.tooltip();
        }
        return new String[]{field.getName()};
    }

    private static Object getDefaultValue(Field field) throws Exception {
        Class<?> clazz = field.getDeclaringClass();
        Object inst;
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            inst = ctor.newInstance();
        } catch (Exception e) {
            return getDefaultByType(field.getType());
        }
        return field.get(inst);
    }

    private static Object getDefaultByType(Class<?> type) {
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == double.class || type == Double.class) return 0.0;
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == String.class) return "";
        if (List.class.isAssignableFrom(type)) return new ArrayList<>();
        if (type.isEnum()) return type.getEnumConstants().length > 0 ? type.getEnumConstants()[0] : null;
        return null;
    }

    private static void addDescriptor(String path, ForgeConfigSpec.ConfigValue<?> cv,
                                      List<ConfigEntryDescriptor> descriptors,
                                      Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap,
                                      ConfigEntryDescriptor.ConfigValueType valueType,
                                      Object defaultValue, Number min, Number max, Class<? extends Enum<?>> enumClass,
                                      List<String> tooltip) {
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(path.substring(path.lastIndexOf('.') + 1))
                .tooltip(tooltip != null ? tooltip : Collections.emptyList())
                .valueType(valueType)
                .defaultValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .enumClass(enumClass)
                .configValue(cv)
                .build());
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    fields.add(f);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Object createProxy(Class<?>[] ifaces, Class<?> configClass, ConfigHolder holder, String prefix) {
        return Proxy.newProxyInstance(configClass.getClassLoader(), ifaces,
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if (method.getParameterCount() == 0) {
                        String path = resolvePath(holder, methodName, prefix);
                        if (path != null) {
                            return holder.get(path);
                        }
                    } else if (method.getParameterCount() == 1) {
                        String path = resolvePath(holder, methodName, prefix);
                        if (path != null) {
                            holder.set(path, args[0]);
                            return proxy;
                        }
                    }
                    if ("equals".equals(methodName)) return proxy == args[0];
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("toString".equals(methodName)) return "ConfigProxy@" + configClass.getSimpleName();
                    return null;
                });
    }

    private static String resolvePath(ConfigHolder holder, String methodName, String prefix) {
        for (String path : holder.getValueMap().keySet()) {
            String fieldName = path.substring(path.lastIndexOf('.') + 1);
            if (methodName.equals(fieldName)) {
                if (prefix.isEmpty()) return path;
                if (path.startsWith(prefix + ".")) return path;
            }
        }
        return null;
    }
}
