package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Mod Menu 从标题界面打开 BaniraScreen 时没有世界帧负责清屏，基类必须先绘制完整背景。
 */
public class ModMenuStandaloneBackgroundContractTest {

    @Test
    public void standaloneScreenClearsBeforeRenderingContent() throws Exception {
        Path path = Paths.get("src/main/java/xin/vanilla/banira/client/gui/BaniraScreen.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int renderStart = source.indexOf("public void render(GuiGraphics graphics");
        int renderEnd = source.indexOf("protected abstract void onRender", renderStart);
        String render = source.substring(renderStart, renderEnd);

        int background = render.indexOf("renderBackground(graphics);");
        int content = render.indexOf("this.onRender(graphics, mouseX, mouseY, partialTicks);");
        assertTrue("Standalone background guard is missing",
                render.contains("this.minecraft == null || this.minecraft.level == null"));
        assertTrue("Standalone background must be drawn before page content",
                background >= 0 && content >= 0 && background < content);
    }
}
