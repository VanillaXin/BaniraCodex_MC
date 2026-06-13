package xin.vanilla.banira.client.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.internal.client.BaniraClientAccess;
import xin.vanilla.banira.internal.client.BaniraClientService;

@Getter
@Accessors(fluent = true)
public final class BaniraDrawContext {
    @Getter(AccessLevel.NONE)
    private final Object nativeContext;
    private final int width;
    private final int height;
    private final float partialTicks;

    public BaniraDrawContext(Object nativeContext, int width, int height, float partialTicks) {
        this.nativeContext = nativeContext;
        this.width = width;
        this.height = height;
        this.partialTicks = partialTicks;
    }

    /**
     * Advanced escape hatch for internal/version-local rendering integrations.
     */
    public <T> T nativeContext(Class<T> type) {
        return type.isInstance(nativeContext) ? type.cast(nativeContext) : null;
    }

    public void fill(int x, int y, int width, int height, int argb) {
        clientService().fill(nativeContext, x, y, width, height, argb);
    }

    public void fill(BaniraHudBounds bounds, int argb) {
        if (bounds != null && bounds.isKnown()) {
            fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), argb);
        }
    }

    /**
     * Draws a left-to-right progress fill inside a HUD element rectangle.
     */
    public void fillHorizontalProgress(BaniraHudBounds bounds, float progress, int backgroundArgb, int foregroundArgb) {
        if (bounds == null || !bounds.isKnown()) {
            return;
        }
        fill(bounds, backgroundArgb);
        int filledWidth = Math.max(0, Math.min(bounds.width(), bounds.progressX(progress) - bounds.x()));
        if (filledWidth > 0) {
            fill(bounds.x(), bounds.y(), filledWidth, bounds.height(), foregroundArgb);
        }
    }

    public void horizontalLine(int x, int y, int width, int argb) {
        fill(x, y, width, 1, argb);
    }

    public void verticalLine(int x, int y, int height, int argb) {
        fill(x, y, 1, height, argb);
    }

    /**
     * Draws a rectangular border using the current branch's fill implementation.
     */
    public void outline(int x, int y, int width, int height, int thickness, int argb) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        int line = Math.min(thickness, Math.min(width, height));
        fill(x, y, width, line, argb);
        fill(x, y + height - line, width, line, argb);
        fill(x, y + line, line, Math.max(0, height - line * 2), argb);
        fill(x + width - line, y + line, line, Math.max(0, height - line * 2), argb);
    }

    public void outline(BaniraHudBounds bounds, int thickness, int argb) {
        if (bounds != null && bounds.isKnown()) {
            outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), thickness, argb);
        }
    }

    public int drawText(String text, int x, int y, int argb, boolean shadow) {
        return clientService().drawText(nativeContext, text, x, y, argb, shadow);
    }

    /**
     * Draws text around a center point without exposing the native font class to child mods.
     */
    public int drawCenteredText(String text, int centerX, int y, int argb, boolean shadow) {
        return drawText(text, centerX - textWidth(text) / 2, y, argb, shadow);
    }

    public int drawRightAlignedText(String text, int rightX, int y, int argb, boolean shadow) {
        return drawText(text, rightX - textWidth(text), y, argb, shadow);
    }

    public int textWidth(String text) {
        return clientService().textWidth(text);
    }

    public int lineHeight() {
        return clientService().lineHeight();
    }

    public void fillScreen(int argb) {
        fill(0, 0, width, height, argb);
    }

    public void blit(ResourceLocation texture, int x, int y, double u, double v, int width, int height, int textureWidth, int textureHeight) {
        clientService().blit(nativeContext, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height) {
        blit(texture, x, y, 0, 0, width, height, width, height);
    }

    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
    }

    public void drawTexture(ResourceLocation texture, BaniraHudBounds bounds) {
        if (bounds != null && bounds.isKnown()) {
            drawTexture(texture, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }
    }

    public void push() {
        clientService().pushTransform(nativeContext);
    }

    public void pop() {
        clientService().popTransform(nativeContext);
    }

    public void translate(double x, double y, double z) {
        clientService().translate(nativeContext, x, y, z);
    }

    public void scale(float x, float y, float z) {
        clientService().scale(nativeContext, x, y, z);
    }

    /**
     * Runs drawing with an isolated transform scope.
     */
    public void withTransform(Runnable renderer) {
        push();
        try {
            if (renderer != null) {
                renderer.run();
            }
        } finally {
            pop();
        }
    }

    private static BaniraClientService clientService() {
        return BaniraClientAccess.service();
    }
}
