package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import static org.junit.Assert.*;

public class UiAliasReplacementTest {
    private UiPageSpec page(String id) {
        return new UiPageSpec("banira_codex:" + id, "banira_codex/ui/form.html", new UiPageSession() {
            public void bind(UiView view) { }
            public void detach(UiDetachReason reason) { }
            public void close(UiCloseReason reason) { }
        }, previous -> new Object());
    }
    @Test public void replacementCanReturnToAnAlreadyParkedAncestorAlias() {
        var f = new UiNativeAliasTest.Fixture();
        f.host.open(page("child"), f.alias);
        f.host.suspendScreen(f.host.view());
        f.host.open(page("replacement"), f.alias);
        f.host.close(UiCloseReason.USER_CLOSE);
        assertSame("Replacing an editor must preserve its parked ancestor", f.page, f.host.page());
        assertEquals(42, f.host.view().state().rowPosition(), 0);
        f.host.disconnect();
    }
}
