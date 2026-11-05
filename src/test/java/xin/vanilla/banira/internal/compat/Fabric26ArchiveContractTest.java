package xin.vanilla.banira.internal.compat;

import org.junit.Test;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class Fabric26ArchiveContractTest {
    @Test
    public void shadowArchiveRetainsTheDevelopmentManifestWithoutRecursiveInheritance() throws Exception {
        Path formal = Path.of(System.getProperty("banira.publication.jar"));
        List<Path> development;
        try (var files = Files.list(formal.getParent())) {
            development = files.filter(path -> path.getFileName().toString().endsWith("-dev.jar")).toList();
        }
        assertEquals(1, development.size());
        try (var shipped = new JarFile(formal.toFile()); var raw = new JarFile(development.getFirst().toFile())) {
            var actual = shipped.getManifest().getMainAttributes();
            var expected = raw.getManifest().getMainAttributes();
            for (String key : List.of("Specification-Title", "Specification-Vendor", "Specification-Version",
                    "Implementation-Title", "Implementation-Version", "Implementation-Vendor", "Implementation-Timestamp")) {
                assertNotNull(key, expected.getValue(key));
                assertEquals(key, expected.getValue(key), actual.getValue(key));
            }
        }
    }

    @Test
    public void officialNamespaceArchiveKeepsJava25AndTheNativeFallback() throws Exception {
        try (var jar = new JarFile(System.getProperty("banira.publication.jar"))) {
            assertNotNull(jar.getEntry("fabric.mod.json"));
            assertNotNull(jar.getEntry("banira_codex.classtweaker"));
            assertNotNull(jar.getEntry("xin/vanilla/banira/client/gui/BaniraScreen.class"));
            assertNotNull(jar.getEntry("xin/vanilla/banira/internal/client/ui/AuiPageHost.class"));
            assertNull(jar.getEntry("com/sighs/apricityui/init/Document.class"));
            assertNull(jar.getEntry("dev/ftb/mods/ftblibrary/sidebar/SidebarButtonManager.class"));
            assertNull(getClass().getClassLoader().getResource("dev/ftb/mods/ftblibrary/sidebar/SidebarButtonManager.class"));
            try (var input = new DataInputStream(jar.getInputStream(jar.getEntry("xin/vanilla/banira/BaniraCodex.class")))) {
                assertEquals(0xCAFEBABE, input.readInt());
                input.readUnsignedShort();
                assertEquals(69, input.readUnsignedShort());
            }
        }
    }
}
