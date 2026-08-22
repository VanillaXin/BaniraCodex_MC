package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionMenuEditSessionTest {
    @Test
    public void targetRemainsStableWhileRowsMoveAroundIt() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.syncMenuItemOrder(Arrays.asList("entry:a", "entry:b", "entry:c"));
        QuickActionMenuEditSession session = new QuickActionMenuEditSession("entry:b");

        assertTrue(session.move(layout, -1));
        assertEquals(Arrays.asList("entry:b", "entry:a", "entry:c"), layout.menuItemOrder());
        assertEquals("entry:b", session.targetKey());
    }

    @Test
    public void visibilityActionTogglesWithoutClosingTheSession() {
        QuickActionLayout layout = new QuickActionLayout();
        QuickActionMenuEditSession session = new QuickActionMenuEditSession("entry:b");

        assertFalse(session.isHidden(layout));
        assertTrue(session.toggleVisibility(layout));
        assertTrue(session.isHidden(layout));
        assertEquals("entry:b", session.targetKey());

        assertFalse(session.toggleVisibility(layout));
        assertFalse(session.isHidden(layout));
        assertEquals("entry:b", session.targetKey());
    }
}
