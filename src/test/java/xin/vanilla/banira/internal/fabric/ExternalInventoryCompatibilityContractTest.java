package xin.vanilla.banira.internal.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalInventoryCompatibilityContractTest {
    @Test
    public void fabricClientInitializesOptionalInventoryBridgesAfterCodexEntries() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java");
        assertTrue(source.contains("BaniraCodexClientBootstrap.init()"));
        assertTrue(source.contains("FabricExternalInventoryCompatibility.init()"));
        assertTrue(source.indexOf("dispatchModClientSetup")
                < source.indexOf("FabricExternalInventoryCompatibility.init()"));
    }

    @Test
    public void compatibilityBootstrapLoadsOptionalTypesReflectively() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/fabric/compat/FabricExternalInventoryCompatibility.java");
        assertTrue(source.contains("loader.isModLoaded"));
        assertTrue(source.contains("Class.forName(className"));
        assertTrue(source.contains("refreshCurrentScreen()"));
        assertFalse(source.contains("import dev.ftb.mods"));
        assertFalse(source.contains("import mezz.jei"));
    }

    @Test
    public void optionalMixinsAreClientOnlyAndOptionalDependenciesAreCompileOnly() throws Exception {
        JsonObject root = new JsonParser().parse(source(
                "src/main/resources/banira_codex.mixins.json")).getAsJsonObject();
        assertTrue(root.get("plugin").getAsString().endsWith("FabricMixinConfigPlugin"));
        JsonArray client = root.getAsJsonArray("client");
        String values = client.toString();
        assertTrue(values.contains("compat.ftblibrary.SidebarGroupGuiButtonMixin"));
        assertTrue(values.contains("compat.ftblibrary.SidebarButtonMixin"));
        assertTrue(values.contains("compat.jei.BookmarkButtonMixin"));
        assertTrue(values.contains("compat.jei.BookmarkButtonAccessor"));

        String build = source("build.gradle");
        assertTrue(build.contains("modCompileOnly \"curse.maven:ftb-library-fabric"));
        assertTrue(build.contains("modCompileOnly \"curse.maven:jei-238222"));

        String plugin = source("src/main/java/xin/vanilla/banira/internal/fabric/mixin/FabricMixinConfigPlugin.java");
        assertTrue(plugin.contains("isModLoaded(\"ftblibrary\")"));
        assertTrue(plugin.contains("isModLoaded(\"jei\")"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
