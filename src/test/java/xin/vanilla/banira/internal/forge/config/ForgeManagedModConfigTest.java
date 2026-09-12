package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ForgeManagedModConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void missingFileUsesDefaultConfigWithoutChangingIt() throws Exception {
        Path defaults = temporary.newFile("defaults.toml").toPath();
        String original = "# template\nitems = [\"a,b\", \"c\"]\n";
        Files.write(defaults, original.getBytes(StandardCharsets.UTF_8));
        Path target = temporary.getRoot().toPath().resolve("nested/config.toml");
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target, defaults)) {
            file.load();
            assertEquals(java.util.Arrays.asList("a,b", "c"), file.get("items"));
            assertEquals(original, new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        }
        assertEquals(original, new String(Files.readAllBytes(defaults), StandardCharsets.UTF_8));
    }

    @Test
    public void existingConfigWinsOverDefaultTemplate() throws Exception {
        Path target = temporary.newFile("existing.toml").toPath();
        Path defaults = temporary.newFile("template.toml").toPath();
        Files.write(target, "value = 3\n".getBytes(StandardCharsets.UTF_8));
        Files.write(defaults, "value = 5\n".getBytes(StandardCharsets.UTF_8));
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target, defaults)) {
            file.load();
            assertEquals(3, ((Number) file.get("value")).intValue());
            file.set("value", 9);
        }
        assertEquals("value = 3\n", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void missingTemplateCreatesAnEmptyFileForTransactionalInitialization() {
        Path target = temporary.getRoot().toPath().resolve("new/config.toml");
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target,
                temporary.getRoot().toPath().resolve("absent.toml"))) {
            file.load();
            assertTrue(Files.isRegularFile(target));
            assertTrue(file.isEmpty());
        }
    }
}
