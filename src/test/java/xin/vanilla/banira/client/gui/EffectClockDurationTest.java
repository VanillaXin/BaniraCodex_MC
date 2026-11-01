package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EffectClockDurationTest {
    private static Object call(String name, Class<?>[] types, Object... values) {
        try {
            return EffectDurationModel.class.getMethod(name, types).invoke(null, values);
        } catch (InvocationTargetException failed) {
            throw (IllegalArgumentException) failed.getCause();
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Clock duration support missing", missing);
        }
    }

    private static String clock(int ticks) {
        return (String) call("clockText", new Class<?>[]{int.class}, ticks);
    }

    private static int parse(String text, int current) {
        return (int) call("parseTime", new Class<?>[]{String.class, int.class}, text, current);
    }

    @Test
    public void displaysHoursMinutesSecondsWithExactSubsecondPrecision() {
        assertEquals("0:00:00.05", clock(1));
        assertEquals("0:00:00.95", clock(19));
        assertEquals("0:00:01", clock(20));
        assertEquals("0:00:30.05", clock(601));
        assertEquals("1:02:03", clock(74460));
        assertEquals("720:00:00", clock(EffectDurationModel.MAX_TICKS));
        assertEquals("\u221e", clock(-1));
    }

    @Test
    public void acceptsClockAndSecondsWithoutInterpretingInputAsTicks() {
        assertEquals(600, parse("0:00:30", 600));
        assertEquals(600, parse("30", 600));
        assertEquals(1201, parse("1:00.05", 600));
        assertEquals(74460, parse("1:02:03", 600));
        assertEquals(1, parse("0.05", 600));
        assertEquals(EffectDurationModel.MAX_TICKS, parse("720:00:00", 600));
    }

    @Test
    public void preservesExistingLongAndInfiniteDurationsOnlyWhenUnchanged() {
        for (int ticks : new int[]{1, 19, 601, 1234, EffectDurationModel.MAX_TICKS, Integer.MAX_VALUE, -1})
            assertEquals(ticks, parse(clock(ticks), ticks));
        assertThrows(IllegalArgumentException.class, () -> parse(clock(Integer.MAX_VALUE), 600));
        assertThrows(IllegalArgumentException.class, () -> parse("\u221e", 600));
    }

    @Test
    public void rejectsMalformedOverflowAndUnrepresentableFractions() {
        for (String text : new String[]{null, "", "0", "-1", " 30", "30 ", "1t", "1:60", "1:60:00", "1:00:60", "1::00", ":30", "1:2:3:4", "1.5:00", "0.01", "720:00:00.05", "99999999999999999999", "NaN"})
            assertThrows(String.valueOf(text), IllegalArgumentException.class, () -> parse(text, 600));
    }
}
