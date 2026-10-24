package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class UiDomCloseTest extends UiDomControlsTest {
    @Test public void closeTextKeepsOneRetainedSharedGlyphAndSemanticLabel() throws Exception {
        Dom dom = new Dom();
        Dom.Node close = dom.new Node("button"); dom.ids.put("close", close);
        NativeUiView view = dom.view();
        view.text("close", BaniraComponent.get().literal("Return without saving"));
        assertEquals("", close.text);
        assertEquals("ui-close", close.attributes.get("class"));
        assertEquals(1, close.children.size());
        Dom.Node glyph = dom.created.stream().filter(n -> "ui-close-glyph".equals(n.attributes.get("class"))).findFirst().orElseThrow();
        assertEquals(UiGameElement.Kind.SPRITE, glyph.gameElement.kind());
        assertEquals("Return without saving", close.tooltip);
        view.text("close", BaniraComponent.get().literal("Close"));
        view.gameElement("close", UiGameElement.preset(ButtonWidget.PresetStyle.CLOSE));
        assertEquals(1, close.children.size());
        assertNull(close.gameElement);
        assertEquals(UiGameElement.Kind.SPRITE, glyph.gameElement.kind());
        view.close();
    }
    @Test public void existingChildCannotReplaceTheSharedCloseGlyph() throws Exception {
        Dom dom = new Dom(); Dom.Node close = dom.new Node("button"), glyph = dom.new Node("div");
        dom.ids.put("close", close); dom.ids.put("close_icon", glyph); close.children.add(glyph.proxy);
        NativeUiView view = dom.view();
        view.gameElement("close_icon", UiGameElement.preset(ButtonWidget.PresetStyle.PLUS));
        assertEquals(UiGameElement.Kind.SPRITE, glyph.gameElement.kind());
        assertEquals(1, close.children.size());
        assertEquals("ui-close-glyph", glyph.attributes.get("class"));
        view.close();
    }
    @Test public void disabledCloseRetainsItsOriginalCallbackGate() throws Exception {
        Dom dom = new Dom(); Dom.Node close = dom.new Node("button"); dom.ids.put("close", close);
        NativeUiView view = dom.view(); AtomicInteger actions = new AtomicInteger();
        view.text("close", BaniraComponent.get().literal("Cancel")); view.onAction("close", actions::incrementAndGet);
        close.emit("click"); assertEquals(1, actions.get());
        close.focused = true; view.enabled("close", false); close.emit("click");
        assertEquals(1, actions.get()); assertFalse(close.focused);
        view.enabled("close", true); close.emit("click"); assertEquals(2, actions.get());
        view.close();
    }
    @Test public void ordinaryCancelIsNotConvertedButMarkedHeaderCancelIs() throws Exception {
        Dom dom = new Dom(); NativeUiView view = dom.view();
        view.text("cancel", BaniraComponent.get().literal("Cancel"));
        assertEquals("Cancel", dom.ids.get("cancel").text); view.close();
        dom = new Dom(); Dom.Node glyph = dom.new Node("div"); dom.ids.put("cancel_close_icon", glyph);
        view = dom.view(); view.text("cancel", BaniraComponent.get().literal("Back"));
        assertEquals("", dom.ids.get("cancel").text);
        assertEquals(UiGameElement.Kind.SPRITE, glyph.gameElement.kind()); view.close();
    }
    @Test public void sharedResourceAndConfigurationHeaderAreBounded() throws Exception {
        Path root = Path.of("src/main/resources/assets/apricityui/apricity/banira_codex");
        assertTrue(Files.readString(root.resolve("ui/config.html")).contains("id=\"brand\""));
        assertTrue(Files.readString(root.resolve("shared.css")).contains(".ui-close-glyph"));
        assertTrue(Files.size(root.resolve("icons/close.png")) < 1024);
    }
    @Test public void createdGlyphUsesOnlyAttributesAcceptedByTheRealAdapter() throws Exception {
        String adapter = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.java"));
        var whitelist = java.util.regex.Pattern.compile("Arrays.asList\\(([^)]*)\\)\\.contains\\(name\\)").matcher(adapter);
        assertTrue("Actual adapter whitelist missing", whitelist.find());
        assertFalse("The adapter contract forbids dynamic IDs", whitelist.group(1).contains("\"id\""));
        Dom dom = new Dom(); dom.rejectUnsupportedId = true;
        Dom.Node close = dom.new Node("button"); dom.ids.put("close", close); NativeUiView view = dom.view();
        try { view.text("close", BaniraComponent.get().literal("Close")); }
        catch (IllegalArgumentException failure) { fail("Close creation violated the actual adapter contract: " + failure); }
        assertEquals(1, close.children.size());
        assertEquals(UiGameElement.Kind.SPRITE, dom.created.stream().filter(n -> "ui-close-glyph".equals(n.attributes.get("class"))).findFirst().orElseThrow().gameElement.kind());
        view.close();
    }
}
