package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import static org.junit.Assert.*;

public class UiMinimapPresentationTest {
    static class Port {
        final List<Cell> cells = new ArrayList<>();
        boolean current = true;
        int writes, listeners;
        final UiDomAccess.Node root = cell(false).node;
        final UiDomAccess dom = (UiDomAccess) Proxy.newProxyInstance(UiDomAccess.class.getClassLoader(), new Class[]{UiDomAccess.class}, (self, m, a) -> {
            switch (m.getName()) {
                case "current": return current;
                case "find": assertEquals("rows", a[0]); return root;
                case "create": return cell(true).node;
                default: throw new AssertionError("Unexpected DOM access " + m.getName());
            }
        });
        Cell cell(boolean keep) { Cell c = new Cell(); if (keep) cells.add(c); return c; }
        class Cell {
            String text = "", style = "";
            int textWrites;
            boolean removed;
            final UiDomAccess.Node node = (UiDomAccess.Node) Proxy.newProxyInstance(UiDomAccess.Node.class.getClassLoader(), new Class[]{UiDomAccess.Node.class}, (self, m, a) -> {
                switch (m.getName()) {
                    case "text": text = (String)a[0]; textWrites++; writes++; return null;
                    case "attribute": assertEquals("style", a[0]); style = (String)a[1]; writes++; return null;
                    case "append": return null;
                    case "remove": removed = true; return null;
                    case "bounds": return new ScreenCoordinate(0, 0, Math.max(1, text.length() * 6), 9);
                    case "on": listeners++; throw new AssertionError("Map text must not register input handlers");
                    default: throw new AssertionError("Unexpected node access " + m.getName());
                }
            });
        }
    }
    @Test public void unchangedCountdownDoesNotRewriteTextOrStyle() {
        Port p = new Port(); var rows = new MinimapDomPresentation(p.dom);
        rows.select("countdown", "10s", -1, false); int writes = p.writes;
        rows.select("countdown", "10s", -1, false); assertEquals(writes, p.writes);
        assertEquals(1, p.cells.size()); assertEquals(1, p.cells.get(0).textWrites);
        assertEquals(18, rows.measure().width(), .001); assertEquals(0, p.listeners);
        rows.close();
    }
    @Test public void twoCallbacksUseOneVisibleCachedRowWithoutRewritingTheirValues() {
        Port p = new Port(); var rows = new MinimapDomPresentation(p.dom);
        rows.select("unread", "2 unread", -1, true); rows.select("countdown", "10s", -1, true);
        rows.select("unread", "2 unread", -1, true); rows.select("countdown", "10s", -1, true);
        assertEquals(2, p.cells.size()); assertTrue(p.cells.get(0).style.contains("visibility:hidden"));
        assertTrue(p.cells.get(1).style.contains("visibility:visible"));
        assertTrue(p.cells.stream().allMatch(c -> c.textWrites == 1 && c.style.contains("pointer-events:none")));
        rows.close(); rows.close(); assertTrue(p.cells.stream().allMatch(c -> c.removed));
    }
    @Test public void detachedAndClosedViewsPerformNoUpdates() {
        Port p = new Port(); var rows = new MinimapDomPresentation(p.dom); rows.select("a", "value", -1, false);
        p.current = false; int writes = p.writes;
        try { rows.select("a", "changed", -1, false); fail(); } catch (IllegalStateException expected) { }
        assertEquals(writes, p.writes); rows.close();
        p.current = true;
        try { rows.select("a", "changed", -1, false); fail(); } catch (IllegalStateException expected) { }
        assertEquals(writes, p.writes);
    }
    @Test public void rowBudgetFailsBeforeAllocatingAnExtraNode() {
        Port p = new Port(); var rows = new MinimapDomPresentation(p.dom);
        for (int i = 0; i < 120; i++) rows.select("row" + i, "value", -1, false);
        try { rows.select("overflow", "value", -1, false); fail(); } catch (IllegalStateException expected) { }
        assertEquals(120, p.cells.size()); rows.close();
    }
}
