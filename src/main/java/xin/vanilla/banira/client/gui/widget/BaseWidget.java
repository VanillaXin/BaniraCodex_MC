package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Widget抽象基类
 */
@Accessors(chain = true, fluent = true)
public abstract class BaseWidget implements IWidget {

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
    @Setter
    protected ScreenCoordinate renderCoordinate;

    @Getter
    @Setter
    protected List<ScreenCoordinate> hoveringCoordinates = new ArrayList<>();

    @Getter
    @Setter
    protected List<ScreenCoordinate> clickingCoordinates = new ArrayList<>();

    @Getter
    @Setter
    protected List<ScreenCoordinate> scrollingCoordinates = new ArrayList<>();

    protected boolean visible = true;

    protected boolean enabled = true;

    protected final Map<String, Object> properties = new HashMap<>();

    protected IWidget parent;

    protected final List<IWidget> children = new ArrayList<>();

    protected boolean mouseInside = false;

    protected boolean mousePressed = false;

    protected int pressedMouseButton = -1;

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

    /**
     * 应用主题到本组件及子组件。子类可重写以设置自身颜色，并调用 super.applyTheme(theme)。
     */
    public void applyTheme(BaniraColorConfig theme) {
        for (IWidget child : children) {
            if (child instanceof BaseWidget) {
                ((BaseWidget) child).applyTheme(theme);
            }
        }
    }

    @Override
    public void update() {
        if (!visible || !enabled) {
            return;
        }
        if (screen != null) {
            updateMouseHover(screen.inputState().mouseX(), screen.inputState().mouseY());
        }
        for (IWidget child : children) {
            if (child != null && child.visible() && child.enabled()) {
                child.update();
            }
        }
    }

    @Override
    public boolean handleMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (!visible || !enabled) {
            return false;
        }

        double relativeMouseX = mouseX - absoluteX() + x();
        double relativeMouseY = mouseY - absoluteY() + y();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseClick(relativeMouseX, relativeMouseY, mouseButton)) {
                    return true;
                }
            }
        }
        if (isMouseInside(mouseX, mouseY)) {
            mousePressed = true;
            pressedMouseButton = mouseButton;
            return onMouseClick(mouseX, mouseY, mouseButton);
        }

        return false;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int mouseButton) {
        if (!visible || !enabled) {
            return false;
        }

        double relativeMouseX = mouseX - absoluteX() + x();
        double relativeMouseY = mouseY - absoluteY() + y();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseRelease(relativeMouseX, relativeMouseY, mouseButton)) {
                    return true;
                }
            }
        }
        boolean wasPressed = mousePressed && pressedMouseButton == mouseButton;
        if (wasPressed) {
            mousePressed = false;
            pressedMouseButton = -1;
            boolean inside = isMouseInside(mouseX, mouseY);
            return onMouseRelease(mouseX, mouseY, mouseButton, inside);
        }

        return false;
    }

    @Override
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleKeyPress(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean handleKeyRelease(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleKeyRelease(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return onKeyRelease(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean handleCharTyped(char codePoint, int modifiers) {
        if (!visible || !enabled) {
            return false;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleCharTyped(codePoint, modifiers)) {
                    return true;
                }
            }
        }

        return onCharTyped(codePoint, modifiers);
    }

    @Override
    public boolean handleMouseDrag(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (!visible || !enabled) {
            return false;
        }

        double relativeMouseX = mouseX - absoluteX() + x();
        double relativeMouseY = mouseY - absoluteY() + y();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseDrag(relativeMouseX, relativeMouseY, mouseButton, dragX, dragY)) {
                    return true;
                }
            }
        }

        if (mousePressed) {
            return onMouseDrag(mouseX, mouseY, mouseButton, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!visible || !enabled) {
            return false;
        }

        double relativeMouseX = mouseX - absoluteX() + x();
        double relativeMouseY = mouseY - absoluteY() + y();

        for (int i = children.size() - 1; i >= 0; i--) {
            IWidget child = children.get(i);
            if (child != null && child.visible() && child.enabled()) {
                if (child.handleMouseScroll(relativeMouseX, relativeMouseY, scrollDelta)) {
                    return true;
                }
            }
        }

        if (focused || isMouseInside(mouseX, mouseY)) {
            return onMouseScroll(mouseX, mouseY, scrollDelta);
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
        double absX = absoluteX();
        double absY = absoluteY();
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        return mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;
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
     * @param mouseX      鼠标X坐标
     * @param mouseY      鼠标Y坐标
     * @param mouseButton 鼠标按钮
     */
    protected boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * 鼠标释放回调（子类可以重写）
     *
     * @param mouseX      鼠标X坐标
     * @param mouseY      鼠标Y坐标
     * @param mouseButton 鼠标按钮
     * @param inside      是否在Widget内释放
     */
    protected boolean onMouseRelease(double mouseX, double mouseY, int mouseButton, boolean inside) {
        return false;
    }

    /**
     * 键盘按下回调
     */
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * 键盘释放回调
     */
    protected boolean onKeyRelease(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * 字符输入回调
     */
    protected boolean onCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * 鼠标拖拽回调
     *
     * @return 是否处理了该事件
     */
    protected boolean onMouseDrag(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        return false;
    }

    /**
     * 鼠标滚动回调
     *
     * @return 是否处理了该事件
     */
    protected boolean onMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }

    public void updateMouseHover(double mouseX, double mouseY) {
        mouseInside = isMouseInside(mouseX, mouseY);
    }

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

    @Override
    public List<IWidget> children() {
        return Collections.unmodifiableList(new ArrayList<>(children));
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
    @SuppressWarnings("unchecked")
    public <W extends IWidget> W findChildByType(Class<W> childType) {
        if (childType == null) {
            return null;
        }
        for (IWidget child : children) {
            if (childType.isInstance(child)) {
                return (W) child;
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

    @Override
    public double absoluteX() {
        if (parent != null && parent instanceof BaseWidget) {
            return parent.absoluteX() + x();
        }
        return x();
    }

    @Override
    public double absoluteY() {
        if (parent != null && parent instanceof BaseWidget) {
            return parent.absoluteY() + y();
        }
        return y();
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

    // endregion
}
