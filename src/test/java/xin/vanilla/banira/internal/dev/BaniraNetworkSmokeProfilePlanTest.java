package xin.vanilla.banira.internal.dev;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaniraNetworkSmokeProfilePlanTest {

    @Test
    public void keepsTheProfileAliveUntilBothTheReportAndMinimumCyclesAreReady() {
        assertTrue(BaniraNetworkSmokeProfilePlan.shouldContinue(false, 20));
        assertTrue(BaniraNetworkSmokeProfilePlan.shouldContinue(true, 19));
        assertFalse(BaniraNetworkSmokeProfilePlan.shouldContinue(true, 20));
    }

    @Test
    public void spacesCompositeCyclesAcrossServerTicks() {
        assertFalse(BaniraNetworkSmokeProfilePlan.isCycleDue(9, 0));
        assertTrue(BaniraNetworkSmokeProfilePlan.isCycleDue(10, 0));
        assertFalse(BaniraNetworkSmokeProfilePlan.isCycleDue(10, 10));
    }

    @Test
    public void schedulesTheNextConfigWriteOnlyAfterThePreviousReloadIsObserved() {
        assertTrue(BaniraNetworkSmokeProfilePlan.shouldScheduleConfigReload(false, 2));
        assertFalse(BaniraNetworkSmokeProfilePlan.shouldScheduleConfigReload(true, 2));
        assertFalse(BaniraNetworkSmokeProfilePlan.shouldScheduleConfigReload(false, 3));
    }
}
