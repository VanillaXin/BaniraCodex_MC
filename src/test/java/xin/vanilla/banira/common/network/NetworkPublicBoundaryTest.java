package xin.vanilla.banira.common.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class NetworkPublicBoundaryTest {
    private static final List<Path> PUBLIC_NETWORK_FILES = Arrays.asList(
            Paths.get("src", "main", "java", "xin", "vanilla", "banira", "common", "network", "BaniraNetworkContext.java"),
            Paths.get("src", "main", "java", "xin", "vanilla", "banira", "common", "network", "BaniraPacketBuffer.java"),
            Paths.get("src", "main", "java", "xin", "vanilla", "banira", "common", "network", "NetworkPacketRegistrar.java")
    );

    private static final List<String> BANNED_IMPORT_PREFIXES = Arrays.asList(
            "import com.mojang.",
            "import net.minecraft.",
            "import net.minecraftforge.",
            "import net.neoforged."
    );

    @Test
    public void publicNetworkContractsDoNotImportMinecraftOrLoaderTypes() throws Exception {
        for (Path file : PUBLIC_NETWORK_FILES) {
            assertFileHasNoBannedImports(file);
        }
    }

    private static void assertFileHasNoBannedImports(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines) {
            for (String prefix : BANNED_IMPORT_PREFIXES) {
                assertFalse(file + " exposes banned import: " + line, line.startsWith(prefix));
            }
        }
    }
}
