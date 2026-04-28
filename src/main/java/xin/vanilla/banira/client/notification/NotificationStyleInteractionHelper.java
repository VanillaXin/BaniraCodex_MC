package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
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
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.net.URI;

/**
 * 通知区域对原版 {@link Style} 的点击与悬停提示处理（与聊天组件行为对齐）
 */
@Environment(EnvType.CLIENT)
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
                if (!mc.options.chatLinks().get()) {
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
                String commandLine = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                mc.player.commandSigned(commandLine, Component.empty());
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
        Minecraft mc = Minecraft.getInstance();
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        EnumSeason season = mc.screen instanceof BaniraScreen ? ((BaniraScreen) mc.screen).season() : EnumSeason.AUTO;
        boolean useTexture = theme.tooltipUseTexture();

        xin.vanilla.banira.common.data.Component wrapped = BaniraComponent.get().object(tip);
        Text tipText = new Text(wrapped);

        stack.pushPose();
        stack.last().pose().setIdentity();
        try {
            TooltipWidget.drawPopupMessage(stack,
                    FontDrawArgs.ofPopo(tipText.stack(stack).font(mc.font)).x(mouseX).y(mouseY).popupUseTexture(useTexture),
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
