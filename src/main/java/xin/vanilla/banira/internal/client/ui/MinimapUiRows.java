package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.*;
import java.util.function.LongSupplier;

public final class MinimapUiRows implements AutoCloseable {
    public static final String ID = "banira_codex:minimap";
    private static final long LIVE_NANOS = 500_000_000L;
    private static final Map<String, Tagged> TAGS = new LinkedHashMap<>();
    private static final Map<ComponentContents, Tagged> IDENTITIES = new IdentityHashMap<>();
    private static MinimapUiRows installed;
    private final AuiHudHost host;
    private final LongSupplier clock;
    private final AutoCloseable lease;
    private long lastDraw;
    private boolean active;

    public interface Renderer {
        void renderRow(GuiGraphics graphics, String key, Component text, ScreenCoordinate bounds,
                       int color, boolean shadow, float depth);
    }

    public MinimapUiRows(AuiHudHost host, LongSupplier clock) {
        this.host = Objects.requireNonNull(host);
        this.clock = Objects.requireNonNull(clock);
        lease = host.register(new UiHudSpec(ID, "banira_codex/hud/minimap.html", new UiHudBinding() {
            public boolean visible() { long age = clock.getAsLong() - lastDraw; return active && age >= 0 && age <= LIVE_NANOS; }
            public void bind(UiView view) { }
            public void detach(UiDetachReason reason) { }
        }));
    }

    public static void install(AuiHudHost host) {
        if (installed != null) installed.close();
        installed = new MinimapUiRows(host, System::nanoTime);
    }

    public static synchronized Component tag(EnumNotificationHudHost host, ResourceLocation id, Component text) {
        String key = host.name() + "/" + id;
        Tagged previous = TAGS.get(key);
        if (previous != null && text != null && previous.value.equals(text.getString()) && plain(text)
                && previous.value.equals(previous.text.getString()) && plain(previous.text)) return previous.text;
        if (previous != null) IDENTITIES.remove(previous.text.getContents());
        TAGS.remove(key);
        if (text == null || text.getString().isEmpty() || !plain(text)) return text;
        if (TAGS.size() >= 120) return text;
        // A fresh literal identity survives Component.copy without matching unrelated equal labels.
        Component copy = Component.literal(text.getString());
        Tagged next = new Tagged(host, key, copy);
        TAGS.put(key, next);
        IDENTITIES.put(copy.getContents(), next);
        return copy;
    }

    private static boolean plain(Component text) {
        return !text.visit((style, fragment) -> style.isEmpty() ? Optional.empty() : Optional.of(Boolean.TRUE), Style.EMPTY).isPresent();
    }

    public static synchronized boolean owns(EnumNotificationHudHost host, Component text) {
        Tagged tag = text == null ? null : IDENTITIES.get(text.getContents());
        return tag != null && tag.host == host && tag.value.equals(text.getString()) && plain(text) && tag.text.equals(text);
    }

    public static synchronized void reset() {
        TAGS.clear(); IDENTITIES.clear();
        if (installed != null) installed.active = false;
    }

    public static ResourceLocation partId(ResourceLocation id, int index) {
        return new ResourceLocation(id.getNamespace(), id.getPath() + "/line_" + index);
    }

    public static boolean drawOwned(EnumNotificationHudHost host, GuiGraphics graphics, Component text,
                                    ScreenCoordinate bounds, int color, boolean shadow, Runnable flush) {
        return owns(host, text) && draw(graphics, text, bounds, color, shadow, flush);
    }

    public static boolean draw(GuiGraphics graphics, Component text, ScreenCoordinate bounds, int color, boolean shadow) {
        return draw(graphics, text, bounds, color, shadow, graphics::flush);
    }

    private static boolean draw(GuiGraphics graphics, Component text, ScreenCoordinate bounds, int color,
                                boolean shadow, Runnable flush) {
        Tagged tag;
        synchronized (MinimapUiRows.class) { tag = text == null ? null : IDENTITIES.get(text.getContents()); }
        if (installed == null || tag == null || !owns(tag.host, text)) return false;
        Minecraft mc = Minecraft.getInstance();
        ScreenCoordinate projected = project(graphics.pose().last().pose(), bounds,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        if (projected == null) return false;
        return installed.paint(graphics, tag.key, text, projected, color, shadow,
                graphics.pose().last().pose().m32(), flush);
    }

    boolean paint(GuiGraphics graphics, String key, Component text, ScreenCoordinate bounds,
                  int color, boolean shadow, float depth, Runnable flush) {
        if (!valid(bounds)) return false;
        active = true; lastDraw = clock.getAsLong();
        return host.render(ID, view -> {
            if (!(view instanceof Renderer)) throw new IllegalStateException("Minimap UI renderer unavailable");
            flush.run();
            ((Renderer) view).renderRow(graphics, key, text, bounds, color, shadow, depth);
        });
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
        active = false;
        try { lease.close(); } catch (Exception failure) { throw new IllegalStateException("Cannot release minimap UI", failure); }
    }

    private static final class Tagged {
        final EnumNotificationHudHost host;
        final String key;
        final Component text;
        final String value;
        Tagged(EnumNotificationHudHost host, String key, Component text) { this.host = host; this.key = key; this.text = text; this.value = text.getString(); }
    }
}
