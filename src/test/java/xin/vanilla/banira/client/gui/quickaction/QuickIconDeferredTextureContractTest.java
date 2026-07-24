package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 资源重载前注册的快捷图标，首次绘制时必须重新解析真实纹理尺寸。 */
public class QuickIconDeferredTextureContractTest {
    @Test
    public void resourceIconResolvesMissingDimensionsAtDrawTime() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickIcon.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("resolvedResourceTexture()"));
        assertTrue(source.contains("TextureUtils.resolveTextureSizeForDraw"));
        assertTrue(source.contains("Texture resourceTexture = resolvedResourceTexture()"));
    }
}
