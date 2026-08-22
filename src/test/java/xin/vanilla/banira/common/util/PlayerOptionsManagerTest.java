package xin.vanilla.banira.common.util;

import org.junit.After;
import org.junit.Test;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

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
        assertEquals(2, PlayerOptionsManager.getViewDistance(uuid));
        assertEquals(ChatVisiblity.FULL, PlayerOptionsManager.getChatVisibility(uuid));
        assertTrue(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HumanoidArm.RIGHT, PlayerOptionsManager.getMainHand(uuid));
        assertFalse(PlayerOptionsManager.isTextFilteringEnabled(uuid));
        assertFalse(PlayerOptionsManager.getAllowsListing(uuid));
    }

    @Test
    public void individualUpdatesPreserveTheOtherOption() {
        UUID uuid = UUID.randomUUID();

        PlayerOptionsManager.setLanguage(uuid, "zh_cn");
        PlayerOptionsManager.setAllowsListing(uuid, true);

        assertEquals("zh_cn", PlayerOptionsManager.getLanguage(uuid));
        assertTrue(PlayerOptionsManager.getAllowsListing(uuid));
    }

    @Test
    public void combinedUpdateAndRemoveAffectBothOptions() {
        UUID uuid = UUID.randomUUID();

        assertFalse(PlayerOptionsManager.has(uuid));
        PlayerOptionsManager.set(uuid, "ja_jp", 12, ChatVisiblity.SYSTEM,
                false, HumanoidArm.LEFT, true, true);
        assertTrue(PlayerOptionsManager.has(uuid));
        assertEquals("ja_jp", PlayerOptionsManager.getLanguage(uuid));
        assertEquals(12, PlayerOptionsManager.getViewDistance(uuid));
        assertEquals(ChatVisiblity.SYSTEM, PlayerOptionsManager.getChatVisibility(uuid));
        assertFalse(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HumanoidArm.LEFT, PlayerOptionsManager.getMainHand(uuid));
        assertTrue(PlayerOptionsManager.isTextFilteringEnabled(uuid));
        assertTrue(PlayerOptionsManager.getAllowsListing(uuid));

        PlayerOptionsManager.remove(uuid);
        assertFalse(PlayerOptionsManager.has(uuid));
        assertEquals("en_us", PlayerOptionsManager.getLanguage(uuid));
        assertEquals(2, PlayerOptionsManager.getViewDistance(uuid));
        assertEquals(ChatVisiblity.FULL, PlayerOptionsManager.getChatVisibility(uuid));
        assertTrue(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HumanoidArm.RIGHT, PlayerOptionsManager.getMainHand(uuid));
        assertFalse(PlayerOptionsManager.isTextFilteringEnabled(uuid));
        assertFalse(PlayerOptionsManager.getAllowsListing(uuid));
    }
}
