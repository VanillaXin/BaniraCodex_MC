package xin.vanilla.banira.internal.fabric.config;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigValueStore;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Fabric 简单配置后端，按 Banira 配置路径读写 properties 文件。
 */
final class FabricConfigValueStore implements ConfigValueStore {
    private final Path file;
    private final Map<String, ConfigEntryDescriptor> descriptors;
    private final Map<String, Object> values = new LinkedHashMap<>();

    FabricConfigValueStore(Path file, List<ConfigEntryDescriptor> descriptors) {
        this.file = file;
        Map<String, ConfigEntryDescriptor> byPath = new LinkedHashMap<>();
        for (ConfigEntryDescriptor descriptor : descriptors) {
            byPath.put(descriptor.getPath(), descriptor);
            values.put(descriptor.getPath(), descriptor.getDefaultValue());
        }
        this.descriptors = Collections.unmodifiableMap(byPath);
        load();
    }

    @Override
    public Set<String> paths() {
        return Collections.unmodifiableSet(values.keySet());
    }

    @Nullable
    @Override
    public Object get(String path) {
        return values.get(path);
    }

    @Override
    public void set(String path, Object value) {
        if (validate(path, value)) {
            values.put(path, value);
        }
    }

    @Override
    public Class<?> valueClass(String path) {
        Object value = get(path);
        return value != null ? value.getClass() : Object.class;
    }

    @Nullable
    @Override
    public Object defaultValue(String path) {
        ConfigEntryDescriptor descriptor = descriptors.get(path);
        return descriptor != null ? descriptor.getDefaultValue() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        ConfigEntryDescriptor descriptor = descriptors.get(path);
        if (descriptor == null || value == null) {
            return false;
        }
        switch (descriptor.getValueType()) {
            case INTEGER:
                return value instanceof Integer && inRange((Integer) value, descriptor.getMinValue(), descriptor.getMaxValue());
            case LONG:
                return value instanceof Long && inRange((Long) value, descriptor.getMinValue(), descriptor.getMaxValue());
            case DOUBLE:
                return value instanceof Double && inRange((Double) value, descriptor.getMinValue(), descriptor.getMaxValue());
            case BOOLEAN:
                return value instanceof Boolean;
            case STRING:
                return value instanceof String;
            case ENUM:
                return descriptor.getEnumClass() != null && descriptor.getEnumClass().isInstance(value);
            default:
                return value instanceof List;
        }
    }

    @Override
    public void save() {
        Properties properties = new Properties();
        values.forEach((path, value) -> properties.setProperty(path, encode(value)));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Banira Codex config");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save config: " + file, e);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            return;
        }
        for (String path : descriptors.keySet()) {
            String raw = properties.getProperty(path);
            if (raw == null) {
                continue;
            }
            Object parsed = parse(descriptors.get(path), raw);
            if (parsed != null && validate(path, parsed)) {
                values.put(path, parsed);
            }
        }
    }

    private Object parse(ConfigEntryDescriptor descriptor, String raw) {
        try {
            switch (descriptor.getValueType()) {
                case STRING:
                    return raw;
                case BOOLEAN:
                    return Boolean.parseBoolean(raw);
                case INTEGER:
                    return Integer.parseInt(raw);
                case LONG:
                    return Long.parseLong(raw);
                case DOUBLE:
                    return Double.parseDouble(raw);
                case ENUM:
                    return parseEnum(descriptor.getEnumClass(), raw);
                default:
                    return parseList(descriptor, raw);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseEnum(Class<? extends Enum<?>> enumClass, String raw) {
        return enumClass == null ? null : Enum.valueOf((Class) enumClass, raw);
    }

    private Object parseList(ConfigEntryDescriptor descriptor, String raw) {
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split(",", -1);
        List<Object> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(parseListValue(descriptor, part));
        }
        return result;
    }

    private Object parseListValue(ConfigEntryDescriptor descriptor, String raw) {
        switch (descriptor.getValueType()) {
            case INTEGER_LIST:
                return Integer.parseInt(raw);
            case LONG_LIST:
                return Long.parseLong(raw);
            case DOUBLE_LIST:
                return Double.parseDouble(raw);
            case BOOLEAN_LIST:
                return Boolean.parseBoolean(raw);
            case ENUM_LIST:
                return parseEnum(descriptor.getEnumClass(), raw);
            default:
                return raw.replace("\\,", ",");
        }
    }

    private String encode(Object value) {
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof List<?>) {
            List<String> parts = new ArrayList<>();
            for (Object element : (List<?>) value) {
                parts.add(element instanceof Enum<?> ? ((Enum<?>) element).name() : String.valueOf(element).replace(",", "\\,"));
            }
            return String.join(",", parts);
        }
        return String.valueOf(value);
    }

    private boolean inRange(Number value, Number min, Number max) {
        double v = value.doubleValue();
        return (min == null || v >= min.doubleValue()) && (max == null || v <= max.doubleValue());
    }
}
