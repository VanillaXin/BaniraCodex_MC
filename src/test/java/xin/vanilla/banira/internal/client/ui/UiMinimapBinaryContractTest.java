package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class UiMinimapBinaryContractTest {
    ClassNode read(String name) throws Exception {
        String paths = System.getProperty("banira.minimap.contractJars");
        assertNotNull(paths);
        for (String path : paths.split(java.util.regex.Pattern.quote(File.pathSeparator)))
            try (ZipFile jar = new ZipFile(path)) {
                var entry = jar.getEntry(name + ".class");
                if (entry != null) try (InputStream in = jar.getInputStream(entry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, 0);
                    return node;
                }
            }
        throw new AssertionError("Pinned map contract missing " + name);
    }

    List<MethodInsnNode> calls(ClassNode node, String method, String owner) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (MethodNode m : node.methods)
            if (m.name.equals(method)) for (AbstractInsnNode i : m.instructions)
                if (i instanceof MethodInsnNode && ((MethodInsnNode) i).owner.equals(owner))
                    result.add((MethodInsnNode) i);
        return result;
    }

    @Test
    public void fixedXaeroCompilerAndRendererExposeTheExactIdentityAndTextCall() throws Exception {
        ClassNode manager = read("xaero/hud/minimap/info/InfoDisplayManager");
        var match = xin.vanilla.banira.internal.mixin.OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        assertEquals(true, match.invoke(null, "XaeroInfoDisplayManagerMixin", manager));
        MethodNode add = read("xaero/hud/minimap/info/render/compile/InfoDisplayCompiler").methods.stream()
                .filter(m -> m.name.equals("addLine") && m.desc.equals("(Lnet/minecraft/network/chat/Component;)V")).findFirst().get();
        assertTrue(calls(read("xaero/hud/minimap/info/render/compile/InfoDisplayCompiler"), "addLine", "java/util/List").stream().anyMatch(c -> c.name.equals("add")));
        MethodInsnNode append = java.util.stream.StreamSupport.stream(add.instructions.spliterator(), false)
                .filter(i -> i instanceof MethodInsnNode && ((MethodInsnNode) i).owner.equals("java/util/List") && ((MethodInsnNode) i).name.equals("add"))
                .map(i -> (MethodInsnNode) i).findFirst().get();
        assertTrue(append.getPrevious() instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, append.getPrevious().getOpcode());
        assertEquals(1, ((VarInsnNode) append.getPrevious()).var);
        manager.methods.removeIf(m -> m.name.equals("add"));
        assertEquals(false, match.invoke(null, "XaeroInfoDisplayManagerMixin", manager));
    }

    @Test
    public void fixedJourneyDrawsTextSeparatelyFromBackgroundAndRetainsSupplierObject() throws Exception {
        var font = calls(read("journeymap/client/render/draw/DrawUtil"), "drawBatchLabel", "net/minecraft/client/gui/Font");
        assertEquals(1, font.stream().filter(c -> c.desc.equals("(Lnet/minecraft/network/chat/Component;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I")).count());
        ClassNode slot = read("journeymap/client/ui/theme/ThemeLabelSource$InfoSlot");
        assertEquals(1, calls(slot, "getLabelText", "java/util/function/Supplier").size());
        assertTrue(slot.fields.stream().anyMatch(f -> f.name.equals("lastValue") && f.desc.equals("Ljava/lang/String;")));
        ClassNode labels = read("journeymap/client/ui/minimap/LabelVars");
        for (MethodNode m : labels.methods)
            for (AbstractInsnNode i : m.instructions)
                if (i instanceof MethodInsnNode) {
                    var c = (MethodInsnNode) i;
                    if (m.name.equals("draw") || c.name.equals("getLabelText"))
                        System.out.println("JourneyMap label path " + labels.name + "#" + m.name + " -> " + c.owner + "#" + c.name + c.desc);
                }
    }

    @Test
    public void fixedFtbExposesLegacyNativeTextListAndHudCallbacks() throws Exception {
        ClassNode ftb = read("dev/ftb/mods/ftbchunks/client/FTBChunksClient");
        var match = xin.vanilla.banira.internal.mixin.OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        assertEquals(true, match.invoke(null, "FtbChunksNotificationMixin", ftb));
        assertTrue(ftb.methods.stream().anyMatch(m -> m.name.equals("buildMinimapTextData") && m.desc.endsWith(")Ljava/util/List;")));
        ftb.methods.removeIf(m -> m.name.equals("renderHud"));
        assertEquals(false, match.invoke(null, "FtbChunksNotificationMixin", ftb));
        ClassNode legacy = UiMinimapHostContractTest.node("client/notification/NotificationMapLineRenderer");
        assertTrue(calls(legacy, "drawCentered", "net/minecraft/client/gui/Font").stream()
                .anyMatch(c -> c.name.equals("draw") && c.desc.startsWith("(Lcom/mojang/blaze3d/vertex/PoseStack;")));
    }

    @Test
    public void productionHostAndLegacyRowsCannotRegisterOrPaintMinimapAui() throws Exception {
        String legacy = "xin/vanilla/banira/internal/client/ui/MinimapUiRows";
        ClassNode hosts = UiMinimapHostContractTest.node("internal/client/ui/SharedHudHosts");
        assertTrue(calls(hosts, "install", legacy).isEmpty());
        ClassNode rows = UiMinimapHostContractTest.node("internal/client/ui/MinimapUiRows");
        for (MethodNode method : rows.methods) {
            assertTrue(calls(rows, method.name, "xin/vanilla/banira/internal/client/ui/AuiHudHost").isEmpty());
            assertTrue(calls(rows, method.name, "com/sighs/apricityui/render/Base").isEmpty());
            assertTrue(calls(rows, method.name, "net/minecraft/client/Minecraft").isEmpty());
        }
    }
}
