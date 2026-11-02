package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.enums.EnumSeason;

import static org.junit.Assert.*;

public class UiQuickActionHudPresentationTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        new UiDomControlsTest().platform();
    }

    private QuickActionHudFrame frame() {
        lombok.var frame = new QuickActionHudFrame();
        frame.add(QuickActionHudFrame.Kind.MENU, 10, 20, 130, 90, null, "",
                BaniraColorConfig.forSeason(EnumSeason.SPRING), false, false, 400);
        frame.add(QuickActionHudFrame.Kind.ROW, 30, 50, 100, 12,
                new ScreenCoordinate(11, 40, 128, 60), "Literal <tag> \u6a31\u82b1\u7b7e",
                BaniraColorConfig.forSeason(EnumSeason.SPRING), true, false, 400);
        return frame;
    }

    @Test
    public void realTextIsLiteralAndNoDomInputHandlerCanDuplicateInventoryActions() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        lombok.var p = new QuickActionHudPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        p.update(frame());
        assertEquals(4, dom.created.size());
        assertTrue(dom.created.stream().anyMatch(node -> "Literal <tag> \u6a31\u82b1\u7b7e".equals(node.text)));
        assertTrue(dom.created.stream().allMatch(node -> node.events.isEmpty()));
        assertTrue(dom.created.stream().allMatch(node -> node.attributes.get("style").contains("pointer-events:none")));
    }

    @Test
    public void stableFramesDoNotRewriteDomAndNullFrameClearsAllNodes() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        lombok.var p = new QuickActionHudPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        p.update(frame());
        int writes = dom.created.stream().mapToInt(n -> n.writes).sum();
        p.update(frame());
        assertEquals(writes, dom.created.stream().mapToInt(n -> n.writes).sum());
        p.update(null);
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
        p.close();
        p.close();
        p.update(frame());
        assertEquals(4, dom.created.size());
    }

    @Test
    public void projectedFontClipAndRowCoordinatesRemainInTheSameSpace() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        lombok.var p = new QuickActionHudPresentation((UiDomAccess) dom.access,
                b -> new ScreenCoordinate(b.x() * 2, b.y() * 2, b.width() * 2, b.height() * 2));
        p.update(frame());
        assertTrue(dom.created.stream().anyMatch(n -> n.attributes.get("style").contains("left:22.0px;top:80.0px;width:256.0px;height:120.0px;overflow:hidden")));
        assertTrue(dom.created.stream().anyMatch(n -> n.attributes.get("style").contains("left:38.0px;top:20.0px;width:200.0px;height:24.0px;")));
        assertTrue(dom.created.stream().anyMatch(n -> n.attributes.get("style").contains("font-size:14.0px;line-height:24.0px")));
    }

    @Test
    public void expiredDomDoesNotCreateOrRemoveNodesUntilOwnedClose() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        lombok.var p = new QuickActionHudPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        p.update(frame());
        dom.current = false;
        p.update(null);
        assertTrue(dom.created.stream().noneMatch(n -> n.removed));
        p.close();
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }

    @Test
    public void slotEmptyHintAndAccentDoNotReplaceTheRealIcon() throws Exception {
        lombok.var frame = new QuickActionHudFrame();
        frame.add(QuickActionHudFrame.Kind.SLOT, 4, 4, 16, 16, null, "",
                BaniraColorConfig.forSeason(EnumSeason.SPRING), true, true, 0);
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        new QuickActionHudPresentation((UiDomAccess) dom.access, java.util.function.Function.identity()).update(frame);
        assertTrue(dom.created.get(1).attributes.get("style").contains("background:transparent;border:1.0px dashed"));
        assertTrue(dom.created.get(1).text.isEmpty());
    }

    @Test
    public void semanticFrameDoesNotDuplicateLegacyBoxesInAui() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        lombok.var p = new HudDomPresentation((UiDomAccess) dom.access);
        lombok.var frame = new HudDrawList();
        frame.box(0, 0, 10, 10, -1, 0, 0);
        frame.quickActions(frame());
        p.update(frame);
        assertEquals(4, dom.created.size());
        p.update(new HudDrawList());
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }

    @Test
    public void geometryRejectsNonFiniteValuesAndKeepsOriginalThemeColors() {
        lombok.var frame = frame();
        assertEquals(2, frame.elements().size());
        assertEquals(BaniraColorConfig.forSeason(EnumSeason.SPRING).accentHover() | 0xff000000, frame.elements().get(0).accent());
        try {
            frame.add(QuickActionHudFrame.Kind.MENU, Double.NaN, 0, 1, 1, null, "",
                    BaniraColorConfig.forSeason(EnumSeason.SPRING), false, false, 0);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(2, frame.elements().size());
        }
    }

    @Test
    public void pointerStillUsesExistingAuiBoxesAndPressFeedbackWithoutANewNativeCursor() throws Exception {
        lombok.var theme = BaniraColorConfig.forSeason(EnumSeason.SPRING);
        lombok.var pointer = new UiPointerState();
        lombok.var before = pointer.frame(theme, 20, 30, 700);
        pointer.button(0, true);
        lombok.var after = pointer.frame(theme, 20, 30, 700);
        assertEquals(before.parts().size(), after.parts().size());
        assertNull(after.quickActions());
        assertTrue(after.parts().stream().allMatch(part -> part.kind == HudDrawList.Kind.BOX));
        assertTrue(after.parts().stream().anyMatch(part -> part.color == theme.cursorLightPressed()));
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("overlay", dom.new Node("div"));
        new HudDomPresentation((UiDomAccess) dom.access).update(after);
        assertEquals(after.parts().size() * 2, dom.created.size());
        assertTrue(dom.created.stream().allMatch(node -> node.events.isEmpty()));
    }

    @Test
    public void lowerIconCannotPaintThroughAnOpaqueMenu() {
        lombok.var frame = frame();
        lombok.var icon = new ScreenCoordinate(20, 30, 10, 10);
        assertTrue(frame.occludes(icon, 0));
        assertTrue(frame.uncovered(icon, 0).isEmpty());
        assertFalse(frame.occludes(icon, 400));
        assertEquals(1, frame.uncovered(icon, 400).size());
    }

    @Test
    public void partialOverlapPreservesExactlyTheUncoveredPixels() {
        lombok.var frame = frame();
        lombok.var icon = new ScreenCoordinate(5, 15, 10, 10);
        lombok.var regions = frame.uncovered(icon, 0);
        assertEquals(75, regions.stream().mapToDouble(r -> r.width() * r.height()).sum(), 0);
        assertTrue(regions.stream().allMatch(r -> r.width() > 0 && r.height() > 0));
    }

    @Test
    public void nonOverlappingAndHigherMenuIconsKeepTheirOriginalClip() {
        lombok.var frame = frame();
        lombok.var icon = new ScreenCoordinate(0, 0, 5, 5);
        assertFalse(frame.occludes(icon, 0));
        lombok.var clip = new ScreenCoordinate(0, 0, 400, 240);
        assertEquals(400 * 240, frame.uncovered(clip, 401).stream().mapToDouble(r -> r.width() * r.height()).sum(), 0);
    }
}
