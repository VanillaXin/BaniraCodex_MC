package xin.vanilla.banira.client.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ExtractedGuiContextTest {
    @Test
    public void nestedPassRestoresPreviousExtractorEvenAfterFailure() throws Exception {
        GuiGraphicsExtractor first = extractor(), second = extractor();
        try (var outer = ExtractedGuiRenderer.bind(first, new GuiRenderState(), () -> null)) {
            assertSame(first, ExtractedGuiRenderer.graphics());
            try {
                try (var inner = ExtractedGuiRenderer.bind(second, new GuiRenderState(), () -> null)) {
                    assertSame(second, ExtractedGuiRenderer.graphics());
                    throw new IllegalArgumentException("draw failure");
                }
            } catch (IllegalArgumentException expected) {
                assertEquals("draw failure", expected.getMessage());
            }
            assertSame(first, ExtractedGuiRenderer.graphics());
        }
        assertThrows(IllegalStateException.class, ExtractedGuiRenderer::graphics);
    }

    @Test
    public void geometryCapturesCurrentClipAndDoesNotSubmitOutsideIt() throws Exception {
        var state = new GuiRenderState();
        var clip = new ScreenRectangle[]{new ScreenRectangle(1, 1, 2, 2)};
        try (var scope = ExtractedGuiRenderer.bind(extractor(), state, () -> clip[0])) {
            quad(0, 0, 4, 4).submit();
            clip[0] = new ScreenRectangle(20, 20, 2, 2);
            quad(0, 0, 4, 4).submit();
        }
        var elements = new ArrayList<GuiElementRenderState>();
        state.forEachElement(elements::add, GuiRenderState.TraverseRange.ALL);
        assertEquals(1, elements.size());
        assertEquals(new ScreenRectangle(1, 1, 2, 2), elements.getFirst().bounds());
        assertEquals(new ScreenRectangle(1, 1, 2, 2), elements.getFirst().scissorArea());
    }

    private static ExtractedGuiRenderer.VertexBatch quad(float x, float y, float w, float h) {
        var batch = ExtractedGuiRenderer.begin(ExtractedGuiRenderer.Primitive.QUADS);
        var pose = new Matrix3x2f();
        batch.vertex(pose, x, y, -1);
        batch.vertex(pose, x, y + h, -1);
        batch.vertex(pose, x + w, y + h, -1);
        batch.vertex(pose, x + w, y, -1);
        return batch;
    }

    private static GuiGraphicsExtractor extractor() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (GuiGraphicsExtractor) type.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), GuiGraphicsExtractor.class);
    }
}
