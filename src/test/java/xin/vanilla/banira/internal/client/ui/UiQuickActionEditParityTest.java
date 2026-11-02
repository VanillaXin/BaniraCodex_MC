package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.quickaction.*;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.internal.client.ui.page.FormUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class UiQuickActionEditParityTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private Screen parent() {
        return new Screen(new net.minecraft.network.chat.TextComponent("")) {
        };
    }

    private InputFormScreen.Args args(String name, Class<?>[] types, Object... values) throws Exception {
        Class<?> editor = Class.forName("xin.vanilla.banira.client.gui.quickaction.CustomQuickActionEditor");
        assertTrue("Quick editors must expose their original form model before screen creation", Arrays.stream(editor.getDeclaredMethods()).anyMatch(method -> method.getName().equals(name)));
        lombok.var method = editor.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return (InputFormScreen.Args) method.invoke(null, values);
    }

    @Test
    public void definitionEditsUseOriginalCopyAndSubmitExactlyOnce() throws Exception {
        CustomQuickActionDefinition source = new CustomQuickActionDefinition().setId("custom:test").setLabel("Original");
        List<CustomQuickActionDefinition> accepted = new ArrayList<>();
        lombok.var form = new FormUiSession(args("definitionArgs", new Class<?>[]{Screen.class, CustomQuickActionDefinition.class, Consumer.class},
                parent(), source, (Consumer<CustomQuickActionDefinition>) accepted::add));
        form.draft("label", "Changed");
        assertTrue(form.submit());
        assertFalse(form.submit());
        assertEquals(1, accepted.size());
        assertEquals("Changed", accepted.get(0).getLabel());
        assertEquals("Original", source.getLabel());
    }

    @Test
    public void conditionalStepAndScreenTargetRemainStructuredOriginalValues() throws Exception {
        CustomQuickActionStep source = new CustomQuickActionStep().setType(QuickActionStepType.SCREEN).setValue("banira_codex:navigation");
        List<CustomQuickActionStep> accepted = new ArrayList<>();
        lombok.var form = new FormUiSession(args("stepArgs", new Class<?>[]{Screen.class, CustomQuickActionStep.class,
                        QuickActionExecutionMode.class, Consumer.class, Collection.class}, parent(), source, QuickActionExecutionMode.CHAINED,
                (Consumer<CustomQuickActionStep>) accepted::add, Collections.singletonList("banira_codex:navigation")));
        form.draft("condition", "ON_SUCCESS");
        assertTrue(form.submit());
        assertFalse(form.submit());
        assertEquals(1, accepted.size());
        assertEquals(QuickActionStepCondition.ON_SUCCESS, accepted.get(0).getCondition());
        assertEquals("banira_codex:navigation", accepted.get(0).getValue());
        assertEquals(QuickActionStepCondition.ALWAYS, source.getCondition());
    }

    @Test
    public void menuEditsDoNotMutateOriginalUntilAccepted() throws Exception {
        CustomQuickActionMenuItem source = new CustomQuickActionMenuItem().setLabel("Original");
        List<CustomQuickActionMenuItem> accepted = new ArrayList<>();
        lombok.var form = new FormUiSession(args("menuItemArgs", new Class<?>[]{Screen.class, CustomQuickActionMenuItem.class, Consumer.class},
                parent(), source, (Consumer<CustomQuickActionMenuItem>) accepted::add));
        form.draft("label", "Menu");
        assertTrue(form.submit());
        assertFalse(form.submit());
        assertEquals(1, accepted.size());
        assertEquals("Menu", accepted.get(0).getLabel());
        assertEquals("Original", source.getLabel());
    }
}
