package xin.vanilla.banira.common.util;

import org.junit.After;
import org.junit.Test;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.util.HandSide;

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
        assertEquals(ChatVisibility.FULL, PlayerOptionsManager.getChatVisibility(uuid));
        assertTrue(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HandSide.RIGHT, PlayerOptionsManager.getMainHand(uuid));
    }

    @Test
    public void individualUpdatesPreserveTheOtherOption() {
        UUID uuid = UUID.randomUUID();

        PlayerOptionsManager.setLanguage(uuid, "zh_cn");
        assertEquals("zh_cn", PlayerOptionsManager.getLanguage(uuid));
    }

    @Test
    public void combinedUpdateAndRemoveAffectBothOptions() {
        UUID uuid = UUID.randomUUID();

        PlayerOptionsManager.set(uuid, "ja_jp", ChatVisibility.SYSTEM,
                false, HandSide.LEFT);
        assertEquals("ja_jp", PlayerOptionsManager.getLanguage(uuid));
        assertEquals(ChatVisibility.SYSTEM, PlayerOptionsManager.getChatVisibility(uuid));
        assertFalse(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HandSide.LEFT, PlayerOptionsManager.getMainHand(uuid));

        PlayerOptionsManager.remove(uuid);
        assertEquals("en_us", PlayerOptionsManager.getLanguage(uuid));
        assertEquals(ChatVisibility.FULL, PlayerOptionsManager.getChatVisibility(uuid));
        assertTrue(PlayerOptionsManager.getChatColors(uuid));
        assertEquals(HandSide.RIGHT, PlayerOptionsManager.getMainHand(uuid));
    }
}
