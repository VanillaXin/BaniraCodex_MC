package xin.vanilla.banira.internal.client.ui;

import net.minecraft.util.ResourceLocation;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiHudFrame;

import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiProgressCustomizationTest {
    private UiDomControlsTest.Dom dom() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("progress", dom.new Node("root"));
        return dom;
    }

    private UiHudFrame frame(String time, double x) {
        return new UiHudFrame(com.google.common.collect.ImmutableList.of(
                new UiHudFrame.Sprite("cleanup-leaf", new ResourceLocation("aotake_sweep", "gui/leaf.png"), x, 40, 9, 10, 0),
                new UiHudFrame.Text("cleanup-time", time, 50, 60, 30, 9, 9, .5, 0, 15, 0, 0xff5da530)));
    }

    @Test
    public void stableIdsAndClassesExposeOnlyFiniteHudParts() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
        ui.update(frame("01:00", 30));
        assertEquals("cleanup-leaf", dom.created.get(0).attributes.get("id"));
        assertEquals("progress-sprite", dom.created.get(0).attributes.get("class"));
        assertEquals("cleanup-time", dom.created.get(1).attributes.get("id"));
        assertEquals("progress-text", dom.created.get(1).attributes.get("class"));
        assertEquals("progress-shadow", dom.created.get(2).attributes.get("class"));
        assertEquals("progress-value", dom.created.get(3).attributes.get("class"));
    }

    @Test
    public void layoutUpdatesNeverReplaceAuthoredInlineAppearance() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
        ui.update(frame("01:00", 30));
        String custom = "color:#abcdef;background:#182a20;font-family:serif;transform:scale(2)";
        for (lombok.var node : dom.created) node.attributes.put("style", custom);
        ui.update(frame("00:59", 31));
        for (lombok.var node : dom.created)
            assertEquals("Dynamic data must preserve authored style", custom, node.attributes.get("style"));
        assertEquals(4, dom.created.size());
    }

    @Test
    public void staticFrameIsWriteFreeAndTextRemainsLiteral() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access, Function.identity());
        lombok.var frame = frame("<b>00:59</b>", 30);
        ui.update(frame);
        int writes = dom.created.stream().mapToInt(n -> n.writes).sum();
        ui.update(frame);
        assertEquals(writes, dom.created.stream().mapToInt(n -> n.writes).sum());
        assertEquals("<b>00:59</b>", dom.created.get(3).text);
        ui.close();
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }
}
