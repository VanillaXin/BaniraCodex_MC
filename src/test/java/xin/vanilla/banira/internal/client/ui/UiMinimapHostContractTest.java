package xin.vanilla.banira.internal.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class UiMinimapHostContractTest {
    static Class<?> rows() {
        try { return Class.forName("xin.vanilla.banira.internal.client.ui.MinimapUiRows"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Map callback AUI rows missing", missing); }
    }
    static Object call(String method, Class<?>[] types, Object... args) throws Exception {
        return rows().getMethod(method, types).invoke(null, args);
    }
    Component tag(EnumNotificationHudHost host, String id, Component text) throws Exception {
        return (Component) call("tag", new Class[]{EnumNotificationHudHost.class, ResourceLocation.class, Component.class}, host, new ResourceLocation(id), text);
    }
    boolean owns(EnumNotificationHudHost host, Component text) throws Exception {
        return (boolean) call("owns", new Class[]{EnumNotificationHudHost.class, Component.class}, host, text);
    }
    @Test public void equalForeignLabelsNeverAcquireOwnership() throws Exception {
        call("reset", new Class[]{});
        Component source = Component.literal("same");
        Component owned = tag(EnumNotificationHudHost.XAERO, "test:row", source);
        assertNotSame(source, owned);
        assertTrue(owns(EnumNotificationHudHost.XAERO, owned));
        assertFalse(owns(EnumNotificationHudHost.XAERO, source));
        assertFalse(owns(EnumNotificationHudHost.XAERO, Component.literal("same")));
        assertFalse(owns(EnumNotificationHudHost.JOURNEYMAP, owned));
    }
    @Test public void newSupplierSnapshotRevokesOldIdentityAndResetClearsWorld() throws Exception {
        call("reset", new Class[]{});
        Component a = tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.literal("old"));
        Component b = tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.literal("new"));
        assertFalse(owns(EnumNotificationHudHost.JOURNEYMAP, a));
        assertTrue(owns(EnumNotificationHudHost.JOURNEYMAP, b));
        call("reset", new Class[]{});
        assertFalse(owns(EnumNotificationHudHost.JOURNEYMAP, b));
    }
    @Test public void copiedNativeLabelKeepsIdentityWithoutRetaggingEqualSupplierCalls() throws Exception {
        call("reset", new Class[]{});
        Component a = tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.literal("countdown"));
        assertSame(a, tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.literal("countdown")));
        assertTrue(owns(EnumNotificationHudHost.JOURNEYMAP, a.copy()));
    }
    @Test public void equivalentTranslatableSupplierKeepsCachedNativeIdentity() throws Exception {
        call("reset", new Class[]{});
        Component a = tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.translatable("test.untranslated"));
        assertSame(a, tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.translatable("test.untranslated")));
    }
    @Test public void downstreamMutationCannotClaimTheOriginalSnapshot() throws Exception {
        call("reset", new Class[]{});
        Component a = tag(EnumNotificationHudHost.JOURNEYMAP, "test:row", Component.literal("countdown"));
        ((net.minecraft.network.chat.MutableComponent)a).append(" changed");
        assertFalse("Only the original supplier value may be painted", owns(EnumNotificationHudHost.JOURNEYMAP, a));
    }
    @Test public void styledNativeLabelsArePreservedWithoutFlattening() throws Exception {
        call("reset", new Class[]{});
        Component source = Component.literal("styled").withStyle(net.minecraft.ChatFormatting.BOLD);
        assertSame(source, tag(EnumNotificationHudHost.XAERO, "test:style", source));
        assertFalse(owns(EnumNotificationHudHost.XAERO, source));
    }
    static class View extends NativeUiView implements MinimapUiRows.Renderer {
        int paints;
        boolean fail, partial;
        View() { super(null); }
        public void renderRow(net.minecraft.client.gui.GuiGraphics graphics, String key, Component text,
                              xin.vanilla.banira.client.data.ScreenCoordinate bounds, int color, boolean shadow, float depth) {
            if (partial) { paints++; throw new AuiHudHost.PaintedFailure(new IllegalStateException("partial")); }
            if (fail) throw new IllegalStateException("before paint");
            paints++;
        }
    }
    @Test public void timeoutSwitchReloadAndReconnectReleaseAndRecoverTheSameRegistration() {
        var clock = new java.util.concurrent.atomic.AtomicLong(1);
        var backend = new UiBackendController(() -> true, () -> true);
        List<View> views = new ArrayList<>();
        var h = new AuiHudHost(backend, spec -> { assertEquals("banira_codex/hud/minimap.html", spec.template()); View v = new View(); views.add(v); return v; });
        var rows = new MinimapUiRows(h, clock::get);
        h.tick(true);
        var bounds = new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 80, 9);
        java.util.function.BooleanSupplier paint = () -> rows.paint(null, "test:row", Component.literal("value"), bounds, -1, false, 0, () -> {});
        assertTrue(paint.getAsBoolean()); assertTrue(backend.usesAui(MinimapUiRows.ID));
        clock.addAndGet(500_000_001); h.tick(true); assertTrue(views.get(0).closed()); assertFalse(backend.usesAui(MinimapUiRows.ID));
        assertTrue(paint.getAsBoolean());
        backend.requestSwitch(false); backend.tick(); h.tick(true); assertFalse(paint.getAsBoolean()); assertTrue(views.get(1).closed());
        backend.requestSwitch(true); backend.tick(); h.tick(true); assertTrue(paint.getAsBoolean());
        backend.requestReload(); backend.tick(); h.tick(true); assertTrue(views.get(2).closed()); assertTrue(paint.getAsBoolean());
        h.tick(false); assertTrue(views.get(3).closed()); assertFalse(paint.getAsBoolean());
        h.tick(true); assertTrue(paint.getAsBoolean()); rows.close(); assertTrue(views.stream().allMatch(NativeUiView::closed));
    }
    @Test public void failedAndPartialPaintsNeverDrawBothBackends() {
        for (boolean partial : new boolean[]{false, true}) {
            var backend = new UiBackendController(() -> true, () -> true);
            View v = new View(); v.fail = !partial; v.partial = partial;
            var h = new AuiHudHost(backend, spec -> v); var rows = new MinimapUiRows(h, () -> 1); h.tick(true);
            int nativePaints = 0;
            if (!rows.paint(null, "test:row", Component.literal("value"), new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 80, 9), -1, false, 0, () -> {})) nativePaints++;
            assertEquals(1, nativePaints + v.paints); assertTrue(v.closed()); assertFalse(backend.usesAui(MinimapUiRows.ID));
            assertFalse(rows.paint(null, "test:row", Component.literal("value"), new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 80, 9), -1, false, 0, () -> {}));
            rows.close();
        }
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
    @Test public void allFixedMapCallbacksDelegateOnlyTheirOwnText() throws Exception {
        String owner = "xin/vanilla/banira/internal/client/ui/MinimapUiRows";
        assertTrue(calls("internal/compat/minimap/FtbNotificationInfo", owner, "draw"));
        assertTrue(calls("client/notification/NotificationMapLineRenderer", owner, "draw"));
        assertTrue(calls("internal/mixin/compat/minimap/XaeroInformationRenderMixin", owner, "drawOwned"));
        assertTrue(calls("internal/compat/minimap/JourneyMapTextBounds", owner, "drawOwned"));
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
