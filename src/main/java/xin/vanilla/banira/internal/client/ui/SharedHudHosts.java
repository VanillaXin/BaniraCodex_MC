package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiView;

import java.util.function.BooleanSupplier;

public final class SharedHudHosts {
    public static final String NOTIFICATIONS = "banira_codex:notifications";
    public static final String UNREAD = "banira_codex:unread";
    public static final String QUICK_ACTIONS = "banira_codex:quick-actions";
    public static final String POINTER = "banira_codex:pointer";
    private static AuiHudHost host;

    private SharedHudHosts() {
    }

    public interface Renderer {
        void render(MatrixStack graphics, HudDrawList frame);
    }

    public static void install(AuiHudHost next) {
        host = next;
    }

    public static void register(String id, BooleanSupplier visible) {
        host.register(new UiHudSpec(id, "banira_codex/hud/overlay.html", new UiHudBinding() {
            public boolean visible() {
                return visible.getAsBoolean();
            }

            public void bind(UiView view) {
            }

            public void detach(UiDetachReason reason) {
            }
        }, false, true));
    }

    public static boolean render(String id, MatrixStack graphics, HudDrawList frame) {
        return host != null && host.render(id, view -> {
            if (!(view instanceof Renderer)) throw new IllegalStateException("HUD renderer unavailable");
            ((Renderer) view).render(graphics, frame);
        });
    }

    public static boolean canRender(String id) {
        return host != null && host.canRender(id);
    }

    public static void renderOthers(MatrixStack graphics) {
        if (host != null)
            host.renderOthers(com.google.common.collect.ImmutableSet.of(NOTIFICATIONS, UNREAD, QUICK_ACTIONS, POINTER), view -> {
                if (!(view instanceof Renderer)) throw new IllegalStateException("HUD renderer unavailable");
                ((Renderer) view).render(graphics, new HudDrawList());
            });
    }
}
