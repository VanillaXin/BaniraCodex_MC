package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;
import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
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
                .setSteps(Arrays.asList(null,
                        new CustomQuickActionStep().setType(QuickActionStepType.COMMAND)
                                .setCondition(null).setValue(" /help ")));

        CustomQuickActionDefinition normalized = CustomQuickActionManager.normalize(source);

        assertNotNull(normalized);
        assertEquals("test", normalized.getId());
        assertEquals("test", normalized.getLabel());
        assertEquals("/help", normalized.getSteps().get(0).getValue());
        assertNotNull(normalized.getSteps().get(0).getCondition());
    }

    @Test
    public void missingIdIsRejected() {
        assertNull(CustomQuickActionManager.normalize(new CustomQuickActionDefinition()));
    }
}
