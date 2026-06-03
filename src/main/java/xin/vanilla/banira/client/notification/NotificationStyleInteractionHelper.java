package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
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

import java.net.URI;

/**
 * 通知区域对原版 {@link Style} 的点击与悬停提示处理（与聊天组件行为对齐）
 */
public final class NotificationStyleInteractionHelper {

    private NotificationStyleInteractionHelper() {
    }

    public static boolean tryClickStyle(Minecraft mc, Style style) {
        if (style == null || style.getClickEvent() == null) {
            return false;
        }
        Screen screen = mc.screen;
        if (screen != null) {
            return screen.handleComponentClicked(style);
        }
        return handleClickInGame(mc, style.getClickEvent());
    }

    private static boolean handleClickInGame(Minecraft mc, ClickEvent event) {
        if (Screen.hasShiftDown()) {
            return false;
        }
        switch (event.getAction()) {
            case OPEN_URL:
                if (!mc.options.chatLinks) {
                    return false;
                }
                try {
                    URI uri = new URI(event.getValue());
                    Util.getPlatform().openUri(uri);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
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
                mc.keyboardHandler.setClipboard(event.getValue());
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
        Minecraft mc = Minecraft.getInstance();
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        EnumSeason season = mc.screen instanceof BaniraScreen ? ((BaniraScreen) mc.screen).season() : EnumSeason.AUTO;
        boolean useTexture = theme.tooltipUseTexture();

        Component wrapped = BaniraComponent.get().object(tip);
        Text tipText = new Text(wrapped);

        stack.pushPose();
        stack.last().pose().setIdentity();
        try {
            TooltipWidget.drawPopupMessage(stack,
                    FontDrawArgs.ofPopo(tipText.stack(stack).font(AbstractGuiUtils.getFont())).x(mouseX).y(mouseY).popupUseTexture(useTexture),
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
