package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaniraKeyCodesShortcutTest {
    @Test
    public void capturedShortcutMatchesRegardlessOfSeparatorSpacing() {
        assertTrue(BaniraKeyCodes.matchesShortcut("Ctrl+C", BaniraKeyCodes.KEY_C,
                BaniraKeyCodes.MOD_CONTROL));
        assertTrue(BaniraKeyCodes.matchesShortcut("Ctrl + Shift + C", BaniraKeyCodes.KEY_C,
                BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT));
        assertFalse(BaniraKeyCodes.matchesShortcut("Ctrl+C", BaniraKeyCodes.KEY_C,
                BaniraKeyCodes.MOD_CONTROL | BaniraKeyCodes.MOD_SHIFT));
    }
}
