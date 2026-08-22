package xin.vanilla.banira.internal.fabric;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 经验文本取消不得依赖容易被其他 HUD Mixin 占用的 drawString 调用点。
 */
public class GuiExperienceMixinCompatibilityContractTest {
    @Test
    public void experienceTextCancellationWrapsTheStableRenderMethod() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/mixin/injections/GuiExperienceMixin.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("@WrapMethod(method = \"renderExperienceBar\")"));
        assertTrue(source.contains("@ModifyExpressionValue"));
        assertTrue(source.contains("experienceLevel:I"));
        assertTrue(source.contains("require = 0"));
        assertFalse(source.contains("player.experienceLevel = 0"));
        assertFalse(source.contains("target = \"Lnet/minecraft/client/gui/GuiGraphics;drawString"));
    }
}
