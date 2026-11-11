package xin.vanilla.banira.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.client.InputStateManager;

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
        if (screen != null) return ClickBridge.handle(mc, screen, style.getClickEvent());
        return handleClickInGame(mc, style.getClickEvent());
    }

    private static boolean handleClickInGame(Minecraft mc, ClickEvent event) {
        if (InputStateManager.isKeyPressing(GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputStateManager.isKeyPressing(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return false;
        }
        return ClickBridge.handle(mc, null, event);
    }

    private abstract static class ClickBridge extends Screen {
        private ClickBridge() {
            super(Component.empty());
        }

        private static boolean handle(Minecraft mc, Screen screen, ClickEvent event) {
            if (event instanceof ClickEvent.OpenUrl url) {
                return mc.options.chatLinks().get() && clickUrlAction(mc, screen, url.uri());
            }
            if (event instanceof ClickEvent.RunCommand) {
                if (mc.player == null) return false;
                defaultHandleClickEvent(event, mc, screen);
                return true;
            }
            if (event instanceof ClickEvent.SuggestCommand || event instanceof ClickEvent.CopyToClipboard) {
                defaultHandleClickEvent(event, mc, screen);
                return true;
            }
            return false;
        }
    }

    /**
     * @param screenW screenH 预留与布局相关扩展，当前由 {@link GuiGraphicsExtractor} 自行处理提示框位置
     */
    public static void renderHoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int screenW, int screenH, Style style) {
        if (style == null) {
            return;
        }
        HoverEvent hover = style.getHoverEvent();
        if (hover == null) {
            return;
        }
        if (!(hover instanceof HoverEvent.ShowText showText)) {
            return;
        }
        Component tip = showText.value();
        if (tip == null) {
            return;
        }
        Matrix3x2fStack stack = graphics.pose();
        Minecraft mc = Minecraft.getInstance();
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        renderTextTooltip(stack, mouseX, mouseY, BaniraComponent.get().object(tip), theme);
    }

    public static void renderTextTooltip(Matrix3x2fStack stack, int mouseX, int mouseY,
                                         xin.vanilla.banira.common.data.Component component, BaniraColorConfig theme) {
        Screen screen = Minecraft.getInstance().screen;
        EnumSeason season = screen instanceof BaniraScreen ? ((BaniraScreen) screen).season() : EnumSeason.AUTO;
        Text tipText = new Text(component);

        stack.pushMatrix();
        stack.identity();
        try {
            TooltipWidget.drawPopupMessage(stack,
                    FontDrawArgs.ofPopo(tipText.stack(stack).font(Minecraft.getInstance().font)).x(mouseX).y(mouseY).popupUseTexture(theme.tooltipUseTexture()),
                    theme, season);
        } finally {
            stack.popMatrix();
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
        if (!(hover instanceof HoverEvent.ShowText showText)) {
            return null;
        }
        return showText.value();
    }
}
