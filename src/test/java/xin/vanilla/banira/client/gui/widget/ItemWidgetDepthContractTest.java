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
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/client/BaniraItemRenderBridge.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("itemRenderer.blitOffset = originalBlitOffset + ITEM_DECORATION_DEPTH_OFFSET"));
        assertTrue(source.contains("RenderSystem.disableDepthTest()"));
        assertTrue(source.contains("RenderSystem.depthMask(false)"));
        assertTrue(source.contains("RenderSystem.depthMask(true)"));
        assertTrue(source.contains("RenderSystem.enableDepthTest()"));
        assertTrue(source.contains("finally"));
        assertTrue(source.contains("itemRenderer.blitOffset = originalBlitOffset"));
    }
}
