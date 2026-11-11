package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;

import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric16QuickHudPortTest {
    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private long calls(ClassNode node, String method, String owner, String name) {
        return node.methods.stream().filter(candidate -> method == null || candidate.name.equals(method))
                .flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals(owner) && ((MethodInsnNode) instruction).name.equals(name)).count();
    }

    @Test
    public void bootstrapRegistersFourSurfacesWithTheOriginalVisibilityGates() throws Exception {
        String source = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(4, source.split("SharedHudHosts.register\\(", -1).length - 1);
        for (String id : new String[]{"NOTIFICATIONS", "UNREAD", "QUICK_ACTIONS", "POINTER"})
            assertTrue(source.contains("SharedHudHosts." + id));
        assertTrue(source.contains("QuickActionOverlay.isSupportedInventoryScreen("));
        assertTrue(source.contains("ClientConfig.get().useCustomCursor()"));
        assertTrue(source.contains("UI_SESSIONS.ownsScreen("));
        assertTrue(source.contains("EnumNotificationHudHost.STANDALONE"));
    }

    @Test
    public void bothNativeAndOwnedPagePointersUseTheSamePersistentRenderer() throws Exception {
        lombok.var mouse = node("client/gui/widget/MouseWidget");
        assertTrue(calls(mouse, "draw", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
        assertTrue(calls(mouse, "draw", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative") > 0);
        assertTrue(calls(mouse, "removed", "xin/vanilla/banira/internal/client/ui/UiSystemCursor", "release") > 0);
        assertTrue(calls(mouse, "mouseClicked", "xin/vanilla/banira/internal/client/ui/UiPointerState", "button") > 0);
        assertTrue(calls(mouse, "mouseReleased", "xin/vanilla/banira/internal/client/ui/UiPointerState", "button") > 0);
        assertTrue(calls(mouse, "mouseScrolled", "xin/vanilla/banira/internal/client/ui/UiPointerState", "scroll") > 0);
        lombok.var screen = node("client/gui/BaniraScreen");
        assertTrue(screen.methods.stream().anyMatch(method -> method.name.equals("render")
                && method.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")));
        assertTrue(screen.methods.stream().filter(method -> method.name.equals("render"))
                .flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.endsWith("/MouseWidget") && ((MethodInsnNode) instruction).name.equals("draw")
                        && ((MethodInsnNode) instruction).desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;II)V")));
        assertTrue(calls(node("internal/client/ui/UiPagePointer"), "render",
                "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
    }

    @Test
    public void fabricObserversForwardFeedbackWithoutReplayingOrCancellingInput() throws Exception {
        lombok.var hooks = node("internal/fabric/client/FabricAuiClientHooks");
        assertEquals(1, calls(hooks, "observeButton", "xin/vanilla/banira/internal/client/ui/UiPointerFeedback", "pointerButton"));
        assertEquals(1, calls(hooks, "observeScroll", "xin/vanilla/banira/internal/client/ui/UiPointerFeedback", "pointerScroll"));
        for (lombok.var method : hooks.methods)
            if (method.name.startsWith("observe"))
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode)
                        assertFalse(((MethodInsnNode) instruction).name.equals("mouseClicked") || ((MethodInsnNode) instruction).name.equals("mouseReleased")
                                || ((MethodInsnNode) instruction).name.equals("mouseScrolled") || ((MethodInsnNode) instruction).name.equals("setScreen")
                                || ((MethodInsnNode) instruction).name.equals("cancel") || ((MethodInsnNode) instruction).name.equals("glfwSetInputMode"));
    }

    @Test
    public void inventoryOverlayRetainsNativeActionsAndFabricKeyManager() throws Exception {
        lombok.var overlay = node("client/gui/quickaction/QuickActionOverlay");
        assertTrue(calls(overlay, "render", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
        assertTrue(calls(overlay, "render", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative") > 0);
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseClicked")));
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseReleased")));
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseScroll")));
        lombok.var manager = node("client/gui/quickaction/CustomQuickActionManager");
        assertTrue(manager.methods.stream().anyMatch(method -> method.name.equals("tickKeyBindings")));
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("activeKeyBindings")));
        assertNotNull(QuickActionRegistry.class.getDeclaredMethod("validateMenuAnchor"));
    }

    @Test
    public void drawingChangesCannotGrabOrReleaseTheMouseAndKeepLegacyTextCoordinates() throws Exception {
        for (String name : new String[]{"client/gui/widget/MouseWidget", "internal/client/ui/AuiHudDocument",
                "internal/client/ui/QuickActionHudPresentation"}) {
            for (lombok.var method : node(name).methods)
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode)
                        assertFalse(name, ((MethodInsnNode) instruction).name.equals("grabMouse") || ((MethodInsnNode) instruction).name.equals("releaseMouse")
                                || ((MethodInsnNode) instruction).name.equals("glfwSetInputMode") || ((MethodInsnNode) instruction).name.equals("setScreen"));
        }
        lombok.var drawing = node("internal/client/ui/HudDrawList");
        assertTrue(calls(drawing, "renderContents", "xin/vanilla/banira/client/util/AbstractGuiUtils", "renderByDepth") > 0);
        assertTrue(calls(drawing, null, "xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics", "drawString") > 0);
        for (lombok.var method : drawing.methods)
            for (lombok.var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics")
                        && ((MethodInsnNode) instruction).name.equals("drawString"))
                    assertTrue(((MethodInsnNode) instruction).desc, ((MethodInsnNode) instruction).desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I"));
    }

    @Test
    public void nativeOverlayRestoresTooltipPosesThroughFinally() throws Exception {
        lombok.var overlay = node("client/gui/quickaction/QuickActionOverlay");
        lombok.var render = overlay.methods.stream().filter(method -> method.name.equals("render")).findFirst().orElseThrow(AssertionError::new);
        assertTrue(render.tryCatchBlocks.stream().anyMatch(block -> block.type == null));
        assertEquals(4, calls(overlay, "render", "com/mojang/blaze3d/vertex/PoseStack", "pushPose"));
        assertEquals(6, calls(overlay, "render", "com/mojang/blaze3d/vertex/PoseStack", "popPose"));
        assertTrue(calls(overlay, "render", "com/mojang/blaze3d/systems/RenderSystem", "disableBlend") > 0);
    }
}
