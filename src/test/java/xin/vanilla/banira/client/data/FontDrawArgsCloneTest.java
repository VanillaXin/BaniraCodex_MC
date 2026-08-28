package xin.vanilla.banira.client.data;

import org.junit.Test;
import xin.vanilla.banira.client.gui.component.Text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class FontDrawArgsCloneTest {

    @Test
    public void cloneOwnsAnIndependentTextSnapshot() {
        FontDrawArgs source = FontDrawArgs.of(Text.literal("source"));

        FontDrawArgs cloned = source.clone();
        cloned.text().text("changed");

        assertNotSame(source.text(), cloned.text());
        assertEquals("source", source.text().original().text());
        assertEquals("changed", cloned.text().original().text());
    }

    @Test
    public void cloneOwnsAnIndependentTooltipBackgroundPalette() {
        FontDrawArgs source = FontDrawArgs.of(Text.literal("source"))
                .popupTextBackgroundArgb(new int[]{0xFFF0FFF0, 0xFFE0EEE0});

        FontDrawArgs cloned = source.clone();
        cloned.popupTextBackgroundArgb()[0] = 0xFF000000;

        assertNotSame(source.popupTextBackgroundArgb(), cloned.popupTextBackgroundArgb());
        assertEquals(0xFFF0FFF0, source.popupTextBackgroundArgb()[0]);
    }
}
