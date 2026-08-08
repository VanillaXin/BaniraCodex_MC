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
    public void ftbBridgeUsesVisibleButtonsAndPreservesClicks() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ftblibrary/FtbLibraryCompatibility.java");
        assertTrue(source.contains("isLoaded(\"ftblibrary\")"));
        assertTrue(source.contains("button.isActuallyVisible()"));
        assertTrue(source.contains("button.onClicked(shiftDown)"));
    }

    @Test
    public void optionalMixinsAreDeclaredAsClientMixins() throws Exception {
        JsonObject root = new JsonParser().parse(source(
                "src/main/resources/banira_codex.mixins.json")).getAsJsonObject();
        JsonArray client = root.getAsJsonArray("client");
        String values = client.toString();
        assertTrue(values.contains("compat.ftblibrary.SidebarGroupGuiButtonMixin"));
        assertTrue(values.contains("compat.ftblibrary.SidebarButtonMixin"));
        assertTrue(values.contains("compat.jei.BookmarkButtonMixin"));
        assertTrue(values.contains("compat.jei.BookmarkButtonAccessor"));
        assertTrue(values.contains("compat.ipn.IpnButtonMixin"));
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
    public void hostedFtbButtonsUseARealTranslationKey() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ftblibrary/FtbLibraryCompatibility.java");
        assertTrue(source.contains("hostedButtonTranslationKey"));
        String zh = source("src/main/resources/assets/banira_codex/lang/zh_cn.json");
        String en = source("src/main/resources/assets/banira_codex/lang/en_us.json");
        assertTrue(zh.contains("sidebar_button.banira_codex.external_inventory_button"));
        assertTrue(en.contains("sidebar_button.banira_codex.external_inventory_button"));
    }

    @Test
    public void supportedModsRemainOptionalClientDependencies() throws Exception {
        String metadata = source("src/main/resources/META-INF/mods.toml");
        for (String modId : new String[]{"ftblibrary", "jei", "inventoryprofilesnext"}) {
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
    public void ipnBridgeUsesTheNativeButtonTranslationKeys() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ipn/InventoryProfilesNextCompatibility.java");
        assertTrue(source.contains("inventoryprofiles.tooltip.settings_open"));
        assertTrue(source.contains("inventoryprofiles.tooltip.editor_toggle"));
        assertFalse(source.contains("inventoryprofiles.tooltip.editor_open"));
    }

    @Test
    public void devSmokeRunnerOpensInventoryAndWritesBoundedDiagnostics() throws Exception {
        String runner = source("src/main/java/xin/vanilla/banira/client/gui/quickaction/ExternalInventoryButtonSmokeRunner.java");
        String eventBridge = source("src/main/java/xin/vanilla/banira/internal/forge/client/ForgeBaniraClientEventBridge.java");
        String build = source("build.gradle");

        assertTrue(runner.contains("banira.externalButtonsSmoke"));
        assertTrue(runner.contains("new InventoryScreen"));
        assertTrue(runner.contains("ScreenShotHelper.takeScreenshot"));
        assertTrue(runner.contains("writeToFile"));
        assertTrue(runner.contains("FINISHED"));
        assertTrue(runner.contains("CONFIGURED"));
        assertTrue(runner.contains("refreshCurrentScreen()"));
        assertTrue(eventBridge.contains("ExternalInventoryButtonSmokeRunner.onClientTick()"));
        assertTrue(build.contains("externalButtonsSmoke"));
    }

    @Test
    public void suppressingFtbButtonsClearsTheAreaReservedForJei() throws Exception {
        String compatibility = source("src/main/java/xin/vanilla/banira/internal/forge/compat/ftblibrary/FtbLibraryCompatibility.java");
        String mixin = source("src/main/java/xin/vanilla/banira/internal/mixin/compat/ftblibrary/SidebarGroupGuiButtonMixin.java");
        assertTrue(compatibility.contains("clearReservedArea"));
        assertTrue(mixin.contains("clearReservedArea"));

        Class<?> rectangleClass = Class.forName("net.minecraft.client.renderer.Rectangle2d");
        Object occupied = rectangleClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(4, 8, 32, 48);
        Class<?> groupClass = Class.forName(
                "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton");
        java.lang.reflect.Field area = groupClass.getField("lastDrawnArea");
        area.set(null, occupied);

        Class.forName("xin.vanilla.banira.internal.forge.compat.ftblibrary.FtbLibraryCompatibility")
                .getMethod("clearReservedArea").invoke(null);

        Object cleared = area.get(null);
        assertTrue((Integer) rectangleClass.getMethod("getWidth").invoke(cleared) == 0);
        assertTrue((Integer) rectangleClass.getMethod("getHeight").invoke(cleared) == 0);
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
