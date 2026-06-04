package xin.vanilla.banira.api.client.hud;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HudOverlayElementTest {

    @Test
    public void experienceGroupMatchesSpecificAndLegacyElements() {
        assertTrue(HudOverlayElement.EXPERIENCE.isExperience());
        assertTrue(HudOverlayElement.EXPERIENCE_BAR.isExperience());
        assertTrue(HudOverlayElement.EXPERIENCE_TEXT.isExperience());
        assertFalse(HudOverlayElement.HOTBAR.isExperience());
    }
}
