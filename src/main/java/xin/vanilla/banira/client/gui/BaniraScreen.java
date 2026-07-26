package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.input.BaniraInputState;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.internal.config.ClientConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Banira GUI 基类。
 * <p>
 * 事件分发顺序：
 * <ul>
 *   <li><b>mouseClicked</b>: cursor → popupOption(悬停则处理) → 否则 popupOption.clear + unfocusAllExcept
 *       → 逆序 widgets(visible+enabled, shouldWidgetReceiveClick) handleMouseClick → 首个 consumed 的获得 focus
 *       → 未 consumed 则 onMouseClicked</li>
 *   <li><b>mouseScrolled</b>: cursor → 正序 wantsScrollBeforeSiblings 且 isMouseInside 的 widget 优先
 *       → 逆序 widgets handleMouseScroll → onMouseScrolled</li>
 *   <li><b>keyPressed</b>: focusedWidget 优先 → 逆序 widgets(跳过 focused) handleKeyPress → onKeyPressed</li>
 * </ul>
 * </p>
 */
@Accessors(chain = true, fluent = true)
public abstract class BaniraScreen extends Screen {

    // region 基础属性

    private static final Logger LOGGER = LogManager.getLogger();

    @Getter
    private static long totalRenderCount = 0;

    @Getter
    @Setter
    private Screen previousScreen;

    @Getter
    private long renderCount = 0;

    @Getter
    private long openingTime = System.currentTimeMillis();

    @Getter
    protected final BaniraInputState inputState = InputStateManager.instance();

    public Font getFont() {
        return font;
    }

    protected MouseWidget cursor;
    protected PopupOption popupOption;

    @Getter
    private final List<IWidget> widgets = new ArrayList<>();

    @Getter
    private final Map<String, IWidget> widgetMap = new HashMap<>();

    private final Set<WeakReference<IWidget>> focusableWidgets = new HashSet<>();

    @Nullable
    protected IWidget focusedWidget = null;

    /**
     * 自定义主题，非空时优先使用
     */
    @Getter
    @Setter
    @Nullable
    protected BaniraColorConfig theme;
    /**
     * 季节主题，AUTO 时按当前季节，非 AUTO 时使用指定季节，theme 为空时生效
     */
    @Getter
    @Setter
    protected EnumSeason season = EnumSeason.AUTO;

    /**
     * 获取有效主题：theme 非空时返回 theme，否则按 season 返回季节预设。
     * render 期间使用每帧缓存，避免重复计算。
     */
    @Nonnull
    public BaniraColorConfig getEffectiveTheme() {
        if (cachedTheme != null) return cachedTheme;
        if (theme != null) return theme;
        return BaniraColorConfig.forSeason(season == null ? EnumSeason.AUTO : season);
    }

    // endregion 基础属性

    /**
     * 从父界面继承 theme 和 season
     */
    public static void inheritThemeAndSeason(BaniraScreen target, Screen parent, @Nullable BaniraColorConfig argsTheme, @Nullable EnumSeason argsSeason) {
        if (argsTheme != null) {
            target.theme(argsTheme);
        } else if (parent instanceof BaniraScreen baniraScreen) {
            target.theme(baniraScreen.theme());
        }
        if (argsSeason != null) {
            target.season(argsSeason);
        } else if (parent instanceof BaniraScreen baniraScreen) {
            target.season(baniraScreen.season());
        }
    }

    protected BaniraScreen(net.minecraft.network.chat.Component textComponent) {
        super(textComponent);
    }

    protected BaniraScreen(Component component) {
        super(component.toVanilla(Translator.getClientLanguage()));
    }

    public void renderButtons(GuiGraphics graphics, float partialTicks) {
        this.renderButtons(graphics, inputState.mouseX(), inputState.mouseY(), partialTicks);
    }

    public void renderButtons(GuiGraphics graphics, double mouseX, double mouseY, float partialTicks) {
        for (IWidget widget : widgets) {
            if (widget.visible() && widget.enabled() && widget.parent() == null) {
                widget.render(graphics, partialTicks);
            }
        }
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        } else {
            BaniraColorConfig t = getEffectiveTheme();
            graphics.fillGradient(0, 0, this.width, this.height, t.bgPrimary(), t.bgSecondary());
        }
    }

    @Override
    protected void init() {
        this.cursor = MouseWidget.init(this, ClientConfig.get().useCustomCursor());
        this.popupOption = PopupOption.init(this);
        this.openingTime = System.currentTimeMillis();
        ClientThemeManager.setDefaultTheme(getEffectiveTheme());
        ClientThemeManager.setDefaultSeason(season);

        onInit();

        super.init();

        refreshWidget();
    }

    /**
     * 在 super.init() 之前调用，用于前置检查或初始化
     */
    protected void onInit() {
    }

    /**
     * 每帧 render 时缓存的 theme，避免 getEffectiveTheme 重复计算
     */
    private BaniraColorConfig cachedTheme;

    /**
     * 延迟渲染的 tooltip（在 scissor 关闭后、以屏幕坐标绘制，避免错位和裁剪）
     */
    private final List<Consumer<GuiGraphics>> deferredTooltipRenders = new ArrayList<>();

    /**
     * 注册延迟 tooltip 绘制，将在本帧 render 末尾调用（scissor 已关闭后）
     */
    public void addDeferredTooltipRender(Consumer<GuiGraphics> render) {
        if (render != null) deferredTooltipRenders.add(render);
    }

    /**
     * 是否存在已展开的下拉选择（含子树中的 {@link DropdownSelectWidget}）。
     * 用于在下拉全屏浮层显示时抑制后方控件的 {@link TooltipWidget}，避免与下拉项提示叠显。
     */
    public boolean isAnyDropdownSelectOpen() {
        for (IWidget w : this.widgets) {
            if (anyDropdownSelectOpenInTree(w)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyDropdownSelectOpenInTree(IWidget node) {
        if (node instanceof DropdownSelectWidget widget && widget.dropdownOpen()) {
            return true;
        }
        if (node instanceof BaseWidget) {
            for (IWidget c : node.children()) {
                if (anyDropdownSelectOpenInTree(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void flushDeferredTooltipRenders(GuiGraphics graphics) {
        for (Consumer<GuiGraphics> r : deferredTooltipRenders) r.accept(graphics);
        deferredTooltipRenders.clear();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack stack = graphics.pose();
        if (LOGGER.isDebugEnabled()) {
            totalRenderCount++;
            this.renderCount++;
        }
        cachedTheme = getEffectiveTheme();
        // 标题界面没有世界帧负责清屏，Mod Menu 打开的独立页面必须先覆盖上一帧。
        if (this.minecraft == null || this.minecraft.level == null) {
            this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        }

        this.onRender(graphics, mouseX, mouseY, partialTicks);
        this.flushDeferredTooltipRenders(graphics);

        this.popupOption.render(graphics, inputState);
        this.cursor.draw(stack, mouseX, mouseY);
        cachedTheme = null;
    }

    protected abstract void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

    @Override
    public void removed() {
        ClientThemeManager.setDefaultTheme(getEffectiveTheme());
        ClientThemeManager.setDefaultSeason(season);
        this.cursor.removed();
        this.onRemoved();
        super.removed();
    }

    protected void onRemoved() {
        clearWidgets();
    }

    /**
     * 鼠标点击事件参数。子类在 {@link #onMouseClicked} 中可设置 consumed(true) 以阻止后续处理。
     * consumed 为 true 时表示事件已被处理、不再向下传递。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseClickedHandleArgs {
        private boolean consumed;
        private double mouseX;
        private double mouseY;
        private int button;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseEvent clickEvent = MouseEvent.of(mouseX, mouseY, button);
        this.cursor.mouseClicked(clickEvent);

        MouseClickedHandleArgs args = new MouseClickedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button);

        if (this.popupOption.isHovered()) {
            if (this.popupOption.tryHandleOptionPress(clickEvent)) {
                args.consumed(true);
            }
        } else {
            this.popupOption.clear();
            unfocusAllExcept(null);

            IWidget clicked = findFirstHandlingWidget(w ->
                    shouldWidgetReceiveClick(w, clickEvent) && w.handleMouseClick(clickEvent));
            if (clicked != null) {
                args.consumed(true);
                requestFocus(clicked.getFocusTarget());
            }

            if (!args.consumed()) onMouseClicked(args);
        }

        return args.consumed() || super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 该 widget 是否应接收此次点击
     */
    protected boolean shouldWidgetReceiveClick(IWidget widget, MouseEvent event) {
        return true;
    }

    protected void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        if (!eventArgs.consumed()) {
            unfocusAllExcept(null);
        }
    }

    /**
     * 鼠标释放事件参数。consumed 为 true 时表示事件已被处理。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseReleasedHandleArgs {
        private boolean consumed;
        private double mouseX;
        private double mouseY;
        private int button;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MouseEvent releaseEvent = MouseEvent.of(mouseX, mouseY, button);
        this.cursor.mouseReleased(releaseEvent);

        MouseReleasedHandleArgs args = new MouseReleasedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button);

        if (this.popupOption.isHovered()) {
            if (this.popupOption.tryHandleOptionRelease(releaseEvent)) {
                args.consumed(true);
            }
        }
        if (!this.popupOption.isHovered()) {
            this.popupOption.clear();

            if (findFirstHandlingWidget(w -> w.handleMouseRelease(MouseEvent.of(mouseX, mouseY, button))) != null) {
                args.consumed(true);
            }

            if (!args.consumed()) onMouseReleased(args);
        }

        return args.consumed() || super.mouseReleased(mouseX, mouseY, button);
    }

    protected void onMouseReleased(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double delta = deltaY != 0 ? deltaY : deltaX;
        MouseScrollEvent scrollEvent = MouseScrollEvent.of(mouseX, mouseY, delta);
        this.cursor.mouseScrolled(scrollEvent);

        // 优先让已获得焦点的输入框/滑块处理滚轮，无论鼠标位置
        if (delta != 0 && focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()
                && focusedWidget.wantsScrollBeforeSiblings() && focusedWidget.handleMouseScroll(scrollEvent)) {
            return true;
        }

        // 优先让鼠标下方的输入框/数字框等组件处理滚轮（横向滚动、数值增减），避免被滚动条抢先消费
        if (delta != 0) {
            for (IWidget widget : widgets) {
                if (widget.visible() && widget.enabled() && widget.isMouseInside(scrollEvent.mouseX(), scrollEvent.mouseY())
                        && widget.wantsScrollBeforeSiblings() && widget.handleMouseScroll(scrollEvent)) {
                    return true;
                }
            }
        }

        if (findFirstHandlingWidget(w -> w.handleMouseScroll(scrollEvent)) != null) {
            return true;
        }
        MouseScrolledHandleArgs args = new MouseScrolledHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .delta(delta);
        onMouseScrolled(args);
        return args.consumed() || super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    /**
     * 鼠标滚轮事件参数。子类在 {@link #onMouseScrolled} 中可设置 consumed(true)。
     * delta 为正数表示向上/远离用户滚动。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseScrolledHandleArgs {
        private boolean consumed;
        private double mouseX;
        private double mouseY;
        private double delta;
    }

    protected void onMouseScrolled(MouseScrolledHandleArgs eventArgs) {
    }

    /**
     * 按键按下事件参数。consumed 为 true 时表示事件已被处理。keyCode 为 GLFW 键码。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class KeyPressedHandleArgs {
        private boolean consumed;
        private int keyCode;
        private int scanCode;
        private int modifiers;

        /**
         * 等同于 keyCode，便于语义化调用
         */
        public int key() {
            return keyCode;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()
                && focusedWidget.handleKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (anyWidgetExcludingFocused(w -> w.handleKeyPress(keyCode, scanCode, modifiers))) {
            return true;
        }

        KeyPressedHandleArgs args = new KeyPressedHandleArgs()
                .keyCode(keyCode)
                .scanCode(scanCode)
                .modifiers(modifiers);

        onKeyPressed(args);

        return args.consumed() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void onKeyPressed(KeyPressedHandleArgs eventArgs) {
    }

    /**
     * 按键释放事件参数。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static class KeyReleasedHandleArgs {
        private boolean consumed;
        private int keyCode;
        private int scanCode;
        private int modifiers;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()
                && focusedWidget.handleKeyRelease(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (anyWidgetExcludingFocused(w -> w.handleKeyRelease(keyCode, scanCode, modifiers))) {
            return true;
        }

        KeyReleasedHandleArgs args = new KeyReleasedHandleArgs()
                .keyCode(keyCode)
                .scanCode(scanCode)
                .modifiers(modifiers);

        onKeyReleased(args);

        return args.consumed() || super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected void onKeyReleased(KeyReleasedHandleArgs eventArgs) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        this.onClosed();
        if (this.previousScreen != null) {
            Minecraft.getInstance().setScreen(this.previousScreen);
        }
    }

    /**
     * 界面关闭时的回调，子类可重写。注意与 {@link Screen#onClose()} 区分，后者为 Minecraft 生命周期。
     */
    protected void onClosed() {
    }

    // region Widget 管理

    /**
     * 逆序遍历 widgets，返回首个 visible+enabled 且 handler 返回 true 的 widget。
     */
    @Nullable
    private IWidget findFirstHandlingWidget(Predicate<IWidget> handler) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget w = widgets.get(i);
            if (w.visible() && w.enabled() && handler.test(w)) {
                return w;
            }
        }
        return null;
    }

    /**
     * 逆序遍历 widgets（排除 focusedWidget），返回是否有 widget 使 handler 返回 true。
     */
    private boolean anyWidgetExcludingFocused(Predicate<IWidget> handler) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget w = widgets.get(i);
            if (w == focusedWidget) continue;
            if (w.visible() && w.enabled() && handler.test(w)) {
                return true;
            }
        }
        return false;
    }

    protected void renderWidgets(GuiGraphics graphics, float partialTicks) {
        List<IWidget> snapshot = new ArrayList<>(widgets);
        for (IWidget widget : snapshot) {
            if (widget.visible() && widget.parent() == null) {
                if (widget.enabled() && widget.needsUpdate()) {
                    widget.update();
                }
                widget.render(graphics, partialTicks);
            }
        }
    }

    @Nullable
    public IWidget getWidget(String id) {
        return widgetMap.get(id);
    }

    /**
     * 添加根级 Widget 到 Screen。
     * addWidget 用于 Screen 根级，addChild 用于 Widget 树结构（父组件添加子组件）。
     */
    protected void addWidget(IWidget widget) {
        if (widget != null) {
            widget.applyTheme(getEffectiveTheme());
            if (widget instanceof CollapsiblePanelWidget panelWidget) {
                panelWidget.refreshLayout();
            }
            widgets.add(widget);
            if (widget.id() != null) {
                widgetMap.put(widget.id(), widget);
            }
        }
    }

    protected void removeWidget(IWidget widget) {
        if (widget != null) {
            widgets.remove(widget);
            if (widget.id() != null) {
                widgetMap.remove(widget.id());
            }
        }
    }

    /**
     * 添加覆盖层 Widget（如下拉框浮层），会添加到列表末尾，优先接收点击和滚动事件。
     */
    public void addOverlayWidget(IWidget widget) {
        addWidget(widget);
    }

    /**
     * 移除覆盖层 Widget
     */
    public void removeOverlayWidget(IWidget widget) {
        removeWidget(widget);
    }

    protected void clearWidgets() {
        for (IWidget widget : widgets) {
            if (widget != null) {
                widget.clearChildren();
                widget.parent(null);
            }
        }
        widgets.clear();
        widgetMap.clear();
        focusedWidget = null;
        synchronized (focusableWidgets) {
            focusableWidgets.clear();
        }
    }

    /**
     * 刷新 Widget 列表。
     * 子类通过重写 {@link #initWidgets()} 在代码中创建并添加组件。
     */
    protected void refreshWidget() {
        clearWidgets();
        initWidgets();
        LOGGER.debug("Widgets refreshed: total={}", widgets.size());
    }

    /**
     * 初始化 Widget。子类重写此方法，通过代码创建组件并调用 {@link #addWidget(IWidget)} 添加。
     * <p>
     * 示例（在 initWidgets 中创建后立即 addWidget，以便参与布局和事件分发）：
     * <pre>{@code
     * ButtonWidget closeBtn = new ButtonWidget(this)
     *         .id("close")
     *         .bounds(new ScreenCoordinate(10, 10, 80, 20))
     *         .text("关闭")
     *         .onClick(b -> onClose());
     * addWidget(closeBtn);
     * }</pre>
     * </p>
     */
    protected void initWidgets() {
    }

    public void registerFocusableWidget(IWidget widget) {
        if (widget != null) {
            synchronized (focusableWidgets) {
                focusableWidgets.add(new WeakReference<>(widget));
                focusableWidgets.removeIf(ref -> ref.get() == null);
            }
        }
    }

    public boolean requestFocus(IWidget widget) {
        if (widget == null || !widget.visible() || !widget.enabled()) {
            return false;
        }
        registerFocusableWidget(widget);
        unfocusAllExcept(widget);
        focusedWidget = widget;
        if (widget instanceof BaseWidget baseWidget) {
            baseWidget.focused(true);
        }
        LOGGER.debug("Widget focused: id={}", widget.id());
        return true;
    }

    public void unfocusAllExcept(@Nullable IWidget exceptWidget) {
        if (focusedWidget != null && focusedWidget != exceptWidget) {
            if (focusedWidget instanceof BaseWidget baseWidget) {
                baseWidget.focused(false);
            }
            focusedWidget = null;
        }
        synchronized (focusableWidgets) {
            focusableWidgets.removeIf(ref -> {
                IWidget widget = ref.get();
                if (widget == null) return true;
                if (widget != exceptWidget && widget instanceof BaseWidget baseWidget) {
                    baseWidget.focused(false);
                }
                return false;
            });
        }
    }

    public void unfocusWidget(IWidget widget) {
        if (widget == null) return;
        if (widget instanceof BaseWidget baseWidget) {
            baseWidget.focused(false);
        }
        if (focusedWidget == widget) {
            focusedWidget = null;
        }
    }

    // endregion Widget 管理

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()
                && focusedWidget.handleCharTyped(codePoint, modifiers)) {
            return true;
        }
        if (anyWidgetExcludingFocused(w -> w.handleCharTyped(codePoint, modifiers))) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (findFirstHandlingWidget(w -> w.handleMouseDrag(MouseDragEvent.of(mouseX, mouseY, button, dragX, dragY))) != null) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
