package xin.vanilla.banira.internal.forge.client;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class CreativeQuickActionInputContractTest {
    @Test
    public void forgePreEventsConsumeBaniraOwnedMouseInputFirst() throws Exception {
        String bridge = source("src/main/java/xin/vanilla/banira/internal/forge/client/BaniraClientForgeEventHandler.java");

        assertTrue(bridge.contains("@SubscribeEvent(priority = EventPriority.HIGHEST)"));
        assertTrue(bridge.contains("BaniraClientEventHub.dispatchMouseClickedPre"));
        assertTrue(bridge.contains("BaniraClientEventHub.dispatchMouseReleasedPre"));
        assertTrue(bridge.contains("event.setCanceled(mouseEvent.canceled())"));
    }

    @Test
    public void creativeTabsDoNotHoverOrClickThroughTheBaniraOverlay() throws Exception {
        String mixin = source("src/main/java/xin/vanilla/banira/internal/mixin/injections/CreativeScreenQuickActionMixin.java");
        String config = source("src/main/resources/banira_codex.mixins.json");

        assertTrue(mixin.contains("@Mixin(CreativeModeInventoryScreen.class)"));
        assertTrue(mixin.contains("method = \"checkTabHovering\""));
        assertTrue(mixin.contains("method = \"mouseClicked\""));
        assertTrue(mixin.contains("method = \"mouseReleased\""));
        assertTrue(mixin.contains("QuickActionOverlay.get().capturesPointer"));
        assertTrue(config.contains("injections.CreativeScreenQuickActionMixin"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
