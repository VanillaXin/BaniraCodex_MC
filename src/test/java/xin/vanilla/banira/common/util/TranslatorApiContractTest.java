package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 锁定子 mod 可通过公开接口枚举可用语言。
 */
public class TranslatorApiContractTest {

    @Test
    public void languageFilesAreExposedByTranslatorInterface() throws Exception {
        assertEquals(
                List.class,
                ITranslator.class.getMethod("getI18nFiles").getReturnType()
        );
    }

    @Test
    public void languageFilesKeepExistingTranslatorImplementationsCompatible() throws Exception {
        assertTrue(ITranslator.class.getMethod("getI18nFiles").isDefault());
    }
}
