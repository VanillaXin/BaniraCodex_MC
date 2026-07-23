package xin.vanilla.banira.client.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 约束子 mod 使用的玩家头像纹理工具在版本间保持同名 API。 */
public class PlayerSkinTextureUtilsStructureTest {
    @Test
    public void stablePlayerSkinTextureHelperReplacesOldName() throws Exception {
        Path stable = Paths.get("src/main/java/xin/vanilla/banira/client/util/PlayerSkinTextureUtils.java");
        Path old = Paths.get("src/main/java/xin/vanilla/banira/client/util/PlayerTextureUtils.java");
        assertTrue(Files.isRegularFile(stable));
        assertFalse(Files.exists(old));

        String source = new String(Files.readAllBytes(stable), StandardCharsets.UTF_8);
        assertTrue(source.contains("headFaceTextures(@Nullable ResourceLocation skin)"));
        assertTrue(source.contains("headFaceTextures(@Nullable UUID uuid)"));
    }
}
