package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;

public class TranslatorJarLanguageTest {
    private static final String MOD_ID = "jar_language_test";
    private static final String ANCHOR_RESOURCE =
            "xin/vanilla/banira/common/util/TranslatorJarLanguageAnchor.class";

    @Test
    public void discoversAllLanguagesFromPackagedJar() throws Exception {
        Path jar = Files.createTempFile("banira-language-test", ".jar");
        writeTestModJar(jar);

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            Class<?> anchor = loader.loadClass("xin.vanilla.banira.common.util.TranslatorJarLanguageAnchor");
            Translator translator = new Translator(MOD_ID, anchor);

            assertEquals(Set.of("en_us", "zh_cn"), new HashSet<>(translator.getI18nFiles()));
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private static void writeTestModJar(Path jar) throws Exception {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            try (InputStream classBytes = TranslatorJarLanguageTest.class.getClassLoader()
                    .getResourceAsStream(ANCHOR_RESOURCE)) {
                if (classBytes == null) {
                    throw new IllegalStateException("Missing test anchor class");
                }
                output.putNextEntry(new JarEntry(ANCHOR_RESOURCE));
                classBytes.transferTo(output);
                output.closeEntry();
            }
            writeLanguage(output, "en_us", "Hello");
            writeLanguage(output, "zh_cn", "你好");
        }
    }

    private static void writeLanguage(JarOutputStream output, String language, String value) throws Exception {
        output.putNextEntry(new JarEntry("assets/" + MOD_ID + "/lang/" + language + ".json"));
        output.write(("{\"word." + MOD_ID + ".sample\":\"" + value + "\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        output.closeEntry();
    }
}

final class TranslatorJarLanguageAnchor {
    private TranslatorJarLanguageAnchor() {
    }
}
