package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuickActionAuiHeaderTest {
    @Test public void closeHitIsHalfOpenAndCannotReachTheFirstBodyRow() {
        assertTrue(QuickActionOverlay.auiCloseHit(93, 22, 10, 20, 100));
        assertTrue(QuickActionOverlay.auiCloseHit(107.9, 37.9, 10, 20, 100));
        assertFalse(QuickActionOverlay.auiCloseHit(108, 30, 10, 20, 100));
        assertFalse(QuickActionOverlay.auiCloseHit(95, 38, 10, 20, 100));
        assertFalse(QuickActionOverlay.auiCloseHit(95, 41, 10, 20, 100));
        assertFalse(QuickActionOverlay.auiCloseHit(92.9, 30, 10, 20, 100));
    }
}
