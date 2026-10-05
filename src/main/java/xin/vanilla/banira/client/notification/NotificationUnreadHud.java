package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.client.util.GLFWKeyUtils;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.internal.client.BaniraClientInputService;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.common.util.ColorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotificationUnreadHud {
    public static final int WIDTH = 32, HEIGHT = 22;
    private static final NotificationHudState STATE = new NotificationHudState();
    private static List<String> cachedKeys = Collections.emptyList();
    private static final List<List<Integer>> chords = new ArrayList<>();

    private NotificationUnreadHud() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            STATE.reset();
            return;
        }
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
        boolean active = BaniraClientInputService.isWindowActive();
        boolean down = false;
        if (active) {
            for (List<Integer> chord : chords) {
                if (chord.stream().allMatch(InputStateManager::isKeyPressing)) { down = true; break; }
            }
        }
        STATE.update(cfg.mode(), down, active, mc.screen == null);
    }

    private static boolean canRender(Minecraft mc) {
        return mc.level != null && mc.player != null && (mc.screen == null || mc.screen instanceof ChatScreen)
                && !mc.options.hideGui && BaniraClientInputService.isWindowActive()
                && STATE.visible(NotificationManager.get().unreadCount());
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (button != 0 || !(mc.screen instanceof ChatScreen) || !canRender(mc)) return false;
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        int x = NotificationHudState.position(cfg.x(), mc.getWindow().getGuiScaledWidth(), WIDTH);
        int y = NotificationHudState.position(cfg.y(), mc.getWindow().getGuiScaledHeight(), HEIGHT);
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) return false;
        mc.setScreen(historyScreen((ChatScreen) mc.screen));
        return true;
    }

    private static NotificationLogScreen historyScreen(ChatScreen chat) {
        String draft = chat.children().stream().filter(EditBox.class::isInstance)
                .map(EditBox.class::cast).map(EditBox::getValue).findFirst().orElse("");
        return new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen(chat)) {
            @Override
            public void onClose() {
                super.onClose();
                if (Minecraft.getInstance().screen == chat) {
                    chat.children().stream().filter(EditBox.class::isInstance)
                            .map(EditBox.class::cast).findFirst().ifPresent(input -> input.setValue(draft));
                }
            }
        };
    }

    public static void render(PoseStack stack) {
        Minecraft mc = Minecraft.getInstance();
        int unread = NotificationManager.get().unreadCount();
        if (!canRender(mc)) return;
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        draw(stack, NotificationHudState.position(cfg.x(), mc.getWindow().getGuiScaledWidth(), WIDTH),
                NotificationHudState.position(cfg.y(), mc.getWindow().getGuiScaledHeight(), HEIGHT), unread,
                ClientThemeManager.getEffectiveTheme());
    }

    public static void draw(PoseStack stack, int x, int y, int unread, BaniraColorConfig theme) {
        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.NOTIFICATION.depth() + 1,
                drawStack -> drawContents(drawStack, x, y, unread, theme));
    }

    private static void drawContents(PoseStack stack, int x, int y, int unread, BaniraColorConfig theme) {
        AbstractGuiUtils.drawRoundedRect(stack, x + 1, y + 3, 20, 18, 2, 2, 2, 2,
                ColorUtils.applyAlphaToArgb(theme.popupBg(), 180));
        int color = theme.popupItemText();
        AbstractGuiUtils.drawLine(stack, x + 4, y + 8, x + 18, y + 8, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 4, y + 8, x + 4, y + 17, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 18, y + 8, x + 18, y + 17, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 4, y + 17, x + 18, y + 17, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 4, y + 8, x + 11, y + 13, 1, color);
        AbstractGuiUtils.drawLine(stack, x + 11, y + 13, x + 18, y + 8, 1, color);
        if (unread > 0) {
            String label = NotificationHudState.countLabel(unread);
            int badgeWidth = AbstractGuiUtils.getFont().width(label) + 4;
            int badgeX = x + WIDTH - badgeWidth;
            AbstractGuiUtils.drawRoundedRect(stack, badgeX, y, badgeWidth, 11, 2, 2, 2, 2,
                    theme.popupBg());
            AbstractGuiUtils.getFont().draw(stack, label, badgeX + 2, y + 1, color);
        }
    }
}
