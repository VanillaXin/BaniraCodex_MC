package xin.vanilla.banira.client.gui.search;

import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigSearchQueryTest {

    @Test
    public void queryIgnoresOuterWhitespaceAndCase() {
        ConfigSearchQuery query = ConfigSearchQuery.of("  TelePort  ");

        assertTrue(query.matches("base.teleport.request"));
        assertTrue(query.matches("unrelated", "Teleport request description"));
        assertFalse(query.matches("language", "Notification delivery"));
    }

    @Test
    public void emptyQueryKeepsEveryEntryVisible() {
        assertTrue(ConfigSearchQuery.of("  ").matches((String[]) null));
        assertTrue(ConfigSearchQuery.of("").matches("anything"));
    }

    @Test
    public void highlighterMarksEveryOccurrenceWithoutChangingText() {
        Text text = ConfigSearchText.highlight(
                "help header and help footer", ConfigSearchQuery.of("help"),
                0xFF202020, 0xFFCC5500, false);
        Component component = text.toComponent();
        List<Component> children = component.getChildren();

        assertEquals(0xCC5500, component.color().rgb());
        assertTrue(component.bold());
        assertTrue(component.underlined());
        StringBuilder rendered = new StringBuilder();
        for (Component child : children) {
            rendered.append(child.text());
        }
        assertEquals("help header and help footer", rendered.toString());
        assertEquals(4, children.size());
        assertEquals(0xCC5500, children.get(0).color().rgb());
        assertEquals(0xCC5500, children.get(2).color().rgb());
        assertTrue(children.get(0).bold());
        assertTrue(children.get(0).underlined());
        assertTrue(children.get(2).bold());
        assertTrue(children.get(2).underlined());
        assertFalse(children.get(1).bold());
        assertFalse(children.get(1).underlined());
        assertFalse(children.get(3).bold());
        assertFalse(children.get(3).underlined());
    }

    @Test
    public void themeDerivesSearchColorFromSemanticAccents() {
        BaniraColorConfig light = new BaniraColorConfig()
                .bgSurface(0xFFF8F8F8).accentPressed(0xFF345678).accentHover(0xFFABCDEF);
        BaniraColorConfig dark = new BaniraColorConfig()
                .bgSurface(0xFF101010).accentPressed(0xFF345678).accentHover(0xFFABCDEF);
        BaniraColorConfig lowContrastLight = new BaniraColorConfig()
                .bgSurface(0xFFF0FFF0).accentPressed(0xFF5AB85A).accentHover(0xFFB8E8B8);

        assertEquals(0xFF345678, light.searchMatchText());
        assertEquals(0xFFABCDEF, dark.searchMatchText());
        assertEquals(ColorUtils.ensureReadableTextArgb(0xFF5AB85A, 0xFFF0FFF0),
                lowContrastLight.searchMatchText());
        assertFalse(lowContrastLight.searchMatchText() == 0xFF5AB85A);
    }
}
