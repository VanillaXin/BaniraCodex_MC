package xin.vanilla.banira.client.gui.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NotificationAnimationStateTest {

    @Test
    public void dismissDuringEntryExitsFromCurrentVisibility() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(1000, 600, 5000);
        state.dismiss(1300, 600);
        assertEquals(0.5D, state.visibility(1300, 600), 0.0001D);
        assertEquals(0.25D, state.visibility(1600, 600), 0.0001D);
        assertTrue(state.visibility(1901, 600) < 0);
    }

    @Test
    public void repeatedDismissAndMergeCannotRestartExit() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(0, 600, 5000);
        state.dismiss(1000, 600);
        state.dismiss(1100, 600);
        state.merge(1200, 5000, 600);
        assertEquals(0.5D, state.visibility(1300, 600), 0.0001D);
        assertTrue(state.visibility(1601, 600) < 0);
    }

    @Test
    public void mergeDuringHoldExtendsLifetimeWithoutRestartingEntry() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(1_000L, 600L, 5_000L);

        state.merge(3_000L, 5_000L, 600L);

        assertEquals(1.0D, state.visibility(3_000L, 600L), 0.0001D);
        assertEquals(1.0D, state.visibility(7_999L, 600L), 0.0001D);
        assertTrue(state.visibility(8_300L, 600L) < 1.0D);
    }

    @Test
    public void mergeDuringEntryOnlyExtendsLifetime() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(1_000L, 600L, 5_000L);

        state.merge(1_300L, 7_000L, 600L);

        assertEquals(0.5D, state.visibility(1_300L, 600L), 0.0001D);
        assertEquals(0.0D, state.mergeEmphasis(1_390L), 0.0001D);
        assertEquals(1.0D, state.visibility(8_299L, 600L), 0.0001D);
    }

    @Test
    public void mergeUsesShortPulseInsteadOfEntryAnimation() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(0L, 600L, 5_000L);

        state.merge(2_000L, 5_000L, 600L);

        assertEquals(0.0D, state.mergeEmphasis(2_000L), 0.0001D);
        assertEquals(1.0D, state.mergeEmphasis(2_090L), 0.0001D);
        assertEquals(0.0D, state.mergeEmphasis(2_180L), 0.0001D);
    }

    @Test
    public void mergeDuringExitRestoresVisibilitySmoothly() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(0L, 600L, 1_000L);
        assertEquals(0.5D, state.visibility(1_900L, 600L), 0.0001D);

        state.merge(1_900L, 1_000L, 600L);

        assertEquals(0.5D, state.visibility(1_900L, 600L), 0.0001D);
        assertTrue(state.visibility(1_990L, 600L) > 0.5D);
        assertEquals(1.0D, state.visibility(2_080L, 600L), 0.0001D);
    }

    @Test
    public void repeatedMergeRestartsOnlyThePulse() {
        NotificationAnimationState state = new NotificationAnimationState();
        state.start(0L, 600L, 5_000L);
        state.merge(2_000L, 5_000L, 600L);
        assertEquals(1.0D, state.mergeEmphasis(2_090L), 0.0001D);

        state.merge(2_090L, 5_000L, 600L);

        assertEquals(0.0D, state.mergeEmphasis(2_090L), 0.0001D);
        assertEquals(1.0D, state.mergeEmphasis(2_180L), 0.0001D);
    }
}
