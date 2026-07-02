package xin.vanilla.banira.api;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * 公共 API 不能直接暴露 MC / loader 类型；版本差异必须留在 platform adapter 内部。
 */
public class PublicApiBoundaryTest {
    private static final List<Path> PUBLIC_ROOTS = Arrays.asList(
            Paths.get("src", "main", "java", "xin", "vanilla", "banira", "api"),
            Paths.get("src", "main", "java", "xin", "vanilla", "banira", "platform")
    );

    private static final List<String> BANNED_IMPORT_PREFIXES = Arrays.asList(
            "import com.mojang.",
            "import net.minecraft.",
            "import net.minecraftforge.",
            "import net.neoforged."
    );

    @Test
    public void publicApiDoesNotImportLoaderOrMinecraftTypes() throws Exception {
        for (Path root : PUBLIC_ROOTS) {
            assertRootHasNoBannedImports(root);
        }
    }

    @Test
    public void networkServiceDoesNotExposeLegacyIdentifierFactory() throws Exception {
        Path networkService = Paths.get("src", "main", "java", "xin", "vanilla", "banira", "platform", "BaniraNetworkService.java");
        String source = Files.readString(networkService, StandardCharsets.UTF_8);
        assertFalse("Network service registration must use api.BaniraIdentifier.", source.contains("common.util.IIdentifier"));
        assertFalse("Network service registration must not expose IIdentifier.", source.contains(" IIdentifier "));
    }

    private static void assertRootHasNoBannedImports(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(PublicApiBoundaryTest::assertFileHasNoBannedImports);
        }
    }

    private static void assertFileHasNoBannedImports(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                for (String prefix : BANNED_IMPORT_PREFIXES) {
                    assertFalse(file + " exposes banned import: " + line, line.startsWith(prefix));
                }
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + file, e);
        }
    }
}
