package xin.vanilla.banira.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.internal.client.ui.*;

/**
 * 自定义的鼠标光标。
 */
public class MouseWidget extends BaseWidget {
    private final UiPointerState pointer = new UiPointerState();

    public MouseWidget(BaniraScreen screen, boolean useCustomCursor) {
        super(screen, new ScreenCoordinate(0, 0, 1, 1));
        renderDepth(EnumRenderDepth.MOUSE);
        visible = useCustomCursor;
        if (visible) UiSystemCursor.acquire(this, screen);
    }

    public static MouseWidget init(BaniraScreen screen, boolean useCustomCursor) {
        return new MouseWidget(screen, useCustomCursor);
    }

    public static MouseWidget init(BaniraScreen screen, boolean useCustomCursor, int lightColorMain, int darkColorMain) {
        MouseWidget cursor = new MouseWidget(screen, useCustomCursor);
        cursor.pointer.colors(lightColorMain, darkColorMain);
        return cursor;
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        pointer.themeChanged();
    }

    public void removed() {
        UiSystemCursor.release(this);
        pointer.reset();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTicks) {
        draw(graphics, (int) screen.inputState().mouseX(), (int) screen.inputState().mouseY());
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        mouseClicked(event);
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        mouseReleased(event);
        return false;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        mouseScrolled(event);
        return false;
    }

    public void mouseClicked(MouseEvent event) {
        if (event != null) pointer.button(event.button(), true);
    }

    public void mouseReleased(MouseEvent event) {
        if (event != null) pointer.button(event.button(), false);
    }

    public void mouseScrolled(MouseScrollEvent event) {
        if (event != null) pointer.scroll(event.delta());
    }

    private HudDrawList pointerFrame(int x, int y) {
        return pointer.frame(screen.getEffectiveTheme(), x, y, renderDepth().depth());
    }

    public void draw(Matrix3x2fStack stack, int x, int y) {
        if (!visible || !UiSystemCursor.acquire(this, screen)) return;
        NativeGuiGraphics.draw(stack,
                graphics -> pointerFrame(x, y).renderNative(graphics));
    }

    public void draw(GuiGraphicsExtractor graphics, int x, int y) {
        if (!visible || !UiSystemCursor.acquire(this, screen)) return;
        NativeGuiGraphics.bind(graphics);
        HudDrawList frame = pointerFrame(x, y);
        if (!SharedHudHosts.render(SharedHudHosts.POINTER, graphics, frame)) frame.renderNative(graphics);
    }
}
