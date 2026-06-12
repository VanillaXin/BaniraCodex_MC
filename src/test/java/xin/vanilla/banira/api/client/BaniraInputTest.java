package xin.vanilla.banira.api.client;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraKeyCodes;

import static org.junit.Assert.assertEquals;

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

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyModId() {
        BaniraInput.defaultCategory("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptySuffix() {
        BaniraInput.descriptionId("example", "");
    }
}
