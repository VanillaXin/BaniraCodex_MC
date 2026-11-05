package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.compat.aui.AuiGameElements;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiGameIconLayoutTest {
    private ScreenCoordinate fit(ScreenCoordinate slot, int maximum, int inset) throws Exception {
        Method method = Arrays.stream(AuiGameElements.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("iconBounds")).findFirst().orElse(null);
        assertNotNull("Game icons need a shared centered, inset square", method);
        return (ScreenCoordinate) method.invoke(null, slot, maximum, inset);
    }

    @Test
    public void wideSlotsCenterIconsOnBothAxes() throws Exception {
        ScreenCoordinate bounds = fit(new ScreenCoordinate(19, 85, 24, 24), 16, 2);
        assertEquals(23, bounds.x(), 0);
        assertEquals(89, bounds.y(), 0);
        assertEquals(16, bounds.width(), 0);
        assertEquals(16, bounds.height(), 0);
    }

    @Test
    public void narrowAndFractionalSlotsNeverOverflow() throws Exception {
        ScreenCoordinate bounds = fit(new ScreenCoordinate(4.2, 7.8, 11.7, 9.1), 16, 2);
        assertEquals(4, bounds.width(), 0);
        assertEquals(4, bounds.height(), 0);
        assertTrue(bounds.x() >= 6.2 && bounds.y() >= 9.8);
        assertTrue(bounds.x() + bounds.width() <= 13.9);
        assertTrue(bounds.y() + bounds.height() <= 14.9);
    }

    @Test
    public void tinySlotsAreNotDrawn() throws Exception {
        assertEquals(0, fit(new ScreenCoordinate(0, 0, 3, 3), 16, 2).width(), 0);
    }

    @Test
    public void textHitTestingRetainsClipping() {
        ScreenCoordinate bounds = new ScreenCoordinate(10, 20, 100, 40);
        assertEquals(0, AuiGameElements.lineAt(bounds, bounds, 12, 22, 9, 3));
        assertEquals(-1, AuiGameElements.lineAt(bounds, bounds, 9, 22, 9, 3));
    }

    @Test
    public void gameDrawingExtractsWithinScissorAndRestoresItOnExit() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var input = AuiGameElements.class.getResourceAsStream("AuiGameElements.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
        }
        for (String name : new String[]{"renderElement", "renderOption", "renderText"}) {
            var method = type.methods.stream().filter(value -> value.name.equals(name)).findFirst().orElseThrow();
            var calls = new java.util.ArrayList<String>();
            for (var instruction : method.instructions)
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    var call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                    calls.add(call.owner + "." + call.name);
                }
            int clip = calls.indexOf("net/minecraft/client/gui/GuiGraphicsExtractor.enableScissor");
            int restore = calls.lastIndexOf("net/minecraft/client/gui/GuiGraphicsExtractor.disableScissor");
            assertTrue(name + " must restore clipping after extraction", clip >= 0 && restore > clip);
            assertTrue(name + " must not mutate immediate GPU depth state during extraction",
                    calls.stream().noneMatch(call -> call.startsWith("org/lwjgl/opengl/GL11.")
                            || call.startsWith("com/mojang/blaze3d/systems/RenderSystem.")));
            assertFalse(name + " must restore state on failure", method.tryCatchBlocks.isEmpty());
            if (name.equals("renderText")) {
                int poses = 0, clips = 0;
                for (var instruction : method.instructions)
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                        var call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                        if (call.name.equals("popMatrix")) poses++;
                        if (call.name.equals("disableScissor")) clips++;
                        if (call.owner.equals("net/minecraft/client/gui/GuiGraphicsExtractor")
                                && call.name.equals("text")) {
                            int position = method.instructions.indexOf(call);
                            assertTrue("Text extraction failures must unwind both pose and clipping",
                                    method.tryCatchBlocks.stream().anyMatch(block -> block.type == null
                                            && method.instructions.indexOf(block.start) <= position
                                            && position < method.instructions.indexOf(block.end)));
                        }
                    }
                assertEquals("Normal and exceptional text exits restore the pose", 2, poses);
                assertEquals("Normal and exceptional text exits restore clipping", 2, clips);
            }
        }
    }

    @Test
    public void itemIconsUseTheExistingGuiModelRenderer() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var input = AuiGameElements.class.getResourceAsStream("AuiGameElements.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
        }
        var method = type.methods.stream().filter(value -> value.name.equals("renderOption")).findFirst().orElseThrow();
        boolean model = false;
        for (var instruction : method.instructions)
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                var call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                assertNotEquals("A block's particle texture is not its inventory icon", "renderGuiItemFlatBlit", call.name);
                if (call.name.equals("renderGuiItemScaled")) model = true;
            }
        assertTrue(model);
    }

    @Test
    public void staticHtmlParentsAreIndexedForViewportClipping() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
        }
        var index = type.methods.stream().filter(value -> value.name.equals("linkParents")).findFirst().orElse(null);
        assertNotNull("Static containers need the same parent chain as appended nodes", index);
        boolean parent = false;
        for (var instruction : index.instructions)
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode) {
                var field = (org.objectweb.asm.tree.FieldInsnNode) instruction;
                if (field.getOpcode() == org.objectweb.asm.Opcodes.PUTFIELD && field.name.equals("parent"))
                    parent = true;
            }
        assertTrue(parent);
        var constructor = type.methods.stream().filter(value -> value.name.equals("<init>")
                && value.desc.contains("BooleanSupplier") && value.desc.contains("/Element;")).findFirst().orElseThrow();
        boolean initialized = false;
        for (var instruction : constructor.instructions)
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode
                    && ((org.objectweb.asm.tree.MethodInsnNode) instruction).name.equals("linkParents"))
                initialized = true;
        assertTrue("Parent indexing must run on the loaded HTML tree", initialized);
        var original = type.methods.stream().filter(value -> value.name.equals("<init>")
                && value.desc.contains("BooleanSupplier") && !value.desc.contains("/Element;")).findFirst().orElseThrow();
        boolean delegated = false;
        for (var instruction : original.instructions)
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                    && call.name.equals("<init>") && call.owner.equals(type.name) && call.desc.equals(constructor.desc))
                delegated = true;
        assertTrue("The legacy owner constructor must retain indexed-parent initialization", delegated);
    }

    @Test
    public void accessibleControlMetadataPassesTheActualAttributeGuard() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess$AuiNode.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
        }
        var method = type.methods.stream().filter(value -> value.name.equals("attribute")).findFirst().orElseThrow();
        var strings = new java.util.HashSet<String>();
        for (var instruction : method.instructions)
            if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode
                    && ((org.objectweb.asm.tree.LdcInsnNode) instruction).cst instanceof String) {
                strings.add((String) ((org.objectweb.asm.tree.LdcInsnNode) instruction).cst);
            }
        assertTrue("Named icon buttons must not trigger native fallback", strings.contains("aria-label"));
        assertTrue("Selected menu rows need an explicit state", strings.contains("aria-selected"));
        assertTrue("The attribute boundary must remain closed", strings.contains("Unsupported UI attribute"));
    }

    @Test
    public void templateToolIconsKeepTheirDeclaredButtonDimensions() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess$AuiNode.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
        }
        var method = type.methods.stream().filter(value -> value.name.equals("gameElement")).findFirst().orElseThrow();
        boolean scoped = false;
        for (var instruction : method.instructions)
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode
                    && ((org.objectweb.asm.tree.FieldInsnNode) instruction).getOpcode() == org.objectweb.asm.Opcodes.GETFIELD
                    && ((org.objectweb.asm.tree.FieldInsnNode) instruction).name.equals("created")) scoped = true;
        assertTrue("Static toolbar icons must use their 22px template boxes", scoped);
    }
}
