package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;

import static org.junit.Assert.assertEquals;

public class ConfigEditorNumberPolicyTest {

    @Test
    public void doubleStepUsesDeclaredPrecisionEvenForLargeRanges() {
        assertEquals(0.0001D, ConfigEditorNumberPolicy.stepFor(
                ConfigEntryDescriptor.ConfigValueType.DOUBLE, 0D, 9999D, 4), 0D);
    }

    @Test
    public void discreteValuesKeepUnitStep() {
        assertEquals(1D, ConfigEditorNumberPolicy.stepFor(
                ConfigEntryDescriptor.ConfigValueType.INTEGER, 0D, Integer.MAX_VALUE, 0), 0D);
    }
}
