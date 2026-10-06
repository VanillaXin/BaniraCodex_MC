package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class NeoForgeConfigFileTest {
    private static final String DOCUMENT = "[base.chunk]\nlimit = 100\nretain = 0.25\n"
            + "items = [\"minecraft:arrow\", \"tick, clazz -> tick >= 5\"]\n"
            + "[other]\nenabled = false\n";

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void invalidReloadDoesNotChangeTheLiveConfiguration() throws Exception {
        Path path = file();
        try (CommentedFileConfig config = open(path)) {
            config.load();
            write(path, "[base.chunk]\nlimit = 8\n[base.chunk]\nretain = 0.5\n");
            assertThrows(ParsingException.class, config::load);
            assertComplete(config);
            assertEquals(100, ((Number) config.get("base.chunk.limit")).intValue());
        }
    }

    @Test
    public void saveDoesNotOverwriteAnInvalidExternalEdit() throws Exception {
        Path path = file();
        try (CommentedFileConfig config = open(path)) {
            config.load();
            String invalid = "[base.chunk]\nlimit = [\n";
            write(path, invalid);
            assertThrows(ParsingException.class, config::save);
            assertEquals(invalid, read(path));
            assertComplete(config);
        }
    }

    @Test
    public void saveAcceptsACompleteExternalEditInsteadOfWritingTheOldSnapshot() throws Exception {
        Path path = file();
        try (CommentedFileConfig config = open(path)) {
            config.load();
            String external = DOCUMENT.replace("limit = 100", "limit = 321");
            write(path, external);
            config.save();
            assertEquals(external, read(path));
            assertEquals(321, ((Number) config.get("base.chunk.limit")).intValue());
            assertComplete(config);
        }
    }

    @Test
    public void returnedListsCannotMutateLiveOrSavedValues() throws Exception {
        Path path = file();
        try (CommentedFileConfig config = open(path)) {
            config.load();
            List<String> items = config.get("base.chunk.items");
            items.clear();
            assertComplete(config);
            config.save();
            config.load();
            assertComplete(config);
        }
    }

    @Test
    public void autosavePreservesOtherCategoriesAndCommaExpressions() throws Exception {
        Path path = file();
        try (CommentedFileConfig config = open(path)) {
            config.load();
            config.set("base.chunk.limit", 8);
            assertComplete(config);
            try (CommentedFileConfig reloaded = CommentedFileConfig.of(path)) {
                reloaded.load();
                assertComplete(reloaded);
                assertEquals(8, ((Number) reloaded.get("base.chunk.limit")).intValue());
            }
        }
    }

    @Test
    public void externalChangeDuringStagedWriteAbortsWithoutPublishingOrOverwriting() throws Exception {
        Path path = file();
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        ConfigWriter writer = (candidate, output) -> {
            writing.countDown();
            await(resume);
            TomlFormat.instance().createWriter().write(candidate, output);
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (NeoForgeConfigFile config = new NeoForgeConfigFile(CommentedFileConfig.of(path),
                new net.neoforged.neoforge.common.ModConfigSpec.Builder().build(), candidate -> {
        }, writer)) {
            config.load();
            Future<?> change = worker.submit(() -> config.set("base.chunk.limit", 8));
            try {
                await(writing);
                write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            } finally {
                resume.countDown();
            }
            ExecutionException failure = assertThrows(ExecutionException.class, () -> change.get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof WritingException);
            assertEquals(DOCUMENT.replace("limit = 100", "limit = 321"), read(path));
            assertEquals(100, ((Number) config.get("base.chunk.limit")).intValue());
            config.load();
            assertEquals(321, ((Number) config.get("base.chunk.limit")).intValue());
            assertComplete(config);
            try (java.util.stream.Stream<Path> files = Files.list(path.getParent())) {
                assertEquals(1, files.count());
            }
        } finally {
            resume.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void invalidValueDoesNotPublishOrWriteAnything() throws Exception {
        Path path = file();
        try (NeoForgeConfigFile config = new NeoForgeConfigFile(CommentedFileConfig.of(path),
                new net.neoforged.neoforge.common.ModConfigSpec.Builder().build(), candidate -> {
            if (((Number) candidate.get("base.chunk.limit")).intValue() < 0) {
                throw new ParsingException("Invalid limit");
            }
        })) {
            config.load();
            assertThrows(ParsingException.class, () -> config.set("base.chunk.limit", -1));
            assertEquals(DOCUMENT, read(path));
            assertEquals(100, ((Number) config.get("base.chunk.limit")).intValue());
            write(path, DOCUMENT.replace("limit = 100", "limit = -1"));
            assertThrows(ParsingException.class, config::load);
            assertEquals(100, ((Number) config.get("base.chunk.limit")).intValue());
        }
    }

    @Test
    public void saveDeliversExternalChangesBeforeHolderCapturesItsNewBaseline() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        try (CommentedFileConfig config = fixture.file) {
            List<Set<String>> changes = new ArrayList<>();
            fixture.holder.onReloaded(paths -> {
                assertFalse("Subscribers must not execute under the file monitor", Thread.holdsLock(config));
                changes.add(paths);
            });
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            fixture.holder.save();
            assertEquals(Collections.singleton("base.chunk.limit"), changes.get(0));
            fixture.holder.acceptExternalReload();
            assertEquals(1, changes.size());
        }
    }

    @Test
    public void editReportsPriorExternalChangesSeparatelyFromTheLocalSave() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        try (CommentedFileConfig config = fixture.file) {
            List<Set<String>> reloads = new ArrayList<>();
            List<Set<String>> saves = new ArrayList<>();
            fixture.holder.onReloaded(reloads::add);
            fixture.holder.onSaved(saves::add);
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            fixture.holder.set("other.enabled", true);
            fixture.holder.save();
            assertEquals(Collections.singletonList(Collections.singleton("base.chunk.limit")), reloads);
            assertEquals(Collections.singletonList(Collections.singleton("other.enabled")), saves);
        }
    }

    @Test
    public void saveReportsFirstExternalRevisionAfterTheSameBackendWrapsAnotherFile() throws Exception {
        assertFirstExternalRevisionAfterRebind(false);
    }

    @Test
    public void editReportsFirstExternalRevisionAfterTheSameBackendWrapsAnotherFile() throws Exception {
        assertFirstExternalRevisionAfterRebind(true);
    }

    private void assertFirstExternalRevisionAfterRebind(boolean editBeforeSave) throws Exception {
        Path firstPath = file();
        Fixture fixture = new Fixture(firstPath);
        List<Set<String>> reloads = new ArrayList<>();
        List<Set<String>> saves = new ArrayList<>();
        fixture.holder.onReloaded(reloads::add);
        fixture.holder.onSaved(saves::add);
        try (CommentedFileConfig first = fixture.file) {
            write(firstPath, DOCUMENT.replace("limit = 100", "limit = 321"));
            fixture.holder.save();
            assertEquals(Collections.singletonList(Collections.singleton("base.chunk.limit")), reloads);
        }

        Path secondPath = temporary.newFile("second.toml").toPath();
        write(secondPath, DOCUMENT);
        try (NeoForgeConfigFile second = fixture.rebind(secondPath)) {
            reloads.clear();
            saves.clear();
            write(secondPath, DOCUMENT.replace("limit = 100", "limit = 222"));
            if (editBeforeSave) fixture.holder.set("other.enabled", true);
            fixture.holder.save();
            assertEquals(222, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
            assertEquals(Collections.singletonList(Collections.singleton("base.chunk.limit")), reloads);
            if (editBeforeSave) {
                assertEquals(Collections.singletonList(Collections.singleton("other.enabled")), saves);
            } else {
                assertTrue(saves.isEmpty());
            }
            second.load();
            fixture.holder.acceptExternalReload();
            fixture.holder.save();
            assertEquals("The later watcher delivery must not duplicate the reload", 1, reloads.size());
            assertEquals(DOCUMENT.replace("limit = 100", "limit = 321"), read(firstPath));
        }
    }

    @Test
    public void detachedClosedFileDoesNotReceiveEditsOrSavesForSyncedMemoryConfig() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        try (NeoForgeConfigFile config = fixture.file) {
            fixture.backend.detach(config);
            config.close();
            CommentedConfig synced = fixture.useSyncedMemoryConfig();
            byte[] before = Files.readAllBytes(path);

            fixture.holder.set("base.chunk.limit", 654);
            fixture.holder.save();

            assertEquals(654, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
            assertEquals(654, ((Number) synced.get("base.chunk.limit")).intValue());
            assertEquals(Boolean.TRUE, fixture.holder.get("other.enabled"));
            assertArrayEquals(before, Files.readAllBytes(path));
            assertThrows(IllegalStateException.class, config::load);
        }
    }

    @Test
    public void rebindAfterDetachRestoresStrictFileSaves() throws Exception {
        Path firstPath = file();
        Fixture fixture = new Fixture(firstPath);
        try (NeoForgeConfigFile first = fixture.file) {
            fixture.backend.detach(first);
            first.close();
            fixture.useSyncedMemoryConfig();
            fixture.holder.set("base.chunk.limit", 654);
            fixture.holder.save();
            byte[] firstBytes = Files.readAllBytes(firstPath);

            Path secondPath = temporary.newFile("rebound.toml").toPath();
            write(secondPath, DOCUMENT);
            try (NeoForgeConfigFile second = fixture.rebind(secondPath)) {
                assertEquals(100, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
                assertEquals(Boolean.FALSE, fixture.holder.get("other.enabled"));
                fixture.holder.set("base.chunk.limit", 432);
                fixture.holder.save();
                try (CommentedFileConfig disk = CommentedFileConfig.of(secondPath)) {
                    disk.load();
                    assertEquals(432, ((Number) disk.get("base.chunk.limit")).intValue());
                }

                String invalid = "[base.chunk]\nlimit = [\n";
                write(secondPath, invalid);
                assertThrows(ParsingException.class, fixture.holder::save);
                assertEquals(invalid, read(secondPath));
                assertEquals(432, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
                assertArrayEquals(firstBytes, Files.readAllBytes(firstPath));
            }
        }
    }

    @Test
    public void invalidExternalFileDoesNotPreventUnloadCleanup() throws Exception {
        Path path = file();
        try (NeoForgeConfigFile config = (NeoForgeConfigFile) open(path)) {
            config.load();
            write(path, "broken = [\n");
            config.saveOnUnload();
            assertEquals("broken = [\n", read(path));
            config.close();
            assertThrows(IllegalStateException.class, config::load);
        }
    }

    @Test
    public void syntacticallyValidButIncompleteReloadDoesNotPublishDefaults() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        try (CommentedFileConfig config = fixture.file) {
            write(path, "[base.chunk]\nlimit = 8\n");
            assertThrows(ParsingException.class, config::load);
            assertEquals(100, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
            assertEquals(Boolean.FALSE, fixture.holder.get("other.enabled"));
        }
    }

    @Test
    public void initialDefaultsAreWrittenImmediatelyAndCloseDoesNotRestoreBootstrapData() throws Exception {
        Path path = temporary.newFile("initial.toml").toPath();
        Fixture fixture = new Fixture(path);
        try (CommentedFileConfig config = fixture.file) {
            assertTrue(read(path).contains("limit = 100"));
            fixture.holder.set("base.chunk.limit", 321);
            fixture.holder.save();
        }
        assertTrue(read(path).contains("limit = 321"));
        assertTrue(read(path).contains("enabled = false"));
    }

    @Test
    public void localWriteBackToPreviousValueNotifiesSaveAfterPendingExternalReload() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        java.util.List<Integer> reloads = new java.util.ArrayList<>();
        java.util.List<Integer> saves = new java.util.ArrayList<>();
        try (CommentedFileConfig config = fixture.file) {
            fixture.holder.onReloaded(paths -> reloads.add(fixture.holder.get("base.chunk.limit")));
            fixture.holder.onSaved(paths -> saves.add(fixture.holder.get("base.chunk.limit")));
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            fixture.holder.set("base.chunk.limit", 100);
            fixture.holder.save();
            assertEquals(Collections.singletonList(321), reloads);
            assertEquals(Collections.singletonList(100), saves);
            assertTrue(read(path).contains("limit = 100"));
        }
    }

    @Test
    public void acceptingPendingExternalValueDoesNotNotifyALocalSave() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        java.util.List<java.util.Set<String>> saves = new java.util.ArrayList<>();
        try (CommentedFileConfig config = fixture.file) {
            fixture.holder.onSaved(saves::add);
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            fixture.holder.set("base.chunk.limit", 321);
            fixture.holder.save();
            assertTrue(saves.isEmpty());
            assertEquals(321, ((Number) fixture.holder.get("base.chunk.limit")).intValue());
        }
    }

    @Test
    public void delayedExternalEventDoesNotDiscardLocalSave() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        List<String> events = new ArrayList<>();
        try (CommentedFileConfig config = fixture.file) {
            fixture.holder.onReloaded(paths -> events.add("reload:" + fixture.holder.get("base.chunk.limit")));
            fixture.holder.onSaved(paths -> events.add("save:" + fixture.holder.get("base.chunk.limit")));
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            config.load();
            fixture.holder.set("base.chunk.limit", 100);
            fixture.backend.acceptReload();
            fixture.holder.save();

            assertEquals(Arrays.asList("reload:321", "save:100"), events);
            assertTrue(read(path).contains("limit = 100"));
        }
    }

    @Test
    public void repeatedExternalEventDoesNotDiscardAnUnrelatedPendingEdit() throws Exception {
        Path path = file();
        Fixture fixture = new Fixture(path);
        List<Set<String>> saves = new ArrayList<>();
        List<Integer> reloads = new ArrayList<>();
        try (CommentedFileConfig config = fixture.file) {
            fixture.holder.onReloaded(paths -> reloads.add(fixture.holder.get("base.chunk.limit")));
            fixture.holder.onSaved(saves::add);
            write(path, DOCUMENT.replace("limit = 100", "limit = 321"));
            config.load();
            fixture.backend.acceptReload();
            fixture.holder.set("other.enabled", true);
            fixture.backend.acceptReload();
            fixture.holder.save();

            assertEquals(Collections.singletonList(321), reloads);
            assertEquals(Collections.singletonList(Collections.singleton("other.enabled")), saves);
        }
    }

    private static final class Fixture {
        final NeoForgeConfigFile file;
        final ConfigHolder holder;
        final ModConfigSpec spec;
        final NeoForgeConfigValueStore backend;

        Fixture(Path path) {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            Map<String, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
            values.put("base.chunk.limit", builder.defineInRange("base.chunk.limit", 100, 0, 1000));
            values.put("other.enabled", builder.define("other.enabled", false));
            spec = builder.build();
            backend = new NeoForgeConfigValueStore(spec, values);
            file = (NeoForgeConfigFile) backend.wrap(CommentedFileConfig.of(path));
            file.load();
            LoadedConfigFixture.accept(spec, file);
            holder = ConfigHolder.create("test", "test", ConfigScope.COMMON, backend,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
            backend.setHolder(holder);
        }

        NeoForgeConfigFile rebind(Path path) {
            NeoForgeConfigFile next = (NeoForgeConfigFile) backend.wrap(CommentedFileConfig.of(path));
            try {
                next.load();
                LoadedConfigFixture.accept(spec, next);
                holder.acceptInitialExternalLoad();
                return next;
            } catch (RuntimeException | Error failure) {
                next.close();
                throw failure;
            }
        }

        CommentedConfig useSyncedMemoryConfig() {
            CommentedConfig synced = CommentedConfig.inMemory();
            synced.set("base.chunk.limit", 777);
            synced.set("other.enabled", true);
            LoadedConfigFixture.accept(spec, synced);
            holder.acceptExternalReload();
            return synced;
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private CommentedFileConfig open(Path path) {
        return new NeoForgeConfigFile(CommentedFileConfig.builder(path).sync().autosave().build(),
                new net.neoforged.neoforge.common.ModConfigSpec.Builder().build(), candidate -> {
        });
    }

    private Path file() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        write(path, DOCUMENT);
        return path;
    }

    private static void assertComplete(CommentedFileConfig config) {
        assertEquals(0.25, ((Number) config.get("base.chunk.retain")).doubleValue(), 0);
        assertEquals(Boolean.FALSE, config.get("other.enabled"));
        assertEquals(Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5"), config.get("base.chunk.items"));
    }

    private static void write(Path path, String text) throws Exception {
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
