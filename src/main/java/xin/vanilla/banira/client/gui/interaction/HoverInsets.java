package xin.vanilla.banira.client.gui.interaction;

import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 控件视觉边界之外的悬浮扩展量，仅参与 hover，不改变点击与布局范围。
 */
@Value
@Accessors(fluent = true)
public class HoverInsets {
    private static final HoverInsets NONE = new HoverInsets(0.0D, 0.0D, 0.0D, 0.0D);

    double left;
    double top;
    double right;
    double bottom;

    public HoverInsets(double left, double top, double right, double bottom) {
        this.left = Math.max(0.0D, left);
        this.top = Math.max(0.0D, top);
        this.right = Math.max(0.0D, right);
        this.bottom = Math.max(0.0D, bottom);
    }

    public static HoverInsets none() {
        return NONE;
    }

    /**
     * 将相邻项目的视觉间距等分，使两侧 hover 边界恰好相接。
     */
    public static HoverInsets fromSpacing(double horizontalSpacing, double verticalSpacing) {
        double horizontalHalf = Math.max(0.0D, horizontalSpacing) / 2.0D;
        double verticalHalf = Math.max(0.0D, verticalSpacing) / 2.0D;
        return new HoverInsets(horizontalHalf, verticalHalf, horizontalHalf, verticalHalf);
    }

    /**
     * 将单元格内的内容 hover 扩展到单元格间的共享边界，适合物品图标等内缩内容。
     */
    public static HoverInsets partitionCell(double contentX, double contentY,
                                            double contentWidth, double contentHeight,
                                            double cellWidth, double cellHeight,
                                            double horizontalSpacing, double verticalSpacing) {
        double horizontalHalf = Math.max(0.0D, horizontalSpacing) / 2.0D;
        double verticalHalf = Math.max(0.0D, verticalSpacing) / 2.0D;
        return new HoverInsets(
                Math.max(0.0D, contentX) + horizontalHalf,
                Math.max(0.0D, contentY) + verticalHalf,
                Math.max(0.0D, cellWidth - contentX - contentWidth) + horizontalHalf,
                Math.max(0.0D, cellHeight - contentY - contentHeight) + verticalHalf
        );
    }

    public boolean contains(double mouseX, double mouseY,
                            double x, double y, double width, double height) {
        return mouseX >= x - left && mouseX < x + width + right
                && mouseY >= y - top && mouseY < y + height + bottom;
    }
}
