package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConfigSyncNetworkValueTest {

    @Test
    public void encodesScalarEnumsAndListsForNetwork() {
        assertEquals("true", ConfigSyncToServer.encodeConfigValue(true));
        assertEquals("BETA", ConfigSyncToServer.encodeConfigValue(Mode.BETA));
        assertEquals("1,BETA,text", ConfigSyncToServer.encodeConfigValue(List.of(1, Mode.BETA, "text")));
    }

    @Test
    public void decodesScalarValuesAndRejectsInvalidBooleanByValidation() {
        ConfigHolder holder = holder();

        assertEquals(Boolean.TRUE, ConfigSyncToServer.decodeNetworkValue(holder, "flag", "true"));
        assertEquals(Boolean.FALSE, ConfigSyncToServer.decodeNetworkValue(holder, "flag", "FALSE"));
        Object invalidBoolean = ConfigSyncToServer.decodeNetworkValue(holder, "flag", "maybe");

        assertEquals("maybe", invalidBoolean);
        assertFalse(holder.validate("flag", invalidBoolean));
        assertEquals(3, ConfigSyncToServer.decodeNetworkValue(holder, "count", "3"));
        assertEquals(0.6, (Double) ConfigSyncToServer.decodeNetworkValue(holder, "ratio", "0.6"), 0.00001);
        assertEquals(Mode.ALPHA, ConfigSyncToServer.decodeNetworkValue(holder, "mode", "ALPHA"));
    }

    @Test
    public void invalidScalarValuesFallBackToRawStringAndDoNotValidate() {
        ConfigHolder holder = holder();

        Object badCount = ConfigSyncToServer.decodeNetworkValue(holder, "count", "not-a-number");
        Object badMode = ConfigSyncToServer.decodeNetworkValue(holder, "mode", "missing");

        assertEquals("not-a-number", badCount);
        assertEquals("missing", badMode);
        assertFalse(holder.validate("count", badCount));
        assertFalse(holder.validate("mode", badMode));
    }

    @Test
    public void decodesListsWithFilteringAndRuntimeTypes() {
        ConfigHolder holder = holder();

        assertEquals(List.of(1, 5), ConfigSyncToServer.decodeNetworkValue(holder, "ints", "1, 9, bad, 5"));
        assertEquals(List.of(true, false), ConfigSyncToServer.decodeNetworkValue(holder, "flags", "true, maybe, false"));
        assertEquals(List.of(Mode.ALPHA, Mode.BETA), ConfigSyncToServer.decodeNetworkValue(holder, "modes", "alpha, missing, BETA"));
    }

    @Test
    public void unknownDescriptorKeepsRawString() {
        assertEquals("raw", ConfigSyncToServer.decodeNetworkValue(holder(), "missing.path", "raw"));
    }

    private static ConfigHolder holder() {
        MemoryValueStore store = new MemoryValueStore()
                .value("flag", false, v -> v instanceof Boolean)
                .value("count", 0, v -> v instanceof Integer i && i >= 0 && i <= 5)
                .value("ratio", 0.0, v -> v instanceof Double d && d >= 0.0 && d <= 1.0)
                .value("mode", Mode.ALPHA, v -> v instanceof Mode)
                .value("ints", List.of(), listValidator(ConfigValueType.INTEGER_LIST, null, 0, 5, 2))
                .value("flags", List.of(), listValidator(ConfigValueType.BOOLEAN_LIST, null, null, null, 2))
                .value("modes", List.of(), listValidator(ConfigValueType.ENUM_LIST, Mode.class, null, null, 2));
        return ConfigHolder.create(
                "example_mod",
                "example-common",
                ConfigScope.COMMON,
                store,
                List.of(
                        descriptor("flag", ConfigValueType.BOOLEAN, null, null, null, 2),
                        descriptor("count", ConfigValueType.INTEGER, null, 0, 5, 2),
                        descriptor("ratio", ConfigValueType.DOUBLE, null, 0.0, 1.0, 2),
                        descriptor("mode", ConfigValueType.ENUM, Mode.class, null, null, 2),
                        descriptor("ints", ConfigValueType.INTEGER_LIST, null, 0, 5, 2),
                        descriptor("flags", ConfigValueType.BOOLEAN_LIST, null, null, null, 2),
                        descriptor("modes", ConfigValueType.ENUM_LIST, Mode.class, null, null, 2)
                ),
                Map.of(),
                Map.of()
        );
    }

    private static Predicate<Object> listValidator(ConfigValueType type,
                                                   Class<? extends Enum<?>> enumClass,
                                                   Number min,
                                                   Number max,
                                                   int decimalPlaces) {
        Predicate<Object> elementValidator = ConfigListSpecHelper.listValidator(type, enumClass, min, max, decimalPlaces);
        return value -> value instanceof List<?> list && list.stream().allMatch(elementValidator);
    }

    private static ConfigEntryDescriptor descriptor(String path,
                                                    ConfigValueType type,
                                                    Class<? extends Enum<?>> enumClass,
                                                    Number min,
                                                    Number max,
                                                    int decimalPlaces) {
        return ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(path)
                .tooltip(List.of())
                .valueType(type)
                .enumClass(enumClass)
                .minValue(min)
                .maxValue(max)
                .decimalPlaces(decimalPlaces)
                .defaultValue(null)
                .build();
    }

    private enum Mode {
        ALPHA,
        BETA
    }

    private static final class MemoryValueStore implements ConfigValueStore {
        private final Set<String> paths = new LinkedHashSet<>();
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, Predicate<Object>> validators = new LinkedHashMap<>();

        MemoryValueStore value(String path, Object value, Predicate<Object> validator) {
            paths.add(path);
            values.put(path, value);
            validators.put(path, validator);
            return this;
        }

        @Override
        public Set<String> paths() {
            return paths;
        }

        @Override
        public @Nullable Object get(String path) {
            return values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            values.put(path, value);
        }

        @Override
        public Class<?> valueClass(String path) {
            Object value = values.get(path);
            return value != null ? value.getClass() : Object.class;
        }

        @Override
        public @Nullable Object defaultValue(String path) {
            return null;
        }

        @Override
        public boolean validate(String path, Object value) {
            Predicate<Object> validator = validators.get(path);
            return validator != null && validator.test(value);
        }

        @Override
        public void save() {
        }
    }
}
