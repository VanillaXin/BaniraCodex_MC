package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * 锁定长文本说明页的主题继承、换行、裁剪和滚动能力。
 */
public class ReadOnlyTextScreenContractTest {
    @Test
    public void textScreenWrapsAndScrollsInsideItsViewport() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/ReadOnlyTextScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("BaniraScreen.inheritThemeAndSeason"));
        assertTrue(source.contains("font.split("));
        assertTrue(source.contains("AbstractGuiUtils.enableScissor("));
        assertTrue(source.contains("renderScrollbar(stack, theme)"));
        assertTrue(source.contains("protected void onMouseScrolled("));
        assertTrue(source.contains("Minecraft.getInstance().setScreen(args.parentScreen())"));
    }
}
