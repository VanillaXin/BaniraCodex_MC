package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.enums.EnumScrollDirection;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.KeyValue;

import java.util.function.Consumer;

/**
 * 滚动条组件
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class ScrollBar {
    private static final int DEFAULT_BG_COLOR = 0xCC232323;
    private static final int DEFAULT_THUMB_COLOR = 0xCC8B8B8B;
    private static final int HOVER_THUMB_COLOR = 0xCCFFFFFF;
    private static final int DEFAULT_WIDTH = 5;
    private static final int MIN_THUMB_SIZE = 2;

    /**
     * 滚动条位置和尺寸
     */
    private double x, y;
    private int width = DEFAULT_WIDTH;
    private int height;

    /**
     * 滚动方向
     */
    private EnumScrollDirection direction = EnumScrollDirection.VERTICAL;

    /**
     * 滚动条样式
     */
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int thumbColor = DEFAULT_THUMB_COLOR;
    private int hoverThumbColor = HOVER_THUMB_COLOR;

    /**
     * 总项目数
     */
    private int totalItems;
    /**
     * 可见项目数
     */
    private int visibleItems;
    /**
     * 当前滚动偏移量
     */
    private int scrollOffset;

    /**
     * 状态
     */
    private boolean hovered = false;
    private boolean dragging = false;
    private double dragStartPos = -1;
    private int dragStartOffset = 0;

    /**
     * 滚动变化回调
     */
    private Consumer<Integer> onScrollChanged;

    /**
     * 获取最大滚动偏移量
     */
    public int getMaxScrollOffset() {
        return Math.max(0, totalItems - visibleItems);
    }

    /**
     * 设置滚动偏移量
     */
    public ScrollBar setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(getMaxScrollOffset(), offset));
        if (onScrollChanged != null) {
            onScrollChanged.accept(this.scrollOffset);
        }
        return this;
    }

    /**
     * 更新滚动参数
     */
    public ScrollBar updateScrollParams(int totalItems, int visibleItems) {
        this.totalItems = totalItems;
        this.visibleItems = visibleItems;
        // 确保滚动偏移量在有效范围内
        setScrollOffset(this.scrollOffset);
        return this;
    }

    /**
     * 判断鼠标是否在滚动条上
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * 判断鼠标是否在滑块上
     */
    public boolean isMouseOverThumb(double mouseX, double mouseY) {
        if (totalItems <= visibleItems) return false;

        KeyValue<Double, Double> thumbBounds = getThumbBounds();
        if (direction == EnumScrollDirection.VERTICAL) {
            return mouseX >= x && mouseX <= x + width && mouseY >= thumbBounds.key() && mouseY <= thumbBounds.key() + thumbBounds.val();
        } else {
            return mouseX >= thumbBounds.key() && mouseX <= thumbBounds.key() + thumbBounds.val() && mouseY >= y && mouseY <= y + height;
        }
    }

    /**
     * 获取滑块的位置和尺寸
     * 垂直方向返回: position:size (y:height)
     * 水平方向返回: position:size (x:width)
     */
    private KeyValue<Double, Double> getThumbBounds() {
        if (totalItems <= visibleItems) {
            if (direction == EnumScrollDirection.VERTICAL) {
                return new KeyValue<>(y, (double) height);
            } else {
                return new KeyValue<>(x, (double) width);
            }
        }

        if (direction == EnumScrollDirection.VERTICAL) {
            double scrollableHeight = height - 2;
            double thumbHeight = Math.max(MIN_THUMB_SIZE, scrollableHeight * visibleItems / (double) totalItems);
            double scrollableRange = scrollableHeight - thumbHeight;
            double thumbY = y + 1 + (scrollableRange * scrollOffset / (double) getMaxScrollOffset());
            return new KeyValue<>(thumbY, thumbHeight);
        } else {
            double scrollableWidth = width - 2;
            double thumbWidth = Math.max(MIN_THUMB_SIZE, scrollableWidth * visibleItems / (double) totalItems);
            double scrollableRange = scrollableWidth - thumbWidth;
            double thumbX = x + 1 + (scrollableRange * scrollOffset / (double) getMaxScrollOffset());
            return new KeyValue<>(thumbX, thumbWidth);
        }
    }

    /**
     * 鼠标按下
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            if (totalItems <= visibleItems) {
                return false;
            }

            if (isMouseOverThumb(mouseX, mouseY)) {
                // 开始拖动
                dragging = true;
                dragStartPos = direction == EnumScrollDirection.VERTICAL ? mouseY : mouseX;
                dragStartOffset = scrollOffset;
            } else {
                // 点击滚动条区域，跳转到对应位置
                if (direction == EnumScrollDirection.VERTICAL) {
                    double scrollableHeight = height - 2;
                    double thumbHeight = Math.max(MIN_THUMB_SIZE, scrollableHeight * visibleItems / (double) totalItems);
                    double scrollableRange = scrollableHeight - thumbHeight;
                    double relativeY = mouseY - y - 1;
                    double ratio = Math.max(0, Math.min(1, relativeY / scrollableRange));
                    setScrollOffset((int) (ratio * getMaxScrollOffset()));
                } else {
                    double scrollableWidth = width - 2;
                    double thumbWidth = Math.max(MIN_THUMB_SIZE, scrollableWidth * visibleItems / (double) totalItems);
                    double scrollableRange = scrollableWidth - thumbWidth;
                    double relativeX = mouseX - x - 1;
                    double ratio = Math.max(0, Math.min(1, relativeX / scrollableRange));
                    setScrollOffset((int) (ratio * getMaxScrollOffset()));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 鼠标释放
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            dragStartPos = -1;
            return true;
        }
        return false;
    }

    /**
     * 鼠标移动（用于拖动）
     */
    public void mouseMoved(double mouseX, double mouseY) {
        hovered = isMouseOver(mouseX, mouseY);

        if (dragging && dragStartPos != -1) {
            if (direction == EnumScrollDirection.VERTICAL) {
                double scrollableHeight = height - 2;
                double thumbHeight = Math.max(MIN_THUMB_SIZE, scrollableHeight * visibleItems / (double) totalItems);
                double scrollableRange = scrollableHeight - thumbHeight;
                double deltaY = mouseY - dragStartPos;
                double scale = getMaxScrollOffset() / scrollableRange;
                setScrollOffset((int) (dragStartOffset + deltaY * scale));
            } else {
                double scrollableWidth = width - 2;
                double thumbWidth = Math.max(MIN_THUMB_SIZE, scrollableWidth * visibleItems / (double) totalItems);
                double scrollableRange = scrollableWidth - thumbWidth;
                double deltaX = mouseX - dragStartPos;
                double scale = getMaxScrollOffset() / scrollableRange;
                setScrollOffset((int) (dragStartOffset + deltaX * scale));
            }
        }
    }

    /**
     * 鼠标滚动
     */
    public boolean mouseScrolled(double delta) {
        if (totalItems > visibleItems) {
            setScrollOffset(scrollOffset - (int) delta);
            return true;
        }
        return false;
    }

    /**
     * 渲染滚动条
     */
    public void render(MatrixStack matrixStack) {
        if (totalItems <= visibleItems) {
            return;
        }

        // 绘制背景
        AbstractGuiUtils.fill(matrixStack, (int) x, (int) y, width, height, backgroundColor);

        // 绘制滑块
        KeyValue<Double, Double> thumbBounds = getThumbBounds();
        int thumbColor = (hovered || dragging) ? this.hoverThumbColor : this.thumbColor;
        
        if (direction == EnumScrollDirection.VERTICAL) {
            int thumbY = (int) Math.ceil(thumbBounds.key());
            int thumbHeight = thumbBounds.val().intValue();
            AbstractGuiUtils.fill(matrixStack, (int) x, thumbY, width, thumbHeight, thumbColor);
        } else {
            int thumbX = (int) Math.ceil(thumbBounds.key());
            int thumbWidth = thumbBounds.val().intValue();
            AbstractGuiUtils.fill(matrixStack, thumbX, (int) y, thumbWidth, height, thumbColor);
        }
    }
}
