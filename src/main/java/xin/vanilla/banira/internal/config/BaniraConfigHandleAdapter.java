package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将旧 ConfigHolder 包装成根级 platform API，避免子 mod 直接依赖内部配置模型。
 */
public class BaniraConfigHandleAdapter implements BaniraConfigHandle {
    private final ConfigHolder holder;

    public BaniraConfigHandleAdapter(ConfigHolder holder) {
        this.holder = holder;
    }

    @Override
    public String getModId() {
        return holder.getModId();
    }

    @Override
    public String getConfigName() {
        return holder.getConfigName();
    }

    @Override
    public void save() {
        holder.save();
    }

    @Override
    public <T> T get(String path) {
        return holder.get(path);
    }

    @Override
    public void set(String path, Object value) {
        holder.set(path, value);
    }

    @Override
    public Set<String> valuePaths() {
        return holder.getValuePaths();
    }

    @Override
    public boolean hasValue(String path) {
        return holder.getValuePaths().contains(path);
    }

    @Nullable
    @Override
    public String findValuePath(String key) {
        if (key == null) {
            return null;
        }
        if (hasValue(key)) {
            return key;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        Set<String> matches = holder.getValuePaths().stream()
                .filter(path -> path.toLowerCase(Locale.ROOT).contains(lowerKey))
                .collect(Collectors.toSet());
        return matches.size() == 1 ? matches.iterator().next() : null;
    }

    @Override
    public Class<?> valueClass(String path) {
        Object defaultValue = defaultValue(path);
        if (defaultValue != null) {
            return defaultValue.getClass();
        }
        Object value = get(path);
        return value != null ? value.getClass() : Object.class;
    }

    @Nullable
    @Override
    public Object defaultValue(String path) {
        ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
        return descriptor != null ? descriptor.getDefaultValue() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        if (!hasValue(path)) {
            return false;
        }
        ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
        if (descriptor == null) {
            Object defaultValue = defaultValue(path);
            return value == null || defaultValue == null || defaultValue.getClass().isInstance(value);
        }
        return validateDescriptorValue(descriptor, value);
    }

    @Override
    public boolean setIfValid(String path, Object value) {
        if (!validate(path, value)) {
            return false;
        }
        set(path, value);
        return true;
    }

    private static boolean validateDescriptorValue(ConfigEntryDescriptor descriptor, Object value) {
        if (descriptor == null || value == null) {
            return false;
        }
        if (descriptor.isListType()) {
            if (!(value instanceof List)) {
                return false;
            }
            for (Object one : (List<?>) value) {
                if (ConfigListSpecHelper.coerceListElement(one, descriptor.getValueType(), descriptor.getEnumClass(),
                        descriptor.getMinValue(), descriptor.getMaxValue(), descriptor.getDecimalPlaces()) == null) {
                    return false;
                }
            }
            return true;
        }
        switch (descriptor.getValueType()) {
            case INTEGER:
            case LONG:
            case DOUBLE:
                if (!(value instanceof Number)) {
                    return false;
                }
                double number = ((Number) value).doubleValue();
                return (descriptor.getMinValue() == null || number >= descriptor.getMinValue().doubleValue())
                        && (descriptor.getMaxValue() == null || number <= descriptor.getMaxValue().doubleValue());
            case BOOLEAN:
                return value instanceof Boolean;
            case ENUM:
                return value instanceof Enum && descriptor.getEnumClass() != null
                        && descriptor.getEnumClass().isAssignableFrom(value.getClass());
            case STRING:
            default:
                return value instanceof String;
        }
    }
}
