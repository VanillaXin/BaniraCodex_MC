package xin.vanilla.banira.client.gui;

import org.junit.Test;

import static org.junit.Assert.*;

public class EffectDurationModelTest {
    @Test
    public void usesTicksWithoutChangingStoredDuration() {
        int[] ticks = {1, 19, 20, 21, 600};
        String[] seconds = {"0.05", "0.95", "1", "1.05", "30"};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(ticks[i], EffectDurationModel.parseTicks(Integer.toString(ticks[i])));
            assertEquals(ticks[i] + "t", EffectDurationModel.ticksText(ticks[i]));
            assertEquals(seconds[i], EffectDurationModel.nominalSeconds(ticks[i]));
        }
    }

    @Test
    public void acceptsThirtyDaysAndRejectsOverflow() {
        assertEquals(51_840_000, EffectDurationModel.parseTicks("51840000"));
        for (String input : new String[]{"51840001", "0", "-1", "2147483648", "", "1.5", " 20", "999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> EffectDurationModel.parseTicks(input));
        }
    }

    @Test
    public void preservesAnExistingLongEffectOnlyWhenUnchanged() {
        assertEquals(10, EffectDurationModel.inputDigits(Integer.MAX_VALUE));
        assertEquals(8, EffectDurationModel.inputDigits(600));
        assertEquals(Integer.MAX_VALUE, EffectDurationModel.parseTicks("2147483647", Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> EffectDurationModel.parseTicks("2147483647", 600));
    }

    @Test
    public void iconTextAlwaysFitsItsWidth() {
        assertEquals(0.75f, EffectDurationModel.scaleToFit(10, 16), 0);
        assertTrue(EffectDurationModel.scaleToFit(54, 16) * 54 <= 14.00001f);
    }

    @Test
    public void clockRoundTripsWithoutLosingTickPrecision() {
        for (int ticks : new int[]{1, 19, 20, 21, 601, 1201, EffectDurationModel.MAX_TICKS, Integer.MAX_VALUE, -1}) {
            assertEquals(ticks, EffectDurationModel.parseTime(EffectDurationModel.clockText(ticks), ticks));
        }
        assertEquals("0:00:30.05", EffectDurationModel.clockText(601));
        assertEquals(1300, EffectDurationModel.parseTime("0:01:05", 600));
    }

    @Test
    public void clockRejectsInvalidComponentsAndNonTickPrecision() {
        for (String value : new String[]{"0", "1:60", "1:00:60", "0.01", "-1", "\u221e", "720:00:00.05", "99999999999999999", " 1"}) {
            assertThrows(IllegalArgumentException.class, () -> EffectDurationModel.parseTime(value, 600));
        }
    }
}
