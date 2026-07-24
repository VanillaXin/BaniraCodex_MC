package xin.vanilla.banira;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Minecraft 1.18.2 的内置资源包格式固定为 8。 */
public class ResourcePackFormatContractTest {
    @Test
    public void resourcePackUsesMinecraft118Format() throws IOException {
        String metadata = new String(
                Files.readAllBytes(Paths.get("src/main/resources/pack.mcmeta")),
                StandardCharsets.UTF_8);
        assertTrue(metadata.contains("\"pack_format\": 8"));
    }
}
