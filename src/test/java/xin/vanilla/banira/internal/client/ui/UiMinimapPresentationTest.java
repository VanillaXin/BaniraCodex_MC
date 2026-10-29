package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class UiMinimapPresentationTest {
    private MinimapDomPresentation rows() {
        UiDomAccess dom = (UiDomAccess) Proxy.newProxyInstance(UiDomAccess.class.getClassLoader(),
                new Class[]{UiDomAccess.class}, (self, method, args) -> {
                    throw new AssertionError("Native minimap rows must not access DOM: " + method.getName());
                });
        return new MinimapDomPresentation(dom);
    }

    private void rejects(MinimapDomPresentation rows, String key, String text) {
        try {
            rows.select(key, text, -1, true);
            fail("Minimap AUI presentation is unsupported");
        } catch (UnsupportedOperationException expected) {
        }
        assertNull(rows.measure());
    }

    @Test
    public void unreadCountdownAndThirdPartyRowsCannotCreateDom() {
        var rows = rows();
        rejects(rows, "banira_codex:unread", "2 unread");
        rejects(rows, "aotake_sweep:countdown", "02:34");
        rejects(rows, "test:third_party", "<b>literal</b>");
        rows.close();
    }

    @Test
    public void measureAndRepeatedCloseNeverAccessDomOrRestorePresentation() {
        var rows = rows();
        assertNull(rows.measure());
        rows.close();
        rows.close();
        rejects(rows, "countdown", "02:33");
    }
}
