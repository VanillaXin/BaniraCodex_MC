package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.internal.client.GLFWKeyUtils;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.common.util.ColorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotificationUnreadHud {
    public static final int WIDTH = 48, HEIGHT = 20;
    private static final NotificationHudState STATE = new NotificationHudState();
    private static List<String> cachedKeys = Collections.emptyList();
    private static final List<List<Integer>> chords = new ArrayList<>();

    private NotificationUnreadHud() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        List<String> keys = cfg.keys();
        if (!cachedKeys.equals(keys)) {
            cachedKeys = new ArrayList<>(keys);
            chords.clear();
            for (String chord : keys) {
                String[] names = chord.split("\\+");
                for (int i = 0; i < names.length; i++) names[i] = names[i].trim();
                List<Integer> codes = GLFWKeyUtils.getKeyCodes(names);
                if (!codes.isEmpty() && codes.size() == names.length && codes.stream().allMatch(k -> k >= 32)) chords.add(codes);
            }
        }
        boolean active = mc.level != null && mc.player != null && mc.screen == null && BaniraClientRuntime.isWindowActive();
        boolean down = false;
        if (active) {
            for (List<Integer> chord : chords) {
                if (chord.stream().allMatch(InputStateManager::isKeyPressing)) { down = true; break; }
            }
        }
        STATE.update(cfg.mode(), down, active);
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int unread = NotificationManager.get().unreadCount();
        if (mc.level == null || mc.screen != null || mc.options.hideGui || !BaniraClientRuntime.isWindowActive() || !STATE.visible(unread)) return;
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        draw(graphics, NotificationHudState.position(cfg.x(), mc.getWindow().getGuiScaledWidth(), WIDTH),
                NotificationHudState.position(cfg.y(), mc.getWindow().getGuiScaledHeight(), HEIGHT), unread,
                ClientThemeManager.getEffectiveTheme());
    }

    public static void draw(GuiGraphics graphics, int x, int y, int unread, BaniraColorConfig theme) {
        AbstractGuiUtils.renderByDepth(graphics.pose(), EnumRenderDepth.NOTIFICATION.depth() + 1,
                drawStack -> drawContents(graphics, drawStack, x, y, unread, theme));
    }

    private static void drawContents(GuiGraphics graphics, PoseStack stack, int x, int y, int unread, BaniraColorConfig theme) {
        AbstractGuiUtils.drawRoundedRect(stack, x, y, WIDTH, HEIGHT, 4, 4, 4, 4,
                ColorUtils.applyAlphaToArgb(theme.popupBg(), 180));
        int color = theme.popupItemText();
        AbstractGuiUtils.drawLine(stack, x + 5, y + 5, x + 17, y + 5, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 5, y + 5, x + 5, y + 14, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 17, y + 5, x + 17, y + 14, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 5, y + 14, x + 17, y + 14, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 5, y + 5, x + 11, y + 10, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 11, y + 10, x + 17, y + 5, 1, color);
        graphics.drawString(AbstractGuiUtils.getFont(), NotificationHudState.countLabel(unread), x + 22, y + 6, color, false);
    }
}
