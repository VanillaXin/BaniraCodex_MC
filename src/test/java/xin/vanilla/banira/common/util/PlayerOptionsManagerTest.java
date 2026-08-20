package xin.vanilla.banira.common.util;

import org.junit.After;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class PlayerOptionsManagerTest {
    @After
    public void clearOptions() {
        PlayerOptionsManager.clear();
    }

    @Test
    public void defaultsMatchVanillaClientOptions() {
        UUID uuid = UUID.randomUUID();

        assertEquals("en_us", PlayerOptionsManager.getLanguage(uuid));
        assertTrue(PlayerOptionsManager.getAllowsListing(uuid));
    }

    @Test
    public void individualUpdatesPreserveTheOtherOption() {
        UUID uuid = UUID.randomUUID();

        PlayerOptionsManager.setLanguage(uuid, "zh_cn");
        PlayerOptionsManager.setAllowsListing(uuid, false);

        assertEquals("zh_cn", PlayerOptionsManager.getLanguage(uuid));
        assertFalse(PlayerOptionsManager.getAllowsListing(uuid));
    }

    @Test
    public void combinedUpdateAndRemoveAffectBothOptions() {
        UUID uuid = UUID.randomUUID();

        PlayerOptionsManager.set(uuid, "ja_jp", false);
        assertEquals("ja_jp", PlayerOptionsManager.getLanguage(uuid));
        assertFalse(PlayerOptionsManager.getAllowsListing(uuid));

        PlayerOptionsManager.remove(uuid);
        assertEquals("en_us", PlayerOptionsManager.getLanguage(uuid));
        assertTrue(PlayerOptionsManager.getAllowsListing(uuid));
    }
}
