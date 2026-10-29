package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class UiSelectorRowLayoutTest {
    private final Map<String, byte[]> resources = UiBoundaryChecks.ownResources("banira_codex");

    public UiSelectorRowLayoutTest() throws Exception {
    }

    private String rule(String selector) {
        String css = new String(resources.get("shared.css"), StandardCharsets.UTF_8)
                + new String(resources.get("ui/common-pages.css"), StandardCharsets.UTF_8);
        var match = Pattern.compile(Pattern.quote(selector) + "\\{([^}]+)}").matcher(css);
        assertTrue("Missing selector " + selector, match.find());
        return match.group(1);
    }

    @Test
    public void selectionButtonCoversTheFullVirtualRow() {
        String action = rule(".common-selector .common-choice>.row-action");
        assertTrue(action.contains("height:40px"));
        assertTrue(action.contains("max-height:40px"));
        assertTrue(action.contains("border:0"));
        assertTrue(action.contains("border-radius:0"));
    }

    @Test
    public void registryIdAndIconDoNotBlockTheSelectionButton() {
        assertTrue(rule(".common-selector .common-choice>.label").contains("pointer-events:none"));
        assertTrue(rule(".common-selector .common-choice>.label").contains("position:absolute"));
        String shared = new String(resources.get("shared.css"), StandardCharsets.UTF_8);
        assertTrue(shared.contains(".game-icon{width:24px;height:24px;flex-shrink:0;pointer-events:none}"));
    }

    @Test
    public void allSelectorSelectionsShareTheirCommandFooter() throws Exception {
        for (String kind : new String[]{"item", "effect", "advancement"}) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(resources.get("ui/" + kind + "-select.html")));
            Element selection = null, submit = null;
            var nodes = document.getElementsByTagName("*");
            for (int index = 0; index < nodes.getLength(); index++) {
                Element node = (Element) nodes.item(index);
                if ("selection".equals(node.getAttribute("id"))) selection = node;
                if ("submit".equals(node.getAttribute("id"))) submit = node;
            }
            assertNotNull(selection);
            assertNotNull(submit);
            assertSame(kind, submit.getParentNode(), selection.getParentNode().getParentNode());
        }
    }

    @Test
    public void footerTextCanShrinkWithoutWrappingTheCommands() {
        assertTrue(rule(".selector-page .config-footer.selector-tools").contains("flex-wrap:nowrap"));
        String selection = rule(".selector-selection");
        assertTrue(selection.contains("min-width:0"));
        assertTrue(selection.contains("flex:1"));
    }
}
