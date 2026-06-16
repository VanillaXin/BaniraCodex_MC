package xin.vanilla.banira.internal.fabric.config;

import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.Field;
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

        Path file = BaniraPlatforms.get().pathService().configPath().resolve(config.name() + ".properties");
        ConfigHolder holder = ConfigHolder.create(
                modId,
                config.name(),
                config.type(),
                new FabricConfigValueStore(file, descriptors),
                descriptors,
                categoryTooltips,
                categoryTitleSpecs
        );
        HOLDER_MAP.put(configClass, holder);
        ConfigRegistry.registerHolder(holder);
    }

    @SuppressWarnings("unchecked")
    static <T> T get(Class<T> configClass) {
        ConfigHolder holder = getHolder(configClass);
        if (holder == null) {
            throw new IllegalStateException("Config not registered: " + configClass.getName());
        }
        Class<?>[] ifaces = configClass.getInterfaces();
        if (ifaces.length == 0) {
            throw new IllegalArgumentException("Config class must implement an interface for fluent API: " + configClass.getName());
        }
        return (T) Proxy.newProxyInstance(configClass.getClassLoader(), ifaces, (proxy, method, args) -> {
            String path = resolvePath(holder, method.getName(), "");
            if (path != null && method.getParameterCount() == 0) {
                return holder.get(path);
            }
            if (path != null && method.getParameterCount() == 1) {
                holder.set(path, args[0]);
                return proxy;
            }
            if ("equals".equals(method.getName())) return proxy == args[0];
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("toString".equals(method.getName())) return "FabricConfigProxy@" + configClass.getSimpleName();
            return null;
        });
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
                categoryTooltips.put(path, field.getName());
                categoryTitleSpecs.put(path, ConfigCategoryTitleSpec.literal(field.getName()));
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
            return ConfigEntryDescriptor.builder()
                    .path(path)
                    .displayName(field.getName())
                    .tooltip(Collections.emptyList())
                    .valueType(valueType)
                    .defaultValue(defaultValue)
                    .minValue(min)
                    .maxValue(max)
                    .decimalPlaces(decimalPlaces)
                    .enumClass(enumClass)
                    .build();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to build config descriptor: " + path, e);
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

    private static String resolvePath(ConfigHolder holder, String methodName, String prefix) {
        for (String path : holder.valuePaths()) {
            String fieldName = path.substring(path.lastIndexOf('.') + 1);
            if (methodName.equals(fieldName) && (prefix.isEmpty() || path.startsWith(prefix + "."))) {
                return path;
            }
        }
        return null;
    }
}
