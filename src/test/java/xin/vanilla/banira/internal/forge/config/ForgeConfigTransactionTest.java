package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ForgeConfigTransactionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void batchCommitsOnceAndSnapshotIsImmutable() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        byte[] original = "[base]\na = 1\nb = 2\nc = 3\nd = 4\n".getBytes(StandardCharsets.UTF_8);
        Files.write(path, original);
        AtomicInteger writes = new AtomicInteger();
        ConfigWriter writer = (candidate, output) -> {
            writes.incrementAndGet();
            TomlFormat.instance().createWriter().write(candidate, output);
        };
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path),
                new ForgeConfigSpec.Builder().build(), candidate -> {}, writer)) {
            file.load();
            Set<String> paths = new LinkedHashSet<>(Arrays.asList("base.a", "base.b", "base.c", "base.d"));
            ConfigEditSnapshot snapshot = file.snapshotForEdit(paths);
            assertArrayEquals(original, snapshot.getSourceBytes());
            snapshot.getSourceBytes()[0] = 0;
            assertArrayEquals(original, snapshot.getSourceBytes());
            assertThrows(UnsupportedOperationException.class, () -> snapshot.getValues().clear());
            Map<String, Object> changes = new LinkedHashMap<>();
            int number = 10;
            for (String key : paths) changes.put(key, number++);
            assertEquals(ConfigCommitResult.APPLIED, file.compareAndSetAll(snapshot, changes));
            file.save();
            assertEquals(1, writes.get());
            assertEquals(13, ((Number) file.get("base.d")).intValue());
            assertEquals(ConfigCommitResult.CONFLICT, file.compareAndSetAll(snapshot, changes));
            assertEquals(ConfigCommitResult.UNCHANGED, file.compareAndSetAll(file.snapshotForEdit(paths), changes));
            assertEquals(1, writes.get());
        }
    }

    @Test public void diskConflictsInvalidValuesAndWriteFailuresLeaveMemoryUntouched() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        Files.write(path, "a = 1\nb = 2\n".getBytes(StandardCharsets.UTF_8));
        try (Fixture fixture = new Fixture(path)) {
            Set<String> paths = fixture.holder.valuePaths();
            ConfigEditSnapshot snapshot = fixture.holder.snapshotForEdit(paths);
            AtomicInteger notifications = new AtomicInteger();
            fixture.holder.onSaved(changes -> notifications.incrementAndGet());
            Map<String, Object> invalid = new LinkedHashMap<>();
            invalid.put("a", 5);
            invalid.put("b", -1);
            assertThrows(IllegalArgumentException.class, () ->
                    fixture.holder.compareAndSetAll(snapshot, invalid, ConfigEditOrigin.LOCAL_MIGRATION));
            assertEquals(Integer.valueOf(1), fixture.holder.get("a"));
            byte[] external = "a = 9\nb = 2\n".getBytes(StandardCharsets.UTF_8);
            Files.write(path, external);
            assertEquals(ConfigCommitResult.CONFLICT, fixture.holder.compareAndSetAll(snapshot,
                    Collections.singletonMap("a", 5), ConfigEditOrigin.LOCAL_MIGRATION));
            assertArrayEquals(external, Files.readAllBytes(path));
            assertEquals(Integer.valueOf(1), fixture.holder.get("a"));
            assertEquals(0, notifications.get());
            assertThrows(IllegalStateException.class, () -> fixture.holder.snapshotForEdit(paths));
        }
        ConfigWriter broken = (candidate, output) -> { throw new WritingException("disk full"); };
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path),
                new ForgeConfigSpec.Builder().build(), candidate -> {}, broken)) {
            file.load();
            ConfigEditSnapshot snapshot = file.snapshotForEdit(Collections.singleton("a"));
            assertThrows(WritingException.class, () -> file.compareAndSetAll(snapshot, Collections.singletonMap("a", 5)));
            assertEquals(9, ((Number) file.get("a")).intValue());
            assertArrayEquals(snapshot.getSourceBytes(), Files.readAllBytes(path));
        }
    }

    @Test public void savedListenerRunsAfterFileAndHolderLocksAreReleased() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        Files.write(path, "a = 1\nb = 2\n".getBytes(StandardCharsets.UTF_8));
        try (Fixture fixture = new Fixture(path)) {
            List<Throwable> errors = new ArrayList<>();
            AtomicInteger calls = new AtomicInteger();
            fixture.holder.onSaved(changes -> {
                calls.incrementAndGet();
                try {
                    CompletableFuture.runAsync(() -> fixture.holder.snapshotForEdit(fixture.holder.valuePaths()))
                            .get(1, TimeUnit.SECONDS);
                } catch (Exception error) { errors.add(error); }
            });
            assertEquals(ConfigCommitResult.APPLIED, fixture.holder.compareAndSetAll(
                    fixture.holder.snapshotForEdit(fixture.holder.valuePaths()),
                    Collections.singletonMap("a", 5), ConfigEditOrigin.LOCAL_MIGRATION));
            assertEquals(1, calls.get());
            assertTrue(errors.toString(), errors.isEmpty());
            fixture.holder.save();
            assertEquals(1, calls.get());
        }
    }

    @Test public void guardsRejectWholeBatchAndCannotReenterWrites() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        Files.write(path, "a = 1\nb = 2\n".getBytes(StandardCharsets.UTF_8));
        try (Fixture fixture = new Fixture(path)) {
            byte[] original = Files.readAllBytes(path);
            List<ConfigEditOrigin> seen = new ArrayList<>();
            Runnable unregister = fixture.holder.onEdit((origin, changes) -> {
                seen.add(origin);
                assertThrows(UnsupportedOperationException.class, () -> changes.clear());
                if (changes.containsKey("b")) throw new IllegalArgumentException("rejected");
            });
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("a", 5);
            changes.put("b", 6);
            assertThrows(IllegalArgumentException.class, () -> fixture.holder.setAll(changes, ConfigEditOrigin.REMOTE));
            assertArrayEquals(original, Files.readAllBytes(path));
            assertEquals(Collections.singletonList(ConfigEditOrigin.REMOTE), seen);
            unregister.run();
            Runnable stopReentry = fixture.holder.onEdit((origin, values) -> fixture.holder.set("b", 6));
            assertThrows(IllegalStateException.class, () -> fixture.holder.set("a", 5));
            stopReentry.run();
            fixture.holder.setAll(changes, ConfigEditOrigin.UI);
            assertEquals(Integer.valueOf(5), fixture.holder.get("a"));
            assertEquals(Integer.valueOf(6), fixture.holder.get("b"));
        }
    }

    @Test public void conflictDuringStagedWriteReturnsConflictWithoutPublishing() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        byte[] original = "a = 1\n".getBytes(StandardCharsets.UTF_8);
        byte[] external = "a = 9\n".getBytes(StandardCharsets.UTF_8);
        Files.write(path, original);
        ConfigWriter intervening = (candidate, output) -> {
            TomlFormat.instance().createWriter().write(candidate, output);
            try { Files.write(path, external); }
            catch (java.io.IOException error) { throw new WritingException("test external edit", error); }
        };
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path),
                new ForgeConfigSpec.Builder().build(), candidate -> {}, intervening)) {
            file.load();
            assertEquals(ConfigCommitResult.CONFLICT, file.compareAndSetAll(file.snapshotForEdit(Collections.singleton("a")),
                    Collections.singletonMap("a", 5)));
            assertEquals(1, ((Number) file.get("a")).intValue());
            assertArrayEquals(external, Files.readAllBytes(path));
        }
    }

    @Test public void snapshotRejectsOtherFileAndUnselectedPaths() throws Exception {
        Path a = temporary.newFile("a.toml").toPath();
        Path b = temporary.newFile("b.toml").toPath();
        byte[] initial = "a = 1\nb = 2\n".getBytes(StandardCharsets.UTF_8);
        Files.write(a, initial);
        Files.write(b, initial);
        try (Fixture first = new Fixture(a); Fixture second = new Fixture(b)) {
            ConfigEditSnapshot snapshot = first.holder.snapshotForEdit(Collections.singleton("a"));
            byte[] normalizedA = Files.readAllBytes(a);
            byte[] normalizedB = Files.readAllBytes(b);
            assertThrows(IllegalArgumentException.class, () -> second.holder.compareAndSetAll(snapshot,
                    Collections.singletonMap("a", 4), ConfigEditOrigin.LOCAL_MIGRATION));
            assertThrows(IllegalArgumentException.class, () -> first.holder.compareAndSetAll(snapshot,
                    Collections.singletonMap("b", 4), ConfigEditOrigin.LOCAL_MIGRATION));
            assertArrayEquals(normalizedA, Files.readAllBytes(a));
            assertArrayEquals(normalizedB, Files.readAllBytes(b));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final ConfigHolder holder;
        final CommentedFileConfig file;
        Fixture(Path path) {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
            values.put("a", builder.defineInRange("a", 1, 0, 100));
            values.put("b", builder.defineInRange("b", 2, 0, 100));
            ForgeConfigSpec spec = builder.build();
            ForgeConfigBackend backend = new ForgeConfigBackend(spec, values);
            holder = ConfigHolder.create("test", "test-common", ConfigScope.COMMON, backend,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
            backend.setHolder(holder);
            file = backend.wrap(CommentedFileConfig.of(path));
            file.load();
            spec.setConfig(file);
            holder.acceptInitialExternalLoad();
        }
        @Override public void close() { file.close(); }
    }
}
