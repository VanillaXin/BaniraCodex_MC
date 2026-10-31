package xin.vanilla.banira.api.client.ui;

import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Finite, immutable HUD geometry; no input or business callbacks.
 */
@lombok.Value
@lombok.experimental.Accessors(fluent = true)
public class UiHudFrame {
    List<Part> parts;

    public UiHudFrame(List<Part> parts) {
        this.parts = com.google.common.collect.ImmutableList.copyOf(parts);
        if (this.parts.size() > 16) throw new IllegalArgumentException("HUD frame budget");
        Set<String> ids = new HashSet<>();
        for (Part part : this.parts) {
            if (!(part instanceof Sprite) && !(part instanceof Text)) throw new IllegalArgumentException("Unknown HUD part");
            if (!ids.add(part.id())) throw new IllegalArgumentException("Duplicate HUD part");
        }
    }

    public interface Part {
        String id();
    }

    @lombok.Value
    @lombok.experimental.Accessors(fluent = true)
    public static class Sprite implements Part {
        String id;
        ResourceLocation texture;
        double x, y, width, height, angleDegrees;

        public Sprite(String id, ResourceLocation texture, double x, double y, double width, double height,
                      double angleDegrees) {
            validate(id, x, y, width, height, angleDegrees);
            Objects.requireNonNull(texture);
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("HUD sprite size");
            this.id = id;
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angleDegrees = angleDegrees;
        }
    }

    @lombok.Value
    @lombok.experimental.Accessors(fluent = true)
    public static class Text implements Part {
        String id, value;
        double x, y, width, height;
        float fontSize;
        double scale, angleRadians, pivotX, pivotY;
        int color;

        public Text(String id, String value, double x, double y, double width, double height, float fontSize,
                    double scale, double angleRadians, double pivotX, double pivotY, int color) {
            validate(id, x, y, width, height, fontSize, scale, angleRadians, pivotX, pivotY);
            Objects.requireNonNull(value);
            if (width < 0 || height < 0 || fontSize <= 0 || scale <= 0 || value.length() > 4096)
                throw new IllegalArgumentException("HUD text size");
            this.id = id;
            this.value = value;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fontSize = fontSize;
            this.scale = scale;
            this.angleRadians = angleRadians;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.color = color;
        }
    }

    private static void validate(String id, double... values) {
        if (id == null || !id.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("HUD part id");
        for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("HUD geometry");
    }
}
