package xin.vanilla.banira.common.config.view;

import org.junit.Test;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ConfigViewBindingTest {
    static int constructions;

    static class CounterCategory {
        private int count = 0;
        CounterCategory() { constructions++; }
    }

    @Config(name = "sample")
    static class SampleConfig { }

    static class BrokenCategory {
        private int count;
        BrokenCategory() { throw new IllegalStateException("registry unavailable"); }
    }

    @Test
    public void retainedViewReadsAndWritesOnlyTheCurrentHandle() {
        ConfigViewField<Integer> field = count("left.count");
        RecordingHandle first = counter("left.count", 1);
        RecordingHandle second = counter("left.count", 2);
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>(first);
        ConfigViewBinding view = binding(current, field);
        assertEquals(Integer.valueOf(1), view.read(field));
        current.set(second);
        assertEquals(Integer.valueOf(2), view.read(field));
        view.write(field, 3);
        assertEquals(Integer.valueOf(1), first.get("left.count"));
        assertEquals(Integer.valueOf(3), second.get("left.count"));
        assertEquals(0, first.writes);
        assertEquals(0, second.saves);
        second.save();
        assertEquals(1, second.saves);
        current.set(null);
        assertNull(view.handle());
        assertEquals(Integer.valueOf(0), view.read(field));
        view.write(field, 9);
        assertEquals(1, second.writes);
    }

    @Test
    public void sameHandleReloadAndCurrentDescriptorDefaultsRemainLive() {
        ConfigViewField<Integer> field = count("left.count");
        RecordingHandle handle = counter("left.count", 1);
        ConfigViewBinding view = binding(new AtomicReference<>(handle), field);
        assertEquals(Integer.valueOf(1), view.read(field));
        handle.values.put("left.count", 8);
        assertEquals(Integer.valueOf(8), view.read(field));
        handle.values.put("left.count", null);
        handle.defaults.put("left.count", 4);
        assertEquals(Integer.valueOf(4), view.read(field));
        handle.defaults.put("left.count", 6);
        assertEquals(Integer.valueOf(6), view.read(field));
    }

    @Test
    public void sameNamedLeavesStaySeparateAndExtraPathsAreAllowed() {
        ConfigViewField<Integer> left = count("left.count");
        ConfigViewField<Integer> right = count("right.count");
        RecordingHandle handle = counter("left.count", 1).add("right.count", Integer.class, 2, 0)
                .add("extension", String.class, "keep", "keep");
        ConfigViewBinding view = binding(new AtomicReference<>(handle), left, right);
        view.write(right, 5);
        assertEquals(Integer.valueOf(1), view.read(left));
        assertEquals(Integer.valueOf(5), view.read(right));
        assertEquals("keep", handle.get("extension"));
    }

    @Test
    public void constructionAndAvailabilityDoNotInitializeDefaults() {
        constructions = 0;
        ConfigViewField<Integer> field = count("left.count");
        ConfigViewBinding view = binding(new AtomicReference<>(), field);
        view.requireAvailable();
        assertNull(view.handle());
        view.write(field, 1);
        assertEquals(0, constructions);
        assertEquals(Integer.valueOf(0), view.read(field));
        assertEquals(Integer.valueOf(0), view.read(field));
        assertEquals(1, constructions);
    }

    @Test
    public void requiredBindingFailsWhenUnavailableWithoutConstructingDefaults() {
        constructions = 0;
        ConfigViewField<Integer> field = count("left.count");
        ConfigViewBinding view = new ConfigViewBinding(SampleConfig.class, () -> null,
                Config.UnboundAccess.REQUIRE_REGISTERED, field);
        assertThrows(IllegalStateException.class, view::requireAvailable);
        assertThrows(IllegalStateException.class, view::handle);
        assertThrows(IllegalStateException.class, () -> view.read(field));
        assertThrows(IllegalStateException.class, () -> view.write(field, 1));
        assertEquals(0, constructions);
    }

    @Test
    public void handleIsResolvedExactlyOncePerOperationEvenDuringRebind() {
        ConfigViewField<Integer> field = count("left.count");
        RecordingHandle first = counter("left.count", 1);
        RecordingHandle second = counter("left.count", 2);
        AtomicInteger calls = new AtomicInteger();
        ConfigViewBinding view = new ConfigViewBinding(SampleConfig.class,
                () -> calls.getAndIncrement() == 0 ? first : second,
                Config.UnboundAccess.DEFAULTS, field);
        assertEquals(Integer.valueOf(1), view.read(field));
        assertEquals(1, calls.get());
        view.write(field, 7);
        assertEquals(2, calls.get());
        assertEquals(0, first.writes);
        assertEquals(Integer.valueOf(7), second.get("left.count"));
    }

    @Test
    public void invalidWritesAndIncompatibleRebindingsHaveNoSideEffects() {
        ConfigViewField<Integer> field = count("left.count");
        RecordingHandle valid = counter("left.count", 1);
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>(valid);
        ConfigViewBinding view = binding(current, field);
        valid.allowWrites = false;
        assertThrows(IllegalArgumentException.class, () -> view.write(field, 7));
        assertEquals(Integer.valueOf(1), valid.get("left.count"));
        assertEquals(0, valid.writes);
        assertEquals(0, valid.saves);
        current.set(new RecordingHandle());
        assertTrue(assertThrows(IllegalStateException.class, () -> view.read(field))
                .getMessage().contains("left.count"));
        RecordingHandle wrong = new RecordingHandle().add("left.count", String.class, "bad", "");
        current.set(wrong);
        assertTrue(assertThrows(IllegalStateException.class, () -> view.write(field, 1))
                .getMessage().contains("left.count"));
        assertEquals(0, wrong.writes);
        current.set(valid);
        assertEquals(Integer.valueOf(1), view.read(field));
    }

    @Test
    public void allFieldsAreCheckedBeforeAnyWriteAndForeignFieldsAreRejected() {
        ConfigViewField<Integer> left = count("left.count");
        ConfigViewField<Integer> right = count("right.count");
        RecordingHandle handle = counter("left.count", 1);
        ConfigViewBinding view = binding(new AtomicReference<>(handle), left, right);
        assertThrows(IllegalStateException.class, () -> view.write(left, 2));
        assertEquals(0, handle.writes);
        ConfigViewBinding single = binding(new AtomicReference<>(handle), left);
        assertThrows(IllegalArgumentException.class, () -> single.read(right));
        assertThrows(IllegalArgumentException.class,
                () -> binding(new AtomicReference<>(handle), left, count("left.count")));
    }

    @Test
    public void defaultConstructorFailuresNameThePathAndKeepTheCause() {
        ConfigViewField<Integer> field = ConfigViewField.scalar("broken.count", BrokenCategory.class,
                "count", Integer.class);
        ConfigViewBinding view = binding(new AtomicReference<>(), field);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> view.read(field));
        assertTrue(error.getMessage().contains("broken.count"));
        assertNotNull(error.getCause());
    }

    @Test
    public void schemaUsesDeclaredDefaultsNotTheBackendsCurrentRuntimeValueClass() {
        ConfigViewField<Integer> field = count("left.count");
        RecordingHandle handle = new RecordingHandle() {
            @Override
            public Class<?> valueClass(String path) {
                Object current = values.get(path);
                return current == null ? Object.class : current.getClass();
            }
        };
        handle.add("left.count", Integer.class, null, 0);
        ConfigViewBinding view = binding(new AtomicReference<>(handle), field);
        assertEquals(Integer.valueOf(0), view.read(field));
        handle.values.put("left.count", 17L);
        ConfigViewBinding another = binding(new AtomicReference<>(handle), field);
        assertEquals(Integer.valueOf(17), another.read(field));
    }

    @Test
    public void defaultBeansAreNotReusedAcrossObservedBoundLifecycles() {
        constructions = 0;
        ConfigViewField<Integer> field = count("left.count");
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>();
        ConfigViewBinding view = binding(current, field);
        view.read(field);
        current.set(counter("left.count", 1));
        view.read(field);
        current.set(null);
        view.read(field);
        assertEquals(2, constructions);
    }

    static ConfigViewField<Integer> count(String path) {
        return ConfigViewField.scalar(path, CounterCategory.class, "count", Integer.class);
    }

    static RecordingHandle counter(String path, int value) {
        return new RecordingHandle().add(path, Integer.class, value, 0);
    }

    static ConfigViewBinding binding(AtomicReference<BaniraConfigHandle> current, ConfigViewField<?>... fields) {
        return new ConfigViewBinding(SampleConfig.class, current::get, Config.UnboundAccess.DEFAULTS, fields);
    }

    static class RecordingHandle implements BaniraConfigHandle {
        final Map<String, Object> values = new LinkedHashMap<>();
        final Map<String, Object> defaults = new LinkedHashMap<>();
        final Map<String, Class<?>> types = new LinkedHashMap<>();
        int writes;
        int saves;
        boolean allowWrites = true;

        RecordingHandle add(String path, Class<?> type, Object value, Object fallback) {
            values.put(path, value);
            defaults.put(path, fallback);
            types.put(path, type);
            return this;
        }

        public String getModId() { return "fixture"; }
        public String getConfigName() { return "sample"; }
        public void save() { saves++; }
        @SuppressWarnings("unchecked")
        public <T> T get(String path) { return (T) values.get(path); }
        public void set(String path, Object value) { writes++; values.put(path, value); }
        public Set<String> valuePaths() { return values.keySet(); }
        public boolean hasValue(String path) { return values.containsKey(path); }
        public String findValuePath(String key) { throw new AssertionError("No leaf-name resolution"); }
        public Class<?> valueClass(String path) { return types.get(path); }
        public Object defaultValue(String path) { return defaults.get(path); }
        public boolean validate(String path, Object value) { return allowWrites; }
        public boolean setIfValid(String path, Object value) {
            throw new AssertionError("Expected explicit validate then set");
        }
    }
}
