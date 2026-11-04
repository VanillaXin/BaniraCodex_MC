package xin.vanilla.banira.internal.fabric.config;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AuiBuildContractTest {
    @Test
    public void developmentOptionsAreOnlyWrittenByTheClientPreparationTask() throws Exception {
        String build = new String(Files.readAllBytes(java.nio.file.Paths.get("build.gradle")), java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(build.matches("(?s).*\\n\\s*prepareBaniraDevFpsOverlay\\(\\).*"));
        assertTrue(build.contains("tasks.named('runClient').configure { dependsOn 'prepareDevFpsOverlay' }"));
        assertTrue(build.contains("doLast prepareBaniraDevFpsOverlay"));
    }

    @Test
    public void publicationUsesTheFormalRemappedSources() throws Exception {
        String build = new String(Files.readAllBytes(java.nio.file.Paths.get("build.gradle")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(build.contains("artifact tasks.named('remapSourcesJar')"));
        assertFalse(build.contains("artifact tasks.named('sourcesJar')"));
        assertTrue(build.contains("dependsOn 'remapJar', 'remapSourcesJar'"));
    }

    @Test
    public void fallbackContractIsExplicitCompileOnlyAndNeverARuntimeDependency() throws Exception {
        String build = new String(Files.readAllBytes(java.nio.file.Paths.get("gradle/aui-contract.gradle")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(build.contains("compileOnly=true"));
        assertTrue(build.contains("configurations.compileOnly.extendsFrom(configurations.auiContract)"));
        assertTrue(build.contains("Opcodes.V1_8"));
        assertFalse(build.contains("modCompileOnly"));
        assertFalse(build.contains("modRuntimeOnly"));
        assertFalse(build.contains("implementation files(configurations.auiContract)"));
        assertTrue(build.contains("03C89DCACBFF77F176F46B91457541FD10AE29343BD29DF1F1B7199BF6C3A111"));
    }

    @Test
    public void drawingBoundaryUsesTheMinecraft18PoseStack() throws Exception {
        lombok.var method = xin.vanilla.banira.api.client.ui.BaniraUi.class.getMethod("renderHud",
                String.class, com.mojang.blaze3d.vertex.PoseStack.class);
        assertEquals(boolean.class, method.getReturnType());
        String bridge = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(bridge.contains("AbstractGuiUtils.pushScissor"));
        assertTrue(bridge.contains("buffers().endBatch()"));
    }
}
