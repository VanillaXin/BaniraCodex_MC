package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
import xin.vanilla.banira.api.quickaction.QuickActionIconType;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.internal.client.ui.page.FormUiSession;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class UiQuickIconTransactionTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        new UiDomControlsTest().platform();
    }

    @Test
    public void openingASelectorMustNotChangeTheDraftIconType() throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream stream = getClass().getResourceAsStream("/xin/vanilla/banira/client/gui/quickaction/CustomQuickActionStepsScreen.class")) {
            new ClassReader(stream).accept(node, 0);
        }
        MethodNode open = node.methods.stream().filter(method -> method.name.equals("openIconPicker")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        for (lombok.var instruction : open.instructions)
            if (instruction instanceof MethodInsnNode)
                assertFalse("Opening or cancelling an icon selector must preserve the current type", ((MethodInsnNode) instruction).name.equals("setIconType"));
    }

    @Test
    public void iconFormCommitsTypeAndValueTogetherOnceAndCancelPreservesBoth() throws Exception {
        Class<?> editor = Class.forName("xin.vanilla.banira.client.gui.quickaction.CustomQuickActionEditor");
        lombok.var method = Arrays.stream(editor.getDeclaredMethods()).filter(value -> value.getName().equals("iconArgs")).findFirst().orElse(null);
        assertNotNull("Icon forms need the same testable model boundary as the other quick editors", method);
        method.setAccessible(true);
        CustomQuickActionDefinition draft = new CustomQuickActionDefinition().setIconType(QuickActionIconType.ITEM).setIcon("minecraft:paper");
        lombok.var parent = new net.minecraft.client.gui.screens.Screen(new net.minecraft.network.chat.TextComponent("")) {
        };
        lombok.var args = (InputFormScreen.Args) method.invoke(null, parent, draft, QuickActionIconType.RESOURCE,
                InputFormScreen.WidgetType.DROPDOWN, com.google.common.collect.ImmutableList.of(new DropdownOption("banira_codex:gui/quick_icon.png")));
        lombok.var cancelled = new FormUiSession(args);
        cancelled.draft("icon", "cancelled");
        cancelled.close(xin.vanilla.banira.api.client.ui.UiCloseReason.CANCEL);
        assertEquals(QuickActionIconType.ITEM, draft.getIconType());
        assertEquals("minecraft:paper", draft.getIcon());
        lombok.var accepted = new FormUiSession(args);
        accepted.draft("icon", "banira_codex:gui/quick_icon.png");
        assertTrue(accepted.submit());
        assertFalse(accepted.submit());
        assertEquals(QuickActionIconType.RESOURCE, draft.getIconType());
        assertEquals("banira_codex:gui/quick_icon.png", draft.getIcon());
    }
}
