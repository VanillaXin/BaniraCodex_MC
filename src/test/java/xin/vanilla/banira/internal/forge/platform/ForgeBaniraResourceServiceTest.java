package xin.vanilla.banira.internal.forge.platform;

import com.google.gson.JsonObject;
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

import static org.junit.Assert.assertEquals;

public class ForgeBaniraResourceServiceTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void collectsEveryLanguageFromTheRegisteredModFileDirectory() throws Exception {
        Path lang = temporaryFolder.newFolder("assets", "child_mod", "lang").toPath();
        Files.write(lang.resolve("en_us.json"), "{\"word.child_mod.name\":\"Child\"}"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("zh_cn.json"), "{\"word.child_mod.name\":\"子模组\"}"
                .getBytes(StandardCharsets.UTF_8));

        Map<String, JsonObject> result = new LinkedHashMap<>();
        ForgeBaniraResourceService.collectLanguageDirectory(lang, result);

        assertEquals(new LinkedHashSet<>(Arrays.asList("en_us", "zh_cn")), result.keySet());
        assertEquals("子模组", result.get("zh_cn").get("word.child_mod.name").getAsString());
    }
}
