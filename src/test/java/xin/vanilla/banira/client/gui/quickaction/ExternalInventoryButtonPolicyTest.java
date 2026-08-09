package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import static org.junit.Assert.assertEquals;

public class ExternalInventoryButtonPolicyTest {

    @Test
    public void originalModeNeverAdoptsExternalButtons() {
        assertEquals(EnumExternalInventoryButtonHost.ORIGINAL,
                ExternalInventoryButtonPolicy.resolve(
                        EnumExternalInventoryButtonHost.ORIGINAL, true));
    }

    @Test
    public void baniraModeUsesBaniraHost() {
        assertEquals(EnumExternalInventoryButtonHost.BANIRA,
                ExternalInventoryButtonPolicy.resolve(
                        EnumExternalInventoryButtonHost.BANIRA, true));
    }

    @Test
    public void ftbModeUsesFtbWhenAvailable() {
        assertEquals(EnumExternalInventoryButtonHost.FTB_LIBRARY,
                ExternalInventoryButtonPolicy.resolve(
                        EnumExternalInventoryButtonHost.FTB_LIBRARY, true));
    }

    @Test
    public void ftbModeFallsBackToBaniraWhenFtbIsMissing() {
        assertEquals(EnumExternalInventoryButtonHost.BANIRA,
                ExternalInventoryButtonPolicy.resolve(
                        EnumExternalInventoryButtonHost.FTB_LIBRARY, false));
    }
}
