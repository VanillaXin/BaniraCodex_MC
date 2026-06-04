package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.*;
import xin.vanilla.banira.common.enums.EnumPosition;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Widget抽象基类
 */
@Accessors(chain = true, fluent = true)
public abstract class BaseWidget implements IWidget {

    // region 基础属性

    @Getter
    @Setter
    protected BaniraScreen screen;

    protected String id;

    @Override
    @Nullable
    public String id() {
        return id;
    }

    @Override
    public void id(@Nullable String id) {
        this.id = id;
    }

    @Getter
    protected ScreenCoordinate renderCoordinate;

    public void renderCoordinate(ScreenCoordinate coord) {
        this.renderCoordinate = coord != null ? coord : new ScreenCoordinate();
        invalidateAbsCache();
    }

    @Getter
    @Setter
    protected List<ScreenCoordinate> hoveringCoordinates = new ArrayList<>();

    @Getter
    @Setter
    protected List<ScreenCoordinate> scrollingCoordinates = new ArrayList<>();

    protected boolean visible = true;

    protected boolean enabled = true;

    protected final Map<String, Object> properties = new HashMap<>();

    protected IWidget parent;

    protected final List<IWidget> children = new ArrayList<>();

    /**
     * absoluteX/absoluteY 缓存，parent 或 bounds 变更时失效
     */
    private double cachedAbsX, cachedAbsY;
    private boolean absCacheValid = false;

    protected boolean mouseInside = false;

    protected boolean mousePressed = false;

    protected int pressedMouseButton = -1;

    /**
     * 双击检测：上次点击时间与位置
     */
    private long lastClickTime = 0;
    private double lastClickX = 0;
    private double lastClickY = 0;
    private int lastClickButton = -1;

    private static final long DOUBLE_CLICK_MS = 300;
    private static final double DOUBLE_CLICK_DIST = 5;

    /**
     * 长按检测：按下开始时间，释放时重置
     */
    private long pressStartTime = 0;
    /**
     * 长按已触发标志，避免重复回调
     */
    private boolean longPressFired = false;
    /**
     * 长按默认阈值 ms
     */
    protected static final long LONG_PRESS_MS = 500;

    @Getter
    protected boolean focused = false;

    public void focused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
        }
    }

    @Getter
    @Setter
    private double rotation = 0.0;

    @Getter
    @Setter
    private EnumPosition rotationCenter = EnumPosition.CENTER;

    @Getter
    @Setter
    private double scale = 1.0;

    @Getter
    @Setter
    private int alpha = 255;

    /**
     * 渲染深度
     */
    @Getter
    @Setter
    protected EnumRenderDepth renderDepth = EnumRenderDepth.FOREGROUND;

    protected BaseWidget(BaniraScreen screen) {
        this.screen = screen;
        this.renderCoordinate = new ScreenCoordinate();
    }

    protected BaseWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        this.screen = screen;
        this.renderCoordinate = bounds != null ? bounds : new ScreenCoordinate();
    }

    // endregion 基础属性

    /**
     * 应用主题到本组件及子组件。子类可重写以设置自身颜色，并调用 super.applyTheme(theme)。
     */
    public void applyTheme(BaniraColorConfig theme) {
        for (IWidget child : children) {
            if (child instanceof BaseWidget) {
                child.applyTheme(theme);
            }
        }
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (!visible || !enabled) {
            return;
        }
        if (screen != null) {
            updateMouseHover(screen.inputState().mouseX(), screen.inputState().mouseY());
            if (mousePressed && !longPressFired && (System.currentTimeMillis() - pressStartTime) >= genericLongPressThresholdMs()) {
                longPressFired = true;
                onLongPress(MouseEvent.of(screen.inputState().mouseX(), screen.inputState().mouseY(), pressedMouseButton));
            }
        }
        for (IWidget child : children) {
            if (child != null && child.visible() && child.enabled() && child.needsUpdate()) {
                child.update();
            }
        }
    }

    // region 事件处理

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        int mouseButton = event.button();
        double absX = absoluteX();
        double absY = absoluteY();

        lastClickFocusTarget = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseClick(event)) {
                    lastClickFocusTarget = child;
                    return true;
                }
            }
        }
        if (isMouseInside(mouseX, mouseY, absX, absY)) {
            mousePressed = true;
            pressedMouseButton = mouseButton;
            pressStartTime = System.currentTimeMillis();
            longPressFired = false;
            return onMouseClick(event);
        }

        return false;
    }

    @Override
    public boolean handleMouseRelease(MouseEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        int mouseButton = event.button();
        double absX = absoluteX();
        double absY = absoluteY();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseRelease(event)) {
                    return true;
                }
            }
        }
        boolean wasPressed = mousePressed && pressedMouseButton == mouseButton;
        if (wasPressed) {
            lastReleaseWasLongPress = longPressFired;
            mousePressed = false;
            pressedMouseButton = -1;
            longPressFired = false;
            boolean inside = isMouseInside(mouseX, mouseY, absX, absY);
            return onMouseRelease(event, inside);
        }

        return false;
    }

    @Override
    public boolean handleKeyPress(KeyEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleKeyPress(event)) {
                    return true;
                }
            }
        }
        return onKeyPress(event);
    }

    @Override
    public boolean handleKeyRelease(KeyEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleKeyRelease(event)) {
                    return true;
                }
            }
        }
        return onKeyRelease(event);
    }

    @Override
    public boolean handleCharTyped(CharInputEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleCharTyped(event)) {
                    return true;
                }
            }
        }

        return onCharTyped(event);
    }

    @Override
    public boolean handleMouseDrag(MouseDragEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseDrag(event)) {
                    return true;
                }
            }
        }

        if (mousePressed) {
            return onMouseDrag(event);
        }
        return false;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseScroll(event)) {
                    return true;
                }
            }
        }

        if (focused || isMouseInside(mouseX, mouseY, absX, absY)) {
            return onMouseScroll(event);
        }
        return false;
    }

    @Override
    public void visible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
        }
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public void enabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public ScreenCoordinate bounds() {
        return renderCoordinate;
    }

    /**
     * 设置布局边界，与 {@link #bounds()} 对应。链式调用时推荐使用此方法。
     *
     * @return this，便于链式调用
     */
    @SuppressWarnings("unchecked")
    public final <S extends BaseWidget> S bounds(ScreenCoordinate coord) {
        this.renderCoordinate = coord != null ? coord : new ScreenCoordinate();
        invalidateAbsCache();
        return (S) this;
    }

    @Override
    public void property(String key, Object value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    @Override
    @Nullable
    public Object property(String key) {
        return properties.get(key);
    }

    @Override
    public boolean isMouseInside(double mouseX, double mouseY) {
        if (renderCoordinate == null) {
            return false;
        }
        return isMouseInside(mouseX, mouseY, absoluteX(), absoluteY());
    }

    /**
     * 由调用方传入已计算的绝对坐标，避免重复递归计算。
     * 在 handleMouseClick/Release/Scroll 等已计算 absX/absY 的场景下使用。
     */
    protected boolean isMouseInside(double mouseX, double mouseY, double absX, double absY) {
        return hitTest(mouseX, mouseY, absX, absY);
    }

    /**
     * 命中检测，判断鼠标是否在组件区域内。子类可重写以支持非矩形区域（如圆形、多边形）。
     *
     * @param mouseX 屏幕坐标 X
     * @param mouseY 屏幕坐标 Y
     * @param absX   组件绝对 X
     * @param absY   组件绝对 Y
     * @return 是否命中
     */
    protected boolean hitTest(double mouseX, double mouseY, double absX, double absY) {
        if (renderCoordinate == null) {
            return false;
        }
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        return mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;
    }

    /**
     * 判断本次点击是否为双击。时间窗口 {@value #DOUBLE_CLICK_MS}ms，位置容差 {@value #DOUBLE_CLICK_DIST}px。
     * 子类在 {@link #onMouseClick} 中调用，若返回 true 可执行 {@link #onDoubleClick} 逻辑。
     *
     * @param mouseX      当前点击 X
     * @param mouseY      当前点击 Y
     * @param mouseButton 当前点击按钮
     * @return 是否为双击
     */
    protected boolean isDoubleClick(double mouseX, double mouseY, int mouseButton) {
        long now = System.currentTimeMillis();
        boolean isDouble = (now - lastClickTime < DOUBLE_CLICK_MS)
                && Math.abs(mouseX - lastClickX) <= DOUBLE_CLICK_DIST
                && Math.abs(mouseY - lastClickY) <= DOUBLE_CLICK_DIST
                && lastClickButton == mouseButton;
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;
        lastClickButton = mouseButton;
        return isDouble;
    }

    /**
     * 判断本次点击是否为双击（事件对象重载）。
     */
    protected boolean isDoubleClick(MouseEvent event) {
        if (event == null) {
            return false;
        }
        return event.clickTracked()
                ? event.doubleClick()
                : isDoubleClick(event.mouseX(), event.mouseY(), event.button());
    }

    /**
     * 双击回调。子类重写以处理双击，默认空实现。
     *
     * @param event 鼠标事件
     * @return 是否消费事件
     */
    protected boolean onDoubleClick(MouseEvent event) {
        return false;
    }

    /**
     * 判断当前按下是否已达长按阈值。子类可在按下期间（如 update）调用。
     *
     * @param mouseButton 按钮
     * @param thresholdMs 阈值毫秒数
     * @return 是否已长按
     */
    protected boolean isLongPress(int mouseButton, long thresholdMs) {
        return mousePressed && pressedMouseButton == mouseButton
                && (System.currentTimeMillis() - pressStartTime) >= thresholdMs;
    }

    /**
     * 上次释放时是否为长按。在 {@link #onMouseRelease} 中可读取以区分单击与长按。
     */
    protected boolean lastReleaseWasLongPress = false;

    /**
     * 长按回调。按下超过 {@link #genericLongPressThresholdMs()} 时在 update 中触发一次。
     *
     * @param event 鼠标事件（当前坐标与按钮）
     */
    protected void onLongPress(MouseEvent event) {
    }

    /**
     * 基类长按检测阈值（毫秒）。子类可增大以禁用默认长按（例如由子类自行计时）。
     */
    protected long genericLongPressThresholdMs() {
        return LONG_PRESS_MS;
    }

    /**
     * 当前按下起始时间（与 {@link #isLongPress} 一致），用于子类自定义长按进度等。
     */
    protected long mousePressStartMillis() {
        return pressStartTime;
    }

    /**
     * 子组件处理点击时，应获得焦点的目标
     */
    @Nullable
    protected IWidget lastClickFocusTarget;

    @Override
    public IWidget getFocusTarget() {
        IWidget target = lastClickFocusTarget != null ? lastClickFocusTarget.getFocusTarget() : this;
        lastClickFocusTarget = null;
        return target;
    }

    public double x() {
        return renderCoordinate != null ? renderCoordinate.x() : 0;
    }

    public double y() {
        return renderCoordinate != null ? renderCoordinate.y() : 0;
    }

    public double width() {
        return renderCoordinate != null ? renderCoordinate.width() : 0;
    }

    public double height() {
        return renderCoordinate != null ? renderCoordinate.height() : 0;
    }

    /**
     * 鼠标点击回调（子类可以重写）
     *
     * @param event 鼠标事件（mouseX、mouseY、button）
     * @return 是否消费事件
     */
    protected boolean onMouseClick(MouseEvent event) {
        return false;
    }

    /**
     * 鼠标释放回调（子类可以重写）
     *
     * @param event  鼠标事件
     * @param inside 是否在Widget内释放
     * @return 是否消费事件
     */
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        return false;
    }

    /**
     * 键盘按下回调
     */
    protected boolean onKeyPress(KeyEvent event) {
        return false;
    }

    /**
     * 键盘释放回调
     */
    protected boolean onKeyRelease(KeyEvent event) {
        return false;
    }

    /**
     * 字符输入回调
     */
    protected boolean onCharTyped(CharInputEvent event) {
        return false;
    }

    /**
     * 鼠标拖拽回调
     *
     * @param event 拖拽事件（mouseX、mouseY、button、dragX、dragY）
     * @return 是否处理了该事件
     */
    protected boolean onMouseDrag(MouseDragEvent event) {
        return false;
    }

    /**
     * 鼠标滚动回调
     *
     * @param event 滚轮事件（mouseX、mouseY、delta）
     * @return 是否处理了该事件
     */
    protected boolean onMouseScroll(MouseScrollEvent event) {
        return false;
    }

    public void updateMouseHover(double mouseX, double mouseY) {
        boolean nowInside = isMouseInside(mouseX, mouseY);
        if (nowInside != mouseInside) {
            mouseInside = nowInside;
            MouseEvent evt = MouseEvent.of(mouseX, mouseY, -1);
            if (mouseInside) {
                onMouseEnter(evt);
            } else {
                onMouseExit(evt);
            }
        }
    }

    /**
     * 鼠标移入回调。子类重写以处理移入，默认空实现。
     */
    protected void onMouseEnter(MouseEvent event) {
    }

    /**
     * 鼠标移出回调。子类重写以处理移出，默认空实现。
     */
    protected void onMouseExit(MouseEvent event) {
    }

    // endregion 事件处理

    // region Children Management

    @Override
    @Nullable
    public IWidget parent() {
        return parent;
    }

    @Override
    public void parent(@Nullable IWidget parent) {
        // 防止循环引用
        if (parent == this) return;
        if (isDescendantOf(parent)) return;

        if (this.parent != null && this.parent instanceof BaseWidget) {
            ((BaseWidget) this.parent).children.remove(this);
        }
        this.parent = parent;
        invalidateAbsCache();
        if (parent instanceof BaseWidget) {
            ((BaseWidget) parent).children.add(this);
        }
    }

    private boolean isDescendantOf(IWidget ancestor) {
        if (ancestor == null) {
            return false;
        }
        IWidget current = this.parent;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            if (current instanceof BaseWidget) {
                BaseWidget baseWidget = (BaseWidget) current;
                current = baseWidget.parent;
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * 获取子组件列表（只读，调用方不得修改）。
     */
    @Override
    public List<IWidget> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void addChild(IWidget child) {
        if (child == null || children.contains(child)) {
            return;
        }
        // 防止循环引用
        if (child == this || (child instanceof BaseWidget && ((BaseWidget) child).isDescendantOf(this))) {
            return;
        }
        children.add(child);

        if (child instanceof BaseWidget) {
            BaseWidget childWidget = (BaseWidget) child;
            IWidget oldParent = childWidget.parent;
            if (oldParent instanceof BaseWidget) {
                ((BaseWidget) oldParent).children.remove(child);
            }
            childWidget.parent = this;
        }
    }

    @Override
    public boolean removeChild(IWidget child) {
        if (child == null) {
            return false;
        }
        boolean removed = children.remove(child);
        if (removed && child instanceof BaseWidget) {
            ((BaseWidget) child).parent = null;
        }
        return removed;
    }

    @Override
    @Nullable
    public IWidget findChildById(String childId) {
        if (childId == null) {
            return null;
        }
        for (IWidget child : children) {
            if (childId.equals(child.id())) {
                return child;
            }
            IWidget found = child.findChildById(childId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public <W extends IWidget> W findChildByType(Class<W> childType) {
        if (childType == null) {
            return null;
        }
        for (IWidget child : children) {
            if (childType.isInstance(child)) {
                return childType.cast(child);
            }
            W found = child.findChildByType(childType);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public void clearChildren() {
        for (IWidget child : children) {
            if (child instanceof BaseWidget) {
                ((BaseWidget) child).parent = null;
            }
        }
        children.clear();
    }

    protected void invalidateAbsCache() {
        if (absCacheValid) {
            absCacheValid = false;
            for (IWidget child : children) {
                if (child instanceof BaseWidget) {
                    ((BaseWidget) child).invalidateAbsCache();
                }
            }
        }
    }

    @Override
    public double absoluteX() {
        if (!absCacheValid) {
            cachedAbsX = parent != null && parent instanceof BaseWidget ? parent.absoluteX() + x() : x();
            cachedAbsY = parent != null && parent instanceof BaseWidget ? parent.absoluteY() + y() : y();
            absCacheValid = true;
        }
        return cachedAbsX;
    }

    @Override
    public double absoluteY() {
        if (!absCacheValid) {
            cachedAbsX = parent != null && parent instanceof BaseWidget ? parent.absoluteX() + x() : x();
            cachedAbsY = parent != null && parent instanceof BaseWidget ? parent.absoluteY() + y() : y();
            absCacheValid = true;
        }
        return cachedAbsY;
    }

    protected void renderChildren(MatrixStack stack, float partialTicks) {
        if (!visible || children.isEmpty()) {
            return;
        }

        stack.pushPose();
        stack.translate(x(), y(), 0);

        for (IWidget child : children) {
            if (child != null && child.visible()) {
                child.render(stack, partialTicks);
            }
        }

        stack.popPose();
    }

    // endregion Children Management
}
