package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class UiPlayerConfigEditorResourceTest {
    @Test
    public void compactFieldOffsetsDoNotMoveNavigationCountsOrOverrideUnits() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/player-config-editor.css")) {
            assertNotNull(input);
            String css = new String(com.google.common.io.ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
            assertFalse("Field key offset also moves the sidebar count", css.contains(".player-config-page .row>.paragraph"));
            int key = css.indexOf(".player-config-page .player-config-field>.paragraph");
            int unit = css.indexOf(".player-config-page #content .player-config-field>.field-unit");
            assertTrue("Specific unit placement must follow the shared paragraph offset", key >= 0 && unit > key);
        }
    }

    @Test
    public void approvedEditorHasFixedActionsAndIndependentScrollers() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/player-config-editor.html")) {
            assertNotNull("Approved personal editor template missing", input);
            lombok.var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            lombok.var document = factory.newDocumentBuilder().parse(input);
            lombok.var ids = new java.util.HashMap<String, Element>();
            lombok.var all = document.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                if (element.hasAttribute("id"))
                    assertNull("Duplicate id", ids.put(element.getAttribute("id"), element));
            }
            for (String id : new String[]{"brand", "title", "module_name", "origin", "category_title", "category_count", "categories", "search", "clear_search", "breadcrumb", "entry_count", "content", "fields", "empty", "status", "range", "error", "close", "cancel", "submit"})
                assertNotNull(id, ids.get(id));
            for (String id : new String[]{"search", "submit", "cancel", "close"})
                for (org.w3c.dom.Node p = ids.get(id).getParentNode(); p instanceof Element; p = p.getParentNode())
                    assertNotEquals("content", ((Element) p).getAttribute("id"));
            for (String forbidden : new String[]{"reset", "fetch", "sync", "sync_all", "filename"})
                assertNull(forbidden, ids.get(forbidden));
        }
    }

    @Test
    public void optionalEditorReusesSharedConfigStylesWithinOriginalBudget() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/player-config-editor.html")) {
            assertNotNull("Approved personal editor template missing", input);
            String html = new String(com.google.common.io.ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
            assertTrue(html.contains("href=\"config.css\""));
            assertTrue(html.contains("../shared.css"));
            assertFalse(html.contains("<script"));
        }
        try (java.io.InputStream input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/player-config-editor.css")) {
            assertNotNull(input);
            byte[] bytes = com.google.common.io.ByteStreams.toByteArray(input);
            assertTrue(bytes.length <= 2048);
            assertTrue(new String(bytes, StandardCharsets.UTF_8).contains(".player-config-page"));
        }
        assertTrue(UiBoundaryChecks.resources(UiBoundaryChecks.ownResources("banira_codex"), true).isEmpty());
    }
}
