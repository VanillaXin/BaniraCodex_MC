package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Version-local submission context for GUI extraction, including native HUD fallback.
 */
public final class ExtractedGuiRenderer {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    private static Function<GuiGraphicsExtractor, GuiRenderState> stateAccess;
    private static Function<GuiGraphicsExtractor, ScreenRectangle> scissorAccess;

    private ExtractedGuiRenderer() {
    }

    public static void install(Function<GuiGraphicsExtractor, GuiRenderState> state,
                               Function<GuiGraphicsExtractor, ScreenRectangle> scissor) {
        stateAccess = Objects.requireNonNull(state);
        scissorAccess = Objects.requireNonNull(scissor);
    }

    public static Scope bind(GuiGraphicsExtractor graphics) {
        if (stateAccess == null || scissorAccess == null)
            throw new IllegalStateException("GUI extraction state access has not been installed");
        return bind(graphics, stateAccess.apply(graphics), () -> scissorAccess.apply(graphics));
    }

    public static Scope bind(GuiGraphicsExtractor graphics, GuiRenderState state,
                             Supplier<ScreenRectangle> scissor) {
        Context previous = CURRENT.get();
        CURRENT.set(new Context(Objects.requireNonNull(graphics), Objects.requireNonNull(state),
                Objects.requireNonNull(scissor)));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public static GuiGraphicsExtractor graphics() {
        return context().graphics;
    }

    private static Context context() {
        Context context = CURRENT.get();
        if (context == null) throw new IllegalStateException("GUI rendering requires an active extraction context");
        return context;
    }

    public static void withPose(Matrix3x2fc pose, Runnable draw) {
        var target = graphics().pose();
        Matrix3x2f saved = new Matrix3x2f(target);
        target.set(pose);
        try {
            draw.run();
        } finally {
            target.set(saved);
        }
    }

    public static void nextStratum() {
        graphics().nextStratum();
    }

    public static VertexBatch begin(Primitive primitive) {
        return new VertexBatch(primitive);
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public static void textureColor(int color) {
        context().textureColor = color;
    }

    public static void texture(Matrix3x2fc pose, Identifier texture, int x0, int y0, int x1, int y1,
                               float u0, float u1, float v0, float v1) {
        var size = TextureUtils.resolveTextureSizeForDraw(texture);
        int width = size.key(), height = size.val();
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Texture has no drawable dimensions: " + texture);
        int color = context().textureColor;
        withPose(pose, () -> graphics().blit(RenderPipelines.GUI_TEXTURED, texture, x0, y0,
                u0 * width, v0 * height, x1 - x0, y1 - y0,
                Math.round((u1 - u0) * width), Math.round((v1 - v0) * height), width, height, color));
    }

    private static final class Context {
        private final GuiGraphicsExtractor graphics;
        private final GuiRenderState state;
        private final Supplier<ScreenRectangle> scissor;
        private int textureColor = 0xFFFFFFFF;

        private Context(GuiGraphicsExtractor graphics, GuiRenderState state, Supplier<ScreenRectangle> scissor) {
            this.graphics = graphics;
            this.state = state;
            this.scissor = scissor;
        }
    }

    public enum Primitive {QUADS, TRIANGLES, TRIANGLE_STRIP}

    public static final class VertexBatch {
        private final Primitive primitive;
        private final List<Vertex> vertices = new ArrayList<>();

        private VertexBatch(Primitive primitive) {
            this.primitive = primitive;
        }

        public void vertex(Matrix3x2fc pose, float x, float y, int color) {
            vertices.add(new Vertex(pose.m00() * x + pose.m10() * y + pose.m20(),
                    pose.m01() * x + pose.m11() * y + pose.m21(), color));
        }

        List<Vertex> quads() {
            List<Vertex> result = new ArrayList<>();
            switch (primitive) {
                case QUADS -> {
                    if (vertices.size() % 4 != 0) throw new IllegalStateException("Incomplete GUI quad");
                    for (int i = 0; i < vertices.size(); i += 4) {
                        Vertex a = vertices.get(i), b = vertices.get(i + 1);
                        Vertex c = vertices.get(i + 2), d = vertices.get(i + 3);
                        boolean reverse = area(a, b, c) > 0;
                        result.add(a);
                        result.add(reverse ? d : b);
                        result.add(c);
                        result.add(reverse ? b : d);
                    }
                }
                case TRIANGLES -> {
                    if (vertices.size() % 3 != 0) throw new IllegalStateException("Incomplete GUI triangle");
                    for (int i = 0; i < vertices.size(); i += 3)
                        triangle(result, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2));
                }
                case TRIANGLE_STRIP -> {
                    for (int i = 2; i < vertices.size(); i++) {
                        Vertex a = vertices.get(i - 2), b = vertices.get(i - 1), c = vertices.get(i);
                        triangle(result, (i & 1) == 0 ? a : b, (i & 1) == 0 ? b : a, c);
                    }
                }
            }
            return List.copyOf(result);
        }

        private static void triangle(List<Vertex> result, Vertex a, Vertex b, Vertex c) {
            // GUI uses QUADS; duplicating the third vertex preserves the original triangle exactly.
            if (area(a, b, c) > 0) {
                Vertex swap = b;
                b = c;
                c = swap;
            }
            result.add(a);
            result.add(b);
            result.add(c);
            result.add(c);
        }

        private static float area(Vertex a, Vertex b, Vertex c) {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        }

        public void submit() {
            List<Vertex> quadVertices = quads();
            if (quadVertices.isEmpty()) return;
            Context context = context();
            ScreenRectangle scissor = context.scissor.get();
            ScreenRectangle bounds = bounds(quadVertices);
            if (scissor != null) bounds = bounds.intersection(scissor);
            if (bounds == null) return;
            context.state.addGuiElement(new Geometry(quadVertices, scissor, bounds));
        }
    }

    static record Vertex(float x, float y, int color) {
    }

    static ScreenRectangle bounds(List<Vertex> vertices) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
        }
        int left = (int) Math.floor(minX), top = (int) Math.floor(minY);
        return new ScreenRectangle(left, top, Math.max(1, (int) Math.ceil(maxX) - left),
                Math.max(1, (int) Math.ceil(maxY) - top));
    }

    private record Geometry(List<Vertex> vertices, @Nullable ScreenRectangle scissorArea,
                            ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (Vertex vertex : vertices)
                consumer.addVertex(vertex.x, vertex.y, 0).setColor(vertex.color);
        }
    }
}
