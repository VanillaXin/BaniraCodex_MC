package xin.vanilla.banira.internal.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;

import java.io.InputStream;
import java.util.*;
import static org.junit.Assert.*;

public class UiMinimapHostContractTest {
    static Object call(String method, Class<?>[] types, Object... args) throws Exception {
        return MinimapUiRows.class.getMethod(method, types).invoke(null, args);
    }

    @Test public void legacyTagPreservesEveryNativeComponentAndNeverClaimsOwnership() {
        for (EnumNotificationHudHost host : EnumNotificationHudHost.values()) {
            for (String id : new String[]{"banira_codex:unread", "aotake_sweep:countdown", "test:third_party"}) {
                for (Component text : Arrays.asList(Component.literal("2 unread"),
                        Component.translatable("test.countdown", "02:34"),
                        Component.literal("styled").withStyle(net.minecraft.ChatFormatting.BOLD).append(" sibling"),
                        Component.empty(), null)) {
                    assertSame(text, MinimapUiRows.tag(host, new ResourceLocation(id), text));
                    assertFalse(MinimapUiRows.owns(host, text));
                    MinimapUiRows.reset();
                    assertFalse(MinimapUiRows.owns(host, text));
                }
            }
        }
    }

    @Test public void legacyDrawAlwaysLeavesOneNativeCallbackWithoutFlushOrGraphicsAccess() {
        Runnable flush = () -> { throw new AssertionError("Native map buffers must remain untouched"); };
        for (EnumNotificationHudHost host : EnumNotificationHudHost.values()) {
            Component text = MinimapUiRows.tag(host, new ResourceLocation("test:row"), Component.literal("02:34"));
            var bounds = new ScreenCoordinate(10, 20, 80, 9);
            int nativePaints = 0;
            if (!MinimapUiRows.drawOwned(host, null, text, bounds, 0x7f123456, true, flush)) nativePaints++;
            assertEquals(1, nativePaints);
            assertFalse(MinimapUiRows.draw(null, text, bounds, -1, false));
        }
    }

    @Test public void legacyInstallAndLifecycleNeverRegisterAuiOrReadClock() {
        var backend = new UiBackendController(() -> true, () -> true);
        List<String> created = new ArrayList<>();
        var h = new AuiHudHost(backend, spec -> { created.add(spec.id()); return new NativeUiView(null); });
        var rows = new MinimapUiRows(h, () -> { throw new AssertionError("Inactive row clock read"); });
        MinimapUiRows.install(h);
        SharedHudHosts.install(h);
        try {
            for (int phase = 0; phase < 5; phase++) {
                if (phase == 1 || phase == 2) { backend.requestSwitch(phase == 2); backend.tick(); }
                if (phase == 3) { backend.requestReload(); backend.tick(); }
                h.tick(phase != 4);
                assertFalse(h.render(MinimapUiRows.ID, view -> fail("Minimap AUI registration survived")));
                h.renderOthers(Collections.emptySet(), view -> fail("Unexpected minimap HUD"));
                SharedHudHosts.renderOthers(null);
                assertFalse(rows.paint(null, "test:row", Component.literal("value"),
                        new ScreenCoordinate(0, 0, 80, 9), -1, false, 0, () -> fail("Unexpected flush")));
                assertFalse(backend.usesAui(MinimapUiRows.ID));
                MinimapUiRows.reset();
            }
            h.tick(true);
            rows.close(); rows.close(); MinimapUiRows.install(h);
            assertFalse(h.render(MinimapUiRows.ID, view -> fail("Reconnect restored minimap takeover")));
            assertTrue(created.isEmpty());
        } finally { rows.close(); SharedHudHosts.install(null); }
    }

    static class View extends NativeUiView implements SharedHudHosts.Renderer {
        int paints;
        View() { super(null); }
        public void render(net.minecraft.client.gui.GuiGraphics graphics, HudDrawList frame) { paints++; }
    }

    @Test public void sharedInstallRetainsOtherHudRegistrationsAndRenderOthers() {
        var backend = new UiBackendController(() -> true, () -> true);
        Map<String, View> views = new LinkedHashMap<>();
        var h = new AuiHudHost(backend, spec -> { View v = new View(); views.put(spec.id(), v); return v; });
        SharedHudHosts.install(h);
        try {
            List<String> ids = Arrays.asList(SharedHudHosts.NOTIFICATIONS, SharedHudHosts.UNREAD,
                    SharedHudHosts.QUICK_ACTIONS, SharedHudHosts.POINTER);
            for (String id : ids) SharedHudHosts.register(id, () -> true);
            SharedHudHosts.register("test:other", () -> true);
            h.tick(true);
            for (String id : ids) assertTrue(SharedHudHosts.render(id, null, new HudDrawList()));
            SharedHudHosts.renderOthers(null);
            assertEquals(5, views.size());
            assertFalse(views.containsKey(MinimapUiRows.ID));
            assertTrue(views.values().stream().allMatch(v -> v.paints == 1));
        } finally { h.tick(false); SharedHudHosts.install(null); }
    }

    static ClassNode node(String name) throws Exception {
        try (InputStream in = UiMinimapHostContractTest.class.getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, in); ClassNode n = new ClassNode(); new ClassReader(in).accept(n, 0); return n;
        }
    }
    static boolean calls(String name, String owner, String method) throws Exception {
        for (MethodNode m : node(name).methods) for (AbstractInsnNode i : m.instructions)
            if (i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(owner) && ((MethodInsnNode)i).name.equals(method)) return true;
        return false;
    }

    @Test public void allNativeAdaptersAreIndependentOfAuiRows() throws Exception {
        for (String name : new String[]{"internal/compat/minimap/FtbNotificationInfo",
                "internal/compat/minimap/XaeroNotificationInfo", "client/notification/NotificationMapLineRenderer",
                "internal/forge/compat/minimap/JourneyMapNotificationPlugin", "internal/compat/minimap/JourneyMapTextBounds",
                "internal/mixin/compat/minimap/XaeroInformationRenderMixin",
                "internal/mixin/compat/minimap/JourneyMapTextBoundsMixin", "internal/client/ui/SharedHudHosts"}) {
            for (MethodNode method : node(name).methods) for (AbstractInsnNode i : method.instructions) {
                if (i instanceof MethodInsnNode) {
                    String owner = ((MethodInsnNode)i).owner;
                    assertFalse(name + " still delegates map text to AUI", owner.endsWith("/MinimapUiRows")
                            || owner.endsWith("/MinimapDomPresentation"));
                }
                if (i instanceof org.objectweb.asm.tree.LdcInsnNode)
                    assertNotEquals(name + " retains a minimap registration or exclusion", MinimapUiRows.ID, ((LdcInsnNode)i).cst);
            }
        }
    }

    @Test public void nativeTextMixinsNeverRedirectDrawingAndJourneyKeepsOnlyWidthConstraint() throws Exception {
        ClassNode xaero = node("internal/mixin/compat/minimap/XaeroInformationRenderMixin");
        assertTrue(xaero.methods.stream().allMatch(m -> m.name.equals("<init>")));
        ClassNode journey = node("internal/mixin/compat/minimap/JourneyMapTextBoundsMixin");
        List<MethodNode> hooks = new ArrayList<>();
        for (MethodNode method : journey.methods) if (!method.name.equals("<init>")) hooks.add(method);
        assertEquals(1, hooks.size());
        assertEquals("banira$boundInformationBatch", hooks.get(0).name);
        List<AnnotationNode> annotations = new ArrayList<>();
        if (hooks.get(0).visibleAnnotations != null) annotations.addAll(hooks.get(0).visibleAnnotations);
        if (hooks.get(0).invisibleAnnotations != null) annotations.addAll(hooks.get(0).invisibleAnnotations);
        assertTrue(annotations.stream().anyMatch(a -> a.desc.endsWith("/ModifyVariable;")));
        assertFalse(annotations.stream().anyMatch(a -> a.desc.endsWith("/Redirect;")));
        assertTrue(calls("internal/mixin/compat/minimap/JourneyMapTextBoundsMixin",
                "xin/vanilla/banira/internal/compat/minimap/JourneyMapTextBounds", "scale"));
    }

    @Test public void ftbAndAtlasKeepTheirNativeDrawingCallbacks() throws Exception {
        assertTrue(calls("internal/compat/minimap/FtbNotificationInfo",
                "xin/vanilla/banira/internal/compat/minimap/FtbNotificationInfo", "drawCenteredText"));
        assertTrue(calls("client/notification/NotificationMapLineRenderer", "net/minecraft/client/gui/GuiGraphics", "drawString"));
        assertTrue(calls("internal/compat/minimap/XaeroNotificationInfo",
                "xaero/hud/minimap/info/render/compile/InfoDisplayCompiler", "addLine"));
        for (String adapter : new String[]{"internal/compat/minimap/XaeroNotificationInfo",
                "internal/compat/minimap/FtbNotificationInfo", "internal/forge/compat/minimap/JourneyMapNotificationPlugin"})
            assertTrue(calls(adapter, "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "onRegistration"));
    }

    @Test public void ftbNativeCallbackReceivesOriginalFontGraphicsRowAndLineSpacing() throws Exception {
        MethodNode render = node("internal/compat/minimap/FtbNotificationInfo").methods.stream()
                .filter(m -> m.name.equals("render")).findFirst().get();
        List<AbstractInsnNode> code = new ArrayList<>();
        for (AbstractInsnNode i : render.instructions) if (i.getOpcode() >= 0) code.add(i);
        List<AbstractInsnNode> args = null;
        for (int i = 16; i < code.size(); i++) {
            if (code.get(i) instanceof MethodInsnNode && ((MethodInsnNode)code.get(i)).name.equals("drawCenteredText")) {
                assertNull("Only one native callback per row", args);
                args = code.subList(i - 16, i);
            }
        }
        assertNotNull(args);
        int[] opcodes = {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.GETFIELD,
                Opcodes.ILOAD, Opcodes.INVOKEINTERFACE, Opcodes.CHECKCAST, Opcodes.ILOAD, Opcodes.ALOAD,
                Opcodes.INVOKESTATIC, Opcodes.POP, Opcodes.BIPUSH, Opcodes.ICONST_2, Opcodes.IADD, Opcodes.IMUL};
        for (int i = 0; i < opcodes.length; i++) assertEquals("Native argument instruction " + i, opcodes[i], args.get(i).getOpcode());
        assertEquals(0, ((VarInsnNode)args.get(0)).var);
        assertEquals(3, ((VarInsnNode)args.get(1)).var);
        assertEquals(2, ((VarInsnNode)args.get(2)).var);
        assertEquals("text", ((FieldInsnNode)args.get(4)).name);
        assertEquals("get", ((MethodInsnNode)args.get(6)).name);
        assertEquals(((VarInsnNode)args.get(5)).var, ((VarInsnNode)args.get(8)).var);
        assertEquals(3, ((VarInsnNode)args.get(9)).var);
        MethodInsnNode fontCheck = (MethodInsnNode)args.get(10);
        assertEquals("java/util/Objects", fontCheck.owner);
        assertEquals("requireNonNull", fontCheck.name);
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", fontCheck.desc);
        assertEquals(9, ((IntInsnNode)args.get(12)).operand);
    }

    @Test public void originalHostPreferenceTimeoutAndBadSupplierRemainIsolated() {
        MinimapLineRegistry r = new MinimapLineRegistry();
        r.register(new MinimapLine(new ResourceLocation("test:bad"), Component.literal("bad"), () -> {throw new IllegalStateException("bad");}, () -> EnumNotificationHudHost.AUTO, 0));
        r.register(new MinimapLine(new ResourceLocation("test:good"), Component.literal("good"), () -> Component.literal("countdown"), () -> EnumNotificationHudHost.AUTO, 2));
        r.seen(EnumNotificationHudHost.JOURNEYMAP, 1);
        assertEquals(1, r.snapshot(EnumNotificationHudHost.JOURNEYMAP, 2).size());
        assertEquals(EnumNotificationHudHost.STANDALONE, r.selectedHost(EnumNotificationHudHost.AUTO, 600_000_002));
        r.seen(EnumNotificationHudHost.XAERO, 600_000_003);
        assertEquals(1, r.snapshot(EnumNotificationHudHost.XAERO, 600_000_004).size());
    }
}
