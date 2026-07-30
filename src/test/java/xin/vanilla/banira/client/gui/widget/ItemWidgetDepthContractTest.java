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
                "src/main/java/xin/vanilla/banira/client/gui/widget/ItemWidget.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("ITEM_DECORATION_DEPTH_OFFSET = 101.0F"));
        assertTrue(source.contains("itemRenderer.blitOffset = originalBlitOffset + ITEM_DECORATION_DEPTH_OFFSET"));
        assertTrue(source.contains("finally"));
        assertTrue(source.contains("itemRenderer.blitOffset = originalBlitOffset"));
    }
}
