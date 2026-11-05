package xin.vanilla.banira.internal.neoforge.platform;

import com.google.gson.JsonObject;
import net.neoforged.fml.jarcontents.JarContents;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class NeoForgeBaniraResourceServiceTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void collectsEveryLanguageFromTheRegisteredModFileDirectory() throws Exception {
        Path lang = temporaryFolder.newFolder("assets", "child_mod", "lang").toPath();
        Files.writeString(lang.resolve("en_us.json"),
                "{\"word.child_mod.name\":\"Child\"}", StandardCharsets.UTF_8);
        Files.writeString(lang.resolve("zh_cn.json"),
                "{\"word.child_mod.name\":\"子模组\"}", StandardCharsets.UTF_8);

        Map<String, JsonObject> result = new LinkedHashMap<>();
        try (JarContents contents = JarContents.ofPath(temporaryFolder.getRoot().toPath())) {
            NeoForgeBaniraResourceService.collectModFileLanguages(contents, "child_mod", result);
        }

        assertEquals(new LinkedHashSet<>(Arrays.asList("en_us", "zh_cn")), result.keySet());
        assertEquals("子模组", result.get("zh_cn").get("word.child_mod.name").getAsString());
    }

    @Test
    public void readsPackedLanguagesWithoutDirectoryEntriesAndPreservesPackOverrides() throws Exception {
        Path jar = temporaryFolder.newFile("mod.jar").toPath();
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(jar))) {
            writeEntry(output, "assets/child_mod/lang/en_us.json", "{\"override\":\"default\",\"base\":\"kept\"}");
            writeEntry(output, "assets/child_mod/lang/sub/en_gb.json", "{}");
            writeEntry(output, "assets/other_mod/lang/en_us.json", "{\"foreign\":\"ignored\"}");
        }
        JsonObject override = new JsonObject();
        override.addProperty("override", "pack");
        Map<String, JsonObject> result = new LinkedHashMap<>();
        result.put("en_us", override);
        try (JarContents contents = JarContents.ofPath(jar)) {
            NeoForgeBaniraResourceService.collectModFileLanguages(contents, "child_mod", result);
        }
        assertEquals(java.util.Set.of("en_us"), result.keySet());
        assertEquals(2, result.get("en_us").size());
        assertEquals("pack", result.get("en_us").get("override").getAsString());
        assertEquals("kept", result.get("en_us").get("base").getAsString());
    }

    private static void writeEntry(ZipOutputStream output, String path, String content) throws java.io.IOException {
        output.putNextEntry(new ZipEntry(path));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
