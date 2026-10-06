package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

public class FabricConfigTransactionTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private FabricConfigValueStore store(Path file) {
        return new FabricConfigValueStore(file, Arrays.asList(
                ConfigEntryDescriptor.builder().path("count").valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER)
                        .defaultValue(1).minValue(0).maxValue(10).build(),
                ConfigEntryDescriptor.builder().path("names").valueType(ConfigEntryDescriptor.ConfigValueType.STRING_LIST)
                        .defaultValue(Arrays.asList("a", "b,c")).build()));
    }

    @Test
    public void batchPersistsAllValuesTogether() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        FabricConfigValueStore store = store(file);
        Map<String, Object> edits = new LinkedHashMap<>();
        edits.put("count", 7);
        edits.put("names", Arrays.asList("a,b", "c"));
        store.setAll(edits);
        FabricConfigValueStore reloaded = store(file);
        assertEquals(7, reloaded.get("count"));
        assertEquals(Arrays.asList("a,b", "c"), reloaded.get("names"));
    }

    @Test
    public void invalidBatchLeavesDiskAndMemoryUntouched() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        FabricConfigValueStore store = store(file);
        byte[] before = Files.readAllBytes(file);
        Map<String, Object> edits = new LinkedHashMap<>();
        edits.put("names", Collections.singletonList("changed"));
        edits.put("count", 11);
        try {
            store.setAll(edits);
            fail("invalid batch");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(Arrays.asList("a", "b,c"), store.get("names"));
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    public void externalFileEditConflictsWithoutBeingOverwritten() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        FabricConfigValueStore store = store(file);
        ConfigEditSnapshot snapshot = store.snapshotForEdit(Collections.singleton("count"));
        byte[] external = "count = 4\nnames = [\"new\"]\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, external);
        assertEquals(ConfigCommitResult.CONFLICT, store.compareAndSetAll(snapshot, Collections.singletonMap("count", 9)));
        assertArrayEquals(external, Files.readAllBytes(file));
    }

    @Test
    public void staleMemoryAndForeignSnapshotsAreRejected() throws Exception {
        FabricConfigValueStore store = store(temporary.getRoot().toPath().resolve("config.toml"));
        ConfigEditSnapshot snapshot = store.snapshotForEdit(Collections.singleton("count"));
        store.set("count", 3);
        assertEquals(ConfigCommitResult.CONFLICT, store.compareAndSetAll(snapshot, Collections.singletonMap("count", 9)));
        FabricConfigValueStore other = store(temporary.getRoot().toPath().resolve("other.toml"));
        try {
            other.compareAndSetAll(snapshot, Collections.singletonMap("count", 9));
            fail("foreign snapshot");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void readSnapshotsAndReturnedListsCannotMutateStoredValues() throws Exception {
        FabricConfigValueStore store = store(temporary.getRoot().toPath().resolve("config.toml"));
        ConfigReadSnapshot snapshot = store.snapshotForRead(new LinkedHashSet<>(store.paths()));
        ((List<?>) store.get("names")).clear();
        assertEquals(Arrays.asList("a", "b,c"), store.get("names"));
        try {
            ((List<?>) snapshot.getValues().get("names")).clear();
            fail("mutable snapshot");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void preparedMatchIncludesUnsavedAndPersistedEdits() throws Exception {
        FabricConfigValueStore store = store(temporary.getRoot().toPath().resolve("config.toml"));
        BooleanSupplier match = store.prepareStoredMatch(Collections.singletonMap("count", 1), false);
        assertTrue(match.getAsBoolean());
        store.set("count", 2);
        assertFalse(match.getAsBoolean());
    }

    @Test
    public void delayedSaveNeverOverwritesExternalEdits() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        FabricConfigValueStore store = store(file);
        byte[] external = "count = 4\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, external);
        try {
            store.save();
        } catch (IllegalStateException expected) {
        }
        assertArrayEquals(external, Files.readAllBytes(file));
    }

    @Test
    public void malformedReloadDoesNotPublishPartOfTheFile() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        FabricConfigValueStore store = store(file);
        Files.write(file, "count = 4\nnames = [not_a_string]\n".getBytes(StandardCharsets.UTF_8));
        try {
            store.reload();
            fail("invalid reload");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(1, store.get("count"));
        assertEquals(Arrays.asList("a", "b,c"), store.get("names"));
    }

    @Test
    public void unchangedCommitDoesNotRewriteFileComments() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.toml");
        byte[] original = "# custom comment\ncount = 1\nnames = [\"a\", \"b,c\"]\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);
        FabricConfigValueStore store = store(file);
        assertEquals(ConfigCommitResult.UNCHANGED, store.compareAndSetAll(
                store.snapshotForEdit(Collections.singleton("count")), Collections.singletonMap("count", 1)));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void editCannotEscapeCapturedPaths() throws Exception {
        FabricConfigValueStore store = store(temporary.getRoot().toPath().resolve("config.toml"));
        ConfigEditSnapshot snapshot = store.snapshotForEdit(Collections.singleton("count"));
        try {
            store.compareAndSetAll(snapshot, Collections.singletonMap("names", Collections.singletonList("changed")));
            fail("uncaptured edit");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(Arrays.asList("a", "b,c"), store.get("names"));
    }

    @Test
    public void registeredStoreHotReloadsAndNotifiesItsHolder() throws Exception {
        Path directory = temporary.newFolder().toPath();
        xin.vanilla.banira.platform.BaniraPlatforms.install(
                new xin.vanilla.banira.platform.TestBaniraPlatform().configDir(directory));
        FabricConfigAdapter.register(FabricConfigAdapterTest.NestedConfig.class, "test_mod");
        ConfigHolder holder = FabricConfigAdapter.getHolder(FabricConfigAdapterTest.NestedConfig.class);
        List<Set<String>> notifications = new ArrayList<>();
        Runnable unsubscribe = holder.onReloaded(notifications::add);
        try {
            Files.write(directory.resolve("fabric-adapter-test.toml"), "[section]\ncount = 4\n".getBytes(StandardCharsets.UTF_8));
            java.lang.reflect.Method poll = xin.vanilla.banira.internal.config.ManagedConfigFiles.class
                    .getDeclaredMethod("pollNowForTests", xin.vanilla.banira.internal.config.ManagedConfigFiles.Scope.class);
            poll.setAccessible(true);
            poll.invoke(null, xin.vanilla.banira.internal.config.ManagedConfigFiles.Scope.COMMON);
            assertEquals(4, ((Number) holder.get("section.count")).intValue());
            assertEquals(Collections.singletonList(Collections.singleton("section.count")), notifications);
            holder.save();
            assertTrue(new String(Files.readAllBytes(directory.resolve("fabric-adapter-test.toml")), StandardCharsets.UTF_8).contains("count = 4"));
        } finally {
            unsubscribe.run();
        }
    }
}
