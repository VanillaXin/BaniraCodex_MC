package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.api.client.ui.*;
import java.util.function.BooleanSupplier;

public final class SharedHudHosts {
    public static final String NOTIFICATIONS = "banira_codex:notifications";
    public static final String UNREAD = "banira_codex:unread";
    public static final String QUICK_ACTIONS = "banira_codex:quick-actions";
    public static final String POINTER = "banira_codex:pointer";
    private static AuiHudHost host;
    private SharedHudHosts() { }
    public interface Renderer { void render(GuiGraphics graphics, HudDrawList frame); }
    public static void install(AuiHudHost next) { host = next; MinimapUiRows.install(next); }
    public static void register(String id, BooleanSupplier visible) {
        host.register(new UiHudSpec(id, "banira_codex/hud/overlay.html", new UiHudBinding() {
            public boolean visible() { return visible.getAsBoolean(); }
            public void bind(UiView view) { }
            public void detach(UiDetachReason reason) { }
        }, false, true));
    }
    public static boolean render(String id, GuiGraphics graphics, HudDrawList frame) {
        return host != null && host.render(id, view -> {
            if (!(view instanceof Renderer)) throw new IllegalStateException("HUD renderer unavailable");
            ((Renderer) view).render(graphics, frame);
        });
    }

    public static void renderOthers(GuiGraphics graphics) {
        if (host != null) host.renderOthers(java.util.Set.of(NOTIFICATIONS, UNREAD, QUICK_ACTIONS, POINTER, MinimapUiRows.ID), view -> {
            if (!(view instanceof Renderer)) throw new IllegalStateException("HUD renderer unavailable");
            ((Renderer) view).render(graphics, new HudDrawList());
        });
    }
}
