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
        assertEquals("03c89dcacbff77f176f46b91457541fd10ae29343bd29df1f1b7199bf6c3a111",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(contract))));
        assertEquals("false", System.getProperty("banira.aui.contractOnRuntimeClasspath"));
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
