package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import static org.junit.Assert.*;

public class ForgeConfigValueComparisonTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

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
            assertFalse(holder.matchesStoredValue("missing", null));
            assertFalse(holder.matchesStoredValue("rules", "rule:0"));
            com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            org.junit.Assume.assumeTrue(allocation.isThreadAllocatedMemorySupported());
            allocation.setThreadAllocatedMemoryEnabled(true);
            for (int i = 0; i < 10000; i++) assertTrue(holder.matchesStoredValue("rules", expected));
            long thread = Thread.currentThread().getId(), before = allocation.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 1000; i++) assertTrue(holder.matchesStoredValue("rules", expected));
            assertTrue("Comparison allocated per-entry snapshots or iterators", allocation.getThreadAllocatedBytes(thread) - before < 16000);
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
