package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class DevFpsTaskContractTest {
    private String buildScript() throws Exception {
        return Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
    }

    @Test
    public void configurationCannotWriteClientFpsOptions() throws Exception {
        assertFalse(Pattern.compile("(?m)^\\s*prepareBaniraDevFpsOverlay\\(\\)\\s*$")
                .matcher(buildScript()).find());
        assertTrue(buildScript().contains("doLast prepareBaniraDevFpsOverlay"));
    }

    @Test
    public void clientRunStillPreparesTheDevelopmentOverlay() throws Exception {
        assertTrue(Pattern.compile("tasks\\.named\\('runClient'\\)\\.configure\\s*\\{\\s*dependsOn\\s+'prepareDevFpsOverlay'\\s*}")
                .matcher(buildScript()).find());
    }
}
