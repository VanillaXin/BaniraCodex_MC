package xin.vanilla.banira.client.gui;

import org.junit.Test;
import static org.junit.Assert.*;

public class EffectDurationModelTest {
    @Test public void usesTicksWithoutChangingStoredDuration() {
        int[] ticks = {1, 19, 20, 21, 600};
        String[] seconds = {"0.05", "0.95", "1", "1.05", "30"};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(ticks[i], EffectDurationModel.parseTicks(Integer.toString(ticks[i])));
            assertEquals(ticks[i] + "t", EffectDurationModel.ticksText(ticks[i]));
            assertEquals(seconds[i], EffectDurationModel.nominalSeconds(ticks[i]));
        }
    }
    @Test public void acceptsThirtyDaysAndRejectsOverflow() {
        assertEquals(51_840_000, EffectDurationModel.parseTicks("51840000"));
        for (String input : new String[]{"51840001", "0", "-1", "2147483648", "", "1.5", " 20", "999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> EffectDurationModel.parseTicks(input));
        }
    }
    @Test public void preservesAnExistingLongEffectOnlyWhenUnchanged() {
        assertEquals(10, EffectDurationModel.inputDigits(Integer.MAX_VALUE));
        assertEquals(8, EffectDurationModel.inputDigits(600));
        assertEquals(Integer.MAX_VALUE, EffectDurationModel.parseTicks("2147483647", Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> EffectDurationModel.parseTicks("2147483647", 600));
    }
    @Test public void iconTextAlwaysFitsItsWidth() {
        assertEquals(0.75f, EffectDurationModel.scaleToFit(10, 16), 0);
        assertTrue(EffectDurationModel.scaleToFit(54, 16) * 54 <= 14.00001f);
    }
}
