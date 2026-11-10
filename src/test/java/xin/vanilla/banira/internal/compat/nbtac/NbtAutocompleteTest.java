package xin.vanilla.banira.internal.compat.nbtac;

import net.mt1006.nbtac.api.NBTacAPI;
import org.junit.Test;

import static org.junit.Assert.*;

public class NbtAutocompleteTest {
    private static final String CUSTOM_DATA = "item/minecraft:custom_data";

    @Test
    public void optionalDependencyMayBeAbsent() {
        ClassLoader absent = new ClassLoader(null) {
        };
        assertNull(NbtAutocomplete.bind(absent, "item/minecraft:diamond_sword"));
    }

    @Test
    public void callsPublicApiWithCustomDataContextAndOnlyTextBeforeCursor() throws Exception {
        NBTacAPI.fail = false;
        var provider = NbtAutocomplete.item("minecraft:diamond_sword");
        assertNotNull(provider);
        var result = provider.complete("{Da,display:{}}", 3).get();
        assertEquals("{Da", NBTacAPI.input);
        assertEquals("item/minecraft:custom_data", NBTacAPI.name);
        assertEquals("{custom_flag:,display:{}}", result.get(0).apply("{Da,display:{}}"));
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

    @Test
    public void legacyComponentParserPreservesContextAndSuffix() throws Exception {
        var map = com.mt1006.nbt_ac.autocomplete.DataComponentManager.componentMap;
        map.put(CUSTOM_DATA, new com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion("custom_flag:"));
        try {
            var provider = NbtAutocomplete.bind(legacyOnly(), CUSTOM_DATA, "minecraft:diamond_sword");
            assertNotNull(provider);
            int prepared = com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.preparations;
            var result = provider.complete("{Da,display:{}}", 3).get();
            assertEquals(prepared + 1, com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager.preparations);
            assertEquals("{Da", com.mt1006.nbt_ac.autocomplete.CustomTagParser.input);
            assertEquals("minecraft:diamond_sword", com.mt1006.nbt_ac.autocomplete.CustomTagParser.itemId);
            assertEquals(com.mt1006.nbt_ac.autocomplete.CustomTagParser.Type.COMPONENT,
                    com.mt1006.nbt_ac.autocomplete.CustomTagParser.mode);
            assertEquals("{custom_flag:,display:{}}", result.getFirst().apply("{Da,display:{}}"));
        } finally {
            map.clear();
        }
    }

    @Test
    public void componentSchemaMayArriveOrReloadAfterBinding() throws Exception {
        var map = com.mt1006.nbt_ac.autocomplete.DataComponentManager.componentMap;
        map.clear();
        var provider = NbtAutocomplete.bind(legacyOnly(), CUSTOM_DATA);
        assertNotNull(provider);
        int before = com.mt1006.nbt_ac.autocomplete.CustomTagParser.calls;
        assertTrue(provider.complete("{Da}", 3).get().isEmpty());
        assertEquals(before, com.mt1006.nbt_ac.autocomplete.CustomTagParser.calls);
        try {
            map.put(CUSTOM_DATA, new com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion("first:"));
            assertEquals("first:", provider.complete("{Da}", 3).get().getFirst().text());
            map.put(CUSTOM_DATA, new com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion("second:"));
            assertEquals("second:", provider.complete("{Da}", 3).get().getFirst().text());
            map.put(CUSTOM_DATA, null);
            assertTrue(provider.complete("{Da}", 3).get().isEmpty());
        } finally {
            map.clear();
        }
    }

    @Test
    public void legacyParserFailureAndInvalidCursorStillAllowManualEditing() throws Exception {
        var map = com.mt1006.nbt_ac.autocomplete.DataComponentManager.componentMap;
        map.put(CUSTOM_DATA, new com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion("custom_flag:"));
        com.mt1006.nbt_ac.autocomplete.CustomTagParser.fail = true;
        try {
            var provider = NbtAutocomplete.bind(legacyOnly(), CUSTOM_DATA);
            assertTrue(provider.complete("{Da}", 3).get().isEmpty());
            assertTrue(provider.complete("{Da}", -1).get().isEmpty());
            assertTrue(provider.complete("{Da}", 5).get().isEmpty());
        } finally {
            com.mt1006.nbt_ac.autocomplete.CustomTagParser.fail = false;
            map.clear();
        }
    }

    private ClassLoader legacyOnly() {
        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.mt1006.nbtac.")) throw new ClassNotFoundException(name);
                return super.loadClass(name, resolve);
            }
        };
    }

    @Test
    public void unfinishedLoaderDoesNotParsePartialComponentSchemas() throws Exception {
        var map = com.mt1006.nbt_ac.autocomplete.DataComponentManager.componentMap;
        map.put(CUSTOM_DATA, new com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion("custom_flag:"));
        com.mt1006.nbt_ac.autocomplete.loader.Loader.finished = false;
        try {
            int before = com.mt1006.nbt_ac.autocomplete.CustomTagParser.calls;
            assertTrue(NbtAutocomplete.bind(legacyOnly(), CUSTOM_DATA).complete("{Da}", 3).get().isEmpty());
            assertEquals(before, com.mt1006.nbt_ac.autocomplete.CustomTagParser.calls);
        } finally {
            com.mt1006.nbt_ac.autocomplete.loader.Loader.finished = true;
            map.clear();
        }
    }
}
