package xin.vanilla.banira.common.util;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;

public class TranslatorTest {

    @Test
    public void explicitModIdDoesNotDependOnLoaderAnnotation() {
        BaniraPlatforms.install(new TestBaniraPlatform()
                .modIdFromMainClass(TranslatorTest.class, "wrong_mod_id"));

        assertEquals("explicit_translation_test", new ExplicitTranslator().getModId());
    }

    @Test
    public void classConstructorUsesPlatformMetadata() {
        BaniraPlatforms.install(new TestBaniraPlatform()
                .modIdFromMainClass(TranslatorTest.class, "platform_translation_test"));

        assertEquals("platform_translation_test", new PlatformTranslator().getModId());
    }

    private static final class ExplicitTranslator extends Translator {
        private ExplicitTranslator() {
            super("explicit_translation_test", TranslatorTest.class);
        }
    }

    private static final class PlatformTranslator extends Translator {
        private PlatformTranslator() {
            super(TranslatorTest.class);
        }
    }
}
