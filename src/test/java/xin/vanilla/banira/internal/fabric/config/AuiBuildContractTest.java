package xin.vanilla.banira.internal.fabric.config;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AuiBuildContractTest {


    @Test
    public void fallbackContractIsExplicitCompileOnlyAndNeverARuntimeDependency() throws Exception {
        String build = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(build.contains("requires an explicit compile-only AUI contract"));
        assertTrue(build.contains("compileOnly files(configurations.auiContract)"));
        assertFalse(build.contains("modCompileOnly"));
        assertFalse(build.contains("modRuntimeOnly"));
        assertFalse(build.contains("implementation files(configurations.auiContract)"));
        assertTrue(build.contains("9F7A199D835EEC069E9C26D42AA8D65D2873ED219CA168750A69BD5408B42C53"));
    }

    @Test
    public void drawingBoundaryUsesTheMinecraft19PoseStack() throws Exception {
        var method = xin.vanilla.banira.api.client.ui.BaniraUi.class.getMethod("renderHud",
                String.class, com.mojang.blaze3d.vertex.PoseStack.class);
        assertEquals(boolean.class, method.getReturnType());
        String bridge = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics.java"));
        assertTrue(bridge.contains("AbstractGuiUtils.pushScissor"));
        assertTrue(bridge.contains("buffers().endBatch()"));
    }
}
