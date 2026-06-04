package xin.vanilla.banira.client.gui.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharInputEventTest {

    @Test
    public void exposesTypedCharacterAsText() {
        CharInputEvent event = CharInputEvent.of('竹', 2);

        assertEquals('竹', event.codePoint());
        assertEquals(2, event.modifiers());
        assertEquals("竹", event.text());
    }
}
