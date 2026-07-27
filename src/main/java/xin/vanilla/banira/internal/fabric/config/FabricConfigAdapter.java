package xin.vanilla.banira.internal.fabric.config;

import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;

/**
 * Fabric 配置适配器：解析 Banira 注解模型并使用轻量文件后端保存。
 */
final class FabricConfigAdapter {
    private static final Map<Class<?>, ConfigHolder> HOLDER_MAP = new LinkedHashMap<>();

    private FabricConfigAdapter() {
    }

    static <T> void register(Class<T> configClass, String modId) {
        Config config = configClass.getAnnotation(Config.class);
        if (config == null) {
            throw new IllegalArgumentException("Config class must be annotated with @Config: " + configClass.getName());
        }
        List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
        Map<String, String> categoryTooltips = new LinkedHashMap<>();
        Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs = new LinkedHashMap<>();
        buildFromClass(configClass, "", descriptors, categoryTooltips, categoryTitleSpecs);

        Path configDirectory = BaniraPlatforms.get().pathService().configPath();
        Path file = configDirectory.resolve(config.name() + ".toml");
        Path legacyPropertiesFile = configDirectory.resolve(config.name() + ".properties");
        ConfigHolder holder = ConfigHolder.create(
                modId,
                config.name(),
                config.type(),
                new FabricConfigValueStore(file, legacyPropertiesFile, descriptors),
                descriptors,
                categoryTooltips,
                categoryTitleSpecs
        );
        HOLDER_MAP.put(configClass, holder);
        ConfigRegistry.registerHolder(holder);
    }

    static <T> T view(Class<?> configClass, Class<T> viewClass) {
        ConfigHolder holder = getHolder(configClass);
        if (holder == null) {
            throw new IllegalStateException("Config not registered: " + configClass.getName());
        }
        if (!viewClass.isInterface()) {
            throw new IllegalArgumentException("Config view must be an interface: " + viewClass.getName());
        }
        return viewClass.cast(viewProxy(configClass.getClassLoader(), viewClass, holder, ""));
    }

    static ConfigHolder getHolder(Class<?> configClass) {
        return HOLDER_MAP.get(configClass);
    }

    private static void buildFromClass(Class<?> type, String prefix, List<ConfigEntryDescriptor> descriptors,
                                       Map<String, String> categoryTooltips,
                                       Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        for (Field field : allFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            String path = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            if (isNested(field)) {
                TooltipResolution tooltip = resolveTooltip(field);
                categoryTooltips.put(path, tooltip.fileLines.isEmpty()
                        ? field.getName() : tooltip.fileLines.get(0));
                categoryTitleSpecs.put(path, tooltip.toCategoryTitleSpec(field.getName()));
                buildFromClass(field.getType(), path, descriptors, categoryTooltips, categoryTitleSpecs);
                continue;
            }
            ConfigEntryDescriptor descriptor = descriptor(field, path);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
    }

    private static ConfigEntryDescriptor descriptor(Field field, String path) {
        try {
            Object defaultValue = defaultValue(field);
            Class<?> type = field.getType();
            Number min = null;
            Number max = null;
            int decimalPlaces = 2;
            ConfigEntryDescriptor.ConfigValueType valueType;
            Class<? extends Enum<?>> enumClass = null;
            if (type == String.class) {
                valueType = ConfigEntryDescriptor.ConfigValueType.STRING;
            } else if (type == boolean.class || type == Boolean.class) {
                valueType = ConfigEntryDescriptor.ConfigValueType.BOOLEAN;
            } else if (type == int.class || type == Integer.class) {
                ConfigEntry.BoundedDiscrete bounded = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                min = bounded != null ? bounded.min() : Integer.MIN_VALUE;
                max = bounded != null ? bounded.max() : Integer.MAX_VALUE;
                valueType = ConfigEntryDescriptor.ConfigValueType.INTEGER;
            } else if (type == long.class || type == Long.class) {
                ConfigEntry.BoundedLong bounded = field.getAnnotation(ConfigEntry.BoundedLong.class);
                min = bounded != null ? bounded.min() : Long.MIN_VALUE;
                max = bounded != null ? bounded.max() : Long.MAX_VALUE;
                valueType = ConfigEntryDescriptor.ConfigValueType.LONG;
            } else if (type == double.class || type == Double.class) {
                ConfigEntry.BoundedDouble bounded = field.getAnnotation(ConfigEntry.BoundedDouble.class);
                min = bounded != null ? bounded.min() : -Double.MAX_VALUE;
                max = bounded != null ? bounded.max() : Double.MAX_VALUE;
                decimalPlaces = bounded != null ? bounded.decimalPlaces() : 2;
                valueType = ConfigEntryDescriptor.ConfigValueType.DOUBLE;
            } else if (List.class.isAssignableFrom(type)) {
                Class<?> elementClass = ConfigListSpecHelper.resolveListElementClass(field);
                valueType = ConfigListSpecHelper.listValueTypeForElement(elementClass);
                Number[] bounds = ConfigListSpecHelper.listBoundsForValueType(field, valueType);
                min = bounds[0];
                max = bounds[1];
                decimalPlaces = ConfigListSpecHelper.decimalPlacesForList(field, valueType);
                enumClass = ConfigListSpecHelper.enumClassForList(elementClass);
                defaultValue = ConfigListSpecHelper.normalizeDefaultList((List<?>) defaultValue, valueType, enumClass, min, max, decimalPlaces);
            } else if (type.isEnum()) {
                valueType = ConfigEntryDescriptor.ConfigValueType.ENUM;
                enumClass = enumClass(type);
            } else {
                return null;
            }
            TooltipResolution tooltip = resolveTooltip(field);
            ConfigEntryDescriptor.ConfigEntryDescriptorBuilder builder = ConfigEntryDescriptor.builder()
                    .path(path)
                    .displayName(field.getName())
                    .tooltip(tooltip.fileLines)
                    .tooltipGuiKind(tooltip.guiKind)
                    .tooltipTranslationKey(tooltip.translationKey)
                    .tooltipLocalizedByLang(tooltip.localizedByLang)
                    .valueType(valueType)
                    .defaultValue(defaultValue)
                    .minValue(min)
                    .maxValue(max)
                    .decimalPlaces(decimalPlaces)
                    .enumClass(enumClass);
            applyRequiresEditPermission(field, builder);
            return builder.build();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to build config descriptor: " + path, e);
        }
    }

    /**
     * 将公共配置注解转换为编辑器和配置文件共用的描述元数据。
     */
    private static TooltipResolution resolveTooltip(Field field) {
        ConfigEntry.Gui.Tooltip annotation = field.getAnnotation(ConfigEntry.Gui.Tooltip.class);
        if (annotation != null) {
            String translationKey = trimmed(annotation.translationKey());
            if (!translationKey.isEmpty()) {
                return TooltipResolution.translationKey(translationKey);
            }
            Map<String, String> localized = localizedTooltip(annotation);
            if (!localized.isEmpty()) {
                return TooltipResolution.localized(localized, localizedFileLines(localized));
            }
            List<String> literalLines = nonNullLines(annotation.value());
            if (!literalLines.isEmpty()) {
                return TooltipResolution.multiline(literalLines);
            }
        }
        ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
        if (entry != null && entry.tooltip().length > 0) {
            return TooltipResolution.multiline(nonNullLines(entry.tooltip()));
        }
        return TooltipResolution.multiline(Collections.singletonList(field.getName()));
    }

    private static Map<String, String> localizedTooltip(ConfigEntry.Gui.Tooltip tooltip) {
        Map<String, String> localized = new LinkedHashMap<>();
        putLocalized(localized, "en_us", tooltip.en_us());
        putLocalized(localized, "en_gb", tooltip.en_gb());
        putLocalized(localized, "zh_cn", tooltip.zh_cn());
        putLocalized(localized, "zh_tw", tooltip.zh_tw());
        putLocalized(localized, "zh_hk", tooltip.zh_hk());
        putLocalized(localized, "ja_jp", tooltip.ja_jp());
        putLocalized(localized, "ko_kr", tooltip.ko_kr());
        putLocalized(localized, "ru_ru", tooltip.ru_ru());
        putLocalized(localized, "de_de", tooltip.de_de());
        putLocalized(localized, "fr_fr", tooltip.fr_fr());
        putLocalized(localized, "fr_ca", tooltip.fr_ca());
        putLocalized(localized, "es_es", tooltip.es_es());
        putLocalized(localized, "es_mx", tooltip.es_mx());
        putLocalized(localized, "pt_br", tooltip.pt_br());
        putLocalized(localized, "pt_pt", tooltip.pt_pt());
        putLocalized(localized, "it_it", tooltip.it_it());
        putLocalized(localized, "pl_pl", tooltip.pl_pl());
        return localized;
    }

    private static void putLocalized(Map<String, String> localized, String language, String text) {
        if (!trimmed(text).isEmpty()) {
            localized.put(language, text);
        }
    }

    private static List<String> localizedFileLines(Map<String, String> localized) {
        String[] preferredOrder = {
                "zh_cn", "zh_tw", "zh_hk", "ja_jp", "ko_kr", "en_us", "en_gb",
                "de_de", "es_es", "es_mx", "fr_fr", "fr_ca", "it_it", "pl_pl",
                "pt_br", "pt_pt", "ru_ru"
        };
        List<String> lines = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        for (String language : preferredOrder) {
            addLocalizedLines(lines, emitted, localized, language);
        }
        List<String> remaining = new ArrayList<>(localized.keySet());
        remaining.removeAll(emitted);
        Collections.sort(remaining);
        for (String language : remaining) {
            addLocalizedLines(lines, emitted, localized, language);
        }
        return lines;
    }

    private static void addLocalizedLines(List<String> lines, Set<String> emitted,
                                          Map<String, String> localized, String language) {
        String text = localized.get(language);
        if (text == null || !emitted.add(language)) {
            return;
        }
        for (String line : text.split("\\n", -1)) {
            String value = line.trim();
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
    }

    private static List<String> nonNullLines(String[] values) {
        List<String> lines = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    lines.add(value);
                }
            }
        }
        return lines;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static void applyRequiresEditPermission(
            Field field, ConfigEntryDescriptor.ConfigEntryDescriptorBuilder builder) {
        ConfigEntry.RequiresEditPermission permission = field.getAnnotation(ConfigEntry.RequiresEditPermission.class);
        if (permission == null || permission.policy() != ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE) {
            return;
        }
        boolean hasLevel = permission.permissionLevel() >= 0;
        boolean hasKey = !trimmed(permission.virtualPermissionKey()).isEmpty();
        if (!hasLevel && !hasKey) {
            return;
        }
        builder.editPermissionPolicy(ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE);
        if (hasLevel) {
            builder.fieldEditPermissionLevel(permission.permissionLevel());
        }
        if (hasKey) {
            builder.fieldEditVirtualPermissionKey(permission.virtualPermissionKey());
        }
    }

    private static final class TooltipResolution {
        private final List<String> fileLines;
        private final ConfigEntryDescriptor.ConfigTooltipGuiKind guiKind;
        private final String translationKey;
        private final Map<String, String> localizedByLang;

        private TooltipResolution(List<String> fileLines, ConfigEntryDescriptor.ConfigTooltipGuiKind guiKind,
                                  String translationKey, Map<String, String> localizedByLang) {
            this.fileLines = Collections.unmodifiableList(new ArrayList<>(fileLines));
            this.guiKind = guiKind;
            this.translationKey = translationKey;
            this.localizedByLang = Collections.unmodifiableMap(new LinkedHashMap<>(localizedByLang));
        }

        private static TooltipResolution translationKey(String key) {
            return new TooltipResolution(Collections.emptyList(),
                    ConfigEntryDescriptor.ConfigTooltipGuiKind.TRANSLATION_KEY, key, Collections.emptyMap());
        }

        private static TooltipResolution localized(Map<String, String> localized, List<String> fileLines) {
            return new TooltipResolution(fileLines,
                    ConfigEntryDescriptor.ConfigTooltipGuiKind.LOCALIZED_STATIC, "", localized);
        }

        private static TooltipResolution multiline(List<String> lines) {
            return new TooltipResolution(lines,
                    ConfigEntryDescriptor.ConfigTooltipGuiKind.MULTILINE_LITERAL, "", Collections.emptyMap());
        }

        private ConfigCategoryTitleSpec toCategoryTitleSpec(String fallback) {
            switch (guiKind) {
                case TRANSLATION_KEY:
                    return ConfigCategoryTitleSpec.translationKey(translationKey);
                case LOCALIZED_STATIC:
                    return localizedByLang.isEmpty()
                            ? ConfigCategoryTitleSpec.literal(fallback)
                            : ConfigCategoryTitleSpec.localized(localizedByLang);
                case MULTILINE_LITERAL:
                default:
                    return ConfigCategoryTitleSpec.literal(fileLines.isEmpty() ? fallback : fileLines.get(0));
            }
        }
    }

    private static boolean isNested(Field field) {
        return field.getAnnotation(ConfigEntry.Gui.CollapsibleObject.class) != null
                || field.getAnnotation(ConfigEntry.CollapsibleObject.class) != null;
    }

    private static Object defaultValue(Field field) throws ReflectiveOperationException {
        Object owner = field.getDeclaringClass().getDeclaredConstructor().newInstance();
        return field.get(owner);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> enumClass(Class<?> type) {
        return (Class<? extends Enum<?>>) type;
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Object viewProxy(ClassLoader classLoader, Class<?> viewInterface, ConfigHolder holder, String prefix) {
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{viewInterface}, (proxy, method, args) ->
                handleViewMethod(classLoader, viewInterface, holder, prefix, proxy, method, args));
    }

    private static Object handleViewMethod(ClassLoader classLoader, Class<?> viewInterface, ConfigHolder holder,
                                           String prefix, Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            String name = method.getName();
            if ("equals".equals(name)) return proxy == args[0];
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("toString".equals(name))
                return "FabricConfigProxy@" + viewInterface.getSimpleName() + "(" + prefix + ")";
            throw new UnsupportedOperationException(method.toString());
        }

        int parameterCount = method.getParameterCount();
        String path = prefix.isEmpty() ? method.getName() : prefix + "." + method.getName();
        if ("holder".equals(method.getName()) && parameterCount == 0 && method.getReturnType().isAssignableFrom(ConfigHolder.class)) {
            return holder;
        }
        if (holder.hasValue(path)) {
            if (parameterCount == 0) {
                return holder.get(path);
            }
            if (parameterCount == 1) {
                holder.set(path, args[0]);
                return proxy;
            }
        }
        if (parameterCount == 0 && method.getReturnType().isInterface() && hasChildValue(holder, path)) {
            return viewProxy(classLoader, method.getReturnType(), holder, path);
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static boolean hasChildValue(ConfigHolder holder, String path) {
        String childPrefix = path + ".";
        for (String valuePath : holder.valuePaths()) {
            if (valuePath.startsWith(childPrefix)) {
                return true;
            }
        }
        return false;
    }
}
