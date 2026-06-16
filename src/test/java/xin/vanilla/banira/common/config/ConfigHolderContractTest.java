package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;

import javax.annotation.Nullable;
import java.util.*;

import static org.junit.Assert.*;

public class ConfigHolderContractTest {

    @Test
    public void exposesValuesDefaultsValidationAndScopeSemantics() {
        MemoryValueStore store = new MemoryValueStore()
                .value("alpha.count", 3, 1, Integer.class, v -> v instanceof Integer i && i >= 0 && i <= 5)
                .value("alpha.enabled", true, true, Boolean.class, v -> v instanceof Boolean);
        ConfigHolder holder = holder(store);

        assertEquals("example_mod", holder.getModId());
        assertEquals("example-common", holder.getConfigName());
        assertEquals(ConfigScope.COMMON, holder.getConfigScope());
        assertEquals(3, (int) holder.get("alpha.count"));
        assertEquals(1, holder.defaultValue("alpha.count"));
        assertTrue(holder.validate("alpha.count", 5));
        assertFalse(holder.validate("alpha.count", 6));
        assertTrue(holder.setIfValid("alpha.count", 4));
        assertEquals(4, (int) holder.get("alpha.count"));
        assertFalse(holder.setIfValid("alpha.count", 9));
        assertEquals(4, (int) holder.get("alpha.count"));
        assertTrue(holder.canSyncToServer());
        assertFalse(holder.isServerConfig());
    }

    @Test
    public void findsValuePathsAndGroupsDescriptorsByCategory() {
        MemoryValueStore store = new MemoryValueStore()
                .value("alpha.count", 3, 1, Integer.class, v -> true)
                .value("alpha.enabled", true, true, Boolean.class, v -> true)
                .value("beta.nested.name", "n", "default", String.class, v -> true);
        ConfigHolder holder = holder(store);

        assertEquals("alpha.count", holder.findValuePath("alpha.count"));
        assertEquals("beta.nested.name", holder.findValuePath("nested.name"));
        assertNull(holder.findValuePath("alpha"));

        List<ConfigHolder.CategoryGroup> groups = holder.getDescriptorsGroupedByCategory();
        assertEquals(List.of("alpha", "beta"), groups.stream().map(ConfigHolder.CategoryGroup::getCategoryPath).toList());
        assertEquals("Alpha", groups.get(0).getDisplayName());
        assertEquals(List.of("alpha.count", "alpha.enabled"),
                groups.get(0).getEntries().stream().map(ConfigEntryDescriptor::getPath).toList());
    }

    @Test
    public void buildsNestedCategoryTreeAndKeepsTitleSpecs() {
        MemoryValueStore store = new MemoryValueStore()
                .value("alpha.count", 3, 1, Integer.class, v -> true)
                .value("beta.nested.name", "n", "default", String.class, v -> true);
        ConfigHolder holder = holder(store);

        assertEquals(ConfigCategoryTitleSpec.Kind.LITERAL, holder.getCategoryTitleSpec("alpha").getKind());

        ConfigHolder.CategoryTreeNode root = holder.getCategoryTree().get(0);
        assertEquals("", root.getCategoryPath());
        List<String> childPaths = root.getChildren().stream().map(ConfigHolder.CategoryTreeNode::getCategoryPath).toList();
        assertEquals(List.of("alpha", "beta"), childPaths);
        ConfigHolder.CategoryTreeNode beta = root.getChildren().get(1);
        assertEquals(List.of("beta.nested"),
                beta.getChildren().stream().map(ConfigHolder.CategoryTreeNode::getCategoryPath).toList());
    }

    private static ConfigHolder holder(MemoryValueStore store) {
        return ConfigHolder.create(
                "example_mod",
                "example-common",
                ConfigScope.COMMON,
                store,
                List.of(
                        descriptor("alpha.count", "Count", ConfigValueType.INTEGER, 1),
                        descriptor("alpha.enabled", "Enabled", ConfigValueType.BOOLEAN, true),
                        descriptor("beta.nested.name", "Name", ConfigValueType.STRING, "default")
                ),
                orderedCategoryTooltips(),
                orderedCategoryTitleSpecs()
        );
    }

    private static Map<String, String> orderedCategoryTooltips() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("alpha", "Alpha");
        result.put("beta", "Beta");
        result.put("beta.nested", "Nested");
        return result;
    }

    private static Map<String, ConfigCategoryTitleSpec> orderedCategoryTitleSpecs() {
        Map<String, ConfigCategoryTitleSpec> result = new LinkedHashMap<>();
        result.put("alpha", ConfigCategoryTitleSpec.literal("Alpha Title"));
        result.put("beta", ConfigCategoryTitleSpec.translationKey("category.beta"));
        return result;
    }

    private static ConfigEntryDescriptor descriptor(String path, String displayName,
                                                    ConfigValueType valueType, Object defaultValue) {
        return ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(displayName)
                .tooltip(List.of())
                .valueType(valueType)
                .defaultValue(defaultValue)
                .build();
    }

    private interface Validator {
        boolean test(Object value);
    }

    private static final class MemoryValueStore implements ConfigValueStore {
        private final Set<String> paths = new LinkedHashSet<>();
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, Object> defaults = new LinkedHashMap<>();
        private final Map<String, Class<?>> classes = new LinkedHashMap<>();
        private final Map<String, Validator> validators = new LinkedHashMap<>();

        MemoryValueStore value(String path, Object value, Object defaultValue, Class<?> valueClass, Validator validator) {
            paths.add(path);
            values.put(path, value);
            defaults.put(path, defaultValue);
            classes.put(path, valueClass);
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
            return classes.get(path);
        }

        @Override
        public @Nullable Object defaultValue(String path) {
            return defaults.get(path);
        }

        @Override
        public boolean validate(String path, Object value) {
            Validator validator = validators.get(path);
            return validator != null && validator.test(value);
        }

        @Override
        public void save() {
        }
    }
}
