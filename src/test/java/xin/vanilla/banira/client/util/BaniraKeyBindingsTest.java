package xin.vanilla.banira.client.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraKeyBindingsTest {

    @Test
    public void buildsStableTranslationKeys() {
        assertEquals("key.demo.categories", BaniraKeyBindings.defaultCategory("demo"));
        assertEquals("key.demo.hold_timer", BaniraKeyBindings.descriptionId("demo", "hold_timer"));
    }

    @Test
    public void handleProvidesSafeNeutralQueries() {
        BaniraKeyHandle handle = new BaniraKeyHandle("key.demo.test", "key.demo.categories", -1, new Object());

        assertFalse(handle.isDown());
        assertFalse(handle.consumeClick());
        assertFalse(BaniraInput.isDown(handle));
        assertFalse(BaniraInput.consumeClick(handle));
        assertNull(handle.nativeBinding(String.class));
    }
}
