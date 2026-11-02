package xin.vanilla.banira.internal.client.ui;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.UiHudFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.*;

public class UiFabric18ManualHudPortTest {
    private ClassNode document() throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/AuiHudDocument.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private UiDomControlsTest.Dom dom() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        dom.ids.put("progress", dom.new Node("root"));
        return dom;
    }

    private UiHudFrame.Text text() {
        return new UiHudFrame.Text("time", "00:59", 12, 24, 30, 9, 9, .5, Math.PI / 2, 15, 3, 0x005da530);
    }

    @Test
    public void realManualRendererCommitsAndFlushesInsideTheDocumentContext() throws Exception {
        var node = document();
        assertTrue(node.interfaces.contains("xin/vanilla/banira/internal/client/ui/AuiHudHost$ManualRenderer"));
        assertFalse(node.interfaces.stream().anyMatch(name -> name.contains("Minimap")));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("hudFrame")
                && method.desc.equals("(Lxin/vanilla/banira/api/client/ui/UiHudFrame;)V")));
        var render = node.methods.stream().filter(method -> method.name.equals("renderHud")).findFirst().orElseThrow();
        List<String> calls = new ArrayList<>();
        for (var instruction : render.instructions)
            if (instruction instanceof MethodInsnNode call) calls.add(call.owner + ":" + call.name);
        int context = calls.indexOf("com/sighs/apricityui/init/Document:withContext");
        int update = calls.indexOf("xin/vanilla/banira/internal/client/ui/ProgressDomPresentation:update");
        int style = calls.indexOf("com/sighs/apricityui/init/Document:commitPendingStyleRecalcForRender");
        int commit = calls.indexOf("com/sighs/apricityui/init/Document:commitRenderState");
        int flush = calls.indexOf("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics:flush");
        int paint = calls.indexOf("com/sighs/apricityui/render/Base:drawOverlayDocument");
        assertTrue(context >= 0 && update > context && style > update && commit > style && flush > commit && paint > flush);
        assertTrue(calls.lastIndexOf("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics:flush") > paint);
        assertTrue(calls.contains("com/sighs/apricityui/init/Document$ContextScope:close"));
        assertTrue(render.tryCatchBlocks.stream().anyMatch(block -> block.type == null));
    }

    @Test
    public void manualDrawingCannotOpenScreensOrChangeSystemInput() throws Exception {
        var node = document();
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("currentDocument")));
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.name.equals("setScreen") || call.name.equals("releaseMouse")
                            || call.name.equals("grabMouse") || call.name.equals("glfwSetInputMode"));
                    if (method.name.startsWith("lambda$")) assertNotEquals("live", call.name);
                }
        var render = node.methods.stream().filter(method -> method.name.equals("renderHud")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(render.instructions.toArray()).anyMatch(instruction ->
                instruction instanceof org.objectweb.asm.tree.TypeInsnNode type
                        && type.desc.equals("xin/vanilla/banira/internal/client/ui/AuiHudHost$PaintedFailure")));
    }

    @Test
    public void defaultTextVariablesKeepNativePivotScaleRotationAndShadow() throws Exception {
        var dom = dom();
        var ui = new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
        ui.update(new UiHudFrame(List.of(text())));
        var variables = dom.created.get(0).styleProperties;
        assertEquals("90.0deg", variables.get("--hud-angle"));
        assertEquals("0.5", variables.get("--hud-scale"));
        assertEquals("-15.0px", variables.get("--hud-pivot-x"));
        assertEquals("-3.0px", variables.get("--hud-pivot-y"));
        assertEquals("9.0px", variables.get("--hud-font-size"));
        assertEquals("1.0px", variables.get("--hud-shadow-offset"));
        assertEquals(HudDrawList.css(0xff5da530), variables.get("--hud-color"));
        assertEquals(HudDrawList.css(0xff17290c), variables.get("--hud-shadow-color"));
        assertEquals("00:59", dom.created.get(1).text);
        assertEquals("00:59", dom.created.get(2).text);
    }

    @Test
    public void replacingPartKindReleasesOldNodesAndPreservesStableIdentity() throws Exception {
        var dom = dom();
        var ui = new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
        var marker = new UiHudFrame.Sprite("marker", new ResourceLocation("test", "leaf.png"), 0, 0, 9, 10, 0);
        ui.update(new UiHudFrame(List.of(new UiHudFrame.Sprite("time", new ResourceLocation("test", "leaf.png"), 0, 0, 9, 10, 0), marker)));
        var old = dom.created.get(0);
        var markerNode = dom.created.get(1);
        ui.update(new UiHudFrame(List.of(text(), marker)));
        assertTrue(old.removed);
        var replacement = dom.created.get(2);
        assertEquals("progress-text", replacement.attributes.get("class"));
        assertEquals("time", replacement.attributes.get("id"));
        var liveChildren = dom.ids.get("progress").children.stream()
                .filter(child -> dom.created.stream().anyMatch(node -> node.proxy == child && !node.removed)).toList();
        assertEquals(List.of(replacement.proxy, markerNode.proxy), liveChildren);
        ui.update(new UiHudFrame(List.of()));
        assertTrue(dom.created.stream().allMatch(node -> node.removed));
    }

    @Test
    public void missingProgressRootFailsBeforeCreatingAnyNodes() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        try {
            new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
            fail("Missing template root must select native fallback");
        } catch (IllegalStateException expected) {
            assertTrue(dom.created.isEmpty());
        }
    }
}
