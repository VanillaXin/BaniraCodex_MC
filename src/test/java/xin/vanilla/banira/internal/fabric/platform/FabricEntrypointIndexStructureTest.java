package xin.vanilla.banira.internal.fabric.platform;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 入口索引不得实例化无关或不兼容的第三方入口。 */
public class FabricEntrypointIndexStructureTest {

    @Test
    public void entrypointIndexFiltersProviderAndContainsIndividualFailures() throws IOException {
        String source = read("src/main/java/xin/vanilla/banira/internal/fabric/platform/FabricBaniraPlatform.java");

        assertTrue(source.contains("refreshEntrypointClassIndex(modId)"));
        assertTrue(source.contains("refreshEntrypointClassIndex(null)"));
        assertTrue(source.contains("if (targetModId != null && !targetModId.equals(providerModId))"));
        assertTrue(source.contains("catch (RuntimeException error)"));
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric platform source: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
