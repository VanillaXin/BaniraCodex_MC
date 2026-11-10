package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;

import static org.junit.Assert.*;

public class AuiOfficialMavenPinTest {
    private Properties properties() throws Exception {
        Properties values = new Properties();
        values.load(new StringReader(new String(Files.readAllBytes(Paths.get("gradle.properties")), StandardCharsets.UTF_8)));
        return values;
    }


    private String expectedHash() throws Exception {
        Properties values = properties();
        if ("forge".equals(values.getProperty("loader_type"))) {
            return "F872FCDCFCCC3A9FDAE1BCD8413B28D7CC7324514293216BCC0126C928D2D5D0";
        }
        return "1.19.2".equals(values.getProperty("minecraft_version"))
                ? "9F7A199D835EEC069E9C26D42AA8D65D2873ED219CA168750A69BD5408B42C53"
                : "03C89DCACBFF77F176F46B91457541FD10AE29343BD29DF1F1B7199BF6C3A111";
    }


    @Test
    public void actualReferenceMatchesPinnedHash() throws Exception {
        String input = System.getProperty("banira.aui.contractJar");
        assertNotNull(input);
        assertTrue(Paths.get(input).isAbsolute());
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Paths.get(input)));
        StringBuilder hex = new StringBuilder();
        for (byte value : hash) hex.append(String.format(java.util.Locale.ROOT, "%02X", value & 255));
        assertEquals(expectedHash(), hex.toString());
    }

    @Test
    public void businessTestsCannotLoadReferenceEngine() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        if ("1.16.5".equals(properties().getProperty("minecraft_version"))) {
            assertNotNull(loader.getResource("META-INF/banira-aui-compile-api.properties"));
        } else {
            assertNull(loader.getResource("com/sighs/apricityui/init/Document.class"));
        }
    }
}

