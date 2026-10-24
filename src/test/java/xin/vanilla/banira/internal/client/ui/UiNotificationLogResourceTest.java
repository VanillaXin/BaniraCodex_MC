package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiNotificationLogResourceTest {
    private String resource(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/" + name)) {
            assertNotNull(name, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @Test public void approvedHistoryHasRealControlsAndIndependentDetail() throws Exception {
        String html = resource("notification-log.html");
        for (String id : new String[]{"brand", "title", "close", "search", "clear_search", "mark_all_read", "types", "position",
                "content", "fields", "detail", "text", "meta_source", "meta_type", "meta_style", "meta_position",
                "meta_animation", "meta_duration", "detail_time", "read_status", "list_count", "unread_count",
                "empty", "select_hint", "back", "footer_close"}) assertTrue(id, html.contains("id=\"" + id + "\""));
        assertTrue(html.contains("href=\"notification-log.css\""));
        assertFalse(html.contains("<script"));
    }
    @Test public void scopedStyleFitsExistingBudgetAndFixedVirtualRow() throws Exception {
        String css = resource("notification-log.css");
        assertTrue(css.getBytes(StandardCharsets.UTF_8).length <= 2048);
        assertTrue(css.contains(".notification-page"));
        assertTrue(css.contains("height:48px"));
        assertFalse(css.contains("@font-face"));
        assertEquals(java.util.List.of(), UiResourceBudgetTest.violations(UiBoundaryChecks.ownResources("banira_codex"), true));
    }
}
