package xin.vanilla.banira.client.gui.tooltip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TooltipTransitionModelTest {

    private static final long MS = 1_000_000L;

    @Test
    public void firstTooltipAbovePointerExpandsFromItsBottomEdge() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(140 * MS, 40 * MS, 0.35D);
        TooltipBounds target = new TooltipBounds(20, 30, 80, 24);

        TooltipTransitionFrame<String> start = model.resolve("a", target, 60, 70, 100 * MS);
        TooltipTransitionFrame<String> middle = model.resolve("a", target, 60, 70, 170 * MS);

        assertEquals(new TooltipBounds(20, 54, 80, 0), start.bounds());
        assertEquals("a", start.contentKey());
        assertEquals(0.0D, start.progress(), 0.0001D);
        assertEquals(target.x(), middle.bounds().x(), 0.0001D);
        assertEquals(target.width(), middle.bounds().width(), 0.0001D);
        assertTrue(middle.bounds().y() > target.y() && middle.bounds().y() < 54);
        assertTrue(middle.bounds().height() > 0 && middle.bounds().height() < target.height());
    }

    @Test
    public void firstTooltipBelowPointerExpandsFromItsTopEdge() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds target = new TooltipBounds(20, 30, 80, 24);

        TooltipTransitionFrame<String> start = model.resolve("below", target, 60, 18, 0L);
        TooltipBounds middle = model.resolve("below", target, 60, 18, 50 * MS).bounds();

        assertEquals(new TooltipBounds(20, 30, 80, 0), start.bounds());
        assertEquals(target.x(), middle.x(), 0.0001D);
        assertEquals(target.y(), middle.y(), 0.0001D);
        assertEquals(target.width(), middle.width(), 0.0001D);
        assertTrue(middle.height() > 0 && middle.height() < target.height());
    }

    @Test
    public void switchingTooltipInterpolatesPositionAndSize() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(140 * MS, 40 * MS, 0.35D);
        model.resolve("a", new TooltipBounds(20, 20, 80, 20), 0L);
        model.resolve("b", new TooltipBounds(180, 120, 160, 60), 10 * MS);

        TooltipTransitionFrame<String> frame = model.resolve(
                "b", new TooltipBounds(180, 120, 160, 60), 80 * MS);

        assertTrue(frame.bounds().x() > 20 && frame.bounds().x() < 180);
        assertTrue(frame.bounds().y() > 20 && frame.bounds().y() < 120);
        assertTrue(frame.bounds().width() > 80 && frame.bounds().width() < 160);
        assertTrue(frame.bounds().height() > 20 && frame.bounds().height() < 60);
    }

    @Test
    public void contentChangesAfterThirtyFivePercentWithoutScalingText() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(0, 0, 100, 40);
        model.resolve("a", a, 0L);
        model.resolve("b", b, 10 * MS);

        assertEquals("a", model.resolve("b", b, 40 * MS).contentKey());
        assertEquals("b", model.resolve("b", b, 50 * MS).contentKey());
    }

    @Test
    public void movingTargetSizeDoesNotKeepOldContentVisible() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        model.resolve("a", a, 0L);
        model.resolve("b", new TooltipBounds(0, 0, 80, 24), 10 * MS);

        assertEquals("a", model.resolve("b", new TooltipBounds(0, 0, 82, 24), 30 * MS).contentKey());
        assertEquals("b", model.resolve("b", new TooltipBounds(0, 0, 84, 24), 50 * MS).contentKey());
    }

    @Test
    public void retargetingStartsFromCurrentAnimatedBounds() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        TooltipBounds c = new TooltipBounds(30, 140, 60, 80);
        model.resolve("a", a, 0L);
        model.resolve("b", b, 10 * MS);
        TooltipTransitionFrame<String> before = model.resolve("b", b, 50 * MS);

        TooltipTransitionFrame<String> retargeted = model.resolve("c", c, 50 * MS);

        assertEquals(before.bounds(), retargeted.bounds());
    }

    @Test
    public void movingAnchorDoesNotRestartAnActiveTransition() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 0L);
        model.resolve("b", b, 10 * MS);
        TooltipTransitionFrame<String> beforeMove = model.resolve("b", b, 50 * MS);

        TooltipTransitionFrame<String> afterMove = model.resolve(
                "b", new TooltipBounds(112, 57, 100, 50), 50 * MS);

        assertEquals(beforeMove.bounds().x() + 12, afterMove.bounds().x(), 0.0001D);
        assertEquals(beforeMove.bounds().y() + 7, afterMove.bounds().y(), 0.0001D);
        assertEquals(beforeMove.progress(), afterMove.progress(), 0.0001D);
    }

    @Test
    public void briefMissingFrameKeepsContinuityButLongGapResets() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(100 * MS, 40 * MS, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 0L);
        model.markMissing(10 * MS);
        TooltipTransitionFrame<String> brief = model.resolve("b", b, 30 * MS);
        assertEquals(a, brief.bounds());

        model.markMissing(80 * MS);
        TooltipTransitionFrame<String> reset = model.resolve("a", a, 130 * MS);
        assertEquals(a, reset.bounds());
        assertEquals(1.0D, reset.progress(), 0.0001D);
    }

    @Test
    public void nearbyTooltipKeepsVisibleContinuityAcrossControlGap() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 10, 10, 0L);

        TooltipTransitionFrame<String> gap = model.resolveMissing(20, 10, 100 * MS);
        assertNotNull(gap);
        assertEquals("a", gap.contentKey());
        TooltipTransitionFrame<String> shrunken = model.resolveMissing(30, 10, 190 * MS);

        TooltipTransitionFrame<String> switched = model.resolve("b", b, 30, 10, 190 * MS);
        assertEquals(shrunken.bounds(), switched.bounds());
        assertEquals("a", switched.contentKey());
    }

    @Test
    public void tooltipGapExpiresAfterGracePeriod() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 10, 10, 0L);
        assertNotNull(model.resolveMissing(12, 10, 10 * MS));

        assertNull(model.resolveMissing(12, 10, 211 * MS));
        TooltipTransitionFrame<String> fresh = model.resolve("b", b, 12, 10, 220 * MS);
        assertEquals(new TooltipBounds(100, 50, 100, 0), fresh.bounds());
        assertEquals("b", fresh.contentKey());
        assertEquals(0.0D, fresh.progress(), 0.0001D);
    }

    @Test
    public void movingTooFarAcrossEmptySpaceCancelsContinuity() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 10, 10, 0L);

        assertNull(model.resolveMissing(70, 10, 40 * MS));
        TooltipTransitionFrame<String> fresh = model.resolve("b", b, 70, 10, 50 * MS);
        assertEquals(new TooltipBounds(100, 50, 100, 0), fresh.bounds());
        assertEquals(0.0D, fresh.progress(), 0.0001D);
    }

    @Test
    public void gapDistanceStartsFromTheMostRecentTooltipPointer() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds a = new TooltipBounds(0, 0, 40, 20);
        TooltipBounds b = new TooltipBounds(100, 50, 100, 50);
        model.resolve("a", a, 10, 10, 0L);
        model.resolve("b", b, 50, 10, 10 * MS);

        assertNotNull(model.resolveMissing(100, 10, 20 * MS));
    }

    @Test
    public void missingTooltipShrinksTowardThePointerFacingVerticalEdge() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds original = new TooltipBounds(20, 30, 100, 40);
        model.resolve("a", original, 70, 80, 0L);
        model.resolve("a", original, 70, 80, 100 * MS);

        TooltipTransitionFrame<String> start = model.resolveMissing(72, 78, 110 * MS);
        TooltipTransitionFrame<String> middle = model.resolveMissing(72, 78, 160 * MS);

        assertEquals(original, start.bounds());
        assertEquals(original.x(), middle.bounds().x(), 0.0001D);
        assertEquals(original.width(), middle.bounds().width(), 0.0001D);
        assertTrue(middle.bounds().height() > 0 && middle.bounds().height() < original.height());
        assertTrue(middle.bounds().y() > original.y() && middle.bounds().y() < 70);
        TooltipTransitionFrame<String> end = model.resolveMissing(72, 78, 210 * MS);
        assertEquals(new TooltipBounds(20, 70, 100, 0), end.bounds());
    }

    @Test
    public void tooltipAfterGapContinuesFromCurrentShrunkenBounds() {
        TooltipTransitionModel<String> model = new TooltipTransitionModel<>(
                100 * MS, 200 * MS, 56.0D, 0.35D);
        TooltipBounds a = new TooltipBounds(20, 30, 100, 40);
        TooltipBounds b = new TooltipBounds(160, 80, 140, 60);
        model.resolve("a", a, 10, 10, 0L);
        model.resolveMissing(12, 10, 10 * MS);
        TooltipTransitionFrame<String> shrunken = model.resolveMissing(12, 10, 60 * MS);

        TooltipTransitionFrame<String> switched = model.resolve("b", b, 14, 10, 60 * MS);

        assertEquals(shrunken.bounds(), switched.bounds());
        assertEquals("a", switched.contentKey());
    }
}
