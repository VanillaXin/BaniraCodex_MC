package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.api.quickaction.QuickActionStepCondition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionCommandsToServerTest {
    @Test
    public void chainedConditionsUseThePreviousCommandResult() {
        assertTrue(QuickActionCommandsToServer.conditionMatches(
                QuickActionStepCondition.ALWAYS, true));
        assertTrue(QuickActionCommandsToServer.conditionMatches(
                QuickActionStepCondition.ON_SUCCESS, true));
        assertFalse(QuickActionCommandsToServer.conditionMatches(
                QuickActionStepCondition.ON_SUCCESS, false));
        assertTrue(QuickActionCommandsToServer.conditionMatches(
                QuickActionStepCondition.ON_FAILURE, false));
    }
}
