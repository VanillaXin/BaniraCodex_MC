package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class CompactCountTest {
    @Test
    public void boundariesUseActualThousands() {
        assertEquals("0", CompactCount.format(0));
        assertEquals("1", CompactCount.format(1));
        assertEquals("999", CompactCount.format(999));
        assertEquals("1k", CompactCount.format(1000));
        assertEquals("1.25k", CompactCount.format(1250));
        assertEquals("12.3k", CompactCount.format(12345));
        assertEquals("999k", CompactCount.format(999999));
        assertEquals("1M", CompactCount.format(1000000));
    }

    @Test
    public void longValuesDoNotOverflowOrRoundUp() {
        assertEquals("1B", CompactCount.format(1000000000));
        assertEquals("1T", CompactCount.format(1000000000000L));
        assertEquals("9.22Qi", CompactCount.format(Long.MAX_VALUE));
        assertEquals("-9.22Qi", CompactCount.format(Long.MIN_VALUE));
        assertEquals("-1.99k", CompactCount.format(-1999));
    }

    @Test
    public void decimalsAndNonNumericOverlaysArePreserved() {
        assertEquals("1.5", CompactCount.format("1.500"));
        assertEquals("1.23k", CompactCount.format("1234.56"));
        assertEquals("", CompactCount.format(""));
        assertEquals("12:30", CompactCount.format("12:30"));
    }

    @Test
    public void hugeExponentsStayBounded() {
        assertEquals("1e999999999", CompactCount.format(new BigDecimal("1e999999999")));
        assertEquals("1e-999999999", CompactCount.format(new BigDecimal("1e-999999999")));
    }
}
