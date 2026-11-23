package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.BaniraVanillaNotificationBridge;
import xin.vanilla.banira.internal.client.InputStateManager;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 通知区域对原版 {@link Style} 的点击与悬停提示处理（与聊天组件行为对齐）
 */
public final class NotificationStyleInteractionHelper {

    private NotificationStyleInteractionHelper() {
    }

    public static boolean tryClickStyle(Style style) {
        ClickEvent event = style == null ? null : style.getClickEvent();
        if (event == null || event.getAction() == ClickEvent.Action.OPEN_FILE) {
            return false;
        }
        Screen screen = BaniraClientRuntime.currentScreen();
        if (event.getAction() == ClickEvent.Action.OPEN_URL) {
            return showLinkConfirmation(screen, event.getValue());
        }
        if (screen != null) {
            return screen.handleComponentClicked(style);
        }
        return handleClickInGame(event);
    }

    private static boolean showLinkConfirmation(Screen parent, String value) {
        if (InputStateManager.isShiftPressingStatic() || !BaniraVanillaNotificationBridge.chatLinksEnabled() || value == null) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(value);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
        } catch (URISyntaxException ignored) {
            return false;
        }
        if (uri.getHost() == null) {
            return false;
        }
        BaniraClientRuntime.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(uri);
            }
            BaniraClientRuntime.setScreen(parent);
        }, uri.toString(), false));
        return true;
    }

    private static boolean handleClickInGame(ClickEvent event) {
        if (InputStateManager.isShiftPressingStatic()) {
            return false;
        }
        switch (event.getAction()) {
            case RUN_COMMAND:
                return BaniraVanillaNotificationBridge.runCommand(event.getValue());
            case SUGGEST_COMMAND:
                BaniraVanillaNotificationBridge.suggestCommand(event.getValue());
                return true;
            case COPY_TO_CLIPBOARD:
                AbstractGuiUtils.setClipboard(event.getValue());
                return true;
            default:
                return false;
        }
    }

    public static void renderHoverTooltip(PoseStack stack, int mouseX, int mouseY, int screenW, int screenH, Style style) {
        if (style == null) {
            return;
        }
        HoverEvent hover = style.getHoverEvent();
        if (hover == null) {
            return;
        }
        if (hover.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return;
        }
        Component tip = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        if (tip == null) {
            return;
        }
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        renderTextTooltip(stack, mouseX, mouseY, BaniraComponent.get().object(tip), theme);
    }

    public static void renderTextTooltip(PoseStack stack, int mouseX, int mouseY,
                                         xin.vanilla.banira.common.data.Component component, BaniraColorConfig theme) {
        Screen screen = BaniraClientRuntime.currentScreen();
        EnumSeason season = screen instanceof BaniraScreen ? ((BaniraScreen) screen).season() : EnumSeason.AUTO;
        Text tipText = new Text(component);

        stack.pushPose();
        stack.last().pose().setIdentity();
        try {
            TooltipWidget.drawPopupMessage(stack,
                    FontDrawArgs.ofPopo(tipText.stack(stack).font(AbstractGuiUtils.getFont())).x(mouseX).y(mouseY).popupUseTexture(theme.tooltipUseTexture()),
                    theme, season);
        } finally {
            stack.popPose();
        }
    }

    /**
     * 从悬停事件中提取 Component（兼容部分实现）
     */
    public static Component hoverTextOrNull(Style style) {
        if (style == null) {
            return null;
        }
        HoverEvent hover = style.getHoverEvent();
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return null;
        }
        return hover.getValue(HoverEvent.Action.SHOW_TEXT);
    }
}
