package xin.vanilla.banira.client.gui.tooltip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TooltipContentClipTest {

    @Test
    public void insetsAnimatedBoundsByEachContentPadding() {
        TooltipBounds bounds = new TooltipBounds(20, 30, 100, 40);

        TooltipBounds clip = TooltipContentClip.inset(bounds, 7, 9, 5, 6);

        assertEquals(new TooltipBounds(27, 35, 84, 29), clip);
    }

    @Test
    public void preservesAsymmetricTexturePadding() {
        TooltipBounds bounds = new TooltipBounds(10, 20, 80, 30);

        TooltipBounds clip = TooltipContentClip.inset(bounds, 6, 12, 3, 7);

        assertEquals(new TooltipBounds(16, 23, 62, 20), clip);
    }

    @Test
    public void clampsContentAreaWhenAnimationIsSmallerThanPadding() {
        TooltipBounds bounds = new TooltipBounds(20, 30, 12, 8);

        TooltipBounds clip = TooltipContentClip.inset(bounds, 8, 7, 5, 6);

        assertEquals(new TooltipBounds(28, 35, 0, 0), clip);
    }
}
