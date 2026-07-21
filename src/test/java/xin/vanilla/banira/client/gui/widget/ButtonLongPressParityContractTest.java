package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ButtonLongPressParityContractTest {

    @Test
    public void usesTheSharedBurstCompletionFeedback() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/xin/vanilla/banira/client/gui/widget/ButtonWidget.java"));

        assertTrue(source.contains("LONG_PRESS_BURST_DURATION_MS"));
        assertTrue(source.contains("spawnLongPressBurst"));
        assertTrue(source.contains("LongPressBurstParticle"));
        assertFalse(source.contains("LongPressCompletionEffect"));
    }
}
