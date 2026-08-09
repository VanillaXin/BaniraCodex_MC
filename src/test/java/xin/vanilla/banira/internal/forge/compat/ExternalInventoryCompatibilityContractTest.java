package xin.vanilla.banira.internal.forge.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalInventoryCompatibilityContractTest {
    @Test
    public void forge21DoesNotLinkUnavailableFtbRuntime() {
        assertFalse(Files.exists(Paths.get("src/main/java/xin/vanilla/banira/internal/forge/compat/ftblibrary/FtbLibraryCompatibility.java")));
        assertFalse(Files.exists(Paths.get("src/main/java/xin/vanilla/banira/internal/mixin/compat/ftblibrary/SidebarButtonMixin.java")));
    }

    @Test
    public void optionalMixinsAreDeclaredAsClientMixins() throws Exception {
        JsonObject root = new JsonParser().parse(source(
                "src/main/resources/banira_codex.mixins.json")).getAsJsonObject();
        JsonArray client = root.getAsJsonArray("client");
        String values = client.toString();
        assertTrue(values.contains("compat.jei.BookmarkButtonMixin"));
        assertTrue(values.contains("compat.jei.GuiIconToggleButtonMixin"));
        assertFalse(values.contains("compat.ftblibrary"));
        assertFalse(values.contains("compat.jei.BookmarkButtonAccessor"));
    }

    @Test
    public void optionalModTypesStayInsideForgeCompatibilityBoundary() throws Exception {
        Path sourceRoot = Paths.get("src/main/java");
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().replace('\\', '/').contains(
                            "/internal/forge/compat/"))
                    .filter(path -> !path.toString().replace('\\', '/').contains(
                            "/internal/mixin/compat/"))
                    .forEach(path -> {
                        try {
                            String value = source(path.toString());
                            assertFalse(path + " leaks an optional mod type",
                                    value.contains("dev.ftb.mods.ftblibrary")
                                            || value.contains("mezz.jei")
                                            || value.contains("org.anti_ad.mc.ipnext"));
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }
    }

    @Test
    public void compatibilityBootstrapDoesNotLinkMissingModClassesEagerly() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ForgeExternalInventoryCompatibility.java");
        assertFalse(source.contains("import xin.vanilla.banira.internal.forge.compat.ftblibrary"));
        assertFalse(source.contains("import xin.vanilla.banira.internal.forge.compat.jei"));
        assertFalse(source.contains("import xin.vanilla.banira.internal.forge.compat.ipn"));
        assertTrue(source.contains("Class.forName(className"));
        assertTrue(source.contains("refreshCurrentScreen()"));
        assertTrue(source.contains("ReflectiveOperationException | LinkageError"));
    }

    @Test
    public void genericExternalEntryApiRemainsAvailableWithoutFtb() throws Exception {
        String manager = source("src/main/java/xin/vanilla/banira/client/gui/quickaction/ExternalInventoryButtonManager.java");
        assertTrue(manager.contains("registerProvider"));
        assertTrue(manager.contains("refreshCurrentScreen"));
    }

    @Test
    public void supportedModsRemainOptionalClientDependencies() throws Exception {
        String metadata = source("src/main/resources/META-INF/mods.toml");
        assertFalse(metadata.contains("modId = \"ftblibrary\""));
        for (String modId : new String[]{"jei", "inventoryprofilesnext"}) {
            int start = metadata.indexOf("modId = \"" + modId + "\"");
            assertTrue("Missing optional dependency metadata for " + modId, start >= 0);
            String block = metadata.substring(start,
                    Math.min(metadata.length(), start + 180));
            assertTrue(block.contains("mandatory = false"));
            assertTrue(block.contains("ordering = \"AFTER\""));
            assertTrue(block.contains("side = \"CLIENT\""));
        }
    }

    @Test
    public void jeiBridgeUsesTheNativeBookmarkDrawable() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/forge/compat/jei/JeiCompatibility.java");
        assertTrue(source.contains("jei.tooltip.bookmarks"));
        assertFalse(source.contains("Items.BOOK"));
        assertTrue(source.contains("IDrawable"));
        assertTrue(source.contains("IClientToggleState"));
        assertTrue(source.contains("QuickIcon.custom"));
        assertTrue(source.contains("isBookmarkOverlayEnabled"));

        String mixin = source("src/main/java/xin/vanilla/banira/internal/mixin/compat/jei/BookmarkButtonMixin.java");
        assertTrue(mixin.contains("offIcon"));
        assertTrue(mixin.contains("onIcon"));
        assertTrue(mixin.contains("toggleState"));
    }

    @Test
    public void devSmokeRunnerOpensInventoryAndWritesBoundedDiagnostics() throws Exception {
        String runner = source("src/main/java/xin/vanilla/banira/client/gui/quickaction/ExternalInventoryButtonSmokeRunner.java");
        String eventBridge = source("src/main/java/xin/vanilla/banira/internal/forge/client/BaniraClientForgeEventHandler.java");
        String build = source("build.gradle");

        assertTrue(runner.contains("banira.externalButtonsSmoke"));
        assertTrue(runner.contains("new InventoryScreen"));
        assertTrue(runner.contains("Screenshot.takeScreenshot"));
        assertTrue(runner.contains("writeToFile"));
        assertTrue(runner.contains("FINISHED"));
        assertTrue(runner.contains("CONFIGURED"));
        assertTrue(runner.contains("refreshCurrentScreen()"));
        assertTrue(eventBridge.contains("ExternalInventoryButtonSmokeRunner.onClientTick()"));
        assertTrue(build.contains("externalButtonsSmoke"));
    }

    @Test
    public void forgeBootstrapDoesNotAttemptToLoadFtbBridge() throws Exception {
        String compatibility = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ForgeExternalInventoryCompatibility.java");
        assertFalse(compatibility.contains("FtbLibraryCompatibility"));
        assertTrue(compatibility.contains("JeiCompatibility"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
