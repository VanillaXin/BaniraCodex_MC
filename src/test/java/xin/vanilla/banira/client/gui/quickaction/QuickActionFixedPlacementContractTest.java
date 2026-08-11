package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionFixedPlacementContractTest {
    @Test
    public void overlayUsesTheSavedAnchorWithoutAutomaticAvoidance() throws Exception {
        String source = source();

        assertFalse(source.contains("QuickActionPlacement.resolve("));
        assertFalse(source.contains("quickActionExclusionAreas("));
        assertFalse(source.contains("resolveTrayGeometry("));
        assertTrue(source.contains("QuickActionAnchorMath.offsetFromTopLeft"));
    }

    @Test
    public void pointerCaptureStillProtectsCoveredCreativeTabs() throws Exception {
        String source = source();

        assertTrue(source.contains("public boolean capturesPointer(Screen screen, double mouseX, double mouseY)"));
        assertTrue(source.contains("contextOpen && mouseX >= ctxLayoutX"));
        assertTrue(source.contains("hitAnyActiveSlot(mouseX, mouseY"));
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickActionOverlay.java")),
                StandardCharsets.UTF_8);
    }
}
