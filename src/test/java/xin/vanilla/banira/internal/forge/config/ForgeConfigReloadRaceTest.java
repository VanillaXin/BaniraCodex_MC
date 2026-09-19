package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Upstream failure characterization, not an assertion that managed reloads are safe. */
public class ForgeConfigReloadRaceTest {
    private static final String DOCUMENT = "[base.chunk]\nlimit = 100\ninterval = 10\nretain = 0.25\n"
            + "entityList = [\"minecraft:arrow\", \"tick, clazz -> tick >= 5\"]\n"
            + "[vault]\nenabled = false\n[unrelated]\ntext = \"keep, this\"\n";

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void sequentialReloadAndWritePreserveOtherCategoriesAndCommaValues() throws Exception {
        Path path = file("sequential.toml");
        try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            ForgeConfigValueStore backend = backend(file);
            file.load();
            backend.set("base.chunk.limit", 8);
            assertComplete(parse(path));
            assertEquals(8, ((Number) parse(path).get("base.chunk.limit")).intValue());
        }
    }

    @Test
    public void malformedExternalReloadAlsoLeavesAPartialTreeThatCanBeSaved() throws Exception {
        Path path = file("invalid.toml");
        try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            backend(file);
            byte[] invalid = "[base.chunk]\nlimit = 8\n[base.chunk]\nretain = 0.5\n"
                    .getBytes(StandardCharsets.UTF_8);
            Files.write(path, invalid);
            assertThrows(ParsingException.class, file::load);
            assertArrayEquals(invalid, Files.readAllBytes(path));
            assertFalse("Failed parsing removed unrelated live values", file.contains("vault.enabled"));
            file.save();
            assertEquals(8, ((Number) parse(path).get("base.chunk.limit")).intValue());
            assertFalse("A later save publishes the partial parse", parse(path).contains("unrelated.text"));
        }
    }

    @Test
    public void saveBeforeTheWatcherRunsOverwritesAValidExternalEdit() throws Exception {
        Path path = file("delayed-watcher.toml");
        try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            backend(file);
            Files.writeString(path, DOCUMENT.replace("limit = 100", "limit = 321"), StandardCharsets.UTF_8);
            assertEquals(321, ((Number) parse(path).get("base.chunk.limit")).intValue());
            // NightConfig's new bulk lock does not serialize an external editor with this save.
            file.save();
            assertComplete(parse(path));
            assertEquals(100, ((Number) parse(path).get("base.chunk.limit")).intValue());
        }
    }

    private Path file(String name) throws Exception {
        Path path = temporary.newFile(name).toPath();
        Files.writeString(path, DOCUMENT, StandardCharsets.UTF_8);
        return path;
    }

    private static CommentedConfig parse(Path path) throws Exception {
        return new TomlParser().parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void assertComplete(CommentedConfig config) {
        assertEquals("keep, this", config.get("unrelated.text"));
        assertEquals(10, ((Number) config.get("base.chunk.interval")).intValue());
        assertEquals(0.25, ((Number) config.get("base.chunk.retain")).doubleValue(), 0);
        assertEquals(Boolean.FALSE, config.get("vault.enabled"));
        assertEquals(Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5"),
                config.get("base.chunk.entityList"));
    }

    private static ForgeConfigValueStore backend(CommentedFileConfig file) {
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
        return new ForgeConfigValueStore(spec, values);
    }
}
