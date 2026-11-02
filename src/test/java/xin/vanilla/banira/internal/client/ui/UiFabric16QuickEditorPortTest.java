package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric16QuickEditorPortTest {
    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode node, String owner, String name) {
        return node.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals(owner) && ((MethodInsnNode) instruction).name.equals(name));
    }

    @Test
    public void listAndStepsKeepNativePageIdentityAndLifecycle() throws Exception {
        for (String name : new String[]{"CustomQuickActionConfigScreen", "CustomQuickActionStepsScreen"}) {
            ClassNode page = node("client/gui/quickaction/" + name);
            assertTrue(page.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
            assertTrue(calls(page, "xin/vanilla/banira/internal/client/ui/CommonUiPages", "close"));
            assertTrue(calls(page, "xin/vanilla/banira/internal/client/ui/CommonUiPages", "removed"));
            assertTrue(calls(page, "xin/vanilla/banira/internal/client/ui/page/QuickActionEditUiSession", "spec"));
            assertTrue(page.methods.stream().anyMatch(method -> method.name.equals("captureUiState")));
            assertTrue(page.methods.stream().anyMatch(method -> method.name.equals("prepareUiState")));
        }
    }

    @Test
    public void nativeEditorsUseTheSharedFormsAndOwnedSubmitNavigation() throws Exception {
        ClassNode editor = node("client/gui/quickaction/CustomQuickActionEditor");
        for (String name : new String[]{"definitionArgs", "stepArgs", "menuItemArgs", "iconArgs"})
            assertTrue(name, editor.methods.stream().anyMatch(method -> method.name.equals(name)));
        ClassNode steps = node("client/gui/quickaction/CustomQuickActionStepsScreen");
        assertTrue(calls(steps, "xin/vanilla/banira/client/gui/quickaction/CustomQuickActionEditor", "iconArgs"));
        assertTrue(calls(steps, "xin/vanilla/banira/client/gui/quickaction/CustomQuickActionStepsScreen", "finish"));
        assertTrue(calls(steps, "xin/vanilla/banira/internal/client/ui/page/QuickActionEditUiSession", "refresh"));
    }

    @Test
    public void editorPortRetainsFabricKeysAndAvoidsBlanketRegistryWrites() throws Exception {
        ClassNode manager = node("client/gui/quickaction/CustomQuickActionManager");
        assertTrue(manager.methods.stream().anyMatch(method -> method.name.equals("tickKeyBindings") && method.desc.equals("()V")));
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("registeredIds")));
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("activeKeyBindings")));
        for (String path : new String[]{"client/gui/quickaction/CustomQuickActionConfigScreen",
                "client/gui/quickaction/CustomQuickActionStepsScreen", "client/gui/quickaction/CustomQuickActionEditor",
                "internal/client/ui/page/QuickActionEditUiSession"})
            assertFalse(path, calls(node(path), "xin/vanilla/banira/client/gui/quickaction/QuickActionRegistry", "clear"));
    }

    @Test
    public void nativeLabelsRetainThePoseStackDrawingContract() throws Exception {
        for (String name : new String[]{"CustomQuickActionConfigScreen", "CustomQuickActionStepsScreen"}) {
            int count = 0;
            for (lombok.var method : node("client/gui/quickaction/" + name).methods)
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("drawString")
                            && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics")) {
                        Type[] arguments = Type.getArgumentTypes(((MethodInsnNode) instruction).desc);
                        assertEquals(Type.getObjectType("com/mojang/blaze3d/vertex/PoseStack"), arguments[0]);
                        assertEquals(Type.getObjectType("net/minecraft/client/gui/Font"), arguments[1]);
                        assertEquals(Type.FLOAT_TYPE, arguments[3]);
                        assertEquals(Type.FLOAT_TYPE, arguments[4]);
                        assertEquals(Opcodes.INVOKESTATIC, ((MethodInsnNode) instruction).getOpcode());
                        count++;
                    }
            assertTrue(count > 0);
        }
    }

    @Test
    public void quickSearchHintIsLocalizedInBothLanguages() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            try (lombok.var input = getClass().getResourceAsStream("/assets/banira_codex/lang/" + language + ".json")) {
                assertNotNull(language, input);
                com.google.gson.JsonObject values = new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(values.has("word.banira_codex.custom_quick_action_search_hint"));
                assertFalse(values.get("word.banira_codex.custom_quick_action_search_hint").getAsString().trim().isEmpty());
            }
        }
    }
}
