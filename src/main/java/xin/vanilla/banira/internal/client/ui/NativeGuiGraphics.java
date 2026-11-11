package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Connects matrix-only callbacks to the current GUI extraction pass.
 */
public final class NativeGuiGraphics {
    private static final ThreadLocal<WeakReference<GuiGraphicsExtractor>> CURRENT = new ThreadLocal<>();

    private NativeGuiGraphics() {
    }

    public static Matrix3x2fStack bind(GuiGraphicsExtractor graphics) {
        CURRENT.set(new WeakReference<>(Objects.requireNonNull(graphics)));
        return graphics.pose();
    }

    public static GuiGraphicsExtractor current() {
        try {
            return xin.vanilla.banira.client.util.ExtractedGuiRenderer.graphics();
        } catch (IllegalStateException noSharedPass) {
            // Direct widget callers can supply an extractor without a custom-shape pass.
        }
        WeakReference<GuiGraphicsExtractor> reference = CURRENT.get();
        GuiGraphicsExtractor graphics = reference == null ? null : reference.get();
        if (graphics == null) throw new IllegalStateException("No active GUI extraction pass");
        return graphics;
    }

    public static void draw(Matrix3x2fStack matrix, Consumer<GuiGraphicsExtractor> draw) {
        GuiGraphicsExtractor graphics = current();
        Matrix3x2fStack pose = graphics.pose();
        if (pose == matrix) {
            draw.accept(graphics);
            return;
        }
        pose.pushMatrix();
        try {
            pose.set(matrix);
            draw.accept(graphics);
        } finally {
            pose.popMatrix();
        }
    }

    public static void text(Matrix3x2fStack matrix, Font font, FormattedCharSequence text,
                            float x, float y, int color, boolean shadow) {
        draw(matrix, graphics -> {
            var pose = graphics.pose();
            pose.pushMatrix();
            try {
                pose.translate(x, y);
                graphics.text(font, text, 0, 0, color, shadow);
            } finally {
                pose.popMatrix();
            }
        });
    }
}
