package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class ConfigListSpecHelperTest {

    @Test
    public void resolvesListElementTypesAndBoundsFromFields() throws Exception {
        Field raw = Fixture.class.getDeclaredField("raw");
        Field integers = Fixture.class.getDeclaredField("integers");
        Field longs = Fixture.class.getDeclaredField("longs");
        Field doubles = Fixture.class.getDeclaredField("doubles");
        Field booleans = Fixture.class.getDeclaredField("booleans");
        Field modes = Fixture.class.getDeclaredField("modes");

        assertEquals(String.class, ConfigListSpecHelper.resolveListElementClass(raw));
        assertEquals(ConfigValueType.INTEGER_LIST, ConfigListSpecHelper.listValueTypeForElement(
                ConfigListSpecHelper.resolveListElementClass(integers)));
        assertEquals(ConfigValueType.LONG_LIST, ConfigListSpecHelper.listValueTypeForElement(
                ConfigListSpecHelper.resolveListElementClass(longs)));
        assertEquals(ConfigValueType.DOUBLE_LIST, ConfigListSpecHelper.listValueTypeForElement(
                ConfigListSpecHelper.resolveListElementClass(doubles)));
        assertEquals(ConfigValueType.BOOLEAN_LIST, ConfigListSpecHelper.listValueTypeForElement(
                ConfigListSpecHelper.resolveListElementClass(booleans)));
        assertEquals(ConfigValueType.ENUM_LIST, ConfigListSpecHelper.listValueTypeForElement(
                ConfigListSpecHelper.resolveListElementClass(modes)));

        assertArrayEquals(new Number[]{1, 5}, ConfigListSpecHelper.listBoundsForValueType(integers, ConfigValueType.INTEGER_LIST));
        assertArrayEquals(new Number[]{10L, 20L}, ConfigListSpecHelper.listBoundsForValueType(longs, ConfigValueType.LONG_LIST));
        assertArrayEquals(new Number[]{0.0, 1.0}, ConfigListSpecHelper.listBoundsForValueType(doubles, ConfigValueType.DOUBLE_LIST));
        assertEquals(1, ConfigListSpecHelper.decimalPlacesForList(doubles, ConfigValueType.DOUBLE_LIST));
        assertEquals(Mode.class, ConfigListSpecHelper.enumClassForList(Mode.class));
    }

    @Test
    public void normalizesDefaultsAndValidatesElements() {
        List<Object> normalized = ConfigListSpecHelper.normalizeDefaultList(
                List.of("1", 2, "6", "bad"),
                ConfigValueType.INTEGER_LIST,
                null,
                1,
                5,
                2);
        Predicate<Object> validator = ConfigListSpecHelper.listValidator(ConfigValueType.INTEGER_LIST, null, 1, 5, 2);

        assertEquals(List.of(1, 2), normalized);
        assertTrue(validator.test("5"));
        assertFalse(validator.test("6"));
        assertFalse(validator.test("bad"));
    }

    @Test
    public void doubleListsRoundAndRespectRange() {
        ConfigEntryDescriptor desc = descriptor(ConfigValueType.DOUBLE_LIST, null, 0.0, 1.0, 1);

        assertEquals(List.of(0.1, 0.6), ConfigListSpecHelper.normalizeListForRuntime(
                List.of("0.14", 0.55, 1.2), desc));
        assertEquals(List.of(0.1, 0.6), ConfigListSpecHelper.normalizeListForGui(
                List.of("0.14", 0.55, 1.2), desc));
        assertEquals(List.of(0.1, 0.6), ConfigListSpecHelper.parseNetworkCsv("0.14, 0.55, bad, 1.2", desc));
    }

    @Test
    public void enumListsUseEnumsAtRuntimeAndNamesForGui() {
        ConfigEntryDescriptor desc = descriptor(ConfigValueType.ENUM_LIST, Mode.class, null, null, 2);

        assertEquals(List.of(Mode.ALPHA, Mode.BETA), ConfigListSpecHelper.normalizeListForRuntime(
                List.of("alpha", Mode.BETA, "missing"), desc));
        assertEquals(List.of("ALPHA", "BETA"), ConfigListSpecHelper.normalizeListForGui(
                List.of("alpha", Mode.BETA, "missing"), desc));
        assertEquals(List.of(Mode.ALPHA, Mode.BETA), ConfigListSpecHelper.listFromGuiItems(
                List.of("ALPHA", "beta", "missing"), desc));
        assertEquals(List.of(Mode.ALPHA, Mode.BETA), ConfigListSpecHelper.parseNetworkCsv(
                "alpha, missing, BETA", desc));
    }

    @Test
    public void booleanNetworkCsvRejectsInvalidBooleanTokens() {
        ConfigEntryDescriptor desc = descriptor(ConfigValueType.BOOLEAN_LIST, null, null, null, 2);

        assertEquals(List.of(true, false), ConfigListSpecHelper.parseNetworkCsv("true, maybe, false", desc));
    }

    private static ConfigEntryDescriptor descriptor(ConfigValueType type,
                                                    Class<? extends Enum<?>> enumClass,
                                                    Number min,
                                                    Number max,
                                                    int decimalPlaces) {
        return ConfigEntryDescriptor.builder()
                .path("list")
                .displayName("List")
                .tooltip(List.of())
                .valueType(type)
                .enumClass(enumClass)
                .minValue(min)
                .maxValue(max)
                .decimalPlaces(decimalPlaces)
                .defaultValue(List.of())
                .build();
    }

    private enum Mode {
        ALPHA,
        BETA
    }

    @SuppressWarnings("rawtypes")
    private static final class Fixture {
        private List raw;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
        private List<Integer> integers;

        @ConfigEntry.BoundedLong(min = 10, max = 20)
        private List<Long> longs;

        @ConfigEntry.BoundedDouble(min = 0.0, max = 1.0, decimalPlaces = 1)
        private List<Double> doubles;

        private List<Boolean> booleans;
        private List<Mode> modes;
    }
}
