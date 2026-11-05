package xin.vanilla.banira.client.gui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NativeScreenBackgroundTest {
    private static final String GUI_RENDER = "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";
    private static final String VANILLA_SCREEN = "net/minecraft/client/gui/screens/Screen";
    private static final String BANIRA_SCREEN = "xin/vanilla/banira/client/gui/BaniraScreen";
    private static final String INPUT_FORM = "xin/vanilla/banira/client/gui/InputFormScreen";

    @Test
    public void vanillaExtractsOneBackgroundBeforeScreenContent() throws Exception {
        assertEquals(List.of("nextStratum", "extractBackground", "nextStratum",
                        "extractRenderState", "extractDeferredElements"),
                calls(VANILLA_SCREEN, "extractRenderStateWithTooltipAndSubtitles"));
        var background = calls(BANIRA_SCREEN, "extractBackground");
        assertTrue("Keep the in-world vanilla background", background.contains("extractBackground"));
        assertTrue("Keep the out-of-world theme gradient", background.contains("fillGradient"));
    }

    @Test
    public void baseScreenContentDoesNotExtractTheBackgroundAgain() throws Exception {
        assertNoBackgroundCalls(BANIRA_SCREEN, "extractRenderState");
    }

    @Test
    public void inputFormContentDoesNotRequestASecondBlur() throws Exception {
        assertNoBackgroundCalls(INPUT_FORM, "onRender");
    }

    private static void assertNoBackgroundCalls(String owner, String method) throws Exception {
        for (String call : calls(owner, method)) {
            assertTrue(owner + "." + method + " repeats background extraction via " + call,
                    !List.of("extractBackground", "extractBlurredBackground", "blurBeforeThisStratum").contains(call));
        }
    }

    private static List<String> calls(String owner, String method) throws Exception {
        var type = new ClassNode();
        try (var input = NativeScreenBackgroundTest.class.getResourceAsStream("/" + owner + ".class")) {
            assertNotNull(owner, input);
            new ClassReader(input).accept(type, 0);
        }
        var render = type.methods.stream().filter(m -> m.name.equals(method) && m.desc.equals(GUI_RENDER))
                .findFirst().orElseThrow();
        var result = new ArrayList<String>();
        for (var instruction : render.instructions) {
            if (instruction instanceof MethodInsnNode call) result.add(call.name);
        }
        return result;
    }
}
