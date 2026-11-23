package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.client.BaniraClientAccess;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 通知区域对原版 {@link Style} 的点击与悬停提示处理（与聊天组件行为对齐）
 */
public final class NotificationStyleInteractionHelper {

    private NotificationStyleInteractionHelper() {
    }

    public static boolean tryClickStyle(Minecraft mc, Style style) {
        ClickEvent event = style == null ? null : style.getClickEvent();
        if (event == null || event.getAction() == ClickEvent.Action.OPEN_FILE) {
            return false;
        }
        Screen screen = mc.screen;
        if (event.getAction() == ClickEvent.Action.OPEN_URL) {
            return showLinkConfirmation(mc, screen, event.getValue());
        }
        if (screen != null) {
            return screen.handleComponentClicked(style);
        }
        return handleClickInGame(mc, event);
    }

    private static boolean showLinkConfirmation(Minecraft mc, Screen parent, String value) {
        if (Screen.hasShiftDown() || !mc.options.chatLinks || value == null) {
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
        mc.setScreen(new ConfirmOpenLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(uri);
            }
            mc.setScreen(parent);
        }, uri.toString(), false));
        return true;
    }

    private static boolean handleClickInGame(Minecraft mc, ClickEvent event) {
        if (Screen.hasShiftDown()) {
            return false;
        }
        switch (event.getAction()) {
            case RUN_COMMAND:
                if (mc.player == null) {
                    return false;
                }
                String cmd = event.getValue();
                if (!cmd.startsWith("/")) {
                    cmd = "/" + cmd;
                }
                mc.player.chat(cmd);
                return true;
            case SUGGEST_COMMAND:
                mc.setScreen(new ChatScreen(event.getValue()));
                return true;
            case COPY_TO_CLIPBOARD:
                BaniraClientAccess.clipboard(event.getValue());
                return true;
            default:
                return false;
        }
    }

    public static void renderHoverTooltip(MatrixStack stack, int mouseX, int mouseY, int screenW, int screenH, Style style) {
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
        ITextComponent tip = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        if (tip == null) {
            return;
        }
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        renderTextTooltip(stack, mouseX, mouseY, BaniraComponent.get().object(tip), theme);
    }

    public static void renderTextTooltip(MatrixStack stack, int mouseX, int mouseY,
                                         Component component, BaniraColorConfig theme) {
        Minecraft mc = Minecraft.getInstance();
        EnumSeason season = mc.screen instanceof BaniraScreen ? ((BaniraScreen) mc.screen).season() : EnumSeason.AUTO;
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
     * 从悬停事件中提取 ITextComponent（兼容部分实现）
     */
    public static ITextComponent hoverTextOrNull(Style style) {
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
