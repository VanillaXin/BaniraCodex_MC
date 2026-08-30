package xin.vanilla.banira.internal.mixin;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

/** Ensures optional integration mixins are never loaded without a guard. */
public final class OptionalCompatibilityMixinConfigTest {
    @Test
    public void guardsOptionalIntegrationMixins() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("banira_codex.mixins.json")) {
            String config = new Scanner(stream, "UTF-8").useDelimiter("\\A").next();
            assertTrue("Optional JEI and FTB Library mixins require a config plugin",
                    config.contains("\"plugin\": \"xin.vanilla.banira.internal.mixin.OptionalCompatibilityMixinPlugin\""));
        }
    }
}
