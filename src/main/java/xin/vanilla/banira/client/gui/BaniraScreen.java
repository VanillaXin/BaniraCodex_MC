package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.MouseWidget;
import xin.vanilla.banira.client.gui.widget.PopupOption;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.*;


@Accessors(chain = true, fluent = true)
public abstract class BaniraScreen extends Screen {
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
    protected final InputStateManager inputState = InputStateManager.instance();

    public FontRenderer getFont() {
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
     */
    @Nonnull
    public BaniraColorConfig getEffectiveTheme() {
        if (theme != null) return theme;
        return BaniraColorConfig.forSeason(season == EnumSeason.AUTO || season == null ? DateUtils.getSeason() : season);
    }

    /**
     * 从父界面继承 theme 和 season
     */
    public static void inheritThemeAndSeason(BaniraScreen target, Screen parent, @Nullable BaniraColorConfig argsTheme, @Nullable EnumSeason argsSeason) {
        if (argsTheme != null) {
            target.theme(argsTheme);
        } else if (parent instanceof BaniraScreen) {
            target.theme(((BaniraScreen) parent).theme());
        }
        if (argsSeason != null) {
            target.season(argsSeason);
        } else if (parent instanceof BaniraScreen) {
            target.season(((BaniraScreen) parent).season());
        }
    }

    protected BaniraScreen(ITextComponent textComponent) {
        super(textComponent);
    }

    protected BaniraScreen(Component component) {
        super(component.toVanilla(Translator.getClientLanguage()));
    }

    public void renderButtons(MatrixStack stack, float partialTicks) {
        this.renderButtons(stack, inputState.mouseX(), inputState.mouseY(), partialTicks);
    }

    public void renderButtons(MatrixStack stack, double mouseX, double mouseY, float partialTicks) {
        this.buttons.forEach(button -> button.render(stack, (int) mouseX, (int) mouseY, partialTicks));
    }

    @Override
    public void renderBackground(@Nonnull MatrixStack stack) {
        if (this.minecraft != null && this.minecraft.level != null) {
            super.renderBackground(stack);
        } else {
            BaniraColorConfig t = getEffectiveTheme();
            fillGradient(stack, 0, 0, this.width, this.height, t.bgPrimary(), t.bgSecondary());
        }
    }

    @Override
    protected void init() {
        this.cursor = MouseWidget.init(this);
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

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        totalRenderCount++;
        this.renderCount++;

        this.renderEvent(stack, partialTicks);

        this.popupOption.render(stack, inputState);
        this.cursor.draw(stack, mouseX, mouseY);
    }

    protected abstract void renderEvent(MatrixStack stack, float partialTicks);

    @Override
    public void removed() {
        ClientThemeManager.setDefaultTheme(getEffectiveTheme());
        ClientThemeManager.setDefaultSeason(season);
        this.cursor.removed();
        this.removedEvent();
        super.removed();
    }

    protected void removedEvent() {
        clearWidgets();
    }

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
        this.cursor.mouseClicked(mouseX, mouseY, button);

        MouseClickedHandleArgs args = new MouseClickedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button);

        if (this.popupOption.isHovered()) {
            if (this.popupOption.tryHandleOptionClick(mouseX, mouseY, button)) {
                args.consumed(true);
            }
        } else {
            this.popupOption.clear();
            unfocusAllExcept(null);

            IWidget clickedWidget = null;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                IWidget widget = widgets.get(i);
                if (widget.visible() && widget.enabled()) {
                    if (!shouldWidgetReceiveClick(widget, mouseX, mouseY, button)) continue;
                    if (widget.handleMouseClick(mouseX, mouseY, button)) {
                        args.consumed(true);
                        clickedWidget = widget;
                        break;
                    }
                }
            }

            if (clickedWidget != null) {
                requestFocus(clickedWidget);
            }

            if (!args.consumed()) mouseClickedEvent(args);
        }

        return args.consumed() || super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 该 widget 是否应接收此次点击
     */
    protected boolean shouldWidgetReceiveClick(IWidget widget, double mouseX, double mouseY, int button) {
        return true;
    }

    protected void mouseClickedEvent(MouseClickedHandleArgs eventArgs) {
        if (!eventArgs.consumed()) {
            unfocusAllExcept(null);
        }
    }

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
        this.cursor.mouseReleased(mouseX, mouseY, button);

        MouseReleasedHandleArgs args = new MouseReleasedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button);

        if (!this.popupOption.isHovered()) {
            this.popupOption.clear();

            for (int i = widgets.size() - 1; i >= 0; i--) {
                IWidget widget = widgets.get(i);
                if (widget.visible() && widget.enabled()) {
                    if (widget.handleMouseRelease(mouseX, mouseY, button)) {
                        args.consumed(true);
                        break;
                    }
                }
            }

            if (!args.consumed()) mouseReleasedEvent(args);
        }

        return args.consumed() || super.mouseReleased(mouseX, mouseY, button);
    }

    protected void mouseReleasedEvent(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.cursor.mouseScrolled(mouseX, mouseY, delta);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget widget = widgets.get(i);
            if (widget.visible() && widget.enabled()) {
                if (widget.handleMouseScroll(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }
        MouseScoredHandleArgs args = new MouseScoredHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .delta(delta);
        mouseScrolledEvent(args);
        return args.consumed() || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseScoredHandleArgs {
        private boolean consumed;
        private double mouseX;
        private double mouseY;
        private double delta;
    }

    protected void mouseScrolledEvent(MouseScoredHandleArgs eventArgs) {
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class KeyPressedHandleArgs {
        private boolean consumed;
        private int keyCode;
        private int scanCode;
        private int modifiers;

        public int key() {
            return keyCode;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()) {
            if (focusedWidget.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget widget = widgets.get(i);
            if (widget == focusedWidget) continue;
            if (widget.visible() && widget.enabled()) {
                if (widget.handleKeyPress(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }

        KeyPressedHandleArgs args = new KeyPressedHandleArgs()
                .keyCode(keyCode)
                .scanCode(scanCode)
                .modifiers(modifiers);

        keyPressedEvent(args);

        return args.consumed() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
    }

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
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()) {
            if (focusedWidget.handleKeyRelease(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget widget = widgets.get(i);
            if (widget == focusedWidget) continue;
            if (widget.visible() && widget.enabled()) {
                if (widget.handleKeyRelease(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }

        KeyReleasedHandleArgs args = new KeyReleasedHandleArgs()
                .keyCode(keyCode)
                .scanCode(scanCode)
                .modifiers(modifiers);

        keyReleasedEvent(args);

        return args.consumed() || super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected void keyReleasedEvent(KeyReleasedHandleArgs eventArgs) {
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
        this.closeEvent();
        if (this.previousScreen != null) {
            Minecraft.getInstance().setScreen(this.previousScreen);
        }
    }

    protected void closeEvent() {
    }

    // region Widget support

    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        for (IWidget widget : widgets) {
            if (widget.visible() && widget.parent() == null) {
                if (widget.enabled()) {
                    widget.update();
                }
                widget.render(stack, partialTicks);
            }
        }
    }

    @Nullable
    public IWidget getWidget(String id) {
        return widgetMap.get(id);
    }

    protected void addWidget(IWidget widget) {
        if (widget != null) {
            if (widget instanceof BaseWidget) {
                ((BaseWidget) widget).applyTheme(getEffectiveTheme());
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
     * 示例：
     * <pre>{@code
     * InputWidget input = new InputWidget(this);
     * input.renderCoordinate(new ScreenCoordinate(10, 10, 200, 20));
     * input.value("默认值");
     * addWidget(input);
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
        if (widget instanceof BaseWidget) {
            ((BaseWidget) widget).focused(true);
        }
        LOGGER.debug("Widget focused: id={}", widget.id());
        return true;
    }

    public void unfocusAllExcept(@Nullable IWidget exceptWidget) {
        if (focusedWidget != null && focusedWidget != exceptWidget) {
            if (focusedWidget instanceof BaseWidget) {
                ((BaseWidget) focusedWidget).focused(false);
            }
            focusedWidget = null;
        }
        synchronized (focusableWidgets) {
            focusableWidgets.removeIf(ref -> {
                IWidget widget = ref.get();
                if (widget == null) return true;
                if (widget != exceptWidget && widget instanceof BaseWidget) {
                    ((BaseWidget) widget).focused(false);
                }
                return false;
            });
        }
    }

    public void unfocusWidget(IWidget widget) {
        if (widget == null) return;
        if (widget instanceof BaseWidget) {
            ((BaseWidget) widget).focused(false);
        }
        if (focusedWidget == widget) {
            focusedWidget = null;
        }
    }

    // endregion Widget support

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedWidget != null && focusedWidget.visible() && focusedWidget.enabled()) {
            if (focusedWidget.handleCharTyped(codePoint, modifiers)) {
                return true;
            }
        }
        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget widget = widgets.get(i);
            if (widget == focusedWidget) continue;
            if (widget.visible() && widget.enabled()) {
                if (widget.handleCharTyped(codePoint, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            IWidget widget = widgets.get(i);
            if (widget.visible() && widget.enabled()) {
                if (widget.handleMouseDrag(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
