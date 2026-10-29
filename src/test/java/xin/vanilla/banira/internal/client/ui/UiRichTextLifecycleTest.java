package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.internal.compat.aui.AuiGameElements;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleUnaryOperator;

import static org.junit.Assert.*;

public class UiRichTextLifecycleTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @org.junit.Before
    public void platform() {
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
    }

    @Test
    public void drawingUsesDepthIndependentFontPipeline() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var in = AuiGameElements.class.getResourceAsStream("AuiGameElements.class")) {
            new org.objectweb.asm.ClassReader(in).accept(type, 0);
        }
        var method = type.methods.stream().filter(m -> m.name.equals("renderText")).findFirst().orElseThrow();
        boolean seeThrough = false;
        for (var instruction : method.instructions) {
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode) {
                var field = (org.objectweb.asm.tree.FieldInsnNode) instruction;
                if (field.owner.equals("net/minecraft/client/gui/Font$DisplayMode") && field.name.equals("SEE_THROUGH"))
                    seeThrough = true;
            }
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                var call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                assertFalse("GuiGraphics.drawString selects the normal depth-testing text batch",
                        call.owner.equals("net/minecraft/client/gui/GuiGraphics") && call.name.equals("drawString"));
            }
        }
        assertTrue("Text overlays need the depth-independent font render type", seeThrough);
    }

    private static final class ScrollFixture {
        final UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        final UiDomControlsTest.Dom.Node content = dom.ids.get("content");
        final UiDomAccess.Node node;
        final UiDomView view;
        double maximum = 16, target;
        boolean ready, smooth;
        int requests;

        ScrollFixture() throws Exception {
            node = (UiDomAccess.Node) Proxy.newProxyInstance(UiDomAccess.Node.class.getClassLoader(), new Class<?>[]{UiDomAccess.Node.class}, (p, m, a) -> {
                if (m.getName().equals("scrollTop") && a != null && a.length == 1) {
                    requests++;
                    double requested = (double) a[0];
                    target = requested > maximum ? maximum + (requested - maximum) * .4 : Math.max(0, requested);
                    if (!smooth) content.scroll = target;
                    content.emit("scroll");
                    return null;
                }
                try {
                    return m.invoke(content.proxy, a);
                } catch (InvocationTargetException failure) {
                    throw failure.getCause();
                }
            });
            var access = (UiDomAccess) Proxy.newProxyInstance(UiDomAccess.class.getClassLoader(), new Class<?>[]{UiDomAccess.class}, (p, m, a) -> {
                if (m.getName().equals("find") && a[0].equals("content")) return node;
                try {
                    return m.invoke(dom.access, a);
                } catch (InvocationTargetException failure) {
                    throw failure.getCause();
                }
            });
            view = new UiDomView(new Object(), access);
            view.state().scrollOffset(150);
        }

        void installGate() throws Exception {
            Method method;
            try {
                method = UiDomView.class.getMethod("scrollRestoration", BooleanSupplier.class, DoubleUnaryOperator.class);
            } catch (NoSuchMethodException missing) {
                throw new AssertionError("Scroll restoration needs committed-layout readiness and a bounded target", missing);
            }
            method.invoke(view, (BooleanSupplier) () -> ready, (DoubleUnaryOperator) value -> {
                node.scrollTop(Math.min(maximum, Math.max(0, value)));
                return target;
            });
        }
    }

    @Test
    public void earlyScrollAndPendingClosePreserveIntent() throws Exception {
        var f = new ScrollFixture();
        f.content.scroll = 16;
        f.content.emit("scroll");
        assertEquals(150, f.view.state().scrollOffset(), 0);
        f.view.close();
        assertEquals(150, f.view.state().scrollOffset(), 0);
    }

    @Test
    public void delayedLayoutAndSmoothRestorationRetainIntent() throws Exception {
        var f = new ScrollFixture();
        f.installGate();
        f.content.scroll = 16;
        f.content.emit("scroll");
        f.view.afterLayout();
        assertEquals(0, f.requests);
        assertEquals(150, f.view.state().scrollOffset(), 0);
        f.maximum = 300;
        f.ready = true;
        f.smooth = true;
        f.view.afterLayout();
        assertEquals(150, f.target, 0);
        f.content.scroll = 60;
        f.content.emit("scroll");
        f.view.afterLayout();
        assertEquals(1, f.requests);
        assertEquals(150, f.view.state().scrollOffset(), 0);
        f.content.scroll = 150;
        f.content.emit("scroll");
        f.view.afterLayout();
        f.content.scroll = 70;
        f.content.emit("scroll");
        f.view.afterLayout();
        assertEquals(70, f.view.state().scrollOffset(), 0);
        assertEquals(1, f.requests);
        f.view.close();
    }

    @Test
    public void settledFinalRangeClampDoesNotSnapBack() throws Exception {
        var f = new ScrollFixture();
        f.installGate();
        f.maximum = 90;
        f.ready = true;
        f.view.afterLayout();
        assertEquals(90, f.target, 0);
        assertEquals(90, f.view.state().scrollOffset(), 0);
        f.content.scroll = 20;
        f.content.emit("scroll");
        f.view.afterLayout();
        assertEquals(20, f.view.state().scrollOffset(), 0);
        assertEquals(1, f.requests);
        f.view.close();
    }

    @Test
    public void renderedTextMustWaitForContainerExtent() throws Exception {
        Method ready;
        try {
            ready = AuiGameElements.class.getDeclaredMethod("scrollExtentReady", double.class, double.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Text bounds can settle before scrollHeight; both metrics must be ready", missing);
        }
        ready.setAccessible(true);
        assertEquals(false, ready.invoke(null, 0d, 2076.4d));
        assertEquals(false, ready.invoke(null, 300d, 2076.4d));
        assertEquals(true, ready.invoke(null, 2082.4d, 2076.4d));
        assertEquals(true, ready.invoke(null, 0d, 0d));
        assertEquals(false, ready.invoke(null, Double.NaN, 10d));
        assertEquals(false, ready.invoke(null, 10d, Double.POSITIVE_INFINITY));
    }

    @Test
    public void userInputTakesOverPendingRestoration() throws Exception {
        for (String event : new String[]{"wheel", "mousedown", "pre-input"}) {
            var f = new ScrollFixture();
            f.installGate();
            f.maximum = 300;
            f.ready = true;
            f.smooth = true;
            f.view.afterLayout();
            f.content.scroll = 70;
            if (event.equals("pre-input")) {
                Method input;
                try {
                    input = UiDomView.class.getMethod("scrollInput", double.class, double.class);
                } catch (NoSuchMethodException missing) {
                    throw new AssertionError("Scrollbar takeover must precede DOM interception", missing);
                }
                input.invoke(f.view, 0d, 0d);
                assertEquals(150, f.target, 0);
                input.invoke(f.view, 208d, 80d);
            } else f.content.emit(event, 0);
            f.view.afterLayout();
            assertEquals(event, 70, f.view.state().scrollOffset(), 0);
            assertEquals(70, f.target, 0);
            f.content.scroll = 75;
            f.content.emit("scroll");
            f.view.afterLayout();
            assertEquals(75, f.view.state().scrollOffset(), 0);
            assertEquals(2, f.requests);
            f.view.close();
        }
    }

    @Test
    public void richAdapterUsesCommittedVisibilityAndBoundedRange() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var in = getClass().getClassLoader().getResourceAsStream("xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            new org.objectweb.asm.ClassReader(in).accept(type, 0);
        }
        var ready = type.methods.stream().filter(m -> m.name.equals("richLayoutReady")).findFirst().orElseThrow();
        assertTrue("Unmeasured and hidden nodes need distinct treatment", java.util.stream.StreamSupport.stream(ready.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof org.objectweb.asm.tree.FieldInsnNode && ((org.objectweb.asm.tree.FieldInsnNode) i).name.equals("isVisible")));
        assertTrue("Container scroll metrics must include the rendered rich text extent before restoring", java.util.stream.StreamSupport.stream(ready.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) i).name.equals("scrollExtentReady")));
        var restore = type.methods.stream().filter(m -> m.name.equals("restoreContentScroll")).findFirst().orElseThrow();
        int bounded = -1, setter = -1;
        for (var i : restore.instructions)
            if (i instanceof org.objectweb.asm.tree.MethodInsnNode) {
                var call = (org.objectweb.asm.tree.MethodInsnNode) i;
                if (call.owner.equals("java/lang/Math") && call.name.equals("min"))
                    bounded = restore.instructions.indexOf(call);
                if (call.name.equals("setScrollTop")) setter = restore.instructions.indexOf(call);
            }
        assertTrue("Bound the request before the engine's elastic target", bounded >= 0 && setter > bounded);
        var host = new org.objectweb.asm.tree.ClassNode();
        try (var in = getClass().getClassLoader().getResourceAsStream("xin/vanilla/banira/internal/client/ui/AuiPageHost.class")) {
            new org.objectweb.asm.ClassReader(in).accept(host, 0);
        }
        for (String action : new String[]{"pointerButton", "pointerScroll"}) {
            var callback = host.methods.stream().filter(m -> m.name.equals(action)).findFirst().orElseThrow();
            assertTrue(action + " must observe scroll intent before engine interception", java.util.stream.StreamSupport.stream(callback.instructions.spliterator(), false)
                    .anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) i).name.equals("scrollInput")));
        }
    }
}
