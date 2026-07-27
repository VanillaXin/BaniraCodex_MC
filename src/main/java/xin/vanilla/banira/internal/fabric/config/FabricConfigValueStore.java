package xin.vanilla.banira.internal.fabric.config;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.common.config.ConfigValueStore;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Fabric 配置后端，读写 Banira 描述符覆盖的 TOML 子集。
 * <p>
 * 仅支持布尔值、数字、字符串、枚举及这些类型的数组；这正好覆盖 Banira 配置模型，
 * 因而无需在 Fabric 运行时额外携带完整 TOML 解析库。
 */
final class FabricConfigValueStore implements ConfigValueStore {
    private final Path file;
    @Nullable
    private final Path legacyPropertiesFile;
    private final Map<String, ConfigEntryDescriptor> descriptors;
    private final Map<String, Object> values = new LinkedHashMap<>();

    FabricConfigValueStore(Path file, List<ConfigEntryDescriptor> descriptors) {
        this(file, null, descriptors);
    }

    FabricConfigValueStore(Path file, @Nullable Path legacyPropertiesFile,
                           List<ConfigEntryDescriptor> descriptors) {
        this.file = file;
        this.legacyPropertiesFile = legacyPropertiesFile;
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
        Object normalized = normalize(path, value);
        if (normalized != null) {
            values.put(path, normalized);
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
        return normalize(path, value) != null;
    }

    @Nullable
    private Object normalize(String path, Object value) {
        ConfigEntryDescriptor descriptor = descriptors.get(path);
        if (descriptor == null || value == null) {
            return null;
        }
        switch (descriptor.getValueType()) {
            case INTEGER:
                return value instanceof Integer && inRange((Integer) value, descriptor.getMinValue(), descriptor.getMaxValue()) ? value : null;
            case LONG:
                return value instanceof Long && inRange((Long) value, descriptor.getMinValue(), descriptor.getMaxValue()) ? value : null;
            case DOUBLE:
                return value instanceof Double && inRange((Double) value, descriptor.getMinValue(), descriptor.getMaxValue()) ? value : null;
            case BOOLEAN:
                return value instanceof Boolean ? value : null;
            case STRING:
                return value instanceof String ? value : null;
            case ENUM:
                return descriptor.getEnumClass() != null && descriptor.getEnumClass().isInstance(value) ? value : null;
            default:
                return normalizeList(descriptor, value);
        }
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, renderToml().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save config: " + file, e);
        }
    }

    private void load() {
        if (Files.isRegularFile(file)) {
            loadToml();
            return;
        }
        if (legacyPropertiesFile != null && Files.isRegularFile(legacyPropertiesFile)) {
            loadLegacyProperties();
            save();
            return;
        }
        save();
    }

    private void loadToml() {
        String table = "";
        try {
            for (String originalLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = stripTomlComment(originalLine).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    table = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                int equals = findUnquoted(line, '=');
                if (equals <= 0) {
                    continue;
                }
                String key = line.substring(0, equals).trim();
                String path = table.isEmpty() ? key : table + "." + key;
                ConfigEntryDescriptor descriptor = descriptors.get(path);
                if (descriptor == null) {
                    continue;
                }
                Object parsed = parseToml(descriptor, line.substring(equals + 1).trim());
                if (parsed != null && validate(path, parsed)) {
                    values.put(path, parsed);
                }
            }
        } catch (IOException ignored) {
            // 读取失败时保留描述符默认值，避免损坏的本地文件阻止游戏启动。
        }
    }

    private void loadLegacyProperties() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(legacyPropertiesFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            return;
        }
        for (String path : descriptors.keySet()) {
            String raw = properties.getProperty(path);
            if (raw == null) {
                continue;
            }
            Object parsed = parseLegacy(descriptors.get(path), raw);
            if (parsed != null && validate(path, parsed)) {
                values.put(path, parsed);
            }
        }
    }

    private Object parseLegacy(ConfigEntryDescriptor descriptor, String raw) {
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
                    return parseLegacyList(descriptor, raw);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseEnum(Class<? extends Enum<?>> enumClass, String raw) {
        return enumClass == null ? null : Enum.valueOf((Class) enumClass, raw);
    }

    private Object parseLegacyList(ConfigEntryDescriptor descriptor, String raw) {
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> parts = splitEscapedCsv(raw);
        List<Object> result = new ArrayList<>(parts.size());
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
                return raw;
        }
    }

    private Object parseToml(ConfigEntryDescriptor descriptor, String raw) {
        try {
            switch (descriptor.getValueType()) {
                case STRING:
                    return parseTomlString(raw);
                case BOOLEAN:
                    return parseTomlBoolean(raw);
                case INTEGER:
                    return Integer.parseInt(raw);
                case LONG:
                    return Long.parseLong(raw);
                case DOUBLE:
                    return Double.parseDouble(raw);
                case ENUM:
                    return parseEnum(descriptor.getEnumClass(), parseTomlString(raw));
                default:
                    return parseTomlList(descriptor, raw);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Boolean parseTomlBoolean(String raw) {
        if ("true".equals(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equals(raw)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid TOML boolean");
    }

    private List<Object> parseTomlList(ConfigEntryDescriptor descriptor, String raw) {
        String value = raw.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IllegalArgumentException("Invalid TOML array");
        }
        String body = value.substring(1, value.length() - 1).trim();
        List<Object> result = new ArrayList<>();
        if (body.isEmpty()) {
            return result;
        }
        for (String element : splitTomlArray(body)) {
            String normalized = element.trim();
            switch (descriptor.getValueType()) {
                case STRING_LIST:
                    result.add(parseTomlString(normalized));
                    break;
                case ENUM_LIST:
                    result.add(parseEnum(descriptor.getEnumClass(), parseTomlString(normalized)));
                    break;
                default:
                    result.add(parseListValue(descriptor, normalized));
                    break;
            }
        }
        return result;
    }

    private String renderToml() {
        Map<String, List<String>> byTable = new LinkedHashMap<>();
        for (String path : descriptors.keySet()) {
            int dot = path.lastIndexOf('.');
            String table = dot < 0 ? "" : path.substring(0, dot);
            byTable.computeIfAbsent(table, ignored -> new ArrayList<>()).add(path);
        }
        StringBuilder out = new StringBuilder("# Banira Codex config\n");
        List<String> rootPaths = byTable.remove("");
        if (rootPaths != null) {
            appendTomlTable(out, "", rootPaths);
        }
        for (Map.Entry<String, List<String>> table : byTable.entrySet()) {
            appendTomlTable(out, table.getKey(), table.getValue());
        }
        return out.toString();
    }

    private void appendTomlTable(StringBuilder out, String table, List<String> paths) {
        if (!table.isEmpty()) {
            out.append('\n').append('[').append(table).append("]\n");
        }
        for (String path : paths) {
            int dot = path.lastIndexOf('.');
            String key = dot < 0 ? path : path.substring(dot + 1);
            out.append(key).append(" = ")
                    .append(encodeToml(descriptors.get(path), values.get(path)))
                    .append('\n');
        }
    }

    private String encodeToml(ConfigEntryDescriptor descriptor, Object value) {
        if (value instanceof List<?>) {
            List<String> encoded = new ArrayList<>();
            for (Object element : (List<?>) value) {
                encoded.add(encodeTomlScalar(descriptor, element, true));
            }
            return "[" + String.join(", ", encoded) + "]";
        }
        return encodeTomlScalar(descriptor, value, false);
    }

    private String encodeTomlScalar(ConfigEntryDescriptor descriptor, Object value, boolean listElement) {
        ConfigEntryDescriptor.ConfigValueType type = descriptor.getValueType();
        boolean stringLike = listElement
                ? type == ConfigEntryDescriptor.ConfigValueType.STRING_LIST
                    || type == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST
                : type == ConfigEntryDescriptor.ConfigValueType.STRING
                    || type == ConfigEntryDescriptor.ConfigValueType.ENUM;
        String raw = value instanceof Enum<?> ? ((Enum<?>) value).name() : String.valueOf(value);
        return stringLike ? quoteTomlString(raw) : raw;
    }

    private String quoteTomlString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\b': out.append("\\b"); break;
                case '\t': out.append("\\t"); break;
                case '\n': out.append("\\n"); break;
                case '\f': out.append("\\f"); break;
                case '\r': out.append("\\r"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    private String parseTomlString(String raw) {
        String value = raw.trim();
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            throw new IllegalArgumentException("Expected TOML basic string");
        }
        StringBuilder out = new StringBuilder(value.length() - 2);
        for (int i = 1; i < value.length() - 1; i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (++i >= value.length() - 1) {
                throw new IllegalArgumentException("Invalid TOML escape");
            }
            char escaped = value.charAt(i);
            switch (escaped) {
                case '\\': out.append('\\'); break;
                case '"': out.append('"'); break;
                case 'b': out.append('\b'); break;
                case 't': out.append('\t'); break;
                case 'n': out.append('\n'); break;
                case 'f': out.append('\f'); break;
                case 'r': out.append('\r'); break;
                case 'u':
                    if (i + 4 >= value.length()) {
                        throw new IllegalArgumentException("Invalid TOML unicode escape");
                    }
                    out.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported TOML escape");
            }
        }
        return out.toString();
    }

    private List<String> splitTomlArray(String body) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\' && quoted) {
                current.append(c);
                escaped = true;
            } else if (c == '"') {
                current.append(c);
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted || escaped) {
            throw new IllegalArgumentException("Invalid TOML array");
        }
        result.add(current.toString());
        return result;
    }

    private String stripTomlComment(String line) {
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\' && quoted) {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private int findUnquoted(String line, char needle) {
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\' && quoted) {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == needle && !quoted) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private Object normalizeList(ConfigEntryDescriptor descriptor, Object value) {
        if (!(value instanceof List<?> raw)) {
            return null;
        }
        List<Object> result = new ArrayList<>(raw.size());
        for (Object element : raw) {
            Object coerced = ConfigListSpecHelper.coerceListElement(
                    element,
                    descriptor.getValueType(),
                    descriptor.getEnumClass(),
                    descriptor.getMinValue(),
                    descriptor.getMaxValue(),
                    descriptor.getDecimalPlaces()
            );
            if (coerced == null) {
                return null;
            }
            result.add(coerced);
        }
        return result;
    }

    private List<String> splitEscapedCsv(String raw) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == ',') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (escaped) {
            current.append('\\');
        }
        result.add(current.toString());
        return result;
    }

    private boolean inRange(Number value, Number min, Number max) {
        double v = value.doubleValue();
        return (min == null || v >= min.doubleValue()) && (max == null || v <= max.doubleValue());
    }
}
