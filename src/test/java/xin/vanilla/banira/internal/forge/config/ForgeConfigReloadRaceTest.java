package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.utils.CommentedConfigWrapper;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;

import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Upstream failure characterization, not an assertion that production reloads are safe.
 */
public class ForgeConfigReloadRaceTest {
    private static final String DOCUMENT = "[base.chunk]\nlimit = 100\ninterval = 10\nretain = 0.25\n"
            + "entityList = [\"minecraft:arrow\", \"tick, clazz -> tick >= 5\"]\n"
            + "[vault]\nenabled = false\n[unrelated]\ntext = \"keep, this\"\n";

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void forgeAutosaveCanPersistAPartialTreeAfterReloadFails() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) reproduce(false);
    }

    @Test
    public void holderMonitorDoesNotProtectTheForgeWatcher() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) reproduce(true);
    }

    @Test
    public void sequentialReloadAndWritePreserveOtherCategoriesAndCommaValues() throws Exception {
        Path path = temporary.newFile("sequential.toml").toPath();
        Files.write(path, DOCUMENT.getBytes(StandardCharsets.UTF_8));
        try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            ForgeConfigBackend backend = backend(file);
            file.load();
            backend.set("base.chunk.limit", 8);
            assertComplete(parse(path));
            assertEquals(8, ((Number) parse(path).get("base.chunk.limit")).intValue());
        }
    }

    @Test
    public void malformedExternalReloadAlsoLeavesAPartialTreeThatCanBeSaved() throws Exception {
        Path path = temporary.newFile("invalid.toml").toPath();
        Files.write(path, DOCUMENT.getBytes(StandardCharsets.UTF_8));
        try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            backend(file);
            byte[] invalid = "[base.chunk]\nlimit = 8\n[base.chunk]\nretain = 0.5\n"
                    .getBytes(StandardCharsets.UTF_8);
            Files.write(path, invalid);
            assertThrows(ParsingException.class, file::load);
            assertArrayEquals("Loading alone does not repair the invalid file", invalid, Files.readAllBytes(path));
            assertFalse("Failed parsing has already removed unrelated live values", file.contains("vault.enabled"));
            file.save();
            assertEquals(8, ((Number) parse(path).get("base.chunk.limit")).intValue());
            assertFalse("A later save publishes the partial parse", parse(path).contains("unrelated.text"));
        }
    }

    @Test
    public void inFlightSaveCanSkipExternalReloadAndOverwriteTheNewerFile() throws Exception {
        Path path = temporary.newFile("save-race.toml").toPath();
        Files.write(path, DOCUMENT.getBytes(StandardCharsets.UTF_8));
        PausingFormat format = new PausingFormat();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (CommentedFileConfig file = CommentedFileConfig.builder(path, format)
                .sync().autosave().writingMode(WritingMode.REPLACE).build()) {
            file.load();
            backend(file);
            format.pauseWrite = true;
            Future<?> save = executor.submit(file::save);
            try {
                await(format.writing);
                Files.write(path, DOCUMENT.replace("limit = 100", "limit = 321")
                        .getBytes(StandardCharsets.UTF_8));
                assertComplete(parse(path));
                assertEquals(321, ((Number) parse(path).get("base.chunk.limit")).intValue());
                // This is the same load() called by Forge's watcher while currentlyWriting is true.
                file.load();
                assertEquals(100, ((Number) file.get("base.chunk.limit")).intValue());
            } finally {
                format.continueWrite.countDown();
                save.get(5, TimeUnit.SECONDS);
            }
            assertComplete(parse(path));
            assertEquals("The valid external edit was overwritten by the older save", 100,
                    ((Number) parse(path).get("base.chunk.limit")).intValue());
        } finally {
            format.continueWrite.countDown();
            executor.shutdownNow();
            assertTrue("Writer must finish", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void reproduce(boolean throughHolder) throws Exception {
        Path path = temporary.newFile().toPath();
        Files.write(path, DOCUMENT.getBytes(StandardCharsets.UTF_8));
        PausingFormat format = new PausingFormat();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (CommentedFileConfig file = CommentedFileConfig.builder(path, format)
                .sync().preserveInsertionOrder().autosave().writingMode(WritingMode.REPLACE).build()) {
            file.load();
            ForgeConfigBackend backend = backend(file);
            ConfigHolder holder = ConfigHolder.create("race", "race", ConfigScope.COMMON, backend,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
            assertComplete(parse(path));
            format.armed = true;
            Future<?> reload = executor.submit(file::load);
            await(format.cleared);
            Future<?> setter = executor.submit(() -> {
                if (throughHolder) holder.set("base.chunk.limit", 8);
                else backend.set("base.chunk.limit", 8);
            });
            try {
                ExecutionException failure = assertThrows(ExecutionException.class,
                        () -> reload.get(5, TimeUnit.SECONDS));
                assertTrue(failure.getCause() instanceof ParsingException);
                assertEquals("Table with path [base, chunk] has been declared twice.",
                        failure.getCause().getMessage());
                setter.get(5, TimeUnit.SECONDS);
                CommentedConfig saved = parse(path);
                assertEquals(8, ((Number) saved.get("base.chunk.limit")).intValue());
                assertFalse(saved.contains("base.chunk.interval"));
                assertFalse(saved.contains("base.chunk.retain"));
                assertFalse(saved.contains("base.chunk.entityList"));
                assertFalse(saved.contains("vault.enabled"));
                assertFalse(saved.contains("unrelated.text"));
                assertEquals(Collections.singleton("base"), saved.valueMap().keySet());
                System.out.println("Reproduced duplicate table and partial autosave; holder=" + throughHolder);
            } finally {
                format.mutated.countDown();
            }
        } finally {
            format.mutated.countDown();
            executor.shutdownNow();
            assertTrue("Workers must finish", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static CommentedConfig parse(Path path) throws Exception {
        return new TomlParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static void assertComplete(CommentedConfig config) {
        assertEquals("keep, this", config.get("unrelated.text"));
        assertEquals(10, ((Number) config.get("base.chunk.interval")).intValue());
        assertEquals(0.25, ((Number) config.get("base.chunk.retain")).doubleValue(), 0);
        assertEquals(Boolean.FALSE, config.get("vault.enabled"));
        assertEquals(Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5"),
                config.get("base.chunk.entityList"));
    }

    private static ForgeConfigBackend backend(CommentedFileConfig file) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        values.put("base.chunk.limit", builder.defineInRange("base.chunk.limit", 100, 0, 10000));
        values.put("base.chunk.interval", builder.defineInRange("base.chunk.interval", 10, 0, 10000));
        values.put("base.chunk.retain", builder.defineInRange("base.chunk.retain", 0.25, 0, 1));
        values.put("base.chunk.entityList", builder.defineList("base.chunk.entityList",
                Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5"), value -> value instanceof String));
        values.put("vault.enabled", builder.define("vault.enabled", false));
        values.put("unrelated.text", builder.define("unrelated.text", "keep, this"));
        ForgeConfigSpec spec = builder.build();
        spec.setConfig(file);
        return new ForgeConfigBackend(spec, values);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue("Controlled interleaving timed out", latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class PausingFormat implements ConfigFormat<CommentedConfig> {
        final CountDownLatch cleared = new CountDownLatch(1);
        final CountDownLatch mutated = new CountDownLatch(1);
        final CountDownLatch writing = new CountDownLatch(1);
        final CountDownLatch continueWrite = new CountDownLatch(1);
        volatile boolean armed;
        volatile boolean pauseWrite;

        @Override
        public ConfigWriter createWriter() {
            ConfigWriter writer = TomlFormat.instance().createWriter();
            return new ConfigWriter() {
                @Override
                public void write(UnmodifiableConfig config, Writer output) {
                    writer.write(config, output);
                }

                @Override
                public void write(UnmodifiableConfig config, Path path, WritingMode mode, Charset charset) {
                    if (pauseWrite) {
                        writing.countDown();
                        await(continueWrite);
                    }
                    writer.write(config, path, mode, charset);
                }
            };
        }

        @Override
        public ConfigParser<CommentedConfig> createParser() {
            return new TomlParser();
        }

        @Override
        public CommentedConfig createConfig(Supplier<Map<String, Object>> mapCreator) {
            return new CommentedConfigWrapper<CommentedConfig>(TomlFormat.instance().createConfig(mapCreator)) {
                @Override
                public void clear() {
                    super.clear();
                    if (armed) {
                        cleared.countDown();
                        await(mutated);
                    }
                }

                @Override
                public <T> T set(List<String> path, Object value) {
                    T old = super.set(path, value);
                    if (armed && path.equals(Arrays.asList("base", "chunk", "limit"))) {
                        mutated.countDown();
                    }
                    return old;
                }
            };
        }

        @Override
        public boolean supportsComments() {
            return true;
        }
    }
}
