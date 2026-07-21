package xin.vanilla.banira.common.data;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 防止调用 1.16.5 合并开发 jar 独有、专用服务端已裁剪的样式方法。 */
public class DedicatedServerStyleContractTest {
    @Test
    public void underlineUsesDedicatedServerSafeFormatting() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/common/data/Component.java")), StandardCharsets.UTF_8);

        assertFalse(source.contains(".withUnderlined("));
        assertTrue(source.contains("applyFormat(net.minecraft.ChatFormatting.UNDERLINE)"));
    }
}
