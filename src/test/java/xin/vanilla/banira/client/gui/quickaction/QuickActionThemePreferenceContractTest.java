package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 子 Mod 快捷入口的悬浮提示和右键菜单必须使用条目命名空间对应的主题。
 */
public class QuickActionThemePreferenceContractTest {
    @Test
    public void overlayResolvesThemeFromEntryNamespace() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickActionOverlay.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("renderContextMenu(graphics, stack, screen, mc, mouseX, mouseY, contextTheme(theme))"));
        assertTrue(source.contains("BaniraThemes.seasonFor(modId)"));
        assertTrue(source.contains("TooltipWidget.drawPopupMessage(stack, args, entryTheme, season)"));
        assertTrue(source.contains("renderContextTooltip"));
        assertTrue(source.contains("TooltipWidget.drawPopupMessage(stack, args, tooltipTheme, season)"));
        assertFalse(source.contains("graphics.renderTooltip(mc.font, Component.literal(contextTooltipLine)"));
    }
}
