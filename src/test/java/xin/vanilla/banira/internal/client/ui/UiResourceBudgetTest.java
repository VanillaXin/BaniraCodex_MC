package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiResourceBudgetTest {
    @Test public void listHeaderCommandsFitTheFixedVirtualRow() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            String css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            java.util.regex.Matcher row = java.util.regex.Pattern.compile("(?s)\\.row\\s*\\{([^}]+)}").matcher(css);
            assertTrue(row.find());
            int gap = pixels(row.group(1), "gap");
            int padding = pixels(row.group(1), "padding-bottom");
            assertTrue("Two header command lines must fit a 48px virtual row", 20 + 20 + gap + padding <= 48);
            assertTrue("Long command labels need a bounded line and their full tooltip", css.matches("(?s).*\\.row > button\\s*\\{[^}]*max-height:\\s*20px[^}]*white-space:\\s*nowrap[^}]*}.*"));
        }
    }
    private static int pixels(String rule, String property) {
        var match = java.util.regex.Pattern.compile("(?:^|;)\\s*" + property + ":\\s*(\\d+)px").matcher(rule);
        assertTrue(property, match.find()); return Integer.parseInt(match.group(1));
    }
    @Test public void longTitlesHaveBoundedHeaderHeight() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            assertNotNull(stream);
            String css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("Long headers must leave room for content and footer", css.matches("(?s).*\\.header\\s*\\{[^}]*max-height:\\s*\\d+px[^}]*}.*"));
        }
    }
    @Test
    public void sharedResourcesStayLocalSmallAndScriptFree() throws Exception {
        int total = 0;
        for (String name : new String[]{"shared.css", "ui/form.html", "ui/confirm.html", "ui/text.html",
                "ui/config.html", "ui/navigation.html", "ui/player-config.html", "ui/quick-actions.html", "ui/quick-steps.html",
                "ui/notification-log.html", "ui/notification-types.html", "ui/hud-position.html"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/" + name)) {
                assertNotNull("Missing shared UI resource: " + name, stream);
                byte[] bytes = stream.readAllBytes();
                total += bytes.length;
                assertTrue(name, bytes.length <= (name.endsWith("css") ? 16384 : 12288));
                String content = new String(bytes, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
                assertFalse(content.contains("<script"));
                assertFalse(content.contains("https://"));
                assertFalse(content.contains("http://"));
                assertFalse(content.contains("@font-face"));
            }
        }
        assertTrue(total <= 262144);
    }
}
