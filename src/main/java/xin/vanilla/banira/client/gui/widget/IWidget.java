package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IWidget {

    /**
     * 应用主题到本组件。默认空实现；BaseWidget 及子类重写以设置自身颜色。
     */
    default void applyTheme(BaniraColorConfig theme) {
    }

    /**
     * 是否希望在兄弟组件之前接收滚轮事件（如输入框需优先处理滚轮以增减数值）。
     * 默认 false；InputWidget 等重写为 true。
     */
    default boolean wantsScrollBeforeSiblings() {
        return false;
    }

    /**
     * 渲染本组件。
     *
     * @param stack        矩阵栈
     * @param partialTicks 部分 tick（用于动画插值）
     */
    void render(MatrixStack stack, float partialTicks);

    /**
     * 每帧更新，用于动画、光标闪烁等。
     */
    void update();

    /**
     * 是否每帧需要调用 update。默认 false；InputWidget、SliderWidget 等有动画的返回 true。
     */
    default boolean needsUpdate() {
        return false;
    }

    /**
     * 处理鼠标点击事件。
     *
     * @param event 鼠标事件（mouseX、mouseY、button）
     * @return 若已消费该事件、不再向下传递则返回 true
     */
    boolean handleMouseClick(MouseEvent event);

    /**
     * 处理鼠标释放事件。
     *
     * @param event 鼠标事件
     * @return 若已消费则返回 true
     */
    boolean handleMouseRelease(MouseEvent event);

    /**
     * 处理按键按下事件。
     *
     * @param event 键盘事件
     * @return 若已消费则返回 true
     */
    boolean handleKeyPress(KeyEvent event);

    /**
     * 处理按键释放事件。
     */
    boolean handleKeyRelease(KeyEvent event);

    /**
     * 处理字符输入（用于文本输入框）。
     */
    boolean handleCharTyped(char codePoint, int modifiers);

    /**
     * 处理鼠标拖拽事件。
     *
     * @param event 拖拽事件（mouseX、mouseY、button、dragX、dragY）
     * @return 若已消费则返回 true
     */
    boolean handleMouseDrag(MouseDragEvent event);

    /**
     * 处理鼠标滚轮事件。
     *
     * @param event 滚轮事件（mouseX、mouseY、delta）
     * @return 若已消费则返回 true
     */
    boolean handleMouseScroll(MouseScrollEvent event);

    /**
     * 获取布局边界。对于有父节点的 Widget，坐标为相对父节点的偏移。
     *
     * @return 非 null 的边界信息，包含 x、y、width、height
     */
    ScreenCoordinate bounds();

    /**
     * 设置可见性。
     */
    void visible(boolean visible);

    /**
     * 是否可见。
     */
    boolean visible();

    /**
     * 设置启用状态。
     */
    void enabled(boolean enabled);

    /**
     * 是否启用。
     */
    boolean enabled();

    /**
     * 存储键值对，用于 Widget 间或 Screen 与 Widget 间传递自定义数据。
     *
     * @param key   键，非 null
     * @param value 值，传 null 表示移除该键
     */
    void property(String key, Object value);

    /**
     * 获取 property 值。
     *
     * @param key 键
     * @return 值，不存在则 null
     */
    @Nullable
    Object property(String key);

    /**
     * 获取指定类型的 property 值。
     *
     * @param key  键
     * @param type 期望类型
     * @return 若存在且类型匹配则返回，否则 null
     */
    @Nullable
    default <T> T property(String key, Class<T> type) {
        Object v = property(key);
        return type != null && v != null && type.isInstance(v) ? type.cast(v) : null;
    }

    /**
     * 获取指定类型的 property 值，不存在或类型不匹配时返回默认值。
     *
     * @param key          键
     * @param type         期望类型
     * @param defaultValue 默认值
     * @return 存在且类型匹配则返回，否则 defaultValue
     */
    default <T> T property(String key, Class<T> type, T defaultValue) {
        T v = property(key, type);
        return v != null ? v : defaultValue;
    }

    /**
     * 获取组件 ID，用于 {@link #findChildById} 查找。
     */
    @Nullable
    String id();

    /**
     * 设置组件 ID。
     */
    void id(String id);

    /**
     * 鼠标是否在组件边界内。
     */
    boolean isMouseInside(double mouseX, double mouseY);

    /**
     * 鼠标是否在组件边界内（事件对象重载）。
     */
    default boolean isMouseInside(MouseEvent event) {
        return event != null && isMouseInside(event.mouseX(), event.mouseY());
    }

    /**
     * 获取父组件。
     */
    @Nullable
    IWidget parent();

    /**
     * 设置父组件。
     */
    void parent(@Nullable IWidget parent);

    /**
     * 获取子组件列表。
     */
    List<IWidget> children();

    /**
     * 添加子组件，用于 Widget 树结构（父组件添加子组件）。
     * Screen 根级添加用 addWidget，树结构用 addChild。
     */
    void addChild(IWidget child);

    /**
     * 移除子组件。
     *
     * @return 是否成功移除
     */
    boolean removeChild(IWidget child);

    /**
     * 按 ID 查找子组件。
     */
    @Nullable
    IWidget findChildById(String childId);

    /**
     * 按类型查找子组件（递归）。
     *
     * @param type 目标类型
     * @return 找到则返回，否则 null
     */
    @Nullable
    <W extends IWidget> W findChildByType(Class<W> type);

    /**
     * 按类型查找子组件（递归），返回 Optional 避免 null 检查。
     */
    default <W extends IWidget> Optional<W> findChildByTypeOpt(Class<W> type) {
        return Optional.ofNullable(findChildByType(type));
    }

    /**
     * 清空所有子组件。
     */
    void clearChildren();

    /**
     * 获取组件在屏幕上的绝对 X 坐标。
     */
    double absoluteX();

    /**
     * 获取组件在屏幕上的绝对 Y 坐标。
     */
    double absoluteY();

    /**
     * 获取控件当前实际占用的高度。默认返回 bounds 高度；可动态变化尺寸的控件可重写。
     */
    default double effectiveHeight() {
        ScreenCoordinate b = bounds();
        return b != null ? b.height() : 0;
    }

    /**
     * 获取控件当前实际占用的宽度。默认返回 bounds 宽度。
     */
    default double effectiveWidth() {
        ScreenCoordinate b = bounds();
        return b != null ? b.width() : 0;
    }

    /**
     * 当此 widget 被点击并处理了点击时，应获得焦点的目标 widget。
     * 默认返回自身；若子组件（如内联输入框）实际处理了点击，可返回该子组件。
     */
    default IWidget getFocusTarget() {
        return this;
    }
}
