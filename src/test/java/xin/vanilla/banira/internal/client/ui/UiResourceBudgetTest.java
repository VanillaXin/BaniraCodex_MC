package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiResourceBudgetTest {
    static java.util.List<String> violations(java.util.Map<String, byte[]> files, boolean shared) { return UiBoundaryChecks.resources(files, shared); }
    @Test public void everyOwnedUiResourceIsBudgeted() throws Exception {
        assertEquals(java.util.List.of(), violations(UiBoundaryChecks.ownResources("banira_codex"), true));
    }
    @Test public void injectedRemoteScriptsFontsAndOversizedResourcesAreRejected() {
        for (var fixture : java.util.List.of(
                java.util.Map.of("page.html", "<script src='https://example.invalid/a.js'></script>".getBytes(StandardCharsets.UTF_8)),
                java.util.Map.of("large.html", new byte[12289]),
                java.util.Map.of("large.css", new byte[16385]),
                java.util.Map.of("font.woff", new byte[1]),
                java.util.Map.of("large.png", new byte[65537]),
                java.util.Map.of("large.js", new byte[8193]))) {
            assertFalse("Unsafe resource accepted", violations(fixture, true).isEmpty());
        }
    }
    @Test public void downstreamCannotDuplicateSharedStylesOrExceedItsCssBudget() {
        assertFalse(violations(java.util.Map.of("shared.css", new byte[1]), false).isEmpty());
        assertFalse(violations(java.util.Map.of("mod.css", new byte[4097]), false).isEmpty());
    }
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
                "ui/notification-log.html", "ui/notification-types.html", "ui/hud-position.html",
                "ui/item-select.html", "ui/effect-select.html", "ui/advancement-select.html", "ui/debug.html", "hud/overlay.html", "hud/minimap.html"}) {
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
