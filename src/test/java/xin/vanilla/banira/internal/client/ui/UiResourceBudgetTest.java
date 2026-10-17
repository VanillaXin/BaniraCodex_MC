package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiResourceBudgetTest {
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
        for (String name : new String[]{"shared.css", "ui/form.html", "ui/confirm.html", "ui/text.html"}) {
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
