package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class QuickIconCustomRendererTest {
    @Test
    public void customIconDelegatesRenderingWithoutOptionalTypes() {
        AtomicInteger renders = new AtomicInteger();
        QuickIcon icon = QuickIcon.custom((stack, minecraft, x, y, size) ->
                renders.incrementAndGet());

        icon.render(null, 1, 2, 16);

        assertEquals(QuickIcon.Kind.CUSTOM, icon.kind());
        assertEquals(1, renders.get());
    }
}
