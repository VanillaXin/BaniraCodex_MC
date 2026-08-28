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
}
