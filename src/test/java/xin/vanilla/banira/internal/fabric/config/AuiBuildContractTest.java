package xin.vanilla.banira.internal.fabric.config;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuiBuildContractTest {


    @Test
    public void fallbackContractIsExplicitCompileOnlyAndNeverARuntimeDependency() throws Exception {
        Path contract = Path.of(System.getProperty("banira.aui.contractJar"));
        assertTrue(Files.isRegularFile(contract));
        assertEquals("9f7a199d835eec069e9c26d42aa8d65d2873ed219ca168750a69bd5408b42c53",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(contract))));
        assertEquals("false", System.getProperty("banira.aui.contractOnRuntimeClasspath"));
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
