package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import java.util.*;

/**
 * Finite GUI primitives shared by the native and persistent HUD renderers.
 */
public final class HudDrawList {
    public enum Kind {BOX, TEXT, ICON}

    public static final int LIMIT = 4096;

    public static final class Part {
        public final Kind kind;
        public final double x, y, width, height, radius, border, depth;
        public final int color;
        public final String text;
        public final QuickIcon icon;
        public final boolean menuIcon;
        public final ScreenCoordinate clip;

        private Part(Kind kind, double x, double y, double width, double height, double radius, double border,
                     int color, String text, QuickIcon icon, boolean menuIcon, ScreenCoordinate clip, double depth) {
            this.kind = kind;
            this.x = finite(x);
            this.y = finite(y);
            this.width = Math.max(0, finite(width));
            this.height = Math.max(0, finite(height));
            this.radius = Math.max(0, finite(radius));
            this.border = Math.max(0, finite(border));
            this.color = color;
            this.text = text;
            this.icon = icon;
            this.menuIcon = menuIcon;
            this.clip = clip == null ? null : new ScreenCoordinate(clip.x(), clip.y(), clip.width(), clip.height());
            this.depth = depth;
        }

        public String style() {
            return style(new ScreenCoordinate(x, y, width, height));
        }

        public String style(ScreenCoordinate bounds) {
            double scale = width <= 0 ? 1 : bounds.width() / width;
            return "position: absolute; pointer-events: none; left: " + bounds.x() + "px; top: " + bounds.y() + "px; width: " + bounds.width() + "px; height: " + bounds.height()
                    + "px; border-radius: " + radius * scale + "px; " + (border > 0 ? "border: " + border * scale + "px solid " + css(color) + "; box-sizing: border-box;" : "background-color: " + css(color) + ";");
        }
    }

    private final List<Part> parts = new ArrayList<>();
    private ScreenCoordinate clip;
    private double depth = EnumRenderDepth.OVERLAY.depth();

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("HUD coordinate");
        return value;
    }

    public static String css(int color) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.4f)", color >> 16 & 255, color >> 8 & 255, color & 255, (color >>> 24) / 255.0);
    }

    public List<Part> parts() {
        return Collections.unmodifiableList(parts);
    }

    public void clip(double x, double y, double width, double height) {
        clip = new ScreenCoordinate(x, y, width, height);
    }

    public void unclip() {
        clip = null;
    }

    public void depth(double value) {
        depth = finite(value);
    }

    private void add(Part part) {
        if (parts.size() >= LIMIT) throw new IllegalStateException("HUD primitive budget");
        parts.add(part);
    }

    public void box(double x, double y, double w, double h, int color, double radius, double border) {
        add(new Part(Kind.BOX, x, y, w, h, radius, border, color, null, null, false, clip, depth));
    }

    public void text(String text, double x, double y, int color) {
        add(new Part(Kind.TEXT, x, y, 0, 0, 0, 0, color, Objects.requireNonNull(text), null, false, clip, depth));
    }

    public void icon(QuickIcon icon, double x, double y, int size, boolean menu) {
        add(new Part(Kind.ICON, x, y, size, size, 0, 0, 0, null, Objects.requireNonNull(icon), menu, clip, depth));
    }

    public void renderNative(PoseStack graphics) {
        renderContents(graphics, true);
    }

    public void renderContents(PoseStack graphics, boolean nativeShells) {
        for (Part part : parts) {
            if (part.kind == Kind.BOX && !nativeShells) continue;
            List<ScreenCoordinate> regions = Collections.singletonList(part.clip);
            for (ScreenCoordinate region : regions) {
                if (region != null)
                    AbstractGuiUtils.pushScissor((int) region.x(), (int) region.y(), (int) region.width(), (int) region.height());
                try {
                    AbstractGuiUtils.renderByDepth(graphics, (int) part.depth, stack -> {
                        switch (part.kind) {
                            case BOX:
                                ShapeDrawArgs shape = ShapeDrawArgs.rect(stack, (float) part.x, (float) part.y, (float) part.width, (float) part.height, part.color);
                                shape.rect().radius((float) part.radius).border((float) part.border);
                                BaseShapeWidget.drawShape(shape);
                                break;
                            case TEXT:
                                xin.vanilla.banira.internal.compat.aui.LegacyUiGraphics.drawString(graphics, Minecraft.getInstance().font, part.text, (int) part.x, (int) part.y, part.color, false);
                                break;
                            case ICON:
                                if (part.menuIcon)
                                    part.icon.renderForMenu(graphics, Minecraft.getInstance(), (int) part.x, (int) part.y, (int) part.width);
                                else
                                    part.icon.render(graphics, Minecraft.getInstance(), (int) part.x, (int) part.y, (int) part.width);
                                break;
                        }
                    });
                } finally {
                    if (region != null) AbstractGuiUtils.popScissor();
                }
            }
        }
    }
}
