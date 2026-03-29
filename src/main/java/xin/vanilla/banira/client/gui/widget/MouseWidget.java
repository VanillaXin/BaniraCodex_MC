package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.util.ColorUtils;

/**
 * 自定义的鼠标光标
 */
@OnlyIn(Dist.CLIENT)
public class MouseWidget extends BaseWidget {

    private long drawCount = 0;
    /**
     * 鼠标状态 1 鼠标左键按下 2 鼠标右键按下 4 鼠标中键按下
     */
    private int status = 0;
    /**
     * 鼠标滚动状态
     */
    private float scroll = 0;

    /**
     * 自定义颜色
     */
    private Integer customLightMain;
    private Integer customDarkMain;

    private int curColorMain;
    private int curColorPressed;

    /**
     * @param useCustomCursor true：隐藏系统光标并绘制自定义指针；false：保留系统光标且不绘制
     */
    public MouseWidget(BaniraScreen screen, boolean useCustomCursor) {
        super(screen, new ScreenCoordinate(0, 0, 1, 1));
        this.renderDepth(EnumRenderDepth.MOUSE);
        if (useCustomCursor) {
            hideSystemCursor();
        } else {
            this.visible = false;
        }
    }

    /**
     * 初始化鼠标光标
     */
    public static MouseWidget init(BaniraScreen screen, boolean useCustomCursor) {
        return new MouseWidget(screen, useCustomCursor);
    }

    /**
     * 初始化鼠标光标（指定主色）
     */
    public static MouseWidget init(BaniraScreen screen, boolean useCustomCursor, int lightColorMain, int darkColorMain) {
        MouseWidget cursor = new MouseWidget(screen, useCustomCursor);
        cursor.customLightMain = lightColorMain;
        cursor.customDarkMain = darkColorMain;
        cursor.curColorMain = lightColorMain;
        return cursor;
    }

    private int getLightMain() {
        return customLightMain != null ? customLightMain : screen.getEffectiveTheme().cursorLightMain();
    }

    private int getDarkMain() {
        return customDarkMain != null ? customDarkMain : screen.getEffectiveTheme().cursorDarkMain();
    }

    private int getLightPressed() {
        return screen.getEffectiveTheme().cursorLightPressed();
    }

    private int getDarkPressed() {
        return screen.getEffectiveTheme().cursorDarkPressed();
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        if (customLightMain == null) {
            this.drawCount = 0;
        }
    }

    private void hideSystemCursor() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    /**
     * Screen 关闭时恢复系统鼠标
     */
    public void removed() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) return;
        int mouseX = (int) screen.inputState().mouseX();
        int mouseY = (int) screen.inputState().mouseY();
        drawCursor(stack, mouseX, mouseY);
    }

    private static final float BRIGHTNESS_THRESHOLD = 0.5f;

    private void drawCursor(MatrixStack stack, int mouseX, int mouseY) {
        if (this.drawCount % 10 == 0) {
            int pixelColor = AbstractGuiUtils.getPixelArgb(mouseX, mouseY);
            float bgBrightness = ColorUtils.getBrightnessFromArgb(pixelColor);
            boolean useDark = bgBrightness < BRIGHTNESS_THRESHOLD;
            this.curColorMain = useDark ? getDarkMain() : getLightMain();
            this.curColorPressed = useDark ? getDarkPressed() : getLightPressed();
        }
        this.drawCount++;

        int pressedColor = curColorPressed;
        // status 位掩码：1=左键 2=右键 4=中键，组合按下时对应区域用 pressedColor
        int color1 = (status == 1 || status == 3 || status == 5 || status == 7) ? pressedColor : curColorMain;
        int color2 = (status == 2 || status == 3 || status == 6 || status == 7) ? pressedColor : curColorMain;
        int color3 = (status == 4 || status == 5 || status == 6 || status == 7) ? pressedColor : curColorMain;

        this.scroll *= 0.72f;
        if (Math.abs(this.scroll) < 0.5f) this.scroll = 0;
        int scrollOffset = (int) this.scroll;

        AbstractGuiUtils.renderByDepth(stack, renderDepth(), (s) -> {
            drawPointerShape(s, mouseX, mouseY + scrollOffset, color1, color2, color3);
            // 滚轮指示线：滚动时在指针旁显示短线
            if (scrollOffset != 0) {
                int scrollLineAlpha = (int) (0x90 * Math.min(1, Math.abs(this.scroll) / 4));
                int scrollLineColor = (scrollLineAlpha << 24) | (curColorMain & 0xFFFFFF);
                int y1 = mouseY;
                int y2 = mouseY + scrollOffset;
                if (y1 > y2) {
                    int t = y1;
                    y1 = y2;
                    y2 = t;
                }
                AbstractGuiUtils.fill(s, mouseX + 6, y1, 1, y2 + 1 - y1, scrollLineColor);
            }
        });
    }

    /**
     * 绘制指针形状
     */
    private void drawPointerShape(MatrixStack stack, int x, int y, int colorLeft, int colorRight, int colorCenter) {
        // 中心/中键
        AbstractGuiUtils.drawPixel(stack, x, y, colorCenter);
        // 左键区域
        AbstractGuiUtils.fill(stack, x - 4, y + 2, 3, 1, colorLeft);
        AbstractGuiUtils.fill(stack, x - 2, y + 2, 1, 3, colorLeft);
        AbstractGuiUtils.fill(stack, x - 4, y - 2, 3, 1, colorLeft);
        AbstractGuiUtils.fill(stack, x - 2, y - 4, 1, 3, colorLeft);
        // 右键区域
        AbstractGuiUtils.fill(stack, x + 2, y + 2, 3, 1, colorRight);
        AbstractGuiUtils.fill(stack, x + 2, y + 2, 1, 3, colorRight);
        AbstractGuiUtils.fill(stack, x + 2, y - 2, 3, 1, colorRight);
        AbstractGuiUtils.fill(stack, x + 2, y - 4, 1, 3, colorRight);
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        if (event != null) updateMouseStatus(event.button(), true);
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        if (event != null) updateMouseStatus(event.button(), false);
        return false;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        if (event != null) accumulateScroll(event.delta());
        return false;
    }

    private void updateMouseStatus(int button, boolean pressed) {
        int op = pressed ? 1 : -1;
        switch (button) {
            case GLFWKey.GLFW_MOUSE_BUTTON_LEFT:
                this.status += 1 * op;
                break;
            case GLFWKey.GLFW_MOUSE_BUTTON_RIGHT:
                this.status += 2 * op;
                break;
            case GLFWKey.GLFW_MOUSE_BUTTON_MIDDLE:
                this.status += 4 * op;
                break;
        }
        if (this.status < 0) this.status = 0;
        else if (this.status > 7) this.status = 7;
    }

    /**
     * 供 BaniraScreen 直接调用，转发到 handleMouseClick
     */
    public void mouseClicked(MouseEvent event) {
        if (event != null) updateMouseStatus(event.button(), true);
    }

    /**
     * 供 BaniraScreen 直接调用，转发到 handleMouseRelease
     */
    public void mouseReleased(MouseEvent event) {
        if (event != null) updateMouseStatus(event.button(), false);
    }

    /**
     * 供 BaniraScreen 直接调用，转发到 handleMouseScroll
     */
    public void mouseScrolled(MouseScrollEvent event) {
        if (event != null) accumulateScroll(event.delta());
    }

    private void accumulateScroll(double delta) {
        float add = (float) Math.max(-6, Math.min(6, delta * 3));
        this.scroll += add;
        this.scroll = Math.max(-8, Math.min(8, this.scroll));
    }

    /**
     * 供 BaniraScreen 直接调用绘制（在顶层渲染）
     */
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!visible) return;
        drawCursor(stack, mouseX, mouseY);
    }
}
