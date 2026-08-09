package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 验证常见宽松输入仍按真实日历严格解析。
 */
public class DateUtilsFormatTest {

    @Test
    public void parsesNonPaddedDateTimePartsAndSeparators() {
        assertParts(DateUtils.format("2026/7/1 15:12"), 2026, 7, 1, 15, 12, 0);
        assertParts(DateUtils.format("2026-7-12 15:8:05"), 2026, 7, 12, 15, 8, 5);
        assertParts(DateUtils.format("2026.7.1"), 2026, 7, 1, 0, 0, 0);
        assertParts(DateUtils.format("2026年7月1日 5时8分"), 2026, 7, 1, 5, 8, 0);
    }

    @Test
    public void rejectsInvalidDatesAndTrailingContent() {
        assertNull(DateUtils.format("2026-02-30 15:00"));
        assertNull(DateUtils.format("2026-7-1abc"));
        assertNull(DateUtils.format("2026-13-1"));
    }

    @Test
    public void keepsLegacyFormatsAndStrictExplicitPatterns() {
        assertParts(DateUtils.format("202607"), 2026, 7, 1, 0, 0, 0);
        assertParts(DateUtils.format("20260701"), 2026, 7, 1, 0, 0, 0);
        assertParts(DateUtils.format("20260701150805"), 2026, 7, 1, 15, 8, 5);
        assertParts(DateUtils.format("2026.7.1.15.8"), 2026, 7, 1, 15, 8, 0);
        assertParts(DateUtils.format("15:8"), 1970, 1, 1, 15, 8, 0);
        assertParts(DateUtils.format("2026_7_1", "yyyy_M_d"), 2026, 7, 1, 0, 0, 0);
        assertNull(DateUtils.format("2026_2_30", "yyyy_M_d"));
        assertNull(DateUtils.format("2026_7_1_tail", "yyyy_M_d"));
    }

    private static void assertParts(Date date, int year, int month, int day,
                                    int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH) + 1);
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, calendar.get(Calendar.MINUTE));
        assertEquals(second, calendar.get(Calendar.SECOND));
    }
}
