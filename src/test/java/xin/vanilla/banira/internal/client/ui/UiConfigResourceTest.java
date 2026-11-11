package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

public class UiConfigResourceTest {
    private Document template() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/config.html")) {
            assertNotNull(input);
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private Element id(Document document, String id) {
        var nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttribute("id"))) return node;
        }
        return null;
    }

    @Test
    public void sidebarAndFieldsHaveIndependentScrollersWithFixedSearchAndFooter() throws Exception {
        Document document = template();
        assertNotNull(id(document, "category_content"));
        assertNotNull(id(document, "categories"));
        assertNotNull(id(document, "content"));
        assertNotNull(id(document, "fields"));
        assertNotSame(id(document, "category_content"), id(document, "content"));
        for (String fixed : new String[]{"search", "save", "clear_search"}) {
            assertNotNull(fixed, id(document, fixed));
            for (Node p = id(document, fixed).getParentNode(); p instanceof Element; p = p.getParentNode()) {
                assertNotEquals("content", ((Element) p).getAttribute("id"));
                assertNotEquals("category_content", ((Element) p).getAttribute("id"));
            }
        }
    }

    @Test
    public void serverActionsAreGroupedAndDoNotRequireLongPressDiscovery() throws Exception {
        var document = template();
        assertNotNull(id(document, "server_actions"));
        for (String action : new String[]{"fetch", "sync", "sync_all"}) {
            assertNotNull(id(document, action));
            assertEquals(id(document, "server_actions"), id(document, action).getParentNode());
        }
    }


}
