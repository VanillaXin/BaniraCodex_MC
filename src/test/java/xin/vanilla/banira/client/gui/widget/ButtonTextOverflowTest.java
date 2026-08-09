package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ButtonTextOverflowTest {
    @Test
    public void maxWidthUsesEndEllipsisByDefault() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/ButtonWidget.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "private EnumEllipsisPosition textEllipsisPosition = EnumEllipsisPosition.END;"));
    }
}
