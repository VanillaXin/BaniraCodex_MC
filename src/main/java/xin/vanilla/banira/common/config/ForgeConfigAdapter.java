package xin.vanilla.banira.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Predicate;

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
 *     &#64;ConfigEntry.Gui.CollapsibleObject
 *     private HelpCategory help = new HelpCategory();
 *     // ... 嵌套分类与 TestConfig 相同风格；运行时取值见 CommonConfig.get().help().helpHeader() 等
 * }
 *
 * // 2. 注册（Forge）
 * ForgeConfigAdapter.register(CommonConfig.class, BaniraCodex.MODID);
 *
 * // 3. 使用
 * CommonConfig.RootView config = CommonConfig.get();
 * String h = config.help().helpHeader();
 * config.help().helpHeader("new");
 * // 或直接 holder：config.holder().set("help.helpHeader", "new");
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
        ConfigScope configScope = configAnn.type();
        ModConfig.Type configType = toForgeType(configScope);

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
        Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap = new LinkedHashMap<>();
        Map<String, String> categoryTooltips = new LinkedHashMap<>();
        Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs = new LinkedHashMap<>();

        buildFromClass(builder, configClass, "", descriptors, valueMap, categoryTooltips, categoryTitleSpecs);

        ForgeConfigSpec spec = builder.build();
        ConfigHolder holder = new ConfigHolder(modId, configName, configScope, spec, descriptors, valueMap, categoryTooltips,
                categoryTitleSpecs);

        String fileName = configName.endsWith(".toml") ? configName : configName + ".toml";
        ModList.get().getModContainerById(modId).ifPresent(container -> {
            ModConfig modConfig = new ModConfig(configType, spec, container, fileName);
            container.addConfig(modConfig);
            holder.setModConfig(modConfig);
        });

        HOLDER_MAP.put(configClass, holder);
        ConfigRegistry.registerHolder(holder);
    }

    static ModConfig.Type toForgeType(ConfigScope scope) {
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
                                       Map<String, String> categoryTooltips,
                                       Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            field.setAccessible(true);
            String key = field.getName();
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            ConfigEntry.Gui.CollapsibleObject collapsible = field.getAnnotation(ConfigEntry.Gui.CollapsibleObject.class);
            ConfigEntry.CollapsibleObject collapsibleAlt = field.getAnnotation(ConfigEntry.CollapsibleObject.class);
            boolean isNested = collapsible != null || collapsibleAlt != null;
            if (isNested) {
                TooltipResolution tr = resolveTooltip(field);
                String[] fileC = tr.fileComments;
                categoryTooltips.put(path, fileC.length > 0 ? fileC[0] : key);
                categoryTitleSpecs.put(path, tr.toCategoryTitleSpec(key));
                if (fileC.length > 0) {
                    builder.comment(fileC);
                }
                builder.push(key);
                buildFromClass(builder, field.getType(), path, descriptors, valueMap, categoryTooltips, categoryTitleSpecs);
                builder.pop();
                continue;
            }

            TooltipResolution tr = resolveTooltip(field);
            String[] comments = tr.fileComments;
            Class<?> type = field.getType();

            try {
                Object defaultValue = getDefaultValue(field);

                if (type == String.class) {
                    ForgeConfigSpec.ConfigValue<String> cv = applyFileComments(builder, comments).define(key, (String) defaultValue);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.STRING, defaultValue, null, null, null, tr, 2);
                } else if (type == boolean.class || type == Boolean.class) {
                    ForgeConfigSpec.ConfigValue<Boolean> cv = applyFileComments(builder, comments).define(key, (Boolean) defaultValue);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.BOOLEAN, defaultValue, null, null, null, tr, 2);
                } else if (type == int.class || type == Integer.class) {
                    ConfigEntry.BoundedDiscrete bd = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                    int min = bd != null ? bd.min() : Integer.MIN_VALUE;
                    int max = bd != null ? bd.max() : Integer.MAX_VALUE;
                    ForgeConfigSpec.IntValue cv = applyFileComments(builder, comments).defineInRange(key, (Integer) defaultValue, min, max);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.INTEGER, defaultValue, min, max, null, tr, 2);
                } else if (type == long.class || type == Long.class) {
                    ConfigEntry.BoundedLong bl = field.getAnnotation(ConfigEntry.BoundedLong.class);
                    long min = bl != null ? bl.min() : Long.MIN_VALUE;
                    long max = bl != null ? bl.max() : Long.MAX_VALUE;
                    ForgeConfigSpec.LongValue cv = applyFileComments(builder, comments).defineInRange(key, (Long) defaultValue, min, max);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.LONG, defaultValue, min, max, null, tr, 2);
                } else if (type == double.class || type == Double.class) {
                    ConfigEntry.BoundedDouble bd = field.getAnnotation(ConfigEntry.BoundedDouble.class);
                    double min = bd != null ? bd.min() : Double.MIN_VALUE;
                    double max = bd != null ? bd.max() : Double.MAX_VALUE;
                    int decimalPlaces = bd != null ? bd.decimalPlaces() : 2;
                    ForgeConfigSpec.DoubleValue cv = applyFileComments(builder, comments).defineInRange(key, (Double) defaultValue, min, max);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.DOUBLE, defaultValue, min, max, null, tr, decimalPlaces);
                } else if (List.class.isAssignableFrom(type)) {
                    Class<?> elemClass = ConfigListSpecHelper.resolveListElementClass(field);
                    ConfigEntryDescriptor.ConfigValueType listType = ConfigListSpecHelper.listValueTypeForElement(elemClass);
                    Number[] bounds = ConfigListSpecHelper.listBoundsForValueType(field, listType);
                    Number min = bounds[0];
                    Number max = bounds[1];
                    int decPlaces = ConfigListSpecHelper.decimalPlacesForList(field, listType);
                    Class<? extends Enum<?>> enumClass = ConfigListSpecHelper.enumClassForList(elemClass);
                    List<?> rawDef = (List<?>) defaultValue;
                    List<Object> normDef = ConfigListSpecHelper.normalizeDefaultList(rawDef, listType, enumClass, min, max, decPlaces);
                    Predicate<Object> pred = ConfigListSpecHelper.listValidator(listType, enumClass, min, max, decPlaces);
                    @SuppressWarnings({"rawtypes"})
                    ForgeConfigSpec.ConfigValue<?> cv = applyFileComments(builder, comments)
                            .defineList(key, (List) normDef, pred);
                    addDescriptor(field, path, cv, descriptors, valueMap, listType, new ArrayList<>(normDef), min, max, enumClass, tr, decPlaces);
                } else if (type.isEnum()) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    ForgeConfigSpec.EnumValue cv = applyFileComments(builder, comments).defineEnum(key, (Enum) defaultValue);
                    addDescriptor(field, path, cv, descriptors, valueMap, ConfigEntryDescriptor.ConfigValueType.ENUM, defaultValue, null, null, (Class<? extends Enum<?>>) type, tr, 2);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to build config for field: " + path, e);
            }
        }
    }

    private static ForgeConfigSpec.Builder applyFileComments(ForgeConfigSpec.Builder builder, String[] comments) {
        if (comments != null && comments.length > 0) {
            return builder.comment(comments);
        }
        return builder;
    }

    private static TooltipResolution resolveTooltip(Field field) {
        ConfigEntry.Gui.Tooltip gt = field.getAnnotation(ConfigEntry.Gui.Tooltip.class);
        if (gt != null) {
            if (!StringUtils.isNullOrEmptyEx(gt.translationKey())) {
                return TooltipResolution.translationKey(gt.translationKey().trim());
            }
            Map<String, String> langMap = tooltipLangMapFromAnnotation(gt);
            if (!langMap.isEmpty()) {
                List<String> fileLines = buildLocalizedTooltipFileLines(langMap);
                return TooltipResolution.localized(Collections.unmodifiableMap(new LinkedHashMap<>(langMap)), fileLines);
            }
            String[] val = gt.value();
            if (val != null && val.length > 0) {
                List<String> lines = new ArrayList<>();
                for (String s : val) {
                    if (s != null) {
                        lines.add(s);
                    }
                }
                if (!lines.isEmpty()) {
                    return TooltipResolution.multiline(lines);
                }
            }
        }
        ConfigEntry ce = field.getAnnotation(ConfigEntry.class);
        if (ce != null && ce.tooltip().length > 0) {
            return TooltipResolution.multiline(Arrays.asList(ce.tooltip()));
        }
        return TooltipResolution.multiline(Collections.singletonList(field.getName()));
    }

    private static List<String> splitNonEmptyLines(String text) {
        if (StringUtils.isNullOrEmptyEx(text)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            String t = line.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 写入 TOML 时各语言块的顺序（未列出的键按字典序排在末尾）
     */
    private static final String[] TOOLTIP_FILE_COMMENT_LANG_ORDER = {
            "zh_cn", "zh_tw", "zh_hk",
            "ja_jp", "ko_kr",
            "en_us", "en_gb",
            "de_de", "es_es", "es_mx", "fr_fr", "fr_ca", "it_it", "pl_pl", "pt_br", "pt_pt", "ru_ru"
    };

    private static Map<String, String> tooltipLangMapFromAnnotation(ConfigEntry.Gui.Tooltip gt) {
        Map<String, String> m = new LinkedHashMap<>();
        putTooltipLang(m, "en_us", gt.en_us());
        putTooltipLang(m, "en_gb", gt.en_gb());
        putTooltipLang(m, "zh_cn", gt.zh_cn());
        putTooltipLang(m, "zh_tw", gt.zh_tw());
        putTooltipLang(m, "zh_hk", gt.zh_hk());
        putTooltipLang(m, "ja_jp", gt.ja_jp());
        putTooltipLang(m, "ko_kr", gt.ko_kr());
        putTooltipLang(m, "ru_ru", gt.ru_ru());
        putTooltipLang(m, "de_de", gt.de_de());
        putTooltipLang(m, "fr_fr", gt.fr_fr());
        putTooltipLang(m, "fr_ca", gt.fr_ca());
        putTooltipLang(m, "es_es", gt.es_es());
        putTooltipLang(m, "es_mx", gt.es_mx());
        putTooltipLang(m, "pt_br", gt.pt_br());
        putTooltipLang(m, "pt_pt", gt.pt_pt());
        putTooltipLang(m, "it_it", gt.it_it());
        putTooltipLang(m, "pl_pl", gt.pl_pl());
        return m;
    }

    private static void putTooltipLang(Map<String, String> m, String code, String text) {
        if (!StringUtils.isNullOrEmptyEx(text)) {
            m.put(code, text);
        }
    }

    private static List<String> buildLocalizedTooltipFileLines(Map<String, String> langMap) {
        List<String> fileLines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String code : TOOLTIP_FILE_COMMENT_LANG_ORDER) {
            String raw = langMap.get(code);
            if (raw == null) {
                continue;
            }
            seen.add(code);
            fileLines.addAll(splitNonEmptyLines(raw));
        }
        TreeSet<String> rest = new TreeSet<>(langMap.keySet());
        rest.removeAll(seen);
        for (String code : rest) {
            String raw = langMap.get(code);
            if (raw != null) {
                fileLines.addAll(splitNonEmptyLines(raw));
            }
        }
        return fileLines;
    }

    private static final class TooltipResolution {
        final String[] fileComments;
        final ConfigEntryDescriptor.ConfigTooltipGuiKind guiKind;
        final String translationKey;
        final Map<String, String> localizedByLang;

        private TooltipResolution(String[] fileComments, ConfigEntryDescriptor.ConfigTooltipGuiKind guiKind,
                                  String translationKey, Map<String, String> localizedByLang) {
            this.fileComments = fileComments != null ? fileComments : new String[0];
            this.guiKind = guiKind;
            this.translationKey = translationKey != null ? translationKey : "";
            this.localizedByLang = localizedByLang != null ? localizedByLang : Collections.emptyMap();
        }

        static TooltipResolution translationKey(String key) {
            return new TooltipResolution(new String[0], ConfigEntryDescriptor.ConfigTooltipGuiKind.TRANSLATION_KEY,
                    key, Collections.emptyMap());
        }

        static TooltipResolution localized(Map<String, String> localizedByLang, List<String> fileLines) {
            return new TooltipResolution(fileLines.toArray(new String[0]),
                    ConfigEntryDescriptor.ConfigTooltipGuiKind.LOCALIZED_STATIC, "", localizedByLang);
        }

        static TooltipResolution multiline(List<String> lines) {
            return new TooltipResolution(lines.toArray(new String[0]),
                    ConfigEntryDescriptor.ConfigTooltipGuiKind.MULTILINE_LITERAL, "", Collections.emptyMap());
        }

        ConfigCategoryTitleSpec toCategoryTitleSpec(String fallbackKey) {
            switch (guiKind) {
                case TRANSLATION_KEY:
                    return ConfigCategoryTitleSpec.translationKey(translationKey);
                case LOCALIZED_STATIC:
                    if (localizedByLang.isEmpty()) {
                        return ConfigCategoryTitleSpec.literal(fallbackKey);
                    }
                    return ConfigCategoryTitleSpec.localized(localizedByLang);
                case MULTILINE_LITERAL:
                default:
                    return ConfigCategoryTitleSpec.literal(
                            fileComments.length > 0 ? fileComments[0] : fallbackKey);
            }
        }
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

    private static void addDescriptor(Field field, String path, ForgeConfigSpec.ConfigValue<?> cv,
                                      List<ConfigEntryDescriptor> descriptors,
                                      Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap,
                                      ConfigEntryDescriptor.ConfigValueType valueType,
                                      Object defaultValue, Number min, Number max, Class<? extends Enum<?>> enumClass,
                                      TooltipResolution tr, int decimalPlaces) {
        valueMap.put(path, cv);
        List<String> fileLines = Arrays.asList(tr.fileComments);
        ConfigEntryDescriptor.ConfigEntryDescriptorBuilder b = ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(path.substring(path.lastIndexOf('.') + 1))
                .tooltip(fileLines)
                .tooltipGuiKind(tr.guiKind)
                .tooltipTranslationKey(tr.translationKey)
                .tooltipLocalizedByLang(tr.localizedByLang)
                .valueType(valueType)
                .defaultValue(defaultValue)
                .minValue(min)
                .maxValue(max)
                .decimalPlaces(decimalPlaces)
                .enumClass(enumClass)
                .configValue(cv);
        applyRequiresEditPermission(field, b);
        descriptors.add(b.build());
    }

    private static void applyRequiresEditPermission(Field field, ConfigEntryDescriptor.ConfigEntryDescriptorBuilder b) {
        ConfigEntry.RequiresEditPermission re = field.getAnnotation(ConfigEntry.RequiresEditPermission.class);
        if (re == null || re.policy() != ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE) {
            return;
        }
        boolean hasLevel = re.permissionLevel() >= 0;
        boolean hasKey = re.virtualPermissionKey() != null && !re.virtualPermissionKey().isEmpty();
        if (!hasLevel && !hasKey) {
            return;
        }
        b.editPermissionPolicy(ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE);
        if (hasLevel) {
            b.fieldEditPermissionLevel(re.permissionLevel());
        }
        if (hasKey) {
            b.fieldEditVirtualPermissionKey(re.virtualPermissionKey());
        }
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
