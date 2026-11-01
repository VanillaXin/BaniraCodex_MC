package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.function.Function;

import static org.junit.Assert.*;

public class UiHudPresentationTest {
    private UiDomControlsTest.Dom dom() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        return dom;
    }

    private HudDrawList frame() {
        HudDrawList frame = new HudDrawList();
        frame.box(10, 20, 30, 40, 0xff123456, 2, 0);
        return frame;
    }

    @Test
    public void identicalFramesDoNotRewriteDomAndClosedViewRejectsUpdates() throws Exception {
        var dom = dom();
        var presentation = new HudDomPresentation((UiDomAccess) dom.access);
        presentation.update(frame());
        assertEquals(2, dom.created.size());
        int writes = dom.created.get(0).writes;
        presentation.update(frame());
        assertEquals(writes, dom.created.get(0).writes);
        dom.current = false;
        presentation.update(new HudDrawList());
        assertFalse(dom.created.get(0).removed);
        presentation.close();
        presentation.close();
        assertTrue(dom.created.get(0).removed);
        dom.current = true;
        presentation.update(frame());
        assertEquals(2, dom.created.size());
        assertTrue(dom.created.get(0).events.isEmpty());
    }

    @Test
    public void shrinkingFrameRemovesNodesWithoutLeavingStaleChrome() throws Exception {
        var dom = dom();
        var presentation = new HudDomPresentation((UiDomAccess) dom.access);
        presentation.update(frame());
        presentation.update(new HudDrawList());
        assertTrue(dom.created.get(0).removed);
        presentation.close();
    }

    @Test
    public void nativeMaximumGridDoesNotInheritAuiNodeBudget() {
        HudDrawList frame = new HudDrawList();
        try {
            for (int i = 0; i < 2304; i++) frame.box(i, 0, 1, 1, -1, 0, 0);
        } catch (IllegalStateException failure) {
            throw new AssertionError("Native 16x16 editable grid must retain all primitives", failure);
        }
        assertEquals(2304, frame.parts().size());
    }

    @Test
    public void auiBudgetRejectsWholeFrameBeforeCreatingAnyNodes() throws Exception {
        var dom = dom();
        var presentation = new HudDomPresentation((UiDomAccess) dom.access);
        HudDrawList frame = new HudDrawList();
        try {
            for (int i = 0; i < 600; i++) frame.box(i, 0, 1, 1, -1, 0, 0);
        } catch (IllegalStateException failure) {
            throw new AssertionError("Collection must remain native-safe", failure);
        }
        try {
            presentation.update(frame);
            fail("Oversized AUI HUD should return to native");
        } catch (IllegalStateException expected) {
            assertEquals(0, dom.created.size());
        }
    }

    @Test
    public void guiCoordinatesAreProjectedBeforeDomLayout() throws Exception {
        var dom = dom();
        Function<ScreenCoordinate, ScreenCoordinate> projection = b -> new ScreenCoordinate(b.x() * 2, b.y() * 2, b.width() * 2, b.height() * 2);
        java.lang.reflect.Constructor<?> ctor = java.util.Arrays.stream(HudDomPresentation.class.getConstructors()).filter(c -> c.getParameterCount() == 2).findFirst().orElse(null);
        assertNotNull("Persistent HUD needs GUI-to-document projection", ctor);
        HudDomPresentation presentation = (HudDomPresentation) ctor.newInstance(dom.access, projection);
        presentation.update(frame());
        String style = dom.created.get(0).attributes.get("style");
        assertTrue(style, style.contains("left: 20.0px"));
        assertTrue(style, style.contains("width: 60.0px"));
        assertTrue("Radius must remain two GUI pixels after projection", style.contains("border-radius: 4.0px"));
        presentation.close();
    }

    @Test
    public void middleEdgeScaleOriginsStayAtTheirOriginalAnchors() throws Exception {
        var method = HudDomPresentation.class.getDeclaredMethod("origin", String.class);
        method.setAccessible(true);
        assertEquals("0% 50%", method.invoke(null, "LEFT_CENTER"));
        assertEquals("100% 50%", method.invoke(null, "RIGHT_CENTER"));
        assertEquals("50% 0%", method.invoke(null, "TOP_CENTER"));
        assertEquals("50% 100%", method.invoke(null, "BOTTOM_CENTER"));
    }

    @Test
    public void regionClipStaysOutsideTheAnimatedNotificationTransform() throws Exception {
        var dom = dom();
        var presentation = new HudDomPresentation((UiDomAccess) dom.access);
        HudDrawList frame = new HudDrawList();
        frame.clip(40, 30, 200, 100);
        frame.box(50, 40, 80, 30, -1, 2, 0);
        presentation.update(frame);
        String chrome = dom.created.get(0).attributes.get("style"), mask = dom.created.get(1).attributes.get("style");
        assertTrue(chrome, chrome.contains("left: 10.0px"));
        assertTrue(chrome, chrome.contains("top: 10.0px"));
        assertTrue(mask, mask.contains("left:40.0px"));
        assertTrue(mask, mask.contains("overflow:hidden"));
        assertFalse(mask.contains("transform:"));
        presentation.close();
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }
}
