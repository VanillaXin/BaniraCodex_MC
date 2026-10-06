package xin.vanilla.banira.client.notification;

import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import static org.junit.Assert.*;

public class NotificationHudIconTest {
    @Test
    public void filledEnvelopeIsCompactAndSeparatesPaperFromThemedEdge() throws Exception {
        try (InputStream paperInput = getClass().getResourceAsStream("/assets/banira_codex/textures/gui/unread_message.png");
             InputStream edgeInput = getClass().getResourceAsStream("/assets/banira_codex/textures/gui/unread_message_edge.png")) {
            assertNotNull(paperInput);
            assertNotNull("Themed edge layer missing", edgeInput);
            BufferedImage paper = ImageIO.read(paperInput), edge = ImageIO.read(edgeInput);
            assertEquals(12, paper.getWidth());
            assertEquals(12, paper.getHeight());
            assertEquals(paper.getWidth(), edge.getWidth());
            assertEquals(paper.getHeight(), edge.getHeight());
            int count = 0, left = 12, right = -1, top = 12, bottom = -1;
            for (int y = 0; y < 12; y++) for (int x = 0; x < 12; x++) {
                if ((paper.getRGB(x, y) >>> 24) > 128 || (edge.getRGB(x, y) >>> 24) > 128) {
                    count++;
                    left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                }
            }
            assertTrue("Paper fill missing", count >= 72);
            assertTrue("Envelope is too wide", right - left + 1 <= (bottom - top + 1) * 1.35);
            assertTrue("Transparent margin missing", count < 144);
        }
    }
}
