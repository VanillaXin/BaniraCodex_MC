package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class UiPlayerConfigEditorResourceTest {


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


}
