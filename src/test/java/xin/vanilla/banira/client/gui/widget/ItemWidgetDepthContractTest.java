package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * 物品数量和耐久等装饰必须绘制在缩放后的物品模型前方。
 */
public class ItemWidgetDepthContractTest {
    @Test
    public void itemDecorationsUseAndRestoreForegroundDepth() throws Exception {
        String bridge = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/client/BaniraItemRenderBridge.java")),
                StandardCharsets.UTF_8);
        String widget = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/ItemWidget.java")),
                StandardCharsets.UTF_8);

        assertDepthGuard(bridge);
        assertDepthGuard(widget);
        assertTrue(bridge.contains("ITEM_DECORATION_FOREGROUND_DEPTH = 250.0F"));
        assertTrue(bridge.contains("graphics.pose().translate(0, 0, ITEM_DECORATION_FOREGROUND_DEPTH)"));
        assertTrue(bridge.contains("graphics.pose().popPose()"));
    }

    private static void assertDepthGuard(String source) {
        assertTrue(source.contains("translate(0, 0, ITEM_DECORATION_FOREGROUND_DEPTH)"));
        assertTrue(source.contains("graphics.renderItemDecorations"));
        assertTrue(source.contains("graphics.flush()"));
        assertFalse(source.contains("RenderSystem.depthMask(false)"));
        assertTrue(source.contains("finally"));
    }
}
