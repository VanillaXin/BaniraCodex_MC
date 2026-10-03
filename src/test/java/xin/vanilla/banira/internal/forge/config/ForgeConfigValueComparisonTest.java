package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ForgeConfigValueComparisonTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void preparedMatchFreezesExpectedValuesAndReadsLiveNativeValuesAndFiles() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<List<? extends String>> rules = builder.defineList("rules", Arrays.asList("first", "second"), v -> v instanceof String);
        ForgeConfigSpec.EnumValue<Mode> mode = builder.defineEnum("mode", Mode.ALL);
        ForgeConfigSpec spec = builder.build();
        Map<String, ForgeConfigSpec.ConfigValue<?>> handles = new LinkedHashMap<>();
        handles.put("rules", rules); handles.put("mode", mode);
        ForgeConfigBackend backend = new ForgeConfigBackend(spec, handles);
        ConfigHolder holder = ConfigHolder.create("test", "test-common", ConfigScope.COMMON, backend,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        backend.setHolder(holder);
        CommentedFileConfig file = backend.wrap(CommentedFileConfig.of(temporary.newFile("prepared.toml")));
        try {
            file.load(); spec.setConfig(file); holder.acceptInitialExternalLoad();
            List<String> expectedRules = new ArrayList<>(Arrays.asList("first", "second"));
            Map<String, Object> expected = new LinkedHashMap<>();
            expected.put("rules", expectedRules); expected.put("mode", Mode.ALL);
            BooleanSupplier match = holder.prepareStoredMatch(expected, true);
            BooleanSupplier missing = holder.prepareStoredMatch(Collections.singletonMap("missing", null), false);
            assertTrue(match.getAsBoolean()); assertFalse(missing.getAsBoolean());
            expectedRules.clear(); expected.clear();
            assertTrue("Expected values must be frozen at preparation", match.getAsBoolean());
            rules.set(new ArrayList<>(Arrays.asList("first", "second")));
            ((List<String>) rules.get()).set(0, "changed");
            assertFalse("Unsaved in-place native edit must invalidate", match.getAsBoolean());
            rules.set(new ArrayList<>(Arrays.asList("first", "second")));
            assertTrue(match.getAsBoolean());
            mode.set(Mode.NONE); assertFalse(match.getAsBoolean());
            Files.write(file.getNioPath(), "rules = [\"first\", \"second\"]\nmode = \"ALL\"\n".getBytes(StandardCharsets.UTF_8));
            file.load(); assertTrue(match.getAsBoolean());
            file.close(); assertFalse("Closed native file must not reuse cached ConfigValues", match.getAsBoolean());
            file = backend.wrap(CommentedFileConfig.of(temporary.newFile("replacement.toml")));
            file.load(); spec.setConfig(file); holder.acceptInitialExternalLoad();
            assertTrue("Prepared match must follow the current file lock", match.getAsBoolean());
            rules.set(Collections.singletonList("replacement")); assertFalse(match.getAsBoolean());
        } finally { file.close(); }
    }

    private enum Mode { ALL, NONE }

    @Test public void readSnapshotsCaptureNativeValuesAndRejectClosedFiles() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<List<? extends String>> rules = builder.defineList("rules", Collections.singletonList("initial"), v -> v instanceof String);
        ForgeConfigSpec spec = builder.build();
        ForgeConfigBackend backend = new ForgeConfigBackend(spec, Collections.singletonMap("rules", rules));
        ConfigHolder holder = ConfigHolder.create("test", "read", ConfigScope.COMMON, backend,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        backend.setHolder(holder);
        CommentedFileConfig file = backend.wrap(CommentedFileConfig.of(temporary.newFile("read.toml")));
        try {
            file.load(); spec.setConfig(file); holder.acceptInitialExternalLoad(); file.save();
            rules.set(new ArrayList<>(Collections.singletonList("unsaved")));
            byte[] before = Files.readAllBytes(file.getNioPath());
            ConfigReadSnapshot read = holder.snapshotForRead(Collections.singleton("rules"));
            ((List<String>) rules.get()).set(0, "later");
            assertEquals(Collections.singletonList("unsaved"), read.getValues().get("rules"));
            assertEquals(Collections.singletonList("later"), holder.snapshotForRead(Collections.singleton("rules")).getValues().get("rules"));
            assertArrayEquals(before, Files.readAllBytes(file.getNioPath()));
            // Runtime capture depends on memory; disk changes still require the normal reload.
            Files.write(file.getNioPath(), "rules=[\"external\"]\n".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalStateException.class, () -> holder.snapshotForEdit(Collections.singleton("rules")));
            assertEquals(Collections.singletonList("later"), holder.snapshotForRead(Collections.singleton("rules")).getValues().get("rules"));
            file.load();
            assertEquals(Collections.singletonList("external"), holder.snapshotForRead(Collections.singleton("rules")).getValues().get("rules"));
            file.close();
            assertThrows(IllegalStateException.class, () -> holder.snapshotForRead(Collections.singleton("rules")));
            file = backend.wrap(CommentedFileConfig.of(temporary.newFile("read-replacement.toml")));
            file.load(); spec.setConfig(file); holder.acceptInitialExternalLoad();
            assertEquals(Collections.singletonList("initial"), holder.snapshotForRead(Collections.singleton("rules")).getValues().get("rules"));
        } finally { file.close(); }
    }

    @Test public void comparesLiveStoredValuesWithoutCopyingOrExposingLists() throws Exception {
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 1024; i++) expected.add("rule:" + i);
        expected = Collections.unmodifiableList(expected);
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<List<? extends String>> rules = builder.defineList("rules", expected, v -> v instanceof String);
        ForgeConfigSpec spec = builder.build();
        ForgeConfigBackend backend = new ForgeConfigBackend(spec, Collections.singletonMap("rules", rules));
        ConfigHolder holder = ConfigHolder.create("test", "test-common", ConfigScope.COMMON, backend,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        backend.setHolder(holder);
        try (CommentedFileConfig file = backend.wrap(CommentedFileConfig.of(temporary.newFile("values.toml")))) {
            file.load(); spec.setConfig(file); holder.acceptInitialExternalLoad(); file.save();
            assertTrue(holder.matchesStoredValue("rules", expected));
            BooleanSupplier prepared = holder.prepareStoredMatch(Collections.singletonMap("rules", expected), false);
            assertFalse(holder.matchesStoredValue("missing", null));
            assertFalse(holder.matchesStoredValue("rules", "rule:0"));
            com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            org.junit.Assume.assumeTrue(allocation.isThreadAllocatedMemorySupported());
            allocation.setThreadAllocatedMemoryEnabled(true);
            for (int i = 0; i < 10000; i++) assertTrue(holder.matchesStoredValue("rules", expected));
            long thread = Thread.currentThread().getId(), before = allocation.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 1000; i++) assertTrue(holder.matchesStoredValue("rules", expected));
            assertTrue("Comparison allocated per-entry snapshots or iterators", allocation.getThreadAllocatedBytes(thread) - before < 16000);
            for (int i = 0; i < 10000; i++) assertTrue(prepared.getAsBoolean());
            before = allocation.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 1000; i++) assertTrue(prepared.getAsBoolean());
            assertTrue("Prepared comparison allocated per-entry snapshots or iterators", allocation.getThreadAllocatedBytes(thread) - before < 16000);
            List<String> copy = holder.get("rules"); copy.clear();
            assertTrue(holder.matchesStoredValue("rules", expected));
            holder.set("rules", Arrays.asList("replacement", "second"));
            assertFalse(holder.matchesStoredValue("rules", expected));
            assertTrue(holder.matchesStoredValue("rules", Arrays.asList("replacement", "second")));
            assertTrue(holder.matchesStoredValue("rules", new LinkedList<>(Arrays.asList("replacement", "second"))));
            assertFalse(holder.matchesStoredValue("rules", Collections.singletonList("replacement")));
            assertFalse(holder.matchesStoredValue("rules", Arrays.asList("replacement", "different")));
            rules.set(new ArrayList<>(Arrays.asList("replacement", "second")));
            ((List<String>) rules.get()).set(0, "direct-edit");
            assertFalse(holder.matchesStoredValue("rules", Arrays.asList("replacement", "second")));
        }
    }
}
