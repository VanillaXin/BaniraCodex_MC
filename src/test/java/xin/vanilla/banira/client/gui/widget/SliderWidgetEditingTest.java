package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SliderWidgetEditingTest {

    @Test
    public void incompleteTextDoesNotOverwriteSliderValue() {
        assertFalse(SliderWidget.shouldApplyInlineText(""));
        assertFalse(SliderWidget.shouldApplyInlineText("-"));
        assertFalse(SliderWidget.shouldApplyInlineText("."));
        assertFalse(SliderWidget.shouldApplyInlineText("-."));
    }

    @Test
    public void completeDecimalTextCanUpdateSliderValue() {
        assertTrue(SliderWidget.shouldApplyInlineText("0.002"));
        assertTrue(SliderWidget.shouldApplyInlineText("-12.5"));
        assertTrue(SliderWidget.shouldApplyInlineText("0."));
    }
}
