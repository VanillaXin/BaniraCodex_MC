package xin.vanilla.banira.internal.forge.compat.jei;

import org.junit.Test;

import static org.junit.Assert.*;

public class JeiBookmarkClickTest {
    @Test
    public void forwardsToTheButtonsRuntimeInputType() throws Exception {
        LegacyButton legacy = new LegacyButton();
        ModernButton modern = new ModernButton();
        Runnable oldClick = JeiBookmarkClick.bind(legacy);
        Runnable newClick = JeiBookmarkClick.bind(modern);
        oldClick.run();
        newClick.run();
        assertEquals(1, legacy.clicks);
        assertEquals(1, modern.clicks);
        newClick.run();
        assertEquals(2, modern.clicks);
    }

    @Test
    public void rejectsUnknownInputContractsBeforeTakingOver() throws Exception {
        try {
            JeiBookmarkClick.bind(new UnsupportedButton());
            fail("Unknown input contract must not be captured");
        } catch (ReflectiveOperationException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void propagatesNativeClickFailure() throws Exception {
        Runnable click = JeiBookmarkClick.bind(new BrokenButton());
        try {
            click.run();
            fail("Native errors must remain visible");
        } catch (IllegalStateException expected) {
            assertEquals("native click failed", expected.getCause().getMessage());
        }
    }

    public enum InputType { SIMULATE, EXECUTE }

    public static class LegacyInput {
        public static LegacyInput fromVanilla(int key, int scan, int modifiers, InputType type) {
            assertEquals(0, key);
            assertEquals(0, scan);
            assertEquals(0, modifiers);
            assertEquals(InputType.EXECUTE, type);
            return new LegacyInput();
        }
    }

    public static class ModernInput {
        public static ModernInput fromVanilla(int key, int scan, int modifiers, InputType type) {
            assertEquals(InputType.EXECUTE, type);
            return new ModernInput();
        }
    }

    public static class LegacyButton {
        int clicks;
        protected boolean onMouseClicked(LegacyInput input) {
            assertNotNull(input);
            clicks++;
            return true;
        }
    }

    public static class ModernButton {
        int clicks;
        protected boolean onMouseClicked(ModernInput input) {
            assertNotNull(input);
            clicks++;
            return true;
        }
    }

    public static class UnsupportedButton {
        protected boolean onMouseClicked(String input) { return true; }
    }

    public static class BrokenButton {
        protected boolean onMouseClicked(LegacyInput input) {
            throw new IllegalArgumentException("native click failed");
        }
    }
}
