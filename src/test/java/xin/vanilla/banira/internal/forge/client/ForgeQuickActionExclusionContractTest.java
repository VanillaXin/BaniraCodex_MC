package xin.vanilla.banira.internal.forge.client;

import org.junit.Test;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRect;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ForgeQuickActionExclusionContractTest {
    @Test
    public void creativeBoundsIncludeTabsAndPageControls() {
        assertEquals(new QuickActionRect(100, 28, 195, 220),
                ForgeQuickActionExclusionProvider.containerBounds(100, 80, 195, 136, true));
        assertEquals(new QuickActionRect(100, 80, 195, 136),
                ForgeQuickActionExclusionProvider.containerBounds(100, 80, 195, 136, false));
    }

    @Test
    public void potionEffectBoundsFollowVanillaRowSpacing() {
        assertEquals(new QuickActionRect(-24, 80, 140, 32),
                ForgeQuickActionExclusionProvider.effectBounds(100, 80, 1));
        assertEquals(new QuickActionRect(-24, 80, 140, 162),
                ForgeQuickActionExclusionProvider.effectBounds(100, 80, 6));
    }

    @Test
    public void clientServiceDelegatesNativeScreenCollectionToForgeAdapter() throws Exception {
        String service = source("src/main/java/xin/vanilla/banira/internal/client/BaniraClientService.java");
        String forgeService = source("src/main/java/xin/vanilla/banira/internal/forge/client/ForgeBaniraClientService.java");
        String provider = source("src/main/java/xin/vanilla/banira/internal/forge/client/ForgeQuickActionExclusionProvider.java");

        assertTrue(service.contains("quickActionExclusionAreas(Object nativeScreen)"));
        assertTrue(forgeService.contains("ForgeQuickActionExclusionProvider.collect(nativeScreen)"));
        assertTrue(provider.contains("instanceof ContainerScreen"));
        assertTrue(provider.contains("instanceof CreativeScreen"));
        assertTrue(provider.contains("instanceof DisplayEffectsScreen"));
        assertTrue(provider.contains("instanceof Widget"));
        assertTrue(provider.contains("shouldRender"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
