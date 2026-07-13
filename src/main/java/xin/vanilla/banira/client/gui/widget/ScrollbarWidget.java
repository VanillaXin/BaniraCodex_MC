package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * 滚动条Widget。
 * 支持通过 {@link #addScrollHoverArea(ScreenCoordinate)} 添加额外悬浮区域，
 * 鼠标位于额外悬浮区域时也可响应滚轮滚动，与悬浮于滚动条自身时行为一致。
 */
@Accessors(chain = true, fluent = true)
public class ScrollbarWidget extends BaseWidget {

    @Getter
    @Setter
    private EnumOrientation orientation = EnumOrientation.VERTICAL;

    @Getter
    @Setter
    private double minValue;

    @Getter
    @Setter
    private double maxValue = 100.0;

    @Getter
    @Setter
    private double value;

    @Getter
    @Setter
    private double visibleSize = 10.0;

    @Getter
    @Setter
    private double scrollStep;

    @Getter
    @Setter
    private int bgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).scrollbarBg();

    @Getter
    @Setter
    private int thumbColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).scrollbarThumb();

    @Getter
    @Setter
    private int hoverThumbColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).scrollbarThumbHover();

    @Getter
    @Setter
    private int minThumbSize;

    @Getter
    private double thumbPosition;

    @Getter
    private double thumbSize = 10.0;

    @Getter
    private boolean dragging;

    /**
     * 拖动时的偏移量
     */
    @Getter
    private double dragOffset;

    @Getter
    @Setter
    private Consumer<Double> onValueChanged;

    public ScrollbarWidget(BaniraScreen screen) {
        super(screen);
    }

    public ScrollbarWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        bgColor(theme.scrollbarBg()).thumbColor(theme.scrollbarThumb()).hoverThumbColor(theme.scrollbarThumbHover());
    }

    /**
     * 添加额外悬浮区域，鼠标位于其中时响应滚轮滚动
     */
    public ScrollbarWidget addScrollHoverArea(ScreenCoordinate area) {
        if (scrollingCoordinates == null) {
            scrollingCoordinates = new ArrayList<>();
        }
        scrollingCoordinates.add(area);
        return this;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        updateThumb();

        // 轨道背景
        int x = (int) x();
        int y = (int) y();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();

        ShapeDrawArgs bgRect = ShapeDrawArgs.rect(stack, x, y, width, height, bgColor);
        BaseShapeWidget.drawShape(bgRect);

        // 滑块（根据方向绘制垂直或水平）
        int currentThumbColor = (mouseInside || dragging) ? hoverThumbColor : thumbColor;
        if (orientation == EnumOrientation.VERTICAL) {
            int thumbY = (int) Math.ceil(y + this.thumbPosition);
            int thumbHeight = (int) Math.max(1, this.thumbSize);
            ShapeDrawArgs thumbRect = ShapeDrawArgs.rect(stack, x, thumbY, width, thumbHeight, currentThumbColor);
            BaseShapeWidget.drawShape(thumbRect);
        } else {
            int thumbX = (int) Math.ceil(x + this.thumbPosition);
            int thumbWidth = (int) Math.max(1, this.thumbSize);
            ShapeDrawArgs thumbRect = ShapeDrawArgs.rect(stack, thumbX, y, thumbWidth, height, currentThumbColor);
            BaseShapeWidget.drawShape(thumbRect);
        }

        renderChildren(stack, partialTicks);
    }

    @Override
    public void update() {
        super.update();
        if (!visible || !enabled) {
            return;
        }
        updateThumb();
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        if (event == null) return false;
        boolean result = super.onMouseClick(event);
        if (event.button() == 0 && enabled) {
            updateThumb();

            double mouseX = event.mouseX();
            double mouseY = event.mouseY();
            double absX = absoluteX();
            double absY = absoluteY();

            double relativeClickPos = orientation == EnumOrientation.VERTICAL ? mouseY - absY : mouseX - absX;

            double relativeThumbStart = thumbPosition;
            double relativeThumbEnd = relativeThumbStart + thumbSize;

            // 点击滑块：记录拖动偏移，进入拖动模式
            if (relativeClickPos >= relativeThumbStart && relativeClickPos <= relativeThumbEnd) {
                double relativeThumbCenter = relativeThumbStart + thumbSize / 2.0;
                dragOffset = relativeClickPos - relativeThumbCenter;
                dragging = true;
            } else {
                // 点击轨道：跳转到对应位置并进入拖动模式
                double trackSize = orientation == EnumOrientation.VERTICAL ? height() : width();
                double availableTrack = trackSize - thumbSize;
                if (availableTrack > 0) {
                    double thumbCenterPos = Math.max(thumbSize / 2.0, Math.min(trackSize - thumbSize / 2.0, relativeClickPos));
                    double ratio = (thumbCenterPos - thumbSize / 2.0) / availableTrack;
                    double newValue = minValue + ratio * (maxValue - minValue);
                    setValue(newValue);
                }
                dragOffset = 0.0;
                dragging = true;
            }
            result = true;
        }
        return result;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        boolean result = false;
        if (event != null && event.button() == 0 && dragging) {
            dragging = false;
            dragOffset = 0.0;
            result = true;
        }
        return result;
    }

    @Override
    protected boolean onMouseDrag(MouseDragEvent event) {
        if (!dragging || event == null || event.button() != 0) {
            return false;
        }

        updateThumb();

        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        double trackSize = orientation == EnumOrientation.VERTICAL ? height() : width();
        double clickPos = orientation == EnumOrientation.VERTICAL ? mouseY : mouseX;
        double relativePos = orientation == EnumOrientation.VERTICAL ? clickPos - absY : clickPos - absX;

        double availableTrack = trackSize - thumbSize;
        if (availableTrack <= 0) {
            return false;
        }

        double thumbCenterPos = relativePos - dragOffset;

        thumbCenterPos = Math.max(thumbSize / 2.0, Math.min(trackSize - thumbSize / 2.0, thumbCenterPos));

        double ratio = (thumbCenterPos - thumbSize / 2.0) / availableTrack;
        double newValue = minValue + ratio * (maxValue - minValue);
        setValue(newValue);
        return true;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double scrollDelta = event.delta();

        boolean inScrollArea = false;

        // 滚动条自身区域
        if (renderCoordinate != null) {
            double absX = absoluteX();
            double absY = absoluteY();
            double width = renderCoordinate.width();
            double height = renderCoordinate.height();
            if (mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height) {
                inScrollArea = true;
            }
        }

        // 额外悬浮区域（如列表区域），鼠标在其中时同样响应滚动
        if (!inScrollArea && scrollingCoordinates != null && !scrollingCoordinates.isEmpty()) {
            for (ScreenCoordinate coord : scrollingCoordinates) {
                if (isMouseInCoordinate(mouseX, mouseY, coord)) {
                    inScrollArea = true;
                    break;
                }
            }
        }

        if (!inScrollArea && !focused) {
            return false;
        }

        double relativeMouseX = mouseX - absoluteX() + x();
        double relativeMouseY = mouseY - absoluteY() + y();

        MouseScrollEvent relEvent = MouseScrollEvent.of(relativeMouseX, relativeMouseY, scrollDelta);
        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseScroll(relEvent)) {
                    return true;
                }
            }
        }

        return onMouseScroll(event);
    }

    private boolean isMouseInCoordinate(double mouseX, double mouseY, ScreenCoordinate coord) {
        if (coord == null) {
            return false;
        }
        double coordX = coord.x();
        double coordY = coord.y();
        double coordWidth = coord.width();
        double coordHeight = coord.height();
        return mouseX >= coordX && mouseX < coordX + coordWidth &&
                mouseY >= coordY && mouseY < coordY + coordHeight;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        if (!enabled || event == null) {
            return false;
        }

        double valueRange = maxValue - minValue;
        if (valueRange <= 0) {
            return false;
        }

        double step;
        if (scrollStep > 0) {
            step = scrollStep;
        } else {
            step = Math.max(1.0, visibleSize * 0.33);
        }

        double scrollDelta = event.delta();
        double newValue = value + (scrollDelta < 0 ? step : -step);
        setValue(newValue);
        return true;
    }

    public void setValue(double value) {
        double newValue = Math.max(minValue, Math.min(maxValue, value));
        if (Math.abs(newValue - this.value) > 0.001) {
            this.value = newValue;
            updateThumb();
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }

    private void updateThumb() {
        double trackSize = orientation == EnumOrientation.VERTICAL ? height() : width();
        double valueRange = maxValue - minValue;
        double totalContentSize = visibleSize + valueRange;

        if (valueRange <= 0 || totalContentSize <= visibleSize) {
            thumbSize = trackSize;
            thumbPosition = 0.0;
        } else {
            // 滑块大小与可见内容占比成正比
            thumbSize = (visibleSize / totalContentSize) * trackSize;
            double minSize = minThumbSize > 0 ? minThumbSize : 10.0;
            thumbSize = Math.max(minSize, Math.min(thumbSize, trackSize));

            double ratio = (value - minValue) / valueRange;
            thumbPosition = ratio * (trackSize - thumbSize);
        }
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (!enabled || !focused) {
            return false;
        }

        double valueRange = maxValue - minValue;
        if (valueRange <= 0) {
            return false;
        }

        double step;
        if (scrollStep > 0) {
            step = scrollStep;
        } else {
            step = Math.max(1.0, visibleSize * 0.33);
        }

        int keyCode = event.keyCode();
        boolean handled = false;
        if (orientation == EnumOrientation.VERTICAL) {
            if (keyCode == GLFWKey.GLFW_KEY_UP) {
                setValue(value - step);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_DOWN) {
                setValue(value + step);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_PAGE_UP) {
                setValue(value - visibleSize);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_PAGE_DOWN) {
                setValue(value + visibleSize);
                handled = true;
            }
        } else {
            if (keyCode == GLFWKey.GLFW_KEY_LEFT) {
                setValue(value - step);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_RIGHT) {
                setValue(value + step);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_PAGE_UP) {
                setValue(value - visibleSize);
                handled = true;
            } else if (keyCode == GLFWKey.GLFW_KEY_PAGE_DOWN) {
                setValue(value + visibleSize);
                handled = true;
            }
        }

        return handled;
    }

}
