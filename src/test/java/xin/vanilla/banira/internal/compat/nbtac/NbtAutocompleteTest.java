package xin.vanilla.banira.internal.compat.nbtac;

import net.mt1006.nbtac.api.NBTacAPI;
import org.junit.Test;

import static org.junit.Assert.*;

public class NbtAutocompleteTest {
    @Test
    public void optionalDependencyMayBeAbsent() {
        ClassLoader absent = new ClassLoader(null) {
        };
        assertNull(NbtAutocomplete.bind(absent, "item/minecraft:diamond_sword"));
    }

    @Test
    public void callsPublicApiWithItemContextAndOnlyTextBeforeCursor() throws Exception {
        NBTacAPI.fail = false;
        var provider = NbtAutocomplete.item("minecraft:diamond_sword");
        assertNotNull(provider);
        var result = provider.complete("{Da,display:{}}", 3).get();
        assertEquals("{Da", NBTacAPI.input);
        assertEquals("item/minecraft:diamond_sword", NBTacAPI.name);
        assertEquals("{Damage:,display:{}}", result.get(0).apply("{Da,display:{}}"));
    }

    @Test
    public void optionalApiFailureDoesNotBreakEditing() throws Exception {
        NBTacAPI.fail = true;
        try {
            assertTrue(NbtAutocomplete.item("minecraft:stone").complete("{Da}", 3).get().isEmpty());
        } finally {
            NBTacAPI.fail = false;
        }
    }
}
