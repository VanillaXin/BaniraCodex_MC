package xin.vanilla.banira.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.*;
import java.util.function.Predicate;

/**
 * ForgeConfigSpec 便捷构建器，提供类似 Fabric Cloth Config 的声明式 API
 * <p>
 * 使用示例：
 * <pre>{@code
 * ConfigHolder holder = ConfigSpecBuilder.create("mymod-server", ConfigScope.SERVER)
 *   .category("base", "基础设置")
 *     .define("helpHeader", "-----==== Help ====-----", "帮助头部")
 *     .defineInRange("helpNumPerPage", 5, 1, 9999, "每页数量")
 *     .define("defaultLanguage", "en_us", "默认语言")
 *   .endCategory()
 *   .category("sweep", "定时清理")
 *     .defineInRange("sweepInterval", 600000L, 0L, 604800000L, "清理间隔(ms)")
 *     .define("entityList", Arrays.asList("minecraft:arrow"), "实体名单")
 *   .endCategory()
 *   .build(modId);
 * }</pre>
 */
public final class ConfigSpecBuilder {

    private final String configName;
    private final ModConfig.Type configType;

    private final ForgeConfigSpec.Builder builder;
    private final List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap = new LinkedHashMap<>();

    private Deque<String> pathStack = new ArrayDeque<>();

    private ConfigSpecBuilder(String configName, ModConfig.Type configType) {
        this.configName = configName;
        this.configType = configType;
        this.builder = new ForgeConfigSpec.Builder();
    }

    /**
     * 创建构建器
     */
    public static ConfigSpecBuilder create(String configName, ConfigScope scope) {
        return new ConfigSpecBuilder(configName, toForgeType(scope));
    }

    private static ModConfig.Type toForgeType(ConfigScope scope) {
        if (scope == null) {
            return ModConfig.Type.COMMON;
        }
        switch (scope) {
            case CLIENT:
                return ModConfig.Type.CLIENT;
            case SERVER:
                return ModConfig.Type.SERVER;
            case COMMON:
            default:
                return ModConfig.Type.COMMON;
        }
    }

    /**
     * 开始一个配置分类（可折叠块）
     */
    public ConfigSpecBuilder category(String path, String... comments) {
        if (pathStack.isEmpty()) {
            builder.comment(comments);
            builder.push(path);
            pathStack.push(path);
        } else {
            builder.comment(comments);
            builder.push(path);
            pathStack.push(pathStack.peek() + "." + path);
        }
        return this;
    }

    /**
     * 结束当前分类
     */
    public ConfigSpecBuilder endCategory() {
        if (!pathStack.isEmpty()) {
            builder.pop();
            pathStack.pop();
        }
        return this;
    }

    /**
     * 定义字符串配置项
     */
    public ConfigSpecBuilder define(String key, String defaultValue, String... comments) {
        ForgeConfigSpec.ConfigValue<String> cv = builder
                .comment(comments)
                .define(key, defaultValue);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.STRING)
                .defaultValue(defaultValue)
                .build());
        return this;
    }

    /**
     * 定义布尔配置项
     */
    public ConfigSpecBuilder define(String key, boolean defaultValue, String... comments) {
        ForgeConfigSpec.ConfigValue<Boolean> cv = builder
                .comment(comments)
                .define(key, defaultValue);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.BOOLEAN)
                .defaultValue(defaultValue)
                .build());
        return this;
    }

    /**
     * 定义整数配置项（带范围）
     */
    public ConfigSpecBuilder defineInRange(String key, int defaultValue, int min, int max, String... comments) {
        ForgeConfigSpec.IntValue cv = builder
                .comment(comments)
                .defineInRange(key, defaultValue, min, max);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER)
                .defaultValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .build());
        return this;
    }

    /**
     * 定义长整数配置项（带范围）
     */
    public ConfigSpecBuilder defineInRange(String key, long defaultValue, long min, long max, String... comments) {
        ForgeConfigSpec.LongValue cv = builder
                .comment(comments)
                .defineInRange(key, defaultValue, min, max);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.LONG)
                .defaultValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .build());
        return this;
    }

    /**
     * 定义双精度浮点数配置项（带范围）
     */
    public ConfigSpecBuilder defineInRange(String key, double defaultValue, double min, double max, String... comments) {
        ForgeConfigSpec.DoubleValue cv = builder
                .comment(comments)
                .defineInRange(key, defaultValue, min, max);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.DOUBLE)
                .defaultValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .build());
        return this;
    }

    /**
     * 定义字符串列表配置项
     */
    public ConfigSpecBuilder defineList(String key, List<String> defaultValue, Predicate<Object> elementValidator, String... comments) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> cv = builder
                .comment(comments)
                .defineList(key, defaultValue, elementValidator != null ? elementValidator : o -> o instanceof String);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.STRING_LIST)
                .defaultValue(defaultValue)
                .build());
        return this;
    }

    /**
     * 定义枚举配置项
     */
    @SuppressWarnings({"unchecked"})
    public <E extends Enum<E>> ConfigSpecBuilder defineEnum(String key, E defaultValue, String... comments) {
        ForgeConfigSpec.EnumValue<E> cv = builder
                .comment(comments)
                .defineEnum(key, defaultValue);
        String path = getPathString(cv);
        valueMap.put(path, cv);
        descriptors.add(ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(comments.length > 0 ? comments[0] : key)
                .tooltip(Arrays.asList(comments))
                .valueType(ConfigEntryDescriptor.ConfigValueType.ENUM)
                .defaultValue(defaultValue)
                .enumClass((Class<? extends Enum<?>>) defaultValue.getClass())
                .build());
        return this;
    }

    private static String getPathString(ForgeConfigSpec.ConfigValue<?> cv) {
        return String.join(".", cv.getPath().stream().map(String::valueOf).toArray(String[]::new));
    }

    /**
     * 构建 ForgeConfigSpec 并创建 ConfigHolder
     */
    public ConfigHolder build() {
        return build("");
    }

    /**
     * 构建 ForgeConfigSpec 并创建 ConfigHolder
     */
    public ConfigHolder build(String modId) {
        // 确保所有 push 都有对应的 pop
        while (!pathStack.isEmpty()) {
            builder.pop();
            pathStack.pop();
        }
        ForgeConfigSpec spec = builder.build();
        return new ConfigHolder(modId, configName, configType, spec, new ArrayList<>(descriptors), new LinkedHashMap<>(valueMap),
                Collections.emptyMap(), Collections.emptyMap());
    }
}
