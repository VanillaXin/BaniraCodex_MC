package xin.vanilla.banira.api.client.ui;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Finite, immutable HUD geometry; no input or business callbacks.
 */
public record UiHudFrame(List<Part> parts) {
    public UiHudFrame {
        parts = List.copyOf(parts);
        if (parts.size() > 16) throw new IllegalArgumentException("HUD frame budget");
        Set<String> ids = new HashSet<>();
        for (Part part : parts) if (!ids.add(part.id())) throw new IllegalArgumentException("Duplicate HUD part");
    }

    public sealed interface Part permits Sprite, Text {
        String id();
    }

    public record Sprite(String id, ResourceLocation texture, double x, double y, double width, double height,
                         double angleDegrees) implements Part {
        public Sprite {
            validate(id, x, y, width, height, angleDegrees);
            Objects.requireNonNull(texture);
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("HUD sprite size");
        }
    }

    public record Text(String id, String value, double x, double y, double width, double height, float fontSize,
                       double scale, double angleRadians, double pivotX, double pivotY, int color) implements Part {
        public Text {
            validate(id, x, y, width, height, fontSize, scale, angleRadians, pivotX, pivotY);
            Objects.requireNonNull(value);
            if (width < 0 || height < 0 || fontSize <= 0 || scale <= 0 || value.length() > 4096)
                throw new IllegalArgumentException("HUD text size");
        }
    }

    private static void validate(String id, double... values) {
        if (id == null || !id.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("HUD part id");
        for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("HUD geometry");
    }
}
