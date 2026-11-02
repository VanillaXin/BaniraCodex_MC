package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric19QuickHudPortTest {
    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private long calls(ClassNode node, String method, String owner, String name) {
        return node.methods.stream().filter(candidate -> method == null || candidate.name.equals(method))
                .flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(owner) && call.name.equals(name)).count();
    }

    @Test
    public void bootstrapRegistersFourSurfacesWithTheOriginalVisibilityGates() throws Exception {
        String source = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java"));
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
        var mouse = node("client/gui/widget/MouseWidget");
        assertTrue(calls(mouse, "draw", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
        assertTrue(calls(mouse, "draw", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative") > 0);
        assertTrue(calls(mouse, "removed", "xin/vanilla/banira/internal/client/ui/UiSystemCursor", "release") > 0);
        assertTrue(calls(mouse, "mouseClicked", "xin/vanilla/banira/internal/client/ui/UiPointerState", "button") > 0);
        assertTrue(calls(mouse, "mouseReleased", "xin/vanilla/banira/internal/client/ui/UiPointerState", "button") > 0);
        assertTrue(calls(mouse, "mouseScrolled", "xin/vanilla/banira/internal/client/ui/UiPointerState", "scroll") > 0);
        var screen = node("client/gui/BaniraScreen");
        assertTrue(screen.methods.stream().anyMatch(method -> method.name.equals("runDevRenderAt")));
        assertTrue(screen.methods.stream().filter(method -> method.name.equals("render"))
                .flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.endsWith("/MouseWidget") && call.name.equals("draw")
                        && call.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;II)V")));
        assertTrue(calls(node("internal/client/ui/UiPagePointer"), "render",
                "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
    }

    @Test
    public void fabricObserversForwardFeedbackWithoutReplayingOrCancellingInput() throws Exception {
        var hooks = node("internal/fabric/client/FabricAuiClientHooks");
        assertEquals(1, calls(hooks, "observeButton", "xin/vanilla/banira/internal/client/ui/UiPointerFeedback", "pointerButton"));
        assertEquals(1, calls(hooks, "observeScroll", "xin/vanilla/banira/internal/client/ui/UiPointerFeedback", "pointerScroll"));
        for (var method : hooks.methods)
            if (method.name.startsWith("observe"))
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call)
                        assertFalse(call.name.equals("mouseClicked") || call.name.equals("mouseReleased")
                                || call.name.equals("mouseScrolled") || call.name.equals("setScreen")
                                || call.name.equals("cancel") || call.name.equals("glfwSetInputMode"));
    }

    @Test
    public void inventoryOverlayRetainsNativeActionsAndFabricKeyManager() throws Exception {
        var overlay = node("client/gui/quickaction/QuickActionOverlay");
        assertTrue(calls(overlay, "render", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render") > 0);
        assertTrue(calls(overlay, "render", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative") > 0);
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseClicked")));
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseReleased")));
        assertTrue(overlay.methods.stream().anyMatch(method -> method.name.equals("handleMouseScroll")));
        var manager = node("client/gui/quickaction/CustomQuickActionManager");
        assertTrue(manager.methods.stream().anyMatch(method -> method.name.equals("tickKeyBindings")));
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("activeKeyBindings")));
        assertNotNull(QuickActionRegistry.class.getDeclaredMethod("validateMenuAnchor"));
    }

    @Test
    public void drawingChangesCannotGrabOrReleaseTheMouseAndKeepLegacyTextCoordinates() throws Exception {
        for (String name : new String[]{"client/gui/widget/MouseWidget", "internal/client/ui/AuiHudDocument",
                "internal/client/ui/QuickActionHudPresentation"}) {
            for (var method : node(name).methods)
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call)
                        assertFalse(name, call.name.equals("grabMouse") || call.name.equals("releaseMouse")
                                || call.name.equals("glfwSetInputMode") || call.name.equals("setScreen"));
        }
        var drawing = node("internal/client/ui/HudDrawList");
        assertTrue(calls(drawing, "renderContents", "xin/vanilla/banira/client/util/AbstractGuiUtils", "renderByDepth") > 0);
        assertTrue(calls(drawing, null, "xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics", "drawString") > 0);
        for (var method : drawing.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics")
                        && call.name.equals("drawString"))
                    assertTrue(call.desc, call.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I"));
    }

    @Test
    public void nativeOverlayRestoresTooltipPosesThroughFinally() throws Exception {
        var overlay = node("client/gui/quickaction/QuickActionOverlay");
        var render = overlay.methods.stream().filter(method -> method.name.equals("render")).findFirst().orElseThrow();
        assertTrue(render.tryCatchBlocks.stream().anyMatch(block -> block.type == null));
        assertEquals(4, calls(overlay, "render", "com/mojang/blaze3d/vertex/PoseStack", "pushPose"));
        assertEquals(6, calls(overlay, "render", "com/mojang/blaze3d/vertex/PoseStack", "popPose"));
        assertTrue(calls(overlay, "render", "com/mojang/blaze3d/systems/RenderSystem", "disableBlend") > 0);
    }
}
