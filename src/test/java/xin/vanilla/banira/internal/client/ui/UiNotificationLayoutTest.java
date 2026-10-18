package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiNotificationLayoutTest {
    @Test public void historyKeepsIndependentListAndDetailViewportsAndLiteralText() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/notification-log.html")) {
            assertNotNull("Missing notification page", in);
            var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var doc = factory.newDocumentBuilder().parse(in);
            var nodes = doc.getElementsByTagName("div"); var ids = new java.util.HashMap<String, org.w3c.dom.Element>();
            for (int i=0; i<nodes.getLength(); i++) { var node=(org.w3c.dom.Element) nodes.item(i); ids.put(node.getAttribute("id"),node); }
            assertNotNull(ids.get("content")); assertNotNull(ids.get("detail")); assertNotNull(ids.get("text"));
            assertSame(ids.get("detail"), ids.get("text").getParentNode());
            assertNotSame(ids.get("content"), ids.get("detail").getParentNode());
        }
    }
}
