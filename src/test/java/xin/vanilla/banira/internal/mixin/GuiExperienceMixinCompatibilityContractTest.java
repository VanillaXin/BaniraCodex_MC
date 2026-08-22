package xin.vanilla.banira.internal.mixin;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuiExperienceMixinCompatibilityContractTest {
    @Test
    public void experienceTextDoesNotRedirectACompetingFontCall() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/mixin/injections/IngameGuiExperienceMixin.java")),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("@Redirect"));
        assertTrue(source.contains("LocalPlayer;experienceLevel:I"));
        assertTrue(source.contains("require = 0"));
        assertFalse(source.contains("Font;draw"));
    }
}
