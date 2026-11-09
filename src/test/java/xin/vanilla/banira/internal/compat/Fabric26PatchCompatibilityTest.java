package xin.vanilla.banira.internal.compat;

import com.google.gson.JsonParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class Fabric26PatchCompatibilityTest {
    private static final Map<String, String> OFFICIAL_SHA1 = Map.of(
            "26.1", "191771837687b766537a8c4607cb6fad79c533a1",
            "26.1.1", "377031a9e733ba8ab4d355959a8f6fb8eb707556",
            "26.1.2", "4e618f09a0c649dde3fdf829df443ce0b8831e65");
    private static final Map<String, JarFile> CLIENTS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> HASHES = new HashMap<>();

    @BeforeClass
    public static void openVerifiedOfficialClients() throws Exception {
        var manifest = JsonParser.parseString(Files.readString(
                Path.of(System.getProperty("banira.patchCompatibilityManifest")), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(OFFICIAL_SHA1.keySet(), manifest.keySet());
        try {
            for (String version : List.of("26.1", "26.1.1", "26.1.2")) {
                Path path = Path.of(manifest.get(version).getAsString());
                assertTrue(path.toString(), path.isAbsolute());
                try (var stream = Files.newInputStream(path)) {
                    assertEquals(version, OFFICIAL_SHA1.get(version), hash(stream, "SHA-1"));
                }
                var jar = new JarFile(path.toFile());
                CLIENTS.put(version, jar);
                var hashes = new HashMap<String, String>();
                for (var entries = jar.entries(); entries.hasMoreElements(); ) {
                    var entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) continue;
                    try (var stream = jar.getInputStream(entry)) {
                        hashes.put(entry.getName(), hash(stream, "SHA-256"));
                    }
                }
                HASHES.put(version, hashes);
            }
        } catch (Throwable failure) {
            closeClients();
            throw failure;
        }
    }

    @AfterClass
    public static void closeClients() throws Exception {
        try {
            for (var jar : CLIENTS.values()) jar.close();
        } finally {
            CLIENTS.clear();
            HASHES.clear();
        }
    }

    private static String hash(InputStream stream, String algorithm) throws Exception {
        var digest = MessageDigest.getInstance(algorithm);
        var buffer = new byte[32768];
        for (int size; (size = stream.read(buffer)) != -1; ) digest.update(buffer, 0, size);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static ClassNode node(JarFile jar, String entry) throws Exception {
        var target = jar.getJarEntry(entry);
        assertNotNull(entry, target);
        try (var stream = jar.getInputStream(target)) {
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    @Test
    public void officialArchivesKeepTheExpectedVersionJavaAndProtocol() throws Exception {
        for (var client : CLIENTS.entrySet()) {
            try (var stream = client.getValue().getInputStream(client.getValue().getJarEntry("version.json"))) {
                var version = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(client.getKey(), version.get("id").getAsString());
                assertEquals(25, version.get("java_version").getAsInt());
                assertEquals(775, version.get("protocol_version").getAsInt());
            }
        }
    }

    @Test
    public void patchesPreserveAllClassesAndExistingDeclaredJvmMembers() throws Exception {
        var baseline = HASHES.get("26.1");
        for (String version : List.of("26.1.1", "26.1.2")) {
            assertEquals(version, baseline.keySet(), HASHES.get(version).keySet());
            int changed = 0;
            for (String entry : baseline.keySet()) {
                if (baseline.get(entry).equals(HASHES.get(version).get(entry))) continue;
                changed++;
                var before = node(CLIENTS.get("26.1"), entry);
                var after = node(CLIENTS.get(version), entry);
                assertEquals(entry, before.access, after.access);
                assertEquals(entry, before.superName, after.superName);
                assertEquals(entry, before.interfaces, after.interfaces);
                for (var field : before.fields) {
                    assertTrue(version + ": " + entry + "." + field.name,
                            after.fields.stream().anyMatch(candidate -> candidate.name.equals(field.name)
                                    && candidate.desc.equals(field.desc) && candidate.access == field.access));
                }
                for (var method : before.methods) {
                    assertTrue(version + ": " + entry + "." + method.name + method.desc,
                            after.methods.stream().anyMatch(candidate -> candidate.name.equals(method.name)
                                    && candidate.desc.equals(method.desc) && candidate.access == method.access));
                }
            }
            assertEquals(version, version.equals("26.1.1") ? 3 : 9, changed);
        }
    }

    @Test
    public void everyVanillaMixinAndClassTweakerTargetRemainsByteIdentical() throws Exception {
        Set<String> targets = new TreeSet<>();
        try (var shipped = new JarFile(System.getProperty("banira.publication.jar"))) {
            for (var entries = shipped.entries(); entries.hasMoreElements(); ) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("xin/vanilla/banira/internal/mixin/")
                        || !entry.getName().endsWith(".class")) continue;
                var type = node(shipped, entry.getName());
                var annotations = new ArrayList<AnnotationNode>();
                if (type.visibleAnnotations != null) annotations.addAll(type.visibleAnnotations);
                if (type.invisibleAnnotations != null) annotations.addAll(type.invisibleAnnotations);
                for (var annotation : annotations) {
                    if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
                    for (int i = 0; i < annotation.values.size(); i += 2) {
                        var key = annotation.values.get(i);
                        if (!key.equals("value") && !key.equals("targets")) continue;
                        for (Object value : (List<?>) annotation.values.get(i + 1)) {
                            String name = value instanceof Type target ? target.getInternalName() : value.toString().replace('.', '/');
                            if (name.startsWith("net/minecraft/") || name.startsWith("com/mojang/"))
                                targets.add(name + ".class");
                        }
                    }
                }
            }
            try (var stream = shipped.getInputStream(shipped.getJarEntry("banira_codex.classtweaker"))) {
                for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
                    if (line.startsWith("accessible ")) targets.add(line.split("\\s+")[2] + ".class");
                }
            }
        }
        assertTrue("Check the actual shipped hook surface", targets.size() >= 8);
        for (String target : targets) assertIdentical(target);
    }

    @Test
    public void translationNativeUiAndNetworkAnchorsRemainByteIdentical() {
        for (String target : List.of("net/minecraft/network/chat/Component.class",
                "net/minecraft/network/chat/contents/TranslatableContents.class",
                "net/minecraft/client/gui/screens/Screen.class",
                "net/minecraft/client/gui/GuiGraphicsExtractor.class",
                "net/minecraft/client/Minecraft.class",
                "net/minecraft/server/level/ServerPlayer.class"))
            assertIdentical(target);
    }

    private static void assertIdentical(String target) {
        String baseline = HASHES.get("26.1").get(target);
        assertNotNull(target, baseline);
        for (String version : List.of("26.1.1", "26.1.2"))
            assertEquals(version + ": " + target, baseline, HASHES.get(version).get(target));
    }
}
