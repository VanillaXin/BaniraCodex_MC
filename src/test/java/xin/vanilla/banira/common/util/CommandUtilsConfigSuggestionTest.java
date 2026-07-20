package xin.vanilla.banira.common.util;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/** 验证数值配置补全不会因泛型重载产生运行时类型转换。 */
public class CommandUtilsConfigSuggestionTest {
    @Test
    public void numericValueSuggestionUsesObjectStringConversion() {
        String path = "base.batch.sweepBatchLimit";
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(path, 250);
        ConfigEntryDescriptor descriptor = ConfigEntryDescriptor.builder()
                .path(path)
                .displayName("sweepBatchLimit")
                .valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER)
                .defaultValue(250)
                .build();
        ConfigHolder holder = ConfigHolder.create("test", "test-common", ConfigScope.COMMON,
                new MapStore(values), Collections.singletonList(descriptor),
                Collections.emptyMap(), Collections.emptyMap());

        SuggestionsBuilder builder = new SuggestionsBuilder("", 0);
        CommandUtils.configValueSuggestion(holder, builder, path);
        List<String> suggestions = builder.build().getList().stream()
                .map(Suggestion::getText)
                .collect(Collectors.toList());

        assertTrue(suggestions.contains("250"));
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values;

        private MapStore(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public Set<String> paths() {
            return values.keySet();
        }

        @Override
        public Object get(String path) {
            return values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            values.put(path, value);
        }

        @Override
        public Class<?> valueClass(String path) {
            return values.get(path).getClass();
        }

        @Override
        public Object defaultValue(String path) {
            return 250;
        }

        @Override
        public boolean validate(String path, Object value) {
            return value instanceof Integer;
        }

        @Override
        public void save() {
        }
    }
}
