package xin.vanilla.banira.common.config.view;

import org.junit.Test;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static xin.vanilla.banira.common.config.view.ConfigViewBindingTest.*;

public class ConfigViewValuesTest {
    enum Mode {
        FIRST, SECOND;
        public static Mode parse(Object value) { return "alias".equals(value) ? SECOND : FIRST; }
    }

    static class Values {
        private int count = 3;
        private double rate = 0.002;
        private boolean enabled = true;
        private String text = "default";
        @ConfigEntry.Access(nulls = ConfigEntry.Access.NullPolicy.KEEP)
        private String nullable = "declaration";
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String nonEmpty = "prefix";
        private Mode mode = Mode.FIRST;
        @ConfigEntry.Access(enumParser = "parse")
        private Mode parsed = Mode.FIRST;
        private List<String> rules = Arrays.asList("tick, clazz -> tick > 5", "minecraft:arrow");
    }

    @Test
    public void explicitNullAndEmptyPoliciesDifferFromOrdinaryFallback() {
        ConfigViewField<String> text = scalar("text", String.class);
        ConfigViewField<String> nullable = scalar("nullable", String.class);
        ConfigViewField<String> nonEmpty = scalar("nonEmpty", String.class);
        RecordingHandle handle = new RecordingHandle().add("text", String.class, "", "default")
                .add("nullable", String.class, null, "declaration")
                .add("nonEmpty", String.class, "", "prefix");
        ConfigViewBinding view = binding(new AtomicReference<>(handle), text, nullable, nonEmpty);
        assertEquals("", view.read(text));
        assertNull(view.read(nullable));
        assertEquals("prefix", view.read(nonEmpty));
        handle.values.put("text", null);
        assertEquals("default", view.read(text));
        ConfigViewBinding unbound = binding(new AtomicReference<>(), text, nullable, nonEmpty);
        assertNull(unbound.read(nullable));
        assertEquals("default", unbound.read(text));
    }

    @Test
    public void numbersEnumsAndParserUseDeclaredTypesWithoutRounding() {
        ConfigViewField<Integer> count = scalar("count", Integer.class);
        ConfigViewField<Double> rate = scalar("rate", Double.class);
        ConfigViewField<Boolean> enabled = scalar("enabled", Boolean.class);
        ConfigViewField<Mode> mode = scalar("mode", Mode.class);
        ConfigViewField<Mode> parsed = ConfigViewField.scalar("parsed", Values.class, "parsed", Mode.class, Mode::parse);
        RecordingHandle handle = new RecordingHandle().add("count", Integer.class, 17L, 3)
                .add("rate", Double.class, 0.002, 0.125)
                .add("enabled", Boolean.class, "invalid", true)
                .add("mode", Mode.class, "SECOND", Mode.FIRST)
                .add("parsed", Mode.class, "alias", Mode.FIRST);
        ConfigViewBinding view = binding(new AtomicReference<>(handle), count, rate, enabled, mode, parsed);
        assertEquals(Integer.valueOf(17), view.read(count));
        assertEquals(Double.valueOf(0.002), view.read(rate));
        assertTrue(view.read(enabled));
        assertEquals(Mode.SECOND, view.read(mode));
        assertEquals(Mode.SECOND, view.read(parsed));
        handle.values.put("mode", "unknown");
        assertEquals(Mode.FIRST, view.read(mode));
        handle.values.put("count", "17");
        assertEquals(Integer.valueOf(3), view.read(count));
    }

    @Test
    public void listsKeepCommasAndUseMutableDetachedSnapshotsOnBothReadAndWrite() {
        ConfigViewField<List<String>> rules = ConfigViewField.list("rules", Values.class, "rules", String.class);
        List<String> original = Arrays.asList("tick, clazz -> tick > 5", "minecraft:arrow");
        RecordingHandle handle = new RecordingHandle().add("rules", List.class, original, original);
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>(handle);
        ConfigViewBinding view = binding(current, rules);
        List<String> read = view.read(rules);
        assertEquals(original, read);
        read.remove(1);
        assertEquals(2, ((List<?>) handle.get("rules")).size());
        view.write(rules, read);
        read.clear();
        assertEquals(Arrays.asList("tick, clazz -> tick > 5"), handle.get("rules"));
        handle.values.put("rules", null);
        view.read(rules).clear();
        assertEquals(original, view.read(rules));
        current.set(null);
        view.read(rules).clear();
        assertEquals(original, view.read(rules));
        current.set(new RecordingHandle().add("rules", List.class, null, Arrays.asList("new,default")));
        assertEquals(Arrays.asList("new,default"), view.read(rules));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void malformedListsAreNotSplitOrPartiallyWritten() {
        ConfigViewField<List<String>> rules = ConfigViewField.list("rules", Values.class, "rules", String.class);
        RecordingHandle handle = new RecordingHandle().add("rules", List.class,
                "a,b", Arrays.asList("default,rule"));
        ConfigViewBinding view = binding(new AtomicReference<>(handle), rules);
        assertEquals(Arrays.asList("default,rule"), view.read(rules));
        handle.values.put("rules", Arrays.asList("a", 7));
        assertThrows(IllegalStateException.class, () -> view.read(rules));
        assertThrows(IllegalArgumentException.class, () -> view.write((ConfigViewField) rules, Arrays.asList("a", 7)));
        assertEquals(0, handle.writes);
        assertEquals(0, handle.saves);
    }

    @Test
    public void backendConcreteListClassesAreAccepted() {
        ConfigViewField<List<String>> rules = ConfigViewField.list("rules", Values.class, "rules", String.class);
        List<String> values = Arrays.asList("a,b", "c");
        RecordingHandle handle = new RecordingHandle().add("rules", values.getClass(), values, values);
        assertEquals(values, binding(new AtomicReference<>(handle), rules).read(rules));
    }

    private static <T> ConfigViewField<T> scalar(String name, Class<T> type) {
        return ConfigViewField.scalar(name, Values.class, name, type);
    }
}
