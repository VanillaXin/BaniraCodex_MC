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
        lombok.var provider = NbtAutocomplete.item("minecraft:diamond_sword");
        assertNotNull(provider);
        lombok.var result = provider.complete("{Da,display:{}}", 3).get();
        assertEquals("{Da", NBTacAPI.input);
        assertEquals("item/minecraft:diamond_sword", NBTacAPI.name);
        assertEquals("{Damage:,display:{}}", result.get(0).apply("{Da,display:{}}"));
    }

    @Test
    public void legacyPublicEntryPointKeepsItemContextAndSuffix() throws Exception {
        lombok.var provider = NbtAutocomplete.bind(legacyOnly(), "item/minecraft:diamond_sword");
        assertNotNull(provider);
        lombok.var result = provider.complete("{Da,display:{}}", 3).get();
        assertEquals("{Da", com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.input);
        assertEquals("item/minecraft:diamond_sword", com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.name);
        assertEquals("{Damage:,display:{}}", result.get(0).apply("{Da,display:{}}"));
    }

    @Test
    public void legacyFailureStillAllowsManualEditing() throws Exception {
        com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.fail = true;
        try {
            assertTrue(NbtAutocomplete.bind(legacyOnly(), "item/minecraft:stone").complete("{Da}", 3).get().isEmpty());
        } finally {
            com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.fail = false;
        }
    }

    private ClassLoader legacyOnly() {
        return new ClassLoader(getClass().getClassLoader()) {
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.mt1006.nbtac.")) throw new ClassNotFoundException(name);
                return super.loadClass(name, resolve);
            }
        };
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
