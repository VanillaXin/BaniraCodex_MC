package xin.vanilla.banira.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslatorIdentityTest {
    @Test
    public void explicitModIdDoesNotDependOnLoaderEntrypointDiscovery() {
        assertEquals("child_mod", new ExplicitTranslator().getModId());
    }

    private static final class ExplicitTranslator extends Translator {
        private ExplicitTranslator() {
            super("child_mod", ExplicitTranslator.class);
        }
    }
}
