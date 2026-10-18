package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.Assert.*;

public class UiMinimapBinaryContractTest {
    ClassNode read(String name) throws Exception {
        String paths = System.getProperty("banira.minimap.contractJars"); assertNotNull(paths);
        for (String path : paths.split(java.util.regex.Pattern.quote(File.pathSeparator))) try (ZipFile jar = new ZipFile(path)) {
            var entry = jar.getEntry(name + ".class");
            if (entry != null) try (InputStream in = jar.getInputStream(entry)) { ClassNode node = new ClassNode(); new ClassReader(in).accept(node, 0); return node; }
        }
        throw new AssertionError("Pinned map contract missing " + name);
    }
    List<MethodInsnNode> calls(ClassNode node, String method, String owner) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (MethodNode m : node.methods) if (m.name.equals(method)) for (AbstractInsnNode i : m.instructions)
            if (i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(owner)) result.add((MethodInsnNode)i);
        return result;
    }
    @Test public void fixedXaeroCompilerAndRendererExposeTheExactIdentityAndTextCall() throws Exception {
        ClassNode renderer = read("xaero/hud/minimap/info/render/InfoDisplayRenderer");
        var match = xin.vanilla.banira.internal.mixin.OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true); assertEquals(true, match.invoke(null, "XaeroInformationRenderMixin", renderer));
        MethodNode add = read("xaero/hud/minimap/info/render/compile/InfoDisplayCompiler").methods.stream()
                .filter(m -> m.name.equals("addLine") && m.desc.equals("(Lnet/minecraft/network/chat/Component;)V")).findFirst().get();
        assertTrue(calls(read("xaero/hud/minimap/info/render/compile/InfoDisplayCompiler"), "addLine", "java/util/List").stream().anyMatch(c -> c.name.equals("add")));
        MethodInsnNode append = java.util.stream.StreamSupport.stream(add.instructions.spliterator(), false)
                .filter(i -> i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("java/util/List") && ((MethodInsnNode)i).name.equals("add"))
                .map(i -> (MethodInsnNode)i).findFirst().get();
        assertTrue(append.getPrevious() instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, append.getPrevious().getOpcode());
        assertEquals(1, ((VarInsnNode)append.getPrevious()).var);
        renderer.methods.stream().filter(m -> m.name.equals("render")).forEach(m -> {
            for (AbstractInsnNode i : m.instructions) if (i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("drawNormalText")) m.instructions.remove(i);
        });
        assertEquals(false, match.invoke(null, "XaeroInformationRenderMixin", renderer));
    }
    @Test public void fixedJourneyDrawsTextSeparatelyFromBackgroundAndRetainsSupplierObject() throws Exception {
        var font = calls(read("journeymap/client/render/draw/DrawUtil"), "drawBatchLabel", "net/minecraft/client/gui/Font");
        assertEquals(1, font.stream().filter(c -> c.desc.equals("(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I")).count());
        ClassNode slot = read("journeymap/client/ui/theme/ThemeLabelSource$InfoSlot");
        assertEquals(1, calls(slot, "getLabelText", "java/util/function/Supplier").size());
        assertTrue(slot.fields.stream().anyMatch(f -> f.name.equals("lastValue") && f.desc.equals("Lnet/minecraft/network/chat/Component;")));
        ClassNode labels = read("journeymap/client/ui/minimap/LabelVars");
        for (MethodNode m : labels.methods) for (AbstractInsnNode i : m.instructions) if (i instanceof MethodInsnNode) {
            var c = (MethodInsnNode)i;
            if (m.name.equals("draw") || c.name.equals("getLabelText"))
                System.out.println("JourneyMap label path " + labels.name + "#" + m.name + " -> " + c.owner + "#" + c.name + c.desc);
        }
    }
    @Test public void fixedFtbAndAtlasExposeTheExistingGeometryCallbacks() throws Exception {
        ClassNode ftb = read("dev/ftb/mods/ftbchunks/api/client/minimap/MinimapInfoComponent");
        assertTrue(ftb.methods.stream().anyMatch(m -> m.name.equals("height") && m.desc.equals("(Ldev/ftb/mods/ftbchunks/api/client/minimap/MinimapContext;)I")));
        assertTrue(ftb.methods.stream().anyMatch(m -> m.name.equals("render") && m.desc.endsWith("Lnet/minecraft/client/gui/Font;)V")));
        ClassNode atlas = read("pepjebs/mapatlases/client/ui/MapAtlasesHUD");
        assertTrue(atlas.fields.stream().anyMatch(f -> f.name.equals("globalScale") && f.desc.equals("F")));
        assertTrue(atlas.methods.stream().anyMatch(m -> m.name.equals("render") && m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;FII)V")));
    }
    @Test public void mapTextUsesActualAuiPaintingWithNoNativeContentsOrInputLayer() throws Exception {
        ClassNode n = UiMinimapHostContractTest.node("internal/client/ui/AuiHudDocument");
        assertEquals(1, calls(n, "renderRow", "com/sighs/apricityui/render/Base").stream().filter(c -> c.name.equals("drawOverlayDocument")).count());
        assertTrue(calls(n, "renderRow", "xin/vanilla/banira/internal/client/ui/HudDrawList").isEmpty());
        assertTrue(calls(n, "renderRow", "net/minecraft/client/Minecraft").isEmpty());
        try (InputStream in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/hud/minimap.html")) {
            assertNotNull(in); String html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(html.contains("aui-mouse-events\" content=\"false")); assertTrue(html.contains("pointer-events:none")); assertFalse(html.contains("<script"));
        }
    }
}
