package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ConfigHolderSavedListenerTest {

    @Test
    public void notifiesChangedPathsOnlyAfterBoundConfigSave() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/common/config/ConfigHolder.java")), StandardCharsets.UTF_8);

        assertTrue(source.contains("if (modConfig == null)"));
        assertTrue(source.contains("modConfig.save();"));
        assertTrue(source.contains("pendingChangedPaths.add(path)"));
        assertTrue(source.contains("listener.accept(changedPaths)"));
    }
}
