package xin.vanilla.banira.editable;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import xin.vanilla.banira.editable.annotation.BaniraFieldMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 从基于 {@link me.shedaniel.autoconfig.annotation.ConfigEntry}（Cloth）与 Banira 扩展注解的配置类扫描 Descriptor。
 */
public final class ConfigFieldStructure {

    private ConfigFieldStructure() {
    }

    public record Result(
            List<ConfigEntryDescriptor> descriptors,
            Map<String, Field[]> bindingsByPath,
            Map<String, String> categoryTooltips,
            Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs
    ) {
    }

    public static Result build(Class<?> configClass) {
        Config cfg = configClass.getAnnotation(Config.class);
        if (cfg == null) {
            throw new IllegalArgumentException("ConfigFieldStructure expects @me.shedaniel.autoconfig.annotation.Config on: " + configClass.getName());
        }
        String autoConfigFileName = cfg.name();
        List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
        Map<String, Field[]> bindingsByPath = new LinkedHashMap<>();
        Map<String, String> categoryTooltips = new LinkedHashMap<>();
        Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs = new LinkedHashMap<>();
        buildFromClass(configClass, "", new ArrayList<>(), descriptors, bindingsByPath, categoryTooltips, categoryTitleSpecs,
                autoConfigFileName);
        return new Result(Collections.unmodifiableList(descriptors), Collections.unmodifiableMap(bindingsByPath),
                Collections.unmodifiableMap(categoryTooltips), Collections.unmodifiableMap(categoryTitleSpecs));
    }

    @SuppressWarnings({"unchecked"})
    private static void buildFromClass(Class<?> clazz, String prefix, List<Field> parentFieldChain,
                                       List<ConfigEntryDescriptor> descriptors,
                                       Map<String, Field[]> bindingsByPath,
                                       Map<String, String> categoryTooltips,
                                       Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs,
                                       String autoConfigFileName) {
        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);

            String key = field.getName();
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            ConfigEntry.Gui.CollapsibleObject collapsibleGui = field.getAnnotation(ConfigEntry.Gui.CollapsibleObject.class);
            boolean explicitNested = collapsibleGui != null;

            Class<?> type = field.getType();

            if (explicitNested || shouldWalkAsNested(type)) {
                TooltipResolution tr = resolveTooltip(field, path, autoConfigFileName);
                String[] fileC = tr.fileComments;
                categoryTooltips.put(path, fileC.length > 0 ? fileC[0] : key);
                categoryTitleSpecs.put(path, tr.toCategoryTitleSpec(key));
                List<Field> nextChain = new ArrayList<>(parentFieldChain);
                nextChain.add(field);
                buildFromClass(type, path, nextChain, descriptors, bindingsByPath, categoryTooltips, categoryTitleSpecs,
                        autoConfigFileName);
                continue;
            }

            TooltipResolution tr = resolveTooltip(field, path, autoConfigFileName);
            String[] comments = tr.fileComments;

            try {
                Object defaultValue = getDefaultValue(field);
                if (type == String.class) {
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.STRING, defaultValue, null, null, null, tr, 2);
                } else if (type == boolean.class || type == Boolean.class) {
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.BOOLEAN, defaultValue, null, null, null, tr, 2);
                } else if (type == int.class || type == Integer.class) {
                    ConfigEntry.BoundedDiscrete bd = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                    int min = bd != null ? Math.toIntExact(bd.min()) : Integer.MIN_VALUE;
                    int max = bd != null ? Math.toIntExact(bd.max()) : Integer.MAX_VALUE;
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.INTEGER, defaultValue, min, max, null, tr, 2);
                } else if (type == long.class || type == Long.class) {
                    BaniraFieldMeta.BoundedLong bl = field.getAnnotation(BaniraFieldMeta.BoundedLong.class);
                    long min = bl != null ? bl.min() : Long.MIN_VALUE;
                    long max = bl != null ? bl.max() : Long.MAX_VALUE;
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.LONG, defaultValue, min, max, null, tr, 2);
                } else if (type == double.class || type == Double.class) {
                    BaniraFieldMeta.BoundedDouble bd = field.getAnnotation(BaniraFieldMeta.BoundedDouble.class);
                    double min = bd != null ? bd.min() : Double.MIN_VALUE;
                    double max = bd != null ? bd.max() : Double.MAX_VALUE;
                    int decimalPlaces = bd != null ? bd.decimalPlaces() : 2;
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.DOUBLE, defaultValue, min, max, null, tr, decimalPlaces);
                } else if (type == float.class || type == Float.class) {
                    BaniraFieldMeta.BoundedDouble bd = field.getAnnotation(BaniraFieldMeta.BoundedDouble.class);
                    double min = bd != null ? bd.min() : -Float.MAX_VALUE;
                    double max = bd != null ? bd.max() : Float.MAX_VALUE;
                    int decimalPlaces = bd != null ? bd.decimalPlaces() : 2;
                    Object dDef = defaultValue instanceof Number n ? n.doubleValue() : defaultValue;
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.DOUBLE, dDef, min, max, null, tr, decimalPlaces);
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
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            listType, new ArrayList<>(normDef), min, max, enumClass, tr, decPlaces);
                } else if (type.isEnum()) {
                    Enum<?> enumDefault = (Enum<?>) defaultValue;
                    addLeaf(field, path, parentFieldChain, descriptors, bindingsByPath,
                            ConfigEntryDescriptor.ConfigValueType.ENUM, enumDefault, null, null, (Class<? extends Enum<?>>) type, tr, 2);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to build config structure for field: " + path, e);
            }
        }
    }

    private static boolean shouldWalkAsNested(Class<?> type) {
        if (type == null || type.isPrimitive() || type.isEnum() || List.class.isAssignableFrom(type) || type == String.class) {
            return false;
        }
        Package p = type.getPackage();
        if (p != null && p.getName().startsWith("java.")) {
            return false;
        }
        if (Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        return hasConfigurableFields(type);
    }

    private static boolean hasConfigurableFields(Class<?> clazz) {
        for (Field f : getAllFields(clazz)) {
            if (!Modifier.isStatic(f.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private static void addLeaf(Field field, String path, List<Field> parentFieldChain,
                                List<ConfigEntryDescriptor> descriptors,
                                Map<String, Field[]> bindingsByPath,
                                ConfigEntryDescriptor.ConfigValueType valueType,
                                Object defaultValue, Number min, Number max, Class<? extends Enum<?>> enumClass,
                                TooltipResolution tr, int decimalPlaces) {
        List<Field> chain = new ArrayList<>(parentFieldChain);
        chain.add(field);
        bindingsByPath.put(path, chain.toArray(new Field[0]));
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
                .enumClass(enumClass);
        applyRequiresEditPermission(field, b);
        descriptors.add(b.build());
    }

    private static void applyRequiresEditPermission(Field field, ConfigEntryDescriptor.ConfigEntryDescriptorBuilder b) {
        BaniraFieldMeta.RequiresEditPermission re = field.getAnnotation(BaniraFieldMeta.RequiresEditPermission.class);
        if (re == null || re.policy() != BaniraFieldMeta.EditPermissionPolicy.FIELD_OVERRIDE) {
            return;
        }
        boolean hasLevel = re.permissionLevel() >= 0;
        boolean hasKey = re.virtualPermissionKey() != null && !re.virtualPermissionKey().isEmpty();
        if (!hasLevel && !hasKey) {
            return;
        }
        b.editPermissionPolicy(BaniraFieldMeta.EditPermissionPolicy.FIELD_OVERRIDE);
        if (hasLevel) {
            b.fieldEditPermissionLevel(re.permissionLevel());
        }
        if (hasKey) {
            b.fieldEditVirtualPermissionKey(re.virtualPermissionKey());
        }
    }

    /**
     * Cloth {@link ConfigEntry.Gui.Tooltip} 仅为 {@code count}（面向默认 AutoConfig GUI 的列表条目），
     * Banira 编辑器统一使用 lang 中与 Cloth 一致的 {@code text.autoconfig.<文件名>.option.<路径>}。
     */
    private static TooltipResolution resolveTooltip(Field field, String dottedPath, String autoConfigFileName) {
        return TooltipResolution.translationKey(defaultAutoconfigOptionTooltipKey(autoConfigFileName, dottedPath));
    }

    /**
     * 与 Cloth / Mod Menu AutoConfig 约定一致的选项说明键：<br>
     * {@code text.autoconfig.<@Config(name)>.option.<字段点分路径>}
     */
    private static String defaultAutoconfigOptionTooltipKey(String autoConfigFileName, String dottedPath) {
        return "text.autoconfig." + autoConfigFileName + ".option." + dottedPath;
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
            return switch (guiKind) {
                case TRANSLATION_KEY -> ConfigCategoryTitleSpec.translationKey(translationKey);
                case LOCALIZED_STATIC -> {
                    if (localizedByLang.isEmpty()) {
                        yield ConfigCategoryTitleSpec.literal(fallbackKey);
                    }
                    yield ConfigCategoryTitleSpec.localized(localizedByLang);
                }
                case MULTILINE_LITERAL -> ConfigCategoryTitleSpec.literal(
                        fileComments.length > 0 ? fileComments[0] : fallbackKey);
            };
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
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class || type == Long.class) {
            return 0L;
        }
        if (type == double.class || type == Double.class) {
            return 0.0;
        }
        if (type == float.class || type == Float.class) {
            return 0.0f;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == String.class) {
            return "";
        }
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<>();
        }
        if (type.isEnum()) {
            return type.getEnumConstants().length > 0 ? type.getEnumConstants()[0] : null;
        }
        return null;
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
}
