package xin.vanilla.banira.api.cost;

import org.junit.Test;
import xin.vanilla.banira.common.cost.CostAmounts;

import static org.junit.Assert.*;

public class CostAmountsTest {
    @Test
    public void clampsBeforeCeilingAndKeepsRawAmount() {
        CostAmount result = CostAmounts.normalize(3.01, 0, 8);
        assertEquals(4, result.amount());
        assertEquals(3.01, result.rawAmount(), 0);
        assertEquals(3, CostAmounts.normalize(.01, 3, 8).amount());
        assertEquals(8, CostAmounts.normalize(Double.MAX_VALUE, 0, 8).amount());
        assertEquals(0, CostAmounts.normalize(5, 0, 0).amount());
    }

    @Test
    public void invalidRawValuesDoNotBecomeFreeEvenWithZeroMaximum() {
        for (double raw : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1}) {
            CostAmount result = CostAmounts.normalize(raw, 0, 0);
            assertEquals(CostFailure.INVALID_RESULT, result.failure());
            try {
                result.amount();
                fail("Failed amount cannot be payable");
            } catch (IllegalStateException expected) { }
        }
    }

    @Test
    public void unlimitedMaximumStillRejectsOverflow() {
        assertEquals(Integer.MAX_VALUE, CostAmounts.normalize(Integer.MAX_VALUE, 0, -1).amount());
        assertEquals(CostFailure.AMOUNT_OVERFLOW, CostAmounts.normalize(Integer.MAX_VALUE + .01, 0, -1).failure());
        assertEquals(21, CostAmounts.normalize(20.01, 0, -1).amount());
    }

    @Test
    public void invalidLimitsAreRejected() {
        for (int[] bounds : new int[][]{{-1, 5}, {0, -2}, {5, 4}}) {
            try {
                CostAmounts.normalize(1, bounds[0], bounds[1]);
                fail("Invalid bounds");
            } catch (IllegalArgumentException expected) { }
        }
    }
}
