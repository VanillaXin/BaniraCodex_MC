package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumSeason;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static xin.vanilla.banira.client.data.BaniraColorToken.*;

/**
 * 折叠面板控件。支持展开/折叠，嵌套时保持正确的层级结构显示。
 * <p>
 * 结构：标题栏（header）+ 内容区（content）。折叠时仅显示标题栏，展开时显示标题栏与子组件。
 * 子组件通过 {@link #addChild(IWidget)} 添加，其 bounds 相对于本面板左上角，内容区起始于 headerHeight。
 * </p>
 */
@Accessors(chain = true, fluent = true)
public class CollapsiblePanelWidget extends BaseWidget implements ITextWidget {

    /**
     * 默认标题栏高度
     */
    public static final int DEFAULT_HEADER_HEIGHT = 20;

    /**
     * 默认内容区内边距（用于嵌套层级缩进）
     */
    public static final int DEFAULT_CONTENT_INDENT = 16;

    /**
     * 自动堆叠时，未指定高度子元素的默认行高
     */
    public static final int DEFAULT_ROW_HEIGHT = 18;

    @Getter
    private Text text = Text.empty();

    @Getter
    private boolean expanded = true;

    @Getter
    @Setter
    private int headerHeight = DEFAULT_HEADER_HEIGHT;

    @Getter
    @Setter
    private int contentIndent = DEFAULT_CONTENT_INDENT;

    /**
     * 内容区内部左边距
     */
    @Getter
    @Setter
    private int paddingLeft = 2;

    /**
     * 内容区内部右边距
     */
    @Getter
    @Setter
    private int paddingRight = 0;

    /**
     * 内容区内部上边距
     */
    @Getter
    @Setter
    private int paddingTop = 2;

    /**
     * 内容区内部下边距
     */
    @Getter
    @Setter
    private int paddingBottom = 2;

    /**
     * 子元素间垂直间距（默认 0 表示紧贴堆叠）
     */
    @Getter
    @Setter
    private int contentGap = 0;

    /**
     * 左侧边框线宽度
     */
    @Getter
    @Setter
    private int borderLeftWidth = 1;

    /**
     * 右侧边框线宽度
     */
    @Getter
    @Setter
    private int borderRightWidth = 0;

    /**
     * 底部边框线宽度
     */
    @Getter
    @Setter
    private int borderBottomWidth = 0;

    public CollapsiblePanelWidget borderWidth(int width) {
        return this.borderLeftWidth(width).borderBottomWidth(width).borderRightWidth(width);
    }

    /**
     * 内容区高度（展开时）。若 <= 0 则根据子组件自动计算
     */
    @Getter
    @Setter
    private double contentHeight = 0;

    @Getter
    @Setter
    private int headerBgColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, BG_SECONDARY);

    @Getter
    @Setter
    private int headerHoverBgColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, BG_TERTIARY);

    @Getter
    @Setter
    private int headerBorderColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, TEXT_HINT);

    @Getter
    @Setter
    private int textColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, TEXT_PRIMARY);

    @Getter
    @Setter
    private Consumer<CollapsiblePanelWidget> onExpandChanged;

    /**
     * 存储展开时的完整高度，用于折叠时恢复
     */
    private double expandedHeightCache = 0;

    /**
     * 子组件处理点击时，应获得焦点的目标（供 getFocusTarget 使用）
     */
    @Nullable
    private IWidget lastClickFocusTarget;

    /**
     * 绝对屏幕坐标裁剪区；仅跳过不可见子树的渲染/鼠标/update，不参与布局计算。
     */
    @Nullable
    private ScreenCoordinate renderViewport;

    public CollapsiblePanelWidget(BaniraScreen screen) {
        super(screen);
    }

    public CollapsiblePanelWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
        refreshLayout();
    }

    /**
     * 创建面板，仅指定位置和宽度，高度由内容自动计算。
     */
    public static CollapsiblePanelWidget createAutoHeight(BaniraScreen screen, double x, double y, double width) {
        return new CollapsiblePanelWidget(screen, new ScreenCoordinate(x, y, width, 0));
    }

    /**
     * 创建子面板，宽度自适应为当前面板内容区最大宽度。用于嵌套时子面板及其子元素能正确填满可用宽度。
     *
     * @return 新建的子面板（未加入当前面板，需后续调用 {@link #addCollapsibleChild(CollapsiblePanelWidget)}）
     */
    public CollapsiblePanelWidget createChildPanel() {
        return new CollapsiblePanelWidget(screen, new ScreenCoordinate(0, 0, getContentWidth(), 0));
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        double ox = x();
        double oy = y();
        double w = width();
        boolean isExpanded = expanded;

        stack.pushPose();
        stack.translate(ox, oy, 0);

        // region 绘制标题栏
        int headerBg = mouseInside ? headerHoverBgColor : headerBgColor;
        AbstractGuiUtils.fill(stack, 0, 0, (int) w, headerHeight, headerBg);

        if (borderBottomWidth > 0) {
            AbstractGuiUtils.fill(stack, 0, headerHeight - borderBottomWidth, (int) w, borderBottomWidth, headerBorderColor);
        }

        int arrowSize = 8;
        int arrowX = contentIndent / 2 - arrowSize / 2;
        int arrowY = (headerHeight - arrowSize) / 2;
        int arrowColor = textColor;
        if (isExpanded) {
            drawArrowDown(stack, arrowX, arrowY, arrowSize, arrowColor);
        } else {
            drawArrowRight(stack, arrowX, arrowY, arrowSize, arrowColor);
        }

        int textX = contentIndent;
        int textY = (headerHeight - 9) / 2;
        if (text != null && !text.content().isEmpty()) {
            FontDrawArgs args = FontDrawArgs.of(
                    text.stack(stack).font(screen != null ? screen.getFont() : AbstractGuiUtils.getFont()).color(textColor));
            args.x(textX).y(textY).maxWidth((int) Math.max(0, w - textX - 4)).wrap(false).inScreen(false);
            LabelWidget.drawLimitedText(args);
        }
        stack.popPose();
        // endregion 绘制标题栏

        // region 绘制内容区（仅展开时）
        if (isExpanded && !children.isEmpty()) {
            renderVisibleChildren(stack, partialTicks);
        }
        // endregion 绘制内容区

        // region 绘制左右下边框线
        if (borderLeftWidth > 0 || borderRightWidth > 0 || (isExpanded && borderBottomWidth > 0)) {
            stack.pushPose();
            stack.translate(ox, oy, 0);
            int totalH = (int) height();
            if (borderLeftWidth > 0) {
                AbstractGuiUtils.fill(stack, 0, 0, borderLeftWidth, totalH, headerBorderColor);
            }
            if (borderRightWidth > 0) {
                AbstractGuiUtils.fill(stack, (int) w - borderRightWidth, 0, borderRightWidth, totalH, headerBorderColor);
            }
            if (isExpanded && borderBottomWidth > 0) {
                AbstractGuiUtils.fill(stack, 0, totalH - borderBottomWidth, (int) w, borderBottomWidth, headerBorderColor);
            }
            stack.popPose();
        }
        // endregion 绘制边框线
    }

    private void drawArrowDown(PoseStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 90, color);
    }

    private void drawArrowRight(PoseStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 0, color);
    }

    public CollapsiblePanelWidget renderViewport(@Nullable ScreenCoordinate viewport) {
        this.renderViewport = viewport;
        return this;
    }

    private void renderVisibleChildren(PoseStack stack, float partialTicks) {
        if (!visible || children.isEmpty()) {
            return;
        }

        stack.pushPose();
        stack.translate(x(), y(), 0);

        for (IWidget child : children) {
            if (shouldRenderChild(child)) {
                applyRenderViewportToChild(child);
                child.render(stack, partialTicks);
            }
        }

        stack.popPose();
    }

    private boolean shouldRenderChild(@Nullable IWidget child) {
        return child != null && child.visible() && shouldProcessChildInViewport(child);
    }

    private boolean shouldUpdateChild(@Nullable IWidget child) {
        return child != null && child.visible() && child.enabled() && child.needsUpdate()
                && shouldProcessChildInViewport(child);
    }

    /**
     * 鼠标事件只分发给当前裁剪区内的子控件，避免滚动面板扫描大量不可见组件。
     */
    @Nullable
    private IWidget findHandlingViewportChild(ChildEventDispatcher dispatcher) {
        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled() && shouldProcessChildInViewport(child)) {
                applyRenderViewportToChild(child);
                if (dispatcher.dispatch(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * 嵌套面板继承父级裁剪区，只影响渲染/update 的跳过判断，不改布局。
     */
    private void applyRenderViewportToChild(IWidget child) {
        if (child instanceof CollapsiblePanelWidget panelWidget) {
            panelWidget.renderViewport(renderViewport);
        }
    }

    private boolean shouldProcessChildInViewport(IWidget child) {
        if (renderViewport == null || child == null) {
            return true;
        }
        double vx = renderViewport.x();
        double vy = renderViewport.y();
        double vw = renderViewport.width();
        double vh = renderViewport.height();
        double x1 = child.absoluteX();
        double y1 = child.absoluteY();
        ScreenCoordinate bounds = child.bounds();
        double childWidth = bounds != null ? bounds.width() : 1;
        double x2 = x1 + Math.max(1, childWidth);
        double y2 = y1 + Math.max(1, child.effectiveHeight());
        return x2 >= vx && x1 <= vx + vw && y2 >= vy && y1 <= vy + vh;
    }

    private boolean isMouseInsideRenderViewport(double mouseX, double mouseY) {
        if (renderViewport == null) {
            return true;
        }
        return mouseX >= renderViewport.x()
                && mouseX < renderViewport.x() + renderViewport.width()
                && mouseY >= renderViewport.y()
                && mouseY < renderViewport.y() + renderViewport.height();
    }

    @Override
    public void update() {
        if (!visible || !enabled) {
            return;
        }
        updateInteractiveState();
        if (expanded) {
            for (IWidget child : children) {
                if (shouldUpdateChild(child)) {
                    applyRenderViewportToChild(child);
                    child.update();
                }
            }
        }
    }

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        double relY = mouseY - absY;

        if (isMouseInsideRenderViewport(mouseX, mouseY) && isMouseInside(mouseX, mouseY, absX, absY)) {
            lastClickFocusTarget = null;
            if (relY < headerHeight && event.button() == 0) {
                toggleExpanded();
                return true;
            }
            if (expanded) {
                IWidget handlingChild = findHandlingViewportChild(child -> child.handleMouseClick(event));
                if (handlingChild != null) {
                    lastClickFocusTarget = handlingChild;
                    return true;
                }
            }
            return onMouseClick(event);
        }
        return false;
    }

    @Override
    public boolean handleMouseRelease(MouseEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (!isMouseInsideRenderViewport(event.mouseX(), event.mouseY()) && !mousePressed) {
            return false;
        }

        if (expanded) {
            if (findHandlingViewportChild(child -> child.handleMouseRelease(event)) != null) {
                return true;
            }
        }

        boolean wasPressed = mousePressed && pressedMouseButton == event.button();
        if (wasPressed) {
            lastReleaseWasLongPress = false;
            mousePressed = false;
            pressedMouseButton = -1;
            boolean inside = isMouseInside(event.mouseX(), event.mouseY(), absoluteX(), absoluteY());
            return onMouseRelease(event, inside);
        }
        return false;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (!isMouseInsideRenderViewport(event.mouseX(), event.mouseY()) && !focused) {
            return false;
        }

        if (expanded) {
            if (findHandlingViewportChild(child -> child.handleMouseScroll(event)) != null) {
                return true;
            }
        }

        if (focused || isMouseInside(event.mouseX(), event.mouseY(), absoluteX(), absoluteY())) {
            return onMouseScroll(event);
        }
        return false;
    }

    @Override
    public boolean handleMouseDrag(MouseDragEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (!isMouseInsideRenderViewport(event.mouseX(), event.mouseY()) && !mousePressed) {
            return false;
        }

        if (expanded) {
            if (findHandlingViewportChild(child -> child.handleMouseDrag(event)) != null) {
                return true;
            }
        }

        if (mousePressed) {
            return onMouseDrag(event);
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(KeyEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (expanded) {
            if (findHandlingChild(child -> child.handleKeyPress(event)) != null) {
                return true;
            }
        }
        return onKeyPress(event);
    }

    @Override
    public boolean handleKeyRelease(KeyEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (expanded) {
            if (findHandlingChild(child -> child.handleKeyRelease(event)) != null) {
                return true;
            }
        }
        return onKeyRelease(event);
    }

    @Override
    public boolean handleCharTyped(CharInputEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (expanded) {
            if (findHandlingChild(child -> child.handleCharTyped(event)) != null) {
                return true;
            }
        }
        return onCharTyped(event);
    }

    @Override
    public IWidget getFocusTarget() {
        IWidget target = lastClickFocusTarget != null ? lastClickFocusTarget.getFocusTarget() : this;
        lastClickFocusTarget = null;
        return target;
    }

    /**
     * 切换展开/折叠状态，并更新面板高度
     */
    public CollapsiblePanelWidget toggleExpanded() {
        return expanded(!expanded);
    }

    /**
     * 设置展开状态，并同步更新 bounds 高度
     */
    public CollapsiblePanelWidget expanded(boolean expanded) {
        if (this.expanded == expanded) {
            return this;
        }
        this.expanded = expanded;
        updateHeightFromExpanded();
        if (onExpandChanged != null) {
            onExpandChanged.accept(this);
        }
        return this;
    }

    /**
     * 当子面板展开/折叠时，由子面板调用以同步外层面板高度。
     * 也可在外部手动调用以根据当前子组件重新计算高度。
     */
    public void refreshHeightFromContent() {
        updateHeightFromExpanded();
    }

    /**
     * 当任意子控件高度变化时，重排该子控件下方的兄弟元素并更新本面板高度。
     * 会向上传播，刷新所有祖先面板的高度。
     */
    public void refreshLayoutFromChild(IWidget child) {
        int idx = children.indexOf(child);
        if (idx < 0) return;
        ScreenCoordinate cb = child.bounds();
        double childY = cb != null ? cb.y() : 0;
        double runningY = childY + child.effectiveHeight() + contentGap;
        for (int i = idx + 1; i < children.size(); i++) {
            IWidget sibling = children.get(i);
            if (sibling == null || !sibling.visible()) continue;
            ScreenCoordinate b = sibling.bounds();
            if (b != null && sibling instanceof BaseWidget baseWidget) {
                double sh = sibling.effectiveHeight();
                baseWidget.bounds(new ScreenCoordinate(b.x(), runningY, b.width(), sh));
                runningY += sh + contentGap;
            }
        }
        contentHeight = 0;
        refreshHeightFromContent();
        IWidget p = parent();
        if (p instanceof CollapsiblePanelWidget panelWidget) {
            panelWidget.refreshLayoutFromChild(this);
        }
    }

    /**
     * 子面板展开/折叠时：更新本面板高度，并重排 toggledChild 下方的兄弟元素位置。
     */
    void refreshHeightAndReorderSiblings(CollapsiblePanelWidget toggledChild) {
        int idx = children.indexOf(toggledChild);
        if (idx < 0) return;
        double runningY = toggledChild.y() + toggledChild.effectiveHeight() + contentGap;
        for (int i = idx + 1; i < children.size(); i++) {
            IWidget sibling = children.get(i);
            if (sibling == null || !sibling.visible()) continue;
            ScreenCoordinate b = sibling.bounds();
            if (b != null && sibling instanceof BaseWidget baseWidget) {
                double sh = sibling.effectiveHeight();
                baseWidget.bounds(new ScreenCoordinate(b.x(), runningY, b.width(), sh));
                runningY += sh + contentGap;
            }
        }
        refreshHeightFromContent();
    }

    /**
     * 根据展开状态更新面板高度
     */
    private void updateHeightFromExpanded() {
        ScreenCoordinate coord = bounds();
        if (coord == null) {
            return;
        }
        if (expanded) {
            double contentH = contentHeight > 0 ? contentHeight : computeContentHeight();
            expandedHeightCache = (contentHeight > 0 ? headerHeight + contentH : contentH) + paddingBottom + borderBottomWidth;
            coord.height(expandedHeightCache);
        } else {
            coord.height(headerHeight);
        }
        invalidateAbsCache();
    }

    /**
     * 根据子组件计算内容区高度（使用 effectiveHeight 支持动态尺寸控件）
     */
    private double computeContentHeight() {
        double maxBottom = 0;
        for (IWidget child : children) {
            if (child == null || !child.visible()) {
                continue;
            }
            ScreenCoordinate b = child.bounds();
            if (b != null) {
                double bottom = b.y() + child.effectiveHeight();
                if (bottom > maxBottom) {
                    maxBottom = bottom;
                }
            }
        }
        return Math.max(0, maxBottom);
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        if (theme != null) {
            headerBgColor(theme.color(BG_SECONDARY));
            headerHoverBgColor(theme.color(BG_TERTIARY));
            headerBorderColor(theme.color(TEXT_HINT));
            textColor(theme.color(TEXT_PRIMARY));
        }
    }

    @Override
    public void addChild(IWidget child) {
        if (child instanceof CollapsiblePanelWidget panelWidget) {
            bindCollapsibleChildExpandListener(panelWidget);
        }
        super.addChild(child);
        if (expanded) {
            updateHeightFromExpanded();
        }
    }

    /**
     * 为嵌套的折叠面板绑定展开/折叠回调，使外层面板高度同步更新，并重排下方兄弟元素
     */
    private void bindCollapsibleChildExpandListener(CollapsiblePanelWidget childPanel) {
        Consumer<CollapsiblePanelWidget> existing = childPanel.onExpandChanged();
        childPanel.onExpandChanged(panel -> {
            IWidget childThatChanged = panel;
            for (IWidget parentPanel = panel.parent(); parentPanel != null; parentPanel = parentPanel.parent()) {
                if (parentPanel instanceof CollapsiblePanelWidget panelWidget) {
                    if (childThatChanged instanceof CollapsiblePanelWidget child) {
                        panelWidget.refreshHeightAndReorderSiblings(child);
                    }
                }
                childThatChanged = parentPanel;
            }
            if (existing != null) {
                existing.accept(panel);
            }
        });
    }

    /**
     * 添加子元素并自动堆叠布局。不要求指定高度，子元素将垂直排列，面板高度自动计算。
     *
     * @param child 子组件
     * @return this，便于链式调用
     */
    public CollapsiblePanelWidget addChildAuto(IWidget child) {
        return addChildAuto(child, 0);
    }

    /**
     * 添加子元素并自动堆叠布局。
     *
     * @param child           子组件
     * @param preferredHeight 期望高度，若 &lt;= 0 则使用 {@link #DEFAULT_ROW_HEIGHT}
     * @return this，便于链式调用
     */
    public CollapsiblePanelWidget addChildAuto(IWidget child, double preferredHeight) {
        if (child == null) return this;
        double cx = getContentStartX();
        double cy = getNextContentY();
        double cw = getContentWidth();
        double ch = child.effectiveHeight() > 0 ? child.effectiveHeight()
                : (preferredHeight > 0 ? preferredHeight : DEFAULT_ROW_HEIGHT);
        ScreenCoordinate existing = child.bounds();
        if (ch <= 0 && existing != null && existing.height() > 0) {
            ch = existing.height();
        }
        if (child instanceof BaseWidget baseWidget) {
            baseWidget.bounds(new ScreenCoordinate(cx, cy, cw, ch));
        }
        addChild(child);
        return this;
    }

    /**
     * 设置 bounds 后若需根据内容自动计算高度，可调用此方法。
     * 若通过 {@link #bounds(ScreenCoordinate)} 传入的 height 大于 headerHeight，会自动推断 contentHeight。
     * 会先递归刷新子折叠面板的布局，再计算本面板高度。
     */
    public CollapsiblePanelWidget refreshLayout() {
        boolean hasCollapsibleChildren = false;
        for (IWidget child : children) {
            if (child instanceof CollapsiblePanelWidget panelWidget) {
                hasCollapsibleChildren = true;
                panelWidget.refreshLayout();
            }
        }
        ScreenCoordinate coord = bounds();
        // 若有可折叠子面板，其高度会动态变化，不得缓存 contentHeight，否则父面板高度无法随子面板展开/折叠更新
        if (!hasCollapsibleChildren && coord != null && coord.height() > headerHeight + borderBottomWidth && contentHeight <= 0) {
            contentHeight = coord.height() - headerHeight - borderBottomWidth;
        }
        updateHeightFromExpanded();
        return this;
    }

    /**
     * 获取嵌套层级深度（用于自定义缩进）。根级为 0，每层嵌套 +1。
     */
    public int getNestingDepth() {
        int depth = 0;
        IWidget p = parent();
        while (p != null) {
            if (p instanceof CollapsiblePanelWidget) {
                depth++;
            }
            p = p.parent();
        }
        return depth;
    }

    /**
     * 获取当前层级建议的内容区起始 X（用于嵌套子面板的缩进）
     */
    public double getContentStartX() {
        return borderLeftWidth + paddingLeft;
    }

    /**
     * 获取内容区可用宽度。
     */
    public double getContentWidth() {
        return Math.max(0, width() - borderLeftWidth - borderRightWidth - paddingLeft - paddingRight);
    }

    /**
     * 获取当前层级建议的内容区起始 Y
     */
    public double getContentStartY() {
        return headerHeight + paddingTop;
    }

    /**
     * 获取下一个可用的内容区 Y 坐标（用于垂直堆叠子组件）。
     * 基于已有子组件的最大 bottom 计算，若有子组件则加 contentGap，若无则返回 getContentStartY()。
     */
    public double getNextContentY() {
        double maxBottom = getContentStartY();
        boolean hasChildren = false;
        for (IWidget child : children) {
            if (child == null || !child.visible()) continue;
            hasChildren = true;
            ScreenCoordinate b = child.bounds();
            if (b != null) {
                double bottom = b.y() + child.effectiveHeight();
                if (bottom > maxBottom) maxBottom = bottom;
            }
        }
        return hasChildren ? maxBottom + contentGap : maxBottom;
    }

    /**
     * 添加嵌套的折叠面板子项，高度根据其子组件自动计算。
     *
     * @param childPanel 子折叠面板（需先向其添加子组件）
     * @return this，便于链式调用
     */
    public CollapsiblePanelWidget addCollapsibleChild(CollapsiblePanelWidget childPanel) {
        return addCollapsibleChild(childPanel, 0);
    }

    /**
     * 添加嵌套的折叠面板子项，自动计算合适的 bounds（缩进、宽度、垂直堆叠）。
     *
     * @param childPanel  子折叠面板
     * @param childHeight 子面板内容区高度（不含 header），若 &lt;= 0 则根据其子组件自动计算
     * @return this，便于链式调用
     */
    public CollapsiblePanelWidget addCollapsibleChild(CollapsiblePanelWidget childPanel, double childHeight) {
        if (childPanel == null) return this;
        double cx = getContentStartX();
        double cy = getNextContentY();
        double cw = getContentWidth();
        double ch;
        if (childPanel.expanded()) {
            double contentH = childHeight > 0 ? childHeight : childPanel.computeContentAreaHeightPublic();
            ch = headerHeight + contentH + borderBottomWidth;
        } else {
            ch = headerHeight;
        }
        childPanel.bounds(new ScreenCoordinate(cx, cy, cw, ch));
        addChild(childPanel);
        return this;
    }

    /**
     * 供 addCollapsibleChild 调用的公开方法，计算内容区高度（不含 header）
     */
    double computeContentAreaHeightPublic() {
        double maxBottom = computeContentHeight();
        return Math.max(0, maxBottom - getContentStartY());
    }

    /**
     * 供内部使用的公开方法，计算子组件最大 bottom 坐标
     */
    double computeContentHeightPublic() {
        return computeContentHeight();
    }

    // region ITextWidget

    public CollapsiblePanelWidget text(String text) {
        this.text = Text.literal(text);
        return this;
    }

    public CollapsiblePanelWidget text(xin.vanilla.banira.common.data.Component text) {
        this.text = Text.from(text);
        return this;
    }

    public CollapsiblePanelWidget text(Text text) {
        this.text = text;
        return this;
    }

    // endregion ITextWidget
}
