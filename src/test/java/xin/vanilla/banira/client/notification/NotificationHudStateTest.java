package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationHudMode;

import static org.junit.Assert.*;

public class NotificationHudStateTest {
    @Test
    public void defaultHudDoesNotCoverCornerNotificationRegions() {
        xin.vanilla.banira.internal.config.ClientConfig.NotificationHudCategory defaults =
                new xin.vanilla.banira.internal.config.ClientConfig.NotificationHudCategory();
        NotificationRegionLayout.Rect hud = new NotificationRegionLayout.Rect(
                NotificationHudState.position(defaults.x(), 400, 48),
                NotificationHudState.position(defaults.y(), 240, 20), 48, 20);
        for (xin.vanilla.banira.common.enums.EnumPosition pos : new xin.vanilla.banira.common.enums.EnumPosition[]{
                xin.vanilla.banira.common.enums.EnumPosition.TOP_LEFT, xin.vanilla.banira.common.enums.EnumPosition.TOP_RIGHT,
                xin.vanilla.banira.common.enums.EnumPosition.BOTTOM_LEFT, xin.vanilla.banira.common.enums.EnumPosition.BOTTOM_RIGHT}) {
            assertFalse(hud.overlaps(NotificationRegionLayout.region(pos, 400, 240, 30, 32, 6)));
        }
    }

    @Test
    public void holdRequiresUnreadAndActiveGame() {
        NotificationHudState state = new NotificationHudState();
        state.update(EnumNotificationHudMode.HOLD, true, true);
        assertTrue(state.visible(1));
        assertFalse(state.visible(0));
        state.update(EnumNotificationHudMode.HOLD, true, false);
        assertFalse(state.visible(1));
        state.update(EnumNotificationHudMode.HOLD, false, true);
        assertFalse(state.visible(1));
    }

    @Test
    public void toggleOnlyChangesOnFreshPressAndResetsOutsideGame() {
        NotificationHudState state = new NotificationHudState();
        state.update(EnumNotificationHudMode.TOGGLE, true, true);
        assertTrue(state.visible(5));
        state.update(EnumNotificationHudMode.TOGGLE, true, true);
        assertTrue(state.visible(5));
        state.update(EnumNotificationHudMode.TOGGLE, false, true);
        state.update(EnumNotificationHudMode.TOGGLE, true, true);
        assertFalse(state.visible(5));
        state.update(EnumNotificationHudMode.TOGGLE, false, false);
        assertFalse(state.visible(5));
    }

    @Test
    public void alwaysAndOffIgnoreKeysAndCountIsBounded() {
        NotificationHudState state = new NotificationHudState();
        state.update(EnumNotificationHudMode.ALWAYS, false, true);
        assertTrue(state.visible(100));
        state.update(EnumNotificationHudMode.OFF, true, true);
        assertFalse(state.visible(100));
        assertEquals("1", NotificationHudState.countLabel(1));
        assertEquals("99", NotificationHudState.countLabel(99));
        assertEquals("99+", NotificationHudState.countLabel(100));
    }

    @Test
    public void normalizedPositionClampsAndAdaptsToResize() {
        assertEquals(0, NotificationHudState.position(-1, 400, 48));
        assertEquals(352, NotificationHudState.position(1, 400, 48));
        assertEquals(176, NotificationHudState.position(.5, 400, 48));
        assertEquals(76, NotificationHudState.position(.5, 200, 48));
        assertEquals(0, NotificationHudState.position(1, 20, 48));
        assertEquals(.5, NotificationHudState.relative(176, 400, 48), .00001);
    }
}
