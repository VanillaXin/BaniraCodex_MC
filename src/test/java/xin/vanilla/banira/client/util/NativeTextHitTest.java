package xin.vanilla.banira.client.util;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NativeTextHitTest {
    private static final Style FIRST = Style.EMPTY.withColor(0x123456);
    private static final Style SECOND = Style.EMPTY.withBold(true);
    private static final StringSplitter SPLITTER = new StringSplitter(
            (codepoint, style) -> codepoint == 'i' ? 1.25f : codepoint == 0x200B ? 0 : 3.5f);

    @Test
    public void visualGlyphBoundariesPreserveFractionalAdvanceAndStyles() {
        FormattedCharSequence text = FormattedCharSequence.composite(
                FormattedCharSequence.forward("ii", FIRST), FormattedCharSequence.forward("X", SECOND));
        assertEquals(FIRST, NativeTextHit.styleAt(SPLITTER, text, 2.49f));
        assertEquals(SECOND, NativeTextHit.styleAt(SPLITTER, text, 2.5f));
        assertEquals(SECOND, NativeTextHit.styleAt(SPLITTER, text, 5.99f));
        assertNull(NativeTextHit.styleAt(SPLITTER, text, 6));
    }

    @Test
    public void supplementaryGlyphAndZeroWidthGlyphDoNotShiftHitRegions() {
        var text = FormattedCharSequence.composite(FormattedCharSequence.forward("\u200B", FIRST),
                FormattedCharSequence.forward("\uD83D\uDE00", SECOND));
        assertEquals(SECOND, NativeTextHit.styleAt(SPLITTER, text, 0));
        assertEquals(SECOND, NativeTextHit.styleAt(SPLITTER, text, 3.49f));
        assertNull(NativeTextHit.styleAt(SPLITTER, text, 3.5f));
        assertNull(NativeTextHit.styleAt(SPLITTER, text, -1));
        assertNull(NativeTextHit.styleAt(SPLITTER, text, Float.NaN));
    }
}
