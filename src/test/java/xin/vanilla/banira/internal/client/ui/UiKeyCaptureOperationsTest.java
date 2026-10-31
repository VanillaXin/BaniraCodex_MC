package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UiKeyCaptureOperationsTest {
    @Test
    public void chordReleaseOrdersAndRepeatProduceExactlyOneCompletedChord() {
        for (int modifier : new int[]{340, 341, 342, 343, 344, 345, 346, 347}) {
            for (boolean modifierFirst : new boolean[]{false, true}) {
                UiKeyCapture capture = new UiKeyCapture();
                assertNull(capture.press(modifier));
                assertNull(capture.press(75));
                assertNull(capture.press(75));
                assertNull(capture.release(modifierFirst ? modifier : 75));
                List<String> result = capture.release(modifierFirst ? 75 : modifier);
                assertNotNull(result);
                assertEquals(2, result.size());
                assertTrue(result.contains("K"));
                assertNull(capture.release(75));
                assertNull(capture.release(modifier));
                assertEquals("", capture.preview());
            }
        }
    }

    @Test
    public void clearCancelAndModifierOnlyReleaseCannotContaminateNextCapture() {
        for (int cancel : new int[]{256, 259, 261}) {
            UiKeyCapture capture = new UiKeyCapture();
            capture.press(341);
            capture.press(75);
            List<String> value = capture.press(cancel);
            if (cancel == 256) assertNull(value);
            else assertEquals(com.google.common.collect.ImmutableList.of(), value);
            assertNull(capture.release(341));
            assertNull(capture.release(75));
            capture.press(258);
            assertEquals(com.google.common.collect.ImmutableList.of("Tab"), capture.release(258));
        }
        UiKeyCapture capture = new UiKeyCapture();
        capture.press(341);
        capture.press(340);
        assertNull(capture.release(341));
        assertNull(capture.release(340));
        capture.press(298);
        assertEquals(com.google.common.collect.ImmutableList.of("F9"), capture.release(298));
    }
}
