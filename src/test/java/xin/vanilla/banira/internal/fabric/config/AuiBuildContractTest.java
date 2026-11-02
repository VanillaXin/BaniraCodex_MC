package xin.vanilla.banira.internal.fabric.config;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AuiBuildContractTest {
    @Test
    public void developmentOptionsAreOnlyWrittenByTheClientPreparationTask() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertFalse(build.matches("(?s).*\\n\\s*prepareBaniraDevFpsOverlay\\(\\).*"));
        assertTrue(build.contains("tasks.named('runClient').configure { dependsOn 'prepareDevFpsOverlay' }"));
        assertTrue(build.contains("doLast prepareBaniraDevFpsOverlay"));
    }

    @Test
    public void publicationUsesTheFormalRemappedSources() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("artifact tasks.named('remapSourcesJar')"));
        assertFalse(build.contains("artifact tasks.named('sourcesJar')"));
        assertTrue(build.contains("dependsOn 'remapJar', 'remapSourcesJar'"));
    }

    @Test
    public void fallbackContractIsExplicitCompileOnlyAndNeverARuntimeDependency() throws Exception {
        String build = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(build.contains("requires an explicit compile-only AUI contract"));
        assertTrue(build.contains("compileOnly files(configurations.auiContract)"));
        assertFalse(build.contains("modCompileOnly"));
        assertFalse(build.contains("modRuntimeOnly"));
        assertFalse(build.contains("implementation files(configurations.auiContract)"));
        assertTrue(build.contains("D557E54540C71D87920056D1F36A05480E4FEF0E0063A1D61D18DC8E2BC81D8B"));
    }

    @Test
    public void drawingBoundaryUsesTheMinecraft18PoseStack() throws Exception {
        var method = xin.vanilla.banira.api.client.ui.BaniraUi.class.getMethod("renderHud",
                String.class, com.mojang.blaze3d.vertex.PoseStack.class);
        assertEquals(boolean.class, method.getReturnType());
        String bridge = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics.java"));
        assertTrue(bridge.contains("AbstractGuiUtils.pushScissor"));
        assertTrue(bridge.contains("buffers().endBatch()"));
    }
}
