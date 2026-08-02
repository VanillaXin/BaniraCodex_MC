package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;
import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
import xin.vanilla.banira.api.quickaction.CustomQuickActionMenuItem;
import xin.vanilla.banira.api.quickaction.CustomQuickActionStep;
import xin.vanilla.banira.api.quickaction.QuickActionStepType;

import java.util.Arrays;

import static org.junit.Assert.*;

public class CustomQuickActionNormalizationTest {
    @Test
    public void malformedOptionalFieldsReceiveSafeDefaults() {
        CustomQuickActionDefinition source = new CustomQuickActionDefinition()
                .setId(" test ").setLabel(null).setDisplay(null).setIconType(null)
                .setIcon(null).setKeyChord(null).setExecutionMode(null)
                .setCloseBeforeExecution(true)
                .setSteps(Arrays.asList(null,
                        new CustomQuickActionStep().setType(QuickActionStepType.COMMAND)
                                .setCondition(null).setValue(" /help ")))
                .setContextMenuItems(Arrays.asList(null,
                        new CustomQuickActionMenuItem().setLabel(" Menu ")
                                .setExecutionMode(null).setSteps(Arrays.asList(
                                new CustomQuickActionStep().setType(QuickActionStepType.COMMAND)
                                        .setValue(" /spawn ")))));

        CustomQuickActionDefinition normalized = CustomQuickActionManager.normalize(source);

        assertNotNull(normalized);
        assertEquals("test", normalized.getId());
        assertEquals("test", normalized.getLabel());
        assertEquals("/help", normalized.getSteps().get(0).getValue());
        assertNotNull(normalized.getSteps().get(0).getCondition());
        assertTrue(normalized.isCloseBeforeExecution());
        assertEquals("Menu", normalized.getContextMenuItems().get(0).getLabel());
        assertEquals("/spawn", normalized.getContextMenuItems().get(0).getSteps().get(0).getValue());
    }

    @Test
    public void missingIdIsRejected() {
        assertNull(CustomQuickActionManager.normalize(new CustomQuickActionDefinition()));
    }
}
