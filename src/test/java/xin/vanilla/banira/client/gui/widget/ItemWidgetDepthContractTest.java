package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

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
        assertTrue(bridge.contains("graphics.pose().translate(0, 0, ITEM_DECORATION_DEPTH_OFFSET)"));
        assertTrue(bridge.contains("graphics.pose().popPose()"));
    }

    private static void assertDepthGuard(String source) {
        assertTrue(source.contains("graphics.renderItemDecorations"));
        assertTrue(source.contains("RenderSystem.disableDepthTest()"));
        assertTrue(source.contains("RenderSystem.depthMask(false)"));
        assertTrue(source.contains("RenderSystem.depthMask(true)"));
        assertTrue(source.contains("RenderSystem.enableDepthTest()"));
        assertTrue(source.contains("finally"));
    }
}
