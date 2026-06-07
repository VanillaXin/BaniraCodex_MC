package xin.vanilla.banira.common.util;

import net.minecraftforge.fml.common.Mod;
import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;

public class TranslatorTest {

    @Test
    public void annotationModIdWinsOverInstalledPlatform() {
        BaniraPlatforms.install(new TestBaniraPlatform()
                .modIdFromMainClass(AnnotatedMod.class, "wrong_mod_id"));

        assertEquals("annotated_translation_test", new AnnotatedTranslator().getModId());
    }

    private static final class AnnotatedTranslator extends Translator {
        private AnnotatedTranslator() {
            super(AnnotatedMod.class);
        }
    }

    @Mod("annotated_translation_test")
    private static final class AnnotatedMod {
    }
}
