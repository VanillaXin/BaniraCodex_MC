package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraKeyCodesTest {

    @Test
    public void formatsShortcutWithControlKeysFirst() {
        int modifiers = BaniraKeyCodes.MOD_ALT | BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT;

        assertEquals("Ctrl + Shift + Alt + K", BaniraKeyCodes.formatShortcut(BaniraKeyCodes.KEY_K, modifiers));
    }

    @Test
    public void matchesRequiredModifiersAndAllowsLockBits() {
        int actual = BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT | BaniraKeyCodes.MOD_CAPS_LOCK;

        assertTrue(BaniraKeyCodes.matchesModifiers(actual, BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT));
        assertFalse(BaniraKeyCodes.matchesModifiers(actual, BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_ALT));
        assertTrue(BaniraKeyCodes.matchesExactModifiers(actual, BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT));
        assertFalse(BaniraKeyCodes.matchesExactModifiers(actual, BaniraKeyCodes.MOD_CONTROL));
    }

    @Test
    public void formatsKnownNavigationKeys() {
        assertEquals("Ctrl + Delete", BaniraKeyCodes.formatShortcut(BaniraKeyCodes.KEY_DELETE, BaniraKeyCodes.MOD_CONTROL));
        assertEquals("Unknown", BaniraKeyCodes.formatShortcut(BaniraKeyCodes.KEY_UNKNOWN, 0));
    }
}
