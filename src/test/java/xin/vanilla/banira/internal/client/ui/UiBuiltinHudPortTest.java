package xin.vanilla.banira.internal.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UiBuiltinHudPortTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    private ClassNode node(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void originalDrawersUseSharedPresentationAndKeepNativeFallback() throws Exception {
        for (String owner : List.of("client/util/NotificationManager", "client/notification/NotificationUnreadHud",
                "client/gui/quickaction/QuickActionOverlay", "client/gui/widget/MouseWidget")) {
            boolean shared = false, fallback = false;
            for (var method : node(owner).methods) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.endsWith("/SharedHudHosts") && call.name.equals("render")) shared = true;
                    if (call.owner.endsWith("/HudDrawList") && call.name.equals("renderNative")) fallback = true;
                }
            assertTrue(owner + " must use the owned HUD host", shared);
            assertTrue(owner + " must retain native drawing", fallback);
        }
    }

    @Test
    public void styledHitGeometryIsCollectedAfterLayoutBeforeNativeIcons() throws Exception {
        var render = node("internal/client/ui/AuiHudDocument").methods.stream()
                .filter(method -> method.name.equals("render")).findFirst().orElseThrow();
        int paint = -1, hits = -1, icons = -1;
        for (int i = 0; i < render.instructions.size(); i++) {
            if (!(render.instructions.get(i) instanceof MethodInsnNode call)) continue;
            if (call.name.equals("drawOverlayDocument")) paint = i;
            if (call.owner.endsWith("/HudDomPresentation") && call.name.equals("afterLayout")) hits = i;
            if (call.owner.endsWith("/AuiDocumentAccess") && call.name.equals("render")) icons = i;
        }
        assertTrue(paint >= 0 && hits > paint && icons > hits);
    }

    @Test
    public void unreadKeepsMinecraft21ResourceFactoriesAndOneChatReturnOwner() throws Exception {
        int factories = 0;
        var unread = node("client/notification/NotificationUnreadHud");
        for (var method : unread.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/resources/ResourceLocation")) {
                assertNotEquals("<init>", call.name);
                if (call.name.equals("fromNamespaceAndPath")) factories++;
            }
        assertEquals(2, factories);
        var history = unread.methods.stream().filter(method -> method.name.equals("historyScreen")).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(history.instructions.spliterator(), false)
                .noneMatch(instruction -> instruction instanceof org.objectweb.asm.tree.TypeInsnNode type
                        && type.desc.startsWith("xin/vanilla/banira/client/notification/NotificationUnreadHud$")));
    }

    @Test
    public void minimapRowsRemainOutsideTheAuiHost() throws Exception {
        assertTrue(node("internal/client/ui/AuiHudDocument").interfaces.stream().noneMatch(name -> name.contains("Minimap")));
        for (var method : node("client/notification/NotificationMinimapBridge").methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call)
                    assertFalse(call.owner.contains("/client/ui/"));
    }

    @Test
    public void oneFailedBuiltinDoesNotDisablePeersAndReloadRecreatesOnlyOwnedViews() {
        var ids = List.of(SharedHudHosts.NOTIFICATIONS, SharedHudHosts.UNREAD, SharedHudHosts.QUICK_ACTIONS, SharedHudHosts.POINTER);
        var backend = new UiBackendController(() -> true, () -> true);
        var views = new ArrayList<NativeUiView>();
        var host = new AuiHudHost(backend, spec -> {
            var view = new NativeUiView(new Object());
            views.add(view);
            return view;
        });
        for (String id : ids) host.register(new UiHudSpec(id, "banira_codex/hud/overlay.html", new UiHudBinding() {
            public boolean visible() { return true; }
            public void bind(UiView view) { }
            public void detach(UiDetachReason reason) { }
        }, false, true));
        backend.tick();
        host.tick(true);
        for (String id : ids) assertTrue(host.render(id, view -> { }));
        assertFalse(host.render(SharedHudHosts.NOTIFICATIONS, view -> { throw new LinkageError("fixture"); }));
        assertTrue(views.get(0).closed());
        assertFalse(host.canRender(SharedHudHosts.NOTIFICATIONS));
        for (String id : ids.subList(1, ids.size())) assertTrue(host.canRender(id));
        backend.requestReload();
        backend.tick();
        host.tick(true);
        assertTrue(views.stream().allMatch(NativeUiView::closed));
        for (String id : ids) assertTrue(host.render(id, view -> { }));
        host.tick(false);
        assertTrue(views.stream().allMatch(NativeUiView::closed));
        host.tick(true);
        for (String id : ids) assertTrue(host.render(id, view -> { }));
        host.tick(false);
        assertTrue(views.stream().allMatch(NativeUiView::closed));
    }
}
