package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import static org.junit.Assert.*;

public class QuickActionScreenTargetTest {
    @Test
    public void persistsExactConstructorAndArguments() {
        QuickActionScreenTarget target = new QuickActionScreenTarget("example.SettingsScreen",
                new String[]{"int", "java.lang.String", "boolean"}, new String[]{"42", "name, \"value\"", "true"});
        QuickActionScreenTarget restored = QuickActionScreenTarget.decode(target.encode());
        assertEquals(target.className, restored.className);
        assertArrayEquals(target.types, restored.types);
        assertArrayEquals(target.arguments, restored.arguments);
        assertArrayEquals(new Class<?>[]{int.class, String.class, boolean.class}, restored.parameterTypes());
        assertArrayEquals(new Object[]{42, "name, \"value\"", true}, restored.values());
    }

    @Test
    public void leavesLegacyFactoryAndClassTargetsUnchanged() {
        assertNull(QuickActionScreenTarget.decode("example:settings"));
        assertNull(QuickActionScreenTarget.decode("example.SettingsScreen"));
        assertEquals("example:settings", QuickActionScreenTarget.summary("example:settings"));
    }

    @Test
    public void supportsPrimitiveAndBoxedScalarTypesOnly() {
        String[] types = {"byte", "short", "int", "long", "float", "double", "char", "boolean",
                "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
                "java.lang.Float", "java.lang.Double", "java.lang.Character", "java.lang.Boolean", "java.lang.String"};
        for (String type : types) assertTrue(type, QuickActionScreenTarget.supported(type));
        for (String type : new String[]{"java.lang.Object", "int[]", "net.minecraft.world.entity.player.Player", "void", null})
            assertFalse(String.valueOf(type), QuickActionScreenTarget.supported(type));
        assertEquals(Character.valueOf('x'), QuickActionScreenTarget.parse("char", "x"));
        assertEquals(Boolean.FALSE, QuickActionScreenTarget.parse("java.lang.Boolean", "false"));
    }

    @Test
    public void rejectsInvalidOrOverflowingValues() {
        invalid("byte", "128");
        invalid("short", "32768");
        invalid("int", "2147483648");
        invalid("long", "9223372036854775808");
        invalid("float", "1e999");
        invalid("double", "NaN");
        invalid("boolean", "yes");
        invalid("char", "ab");
        invalid("char", "");
        invalid("int", "");
        invalid("java.lang.String", null);
    }

    @Test
    public void preservesWhitespaceAndEmptyStrings() {
        QuickActionScreenTarget target = new QuickActionScreenTarget("example.Screen",
                new String[]{"java.lang.String", "java.lang.String", "char"}, new String[]{"", "  name  ", " "});
        assertArrayEquals(new Object[]{"", "  name  ", ' '}, QuickActionScreenTarget.decode(target.encode()).values());
    }

    @Test
    public void snapshotsInputsAndSupportsNoArgumentConstruction() {
        String[] types = {"int"}, values = {"1"};
        QuickActionScreenTarget target = new QuickActionScreenTarget("example.Screen", types, values);
        types[0] = "java.lang.Object";
        values[0] = "bad";
        assertArrayEquals(new Object[]{1}, target.values());
        QuickActionScreenTarget empty = new QuickActionScreenTarget("example.Screen", new String[0], new String[0]);
        assertEquals(0, QuickActionScreenTarget.decode(empty.encode()).values().length);
    }

    @Test
    public void rejectsMalformedSavedTargets() {
        String[] bad = {"null", "{}", "{\"className\":\"example.Screen\",\"types\":[\"int\"],\"arguments\":[]}",
                "{\"className\":\"example.Screen\",\"types\":[\"java.lang.Object\"],\"arguments\":[\"x\"]}"};
        for (String json : bad) {
            try {
                QuickActionScreenTarget.decode("@banira-screen-v1:" + json);
                fail(json);
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void rejectsOversizedEncodedTargetsBeforeTheyCanBeSaved() {
        String[] types = new String[32], arguments = new String[32];
        java.util.Arrays.fill(types, "java.lang.String");
        java.util.Arrays.fill(arguments, String.join("", java.util.Collections.nCopies(1024, "a")));
        try {
            new QuickActionScreenTarget("example.Screen", types, arguments).encode();
            fail("Oversized target saved");
        } catch (IllegalArgumentException expected) {
        }
    }

    private void invalid(String type, String value) {
        try {
            QuickActionScreenTarget.parse(type, value);
            fail(type + " / " + value);
        } catch (IllegalArgumentException expected) {
        }
    }
}
