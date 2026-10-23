package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import static org.junit.Assert.*;

public class UiConfigLayoutTest {
    @Test public void searchIsOutsideBothScrollableTitleAndConfigurationList() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/config.html")) {
            assertNotNull(input);
            var document = factory.newDocumentBuilder().parse(input);
            var elements = document.getElementsByTagName("input");
            Element search = null;
            for (int i = 0; i < elements.getLength(); i++) if (((Element) elements.item(i)).getAttribute("id").equals("search")) {
                search = (Element) elements.item(i);
            }
            assertNotNull(search);
            Element parent = (Element) search.getParentNode();
            assertEquals("search", parent.getAttribute("class"));
            assertEquals("config-main", ((Element) parent.getParentNode()).getAttribute("class"));
            for (org.w3c.dom.Node node = parent; node instanceof Element; node = node.getParentNode()) {
                assertNotEquals("content", ((Element) node).getAttribute("id"));
                assertNotEquals("category_content", ((Element) node).getAttribute("id"));
            }
        }
    }
}
