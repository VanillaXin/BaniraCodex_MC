package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class UiEntryPointCoverageTest {
    @Test
    public void navigationConfigPlayerAndQuickConstructorsRetainSharedRouting() throws Exception {
        for (String name : new String[]{"CodexNavigationScreen", "ConfigEditorScreen", "PlayerConfigScreen",
                "CustomPlayerConfigEditScreen", "quickaction.CustomQuickActionConfigScreen", "quickaction.CustomQuickActionStepsScreen"}) {
            Class<?> screen = Class.forName("xin.vanilla.banira.client.gui." + name);
            assertTrue(name + " must route direct constructors through the shared host", UiNativePage.class.isAssignableFrom(screen));
            assertTrue(name, Arrays.stream(screen.getMethods()).anyMatch(method -> method.getName().equals("uiPage")));
        }
    }
}
