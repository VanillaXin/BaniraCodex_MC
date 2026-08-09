package xin.vanilla.banira.api.client;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraKeyCodes;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BaniraInputTest {

    @Test
    public void createsStableTranslationKeys() {
        assertEquals("key.example.categories", BaniraInput.defaultCategory("example"));
        assertEquals("key.example.open_panel", BaniraInput.descriptionId("example", "open_panel"));
    }

    @Test
    public void specUsesUnknownKeyByDefault() {
        BaniraKeySpec spec = BaniraInput.spec("example", "open_panel");

        assertEquals("example", spec.modId());
        assertEquals("open_panel", spec.suffix());
        assertEquals(BaniraKeyCodes.KEY_UNKNOWN, spec.defaultKey());
    }

    @Test
    public void inputStateQueriesUsePlatformService() {
        BaniraPlatforms.install(new TestBaniraPlatform());

        assertFalse(BaniraInput.isKeyDown(BaniraKeyCodes.KEY_K));
        assertFalse(BaniraInput.isMouseDown(BaniraKeyCodes.MOUSE_LEFT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyModId() {
        BaniraInput.defaultCategory("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptySuffix() {
        BaniraInput.descriptionId("example", "");
    }
}
