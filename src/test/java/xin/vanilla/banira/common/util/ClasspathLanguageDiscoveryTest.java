package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClasspathLanguageDiscoveryTest {
    @Test
    public void discoversEveryLanguageInsidePackagedJar() throws Exception {
        Path jarPath = Files.createTempFile("banira-language", ".jar");
        try (OutputStream output = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(output)) {
            add(jar, "assets/example/lang/en_us.json");
            add(jar, "assets/example/lang/zh_cn.json");
            add(jar, "assets/example/other.json");
        }

        Set<String> languages = ClasspathLanguageDiscovery.discoverFromLocation(
                jarPath.toUri().toURL(), "/assets/example/lang/");

        assertEquals(2, languages.size());
        assertTrue(languages.contains("en_us"));
        assertTrue(languages.contains("zh_cn"));
    }

    private static void add(JarOutputStream jar, String name) throws Exception {
        jar.putNextEntry(new JarEntry(name));
        jar.write("{}".getBytes("UTF-8"));
        jar.closeEntry();
    }
}
