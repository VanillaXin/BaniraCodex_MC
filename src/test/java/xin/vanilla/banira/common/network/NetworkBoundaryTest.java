package xin.vanilla.banira.common.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;

public class NetworkBoundaryTest {
    @Test
    public void sourceDoesNotImportInternalStreamCodecs() throws Exception {
        Path root = Paths.get("src", "main", "java");
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(NetworkBoundaryTest::assertNoInternalStreamCodecs);
        }
    }

    private static void assertNoInternalStreamCodecs(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            assertFalse(file + " imports internal stream codecs",
                    text.contains("xin.vanilla.banira.internal.network.BaniraStreamCodecs"));
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + file, e);
        }
    }
}
