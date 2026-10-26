package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Unsupported compatibility shim. Shared minimap rows always use their native map renderer. */
@Deprecated
public final class MinimapUiRows implements AutoCloseable {
    public static final String ID = "banira_codex:minimap";

    public interface Renderer {
        void renderRow(GuiGraphics graphics, String key, Component text, ScreenCoordinate bounds,
                       int color, boolean shadow, float depth);
    }

    private static xin.vanilla.banira.api.client.ui.UiHudBinding nativeOnlyBinding() {
        return new xin.vanilla.banira.api.client.ui.UiHudBinding() {
            public boolean visible() { return false; }
            public void bind(xin.vanilla.banira.api.client.ui.UiView view) { }
            public void detach(xin.vanilla.banira.api.client.ui.UiDetachReason reason) { }
        };
    }

    private static final class Tagged { }

    public MinimapUiRows(AuiHudHost host, LongSupplier clock) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(clock);
    }

    public static void install(AuiHudHost host) {
    }

    public static Component tag(EnumNotificationHudHost host, ResourceLocation id, Component text) {
        return text;
    }

    public static boolean owns(EnumNotificationHudHost host, Component text) {
        return false;
    }

    public static void reset() {
    }

    public static ResourceLocation partId(ResourceLocation id, int index) {
        return new ResourceLocation(id.getNamespace(), id.getPath() + "/line_" + index);
    }

    public static boolean drawOwned(EnumNotificationHudHost host, GuiGraphics graphics, Component text,
                                    ScreenCoordinate bounds, int color, boolean shadow, Runnable flush) {
        return false;
    }

    public static boolean draw(GuiGraphics graphics, Component text, ScreenCoordinate bounds, int color, boolean shadow) {
        return false;
    }

    boolean paint(GuiGraphics graphics, String key, Component text, ScreenCoordinate bounds,
                  int color, boolean shadow, float depth, Runnable flush) {
        return false;
    }

    public static Matrix4f guiProjection(Matrix4f projection, int width, int height) {
        if (width <= 0 || height <= 0 || !projection.isFinite() || projection.m00() <= 0 || projection.m11() >= 0
                || Math.abs(projection.m01()) > .00001 || Math.abs(projection.m10()) > .00001
                || Math.abs(projection.m02()) > .00001 || Math.abs(projection.m12()) > .00001
                || Math.abs(projection.m20()) > .00001 || Math.abs(projection.m21()) > .00001
                || Math.abs(projection.m03()) > .00001 || Math.abs(projection.m13()) > .00001
                || Math.abs(projection.m23()) > .00001 || Math.abs(projection.m33() - 1) > .00001) return null;
        return new Matrix4f(projection).m00(2f / width).m11(-2f / height).m30(-1).m31(1);
    }

    public static Matrix4f guiPose(Matrix4f pose, Matrix4f projection, int width, int height) {
        if (guiProjection(projection, width, height) == null || !pose.isFinite()) return null;
        // Native map labels may use window pixels while AUI uses scaled GUI units.
        return new Matrix4f().translation((projection.m30() + 1) * width / 2f,
                        (1 - projection.m31()) * height / 2f, 0)
                .scale(projection.m00() * width / 2f, -projection.m11() * height / 2f, 1).mul(pose);
    }

    public static ScreenCoordinate project(Matrix4f pose, ScreenCoordinate bounds, int width, int height) {
        if (!valid(bounds) || !pose.isFinite()
                || pose.m00() <= 0 || pose.m11() <= 0 || Math.abs(pose.m01()) > .00001
                || Math.abs(pose.m10()) > .00001 || Math.abs(pose.m02()) > .00001 || Math.abs(pose.m12()) > .00001
                || Math.abs(pose.m20()) > .00001 || Math.abs(pose.m21()) > .00001
                || Math.abs(pose.m03()) > .00001 || Math.abs(pose.m13()) > .00001 || Math.abs(pose.m23()) > .00001
                || Math.abs(pose.m33() - 1) > .00001 || !Float.isFinite(pose.m32())) return null;
        ScreenCoordinate result = new ScreenCoordinate(pose.m30() + bounds.x() * pose.m00(),
                pose.m31() + bounds.y() * pose.m11(), bounds.width() * pose.m00(), bounds.height() * pose.m11());
        if (!valid(result) || result.x() < 0 || result.y() < 0 || result.x() + result.width() > width + .01
                || result.y() + result.height() > height + .01) return null;
        return result;
    }

    public static ScreenCoordinate fit(ScreenCoordinate measured, ScreenCoordinate reserved) {
        if (!valid(measured) || !valid(reserved)) return null;
        double scale = Math.min(1, Math.min(reserved.width() / measured.width(), reserved.height() / measured.height()));
        double width = measured.width() * scale;
        return new ScreenCoordinate(reserved.x() + (reserved.width() - width) / 2, reserved.y(), width, measured.height() * scale);
    }

    private static boolean valid(ScreenCoordinate bounds) {
        return bounds != null && Double.isFinite(bounds.x()) && Double.isFinite(bounds.y())
                && Double.isFinite(bounds.width()) && Double.isFinite(bounds.height()) && bounds.width() > 0 && bounds.height() > 0;
    }

    @Override public void close() {
    }
}
