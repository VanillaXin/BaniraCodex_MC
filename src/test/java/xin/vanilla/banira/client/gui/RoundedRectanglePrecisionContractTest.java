package xin.vanilla.banira.client.gui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ShapeDrawArgs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoundedRectanglePrecisionContractTest {

    @Test
    public void rectangleArgumentsDefaultToFineCorners() {
        assertEquals(ShapeDrawArgs.RoundedCornerMode.FINE,
                new ShapeDrawArgs.RectParams().cornerMode());
    }

    @Test
    public void genericRoundedRectangleOverloadDelegatesToFineRenderer() throws Exception {
        String source = read("src/main/java/xin/vanilla/banira/client/util/AbstractGuiUtils.java");
        String method = between(source,
                "public static void drawRoundedRect(PoseStack stack, int x, int y, int width, int height, int argb, int radius)",
                "public static void drawRoundedRectRough");

        assertTrue(method.contains("if (radius <= 0)"));
        assertTrue(method.contains("fillEx(stack, x, y, width, height, argb)"));
        assertTrue(method.contains("drawRoundedRect(stack, (float) x"));
        assertFalse(method.contains("drawCircleQuadrant(stack"));
    }

    @Test
    public void normalLabelsAndTooltipsDoNotRequestRoughOutlines() throws Exception {
        String label = read("src/main/java/xin/vanilla/banira/client/gui/widget/LabelWidget.java");
        String tooltip = read("src/main/java/xin/vanilla/banira/client/gui/widget/TooltipWidget.java");

        assertFalse(label.contains("drawRoundedRectOutLineRough"));
        assertFalse(tooltip.contains("drawRoundedRectOutLineRough"));
    }

    @Test
    public void explicitRoughModeKeepsItsNamedRenderer() throws Exception {
        String source = read("src/main/java/xin/vanilla/banira/client/gui/widget/BaseShapeWidget.java");
        assertTrue(source.contains("AbstractGuiUtils.drawRoundedRectRough"));
    }

    private static String read(String path) throws Exception {
        Path source = Paths.get(path);
        return new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex);
        assertTrue(startIndex >= 0 && endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }
}
