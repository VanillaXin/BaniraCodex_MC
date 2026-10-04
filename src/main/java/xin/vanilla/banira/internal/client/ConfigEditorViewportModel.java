package xin.vanilla.banira.internal.client;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;

import java.util.ArrayList;

/**
 * 配置编辑器视口几何模型，集中维护卡片、内容区和滚动条位置。
 */
@Getter
@Accessors(fluent = true)
public final class ConfigEditorViewportModel {
    private final int cardMargin;
    private final int cardInner;
    private final int scrollWidth;
    private final int scrollGap;
    private final int contentTopInset;

    private double scrollOffset;
    private int contentHeight;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int listTop;
    private int listAreaHeight;
    private int contentLeft;
    private int contentW;
    private int contentTotalW;

    public ConfigEditorViewportModel(int cardMargin, int cardInner, int scrollWidth, int scrollGap,
                                     int contentTopInset) {
        this.cardMargin = cardMargin;
        this.cardInner = cardInner;
        this.scrollWidth = scrollWidth;
        this.scrollGap = scrollGap;
        this.contentTopInset = contentTopInset;
    }

    /**
     * 按当前屏幕尺寸重算固定区域。
     */
    public void resize(int screenWidth, int screenHeight) {
        cardX = cardMargin;
        cardY = cardMargin;
        cardW = screenWidth - cardMargin * 2;
        cardH = screenHeight - cardMargin * 2;
        contentLeft = cardX + cardInner;
        contentW = cardW - cardInner * 2 - scrollWidth - scrollGap;
        contentTotalW = contentW + scrollGap + scrollWidth;
        listTop = cardY + cardInner + contentTopInset;
    }

    /**
     * 根据内容高度配置滚动条，并把偏移限制在合法范围内。
     */
    public void layoutContent(int newContentHeight, int maxListHeight, ScrollbarWidget scrollbar) {
        contentHeight = newContentHeight;
        if (contentHeight <= maxListHeight) {
            listAreaHeight = Math.max(1, contentHeight);
            scrollOffset = 0;
            scrollbar.maxValue(0);
            scrollbar.value(0);
            scrollbar.visible(false);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            return;
        }

        listAreaHeight = maxListHeight;
        scrollbar.visible(true);
        scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + scrollGap, listTop, scrollWidth, listAreaHeight));
        scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
        scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
        scrollOffset = scrollbar.value();
        scrollbar.visibleSize(listAreaHeight);
        scrollbar.scrollingCoordinates(new ArrayList<>());
        scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop, contentTotalW, listAreaHeight));
    }

    /**
     * 滚动条值变化时同步内容偏移。
     */
    public void applyScrollbarValue(double value) {
        scrollOffset = value;
    }

    /**
     * 鼠标滚轮滚动当前视口。
     */
    public void scrollBy(double delta, ScrollbarWidget scrollbar) {
        double newVal = scrollbar.value() - delta * 20;
        newVal = Math.max(scrollbar.minValue(), Math.min(scrollbar.maxValue(), newVal));
        scrollbar.value(newVal);
        scrollOffset = newVal;
    }

    /**
     * 把内容根控件移动到当前滚动偏移对应的位置。
     */
    public void applyContentBounds(BaseWidget contentRootPanel) {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset, contentW, contentHeight));
        }
    }
}
