package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class QuickActionAvoidanceContractTest {
    @Test
    public void everyInteractionPathUsesTheSharedRuntimeTrayPlacement() throws Exception {
        String source = source();

        assertTrue(source.contains("private TrayGeometry resolveTrayGeometry(Screen screen)"));
        assertTrue(count(source, "resolveTrayGeometry(screen)") >= 4);
        assertTrue(source.contains("BaniraClientAccess.quickActionExclusionAreas(screen)"));
    }

    @Test
    public void trayAndContextMenuBothUseThePlacementSolver() throws Exception {
        String source = source();

        assertTrue(count(source, "QuickActionPlacement.resolve(") >= 2);
        assertTrue(source.contains("new QuickActionRect(ctxLayoutX, ctxLayoutY, ctxLayoutW, ctxLayoutH)"));
    }

    @Test
    public void pointerCaptureCoversActiveSlotsAndTheOpenMenu() throws Exception {
        String source = source();

        assertTrue(source.contains("public boolean capturesPointer(Screen screen, double mouseX, double mouseY)"));
        assertTrue(source.contains("contextOpen && new QuickActionRect"));
        assertTrue(source.contains("hitAnyActiveSlot(mouseX, mouseY"));
    }

    private static int count(String source, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickActionOverlay.java")),
                StandardCharsets.UTF_8);
    }
}
