package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.client.data.BaniraColorConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiFabric16NotificationHudPortTest {
    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode node, String method, String owner, String name) {
        return node.methods.stream().filter(candidate -> method == null || candidate.name.equals(method))
                .flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals(owner) && ((MethodInsnNode) instruction).name.equals(name));
    }

    @Test
    public void actualProducersReachThePersistentHostAndNativeFallback() throws Exception {
        lombok.var manager = node("client/util/NotificationManager");
        assertTrue(calls(manager, "render", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "canRender"));
        assertTrue(calls(manager, "render", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render"));
        assertTrue(calls(manager, "render", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative"));
        assertTrue(calls(manager, "render", "xin/vanilla/banira/client/gui/component/Notification", "prepareInRegion"));
        assertTrue(calls(manager, "tryHandleHudClick", "xin/vanilla/banira/client/gui/component/Notification", "acceptsInput"));
        lombok.var unread = node("client/notification/NotificationUnreadHud");
        assertTrue(calls(unread, "render", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render"));
        assertTrue(calls(unread, "render", "xin/vanilla/banira/internal/client/ui/HudDrawList", "renderNative"));
        assertTrue(calls(unread, "render", "xin/vanilla/banira/client/notification/NotificationMinimapBridge", "selectedHost"));
    }

    @Test
    public void hudDocumentOwnsItsBranchWithoutScreenOrMouseOperations() throws Exception {
        lombok.var doc = node("internal/client/ui/AuiHudDocument");
        assertTrue(doc.interfaces.contains("xin/vanilla/banira/internal/client/ui/SharedHudHosts$Renderer"));
        assertTrue(calls(doc, "render", "com/sighs/apricityui/render/Base", "drawOverlayDocument"));
        assertTrue(calls(doc, "render", "xin/vanilla/banira/internal/client/ui/HudDomPresentation", "afterLayout"));
        for (lombok.var method : doc.methods) {
            for (lombok.var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    assertFalse(((MethodInsnNode) instruction).name.equals("releaseMouse") || ((MethodInsnNode) instruction).name.equals("grabMouse")
                            || ((MethodInsnNode) instruction).name.equals("setScreen") || ((MethodInsnNode) instruction).name.equals("drawPseudoCursor"));
                    if (((MethodInsnNode) instruction).name.equals("remove") && ((MethodInsnNode) instruction).owner.equals("com/sighs/apricityui/init/Document"))
                        assertEquals("()V", ((MethodInsnNode) instruction).desc);
                    if (method.name.startsWith("lambda$")) assertNotEquals("live", ((MethodInsnNode) instruction).name);
                }
            }
        }
    }

    @Test
    public void bootstrapKeepsNotificationSurfacesAndNativeMinimapIsolation() throws Exception {
        String source = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(4, source.split("SharedHudHosts.register\\(", -1).length - 1);
        assertTrue(source.contains("SharedHudHosts.install(UI_HUDS)"));
        assertTrue(source.contains("SharedHudHosts.NOTIFICATIONS"));
        assertTrue(source.contains("SharedHudHosts.UNREAD"));
        assertTrue(source.contains("EnumNotificationHudHost.STANDALONE"));
        for (String file : new String[]{"NotificationMapLineRenderer", "NotificationMinimapBridge"}) {
            lombok.var map = node("client/notification/" + file);
            assertFalse(calls(map, null, "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "render"));
        }
        assertFalse(calls(node("internal/fabric/client/FabricAuiClientHooks"), null,
                "xin/vanilla/banira/client/util/NotificationManager", "render"));
        lombok.var entry = node("internal/fabric/client/FabricBaniraCodexClient");
        assertTrue(calls(entry, null, "xin/vanilla/banira/client/event/BaniraClientEventHub$Client", "fireRenderOverlayPostNative"));
        assertTrue(calls(entry, null, "xin/vanilla/banira/client/event/BaniraClientEventHub$Client", "fireDrawScreenPostNative"));
        lombok.var bridge = node("client/event/BaniraClientEventHub$Client");
        assertTrue(calls(bridge, "fireRenderOverlayPostNative", "xin/vanilla/banira/client/util/NotificationManager", "render"));
        assertTrue(calls(bridge, "fireDrawScreenPostNative", "xin/vanilla/banira/client/util/NotificationManager", "render"));
    }

    @Test
    public void externalReplacementIsNotRemovedByAnOldOwner() throws Exception {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        AtomicInteger draws = new AtomicInteger();
        AuiHudHost host = new AuiHudHost(backend, spec -> new NativeUiView(null));
        UiHudBinding binding = new UiHudBinding() {
            public boolean visible() { return true; }
            public void bind(xin.vanilla.banira.api.client.ui.UiView view) { }
            public void detach(xin.vanilla.banira.api.client.ui.UiDetachReason reason) { }
        };
        lombok.var spec = new UiHudSpec(SharedHudHosts.NOTIFICATIONS, "banira_codex/hud/overlay.html", binding, false, true);
        lombok.var old = host.register(spec);
        host.tick(true);
        assertTrue(host.render(spec.id(), view -> draws.incrementAndGet()));
        lombok.var external = host.register(spec);
        old.close();
        assertTrue(host.render(spec.id(), view -> draws.incrementAndGet()));
        assertEquals(2, draws.get());
        external.close();
        assertFalse(host.render(spec.id(), view -> fail()));
    }

    @Test
    public void unreadFrameRetainsTheNativeFootprintAndLegacyTextBridge() throws Exception {
        HudDrawList frame = new HudDrawList();
        frame.unread(7, 9, 42, new BaniraColorConfig());
        lombok.var part = frame.parts().get(0);
        assertEquals(HudDrawList.Kind.UNREAD, part.kind);
        assertEquals(32, part.width, 0);
        assertEquals(22, part.height, 0);
        assertEquals(42, part.color);
        lombok.var list = node("internal/client/ui/HudDrawList");
        assertTrue(calls(list, "renderContents", "xin/vanilla/banira/client/util/AbstractGuiUtils", "renderByDepth"));
        assertTrue(calls(list, null, "xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics", "drawString"));
        for (lombok.var method : list.methods)
            for (lombok.var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics")
                        && ((MethodInsnNode) instruction).name.equals("drawString"))
                    assertTrue(((MethodInsnNode) instruction).desc, ((MethodInsnNode) instruction).desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I"));
    }
}
