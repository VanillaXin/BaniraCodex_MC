package xin.vanilla.banira.internal.neoforge.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ExternalInventoryCompatibilityContractTest {
    @Test
    public void jeiBridgeAdoptsBookmarksAndLookupHistory() throws Exception {
        String source = source("src/main/java/xin/vanilla/banira/internal/neoforge/compat/jei/JeiCompatibility.java");
        assertTrue(source.contains("BookmarkButtonController"));
        assertTrue(source.contains("LookupHistoryButtonController"));
        assertTrue(source.contains("lookup_history"));
        assertTrue(source.contains("isLookupHistoryEnabled"));
        assertTrue(source.contains("Math.min((float) size / icon.getWidth()"));
        assertTrue(source.contains("graphics.pose().last().pose().set(stack.last().pose())"));
        assertTrue(source.contains("graphics.pose().scale(scale, scale, 1.0F)"));
        assertTrue(source.contains("actions.add(new ExternalInventoryAction("));
        org.junit.Assert.assertFalse(source.contains("if (bookmarkController != null)"));
        org.junit.Assert.assertFalse(source.contains("if (lookupHistoryController != null)"));
    }

    @Test
    public void optionalJeiMixinsAndDependencyAreDeclared() throws Exception {
        JsonObject root = new JsonParser().parse(source(
                "src/main/resources/banira_codex.mixins.json")).getAsJsonObject();
        JsonArray client = root.getAsJsonArray("client");
        String values = client.toString();
        assertTrue(values.contains("compat.jei.BookmarkButtonMixin"));
        assertTrue(values.contains("compat.jei.GuiIconToggleButtonMixin"));
        assertTrue(values.contains("compat.jei.LookupHistoryButtonMixin"));
        assertTrue(source("build.gradle").contains("jei-238222:${jei_id}"));
        assertTrue(source("gradle.properties").contains("jei_id=7420587"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
