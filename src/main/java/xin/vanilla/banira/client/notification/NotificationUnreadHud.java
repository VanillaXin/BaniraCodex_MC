package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.util.*;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.internal.client.BaniraClientInputService;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotificationUnreadHud {
    private static final ResourceLocation ICON = new ResourceLocation("banira_codex", "textures/gui/unread_message.png");
    private static final ResourceLocation ICON_EDGE = new ResourceLocation("banira_codex", "textures/gui/unread_message_edge.png");
    public static final int WIDTH = 32, HEIGHT = 22;
    private static final NotificationHudState STATE = new NotificationHudState();
    private static List<String> cachedKeys = Collections.emptyList();
    private static final List<List<Integer>> chords = new ArrayList<>();

    private NotificationUnreadHud() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            STATE.reset();
            NotificationMinimapBridge.reset();
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
                if (!codes.isEmpty() && codes.size() == names.length && codes.stream().allMatch(k -> k >= 32))
                    chords.add(codes);
            }
        }
        boolean active = BaniraClientInputService.isWindowActive();
        boolean down = false;
        if (active) {
            for (List<Integer> chord : chords) {
                if (chord.stream().allMatch(InputStateManager::isKeyPressing)) {
                    down = true;
                    break;
                }
            }
        }
        STATE.update(cfg.mode(), down, active, mc.screen == null);
    }

    private static boolean canRender(Minecraft mc) {
        return mc.level != null && mc.player != null && (mc.screen == null || mc.screen instanceof ChatScreen)
                && !mc.options.hideGui && BaniraClientInputService.isWindowActive()
                && STATE.visible(NotificationManager.get().unreadCount());
    }

    public static boolean isVisible() {
        return canRender(Minecraft.getInstance());
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (button != 0 || !(mc.screen instanceof ChatScreen) || !canRender(mc)
                || NotificationMinimapBridge.selectedHost() != EnumNotificationHudHost.STANDALONE) return false;
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        int x = NotificationHudState.position(cfg.x(), mc.getWindow().getGuiScaledWidth(), WIDTH);
        int y = NotificationHudState.position(cfg.y(), mc.getWindow().getGuiScaledHeight(), HEIGHT);
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) return false;
        mc.setScreen(historyScreen((ChatScreen) mc.screen));
        return true;
    }

    private static NotificationLogScreen historyScreen(ChatScreen chat) {
        String draft = chat.children().stream().filter(TextFieldWidget.class::isInstance)
                .map(TextFieldWidget.class::cast).map(TextFieldWidget::getValue).findFirst().orElse("");
        return new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen(chat)) {
            @Override
            public void onClose() {
                super.onClose();
                if (Minecraft.getInstance().screen == chat) {
                    chat.children().stream().filter(TextFieldWidget.class::isInstance)
                            .map(TextFieldWidget.class::cast).findFirst().ifPresent(input -> input.setValue(draft));
                }
            }
        };
    }

    public static void render(MatrixStack stack) {
        Minecraft mc = Minecraft.getInstance();
        int unread = NotificationManager.get().unreadCount();
        if (!canRender(mc) || NotificationMinimapBridge.selectedHost() != EnumNotificationHudHost.STANDALONE) return;
        ClientConfigView.NotificationHudView cfg = ClientConfig.get().notificationHud();
        draw(stack, NotificationHudState.position(cfg.x(), mc.getWindow().getGuiScaledWidth(), WIDTH),
                NotificationHudState.position(cfg.y(), mc.getWindow().getGuiScaledHeight(), HEIGHT), unread,
                ClientThemeManager.getEffectiveTheme());
    }

    public static void draw(MatrixStack stack, int x, int y, int unread, BaniraColorConfig theme) {
        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.NOTIFICATION.depth() + 1,
                drawStack -> drawContents(drawStack, x, y, unread, theme));
    }

    private static void drawContents(MatrixStack stack, int x, int y, int unread, BaniraColorConfig theme) {
        int color = theme.popupItemText();
        RenderSystem.color4f(1, 1, 1, 1);
        AbstractGuiUtils.blitBlend(stack, ICON, x + 2, y + 4, NotificationHudAppearance.ICON_SIZE,
                NotificationHudAppearance.ICON_SIZE, 0, 0, 12, 12, 12, 12);
        int edgeColor = ColorUtils.ensureReadableTextArgb(color, 0xFFF9F4E5);
        RenderSystem.color4f(((edgeColor >> 16) & 255) / 255f, ((edgeColor >> 8) & 255) / 255f, (edgeColor & 255) / 255f, 1);
        try {
            AbstractGuiUtils.blitBlend(stack, ICON_EDGE, x + 2, y + 4, NotificationHudAppearance.ICON_SIZE,
                    NotificationHudAppearance.ICON_SIZE, 0, 0, 12, 12, 12, 12);
        } finally {
            RenderSystem.color4f(1, 1, 1, 1);
        }
        if (unread > 0) {
            String label = NotificationHudState.countLabel(unread);
            stack.pushPose();
            try {
                stack.translate(x + 17, y + 7, 0);
                stack.scale(NotificationHudAppearance.COUNT_SCALE, NotificationHudAppearance.COUNT_SCALE, 1);
                AbstractGuiUtils.getFont().draw(stack, label, 0, 0, color);
            } finally {
                stack.popPose();
            }
        }
    }
}
