package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.gui.component.MouseCursor;
import xin.vanilla.banira.client.gui.component.PopupOption;
import xin.vanilla.banira.client.gui.component.ScrollBar;
import xin.vanilla.banira.client.util.KeyEventManager;
import xin.vanilla.banira.client.util.MouseHelper;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.LanguageHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Accessors(chain = true, fluent = true)
public abstract class BaniraScreen extends Screen {

    // region init

    /**
     * 父级 Screen
     */
    @Getter
    @Setter
    private Screen previousScreen;

    @Getter
    private long renderCount = 0;

    /**
     * 键盘与鼠标事件管理器
     */
    protected final KeyEventManager keyManager = new KeyEventManager();
    protected final MouseHelper mouseHelper = new MouseHelper();

    /**
     * 鼠标光标
     */
    protected MouseCursor cursor;
    /**
     * 弹出层选项
     */
    protected PopupOption popupOption;

    private final Map<String, ScrollBar> scrollBars = new HashMap<>();

    /**
     * 点击拦截区域列表
     */
    private final List<Function<MouseArgs, Boolean>> clickInterceptAreas = new ArrayList<>();
    /**
     * 释放拦截区域列表
     */
    private final List<Function<MouseArgs, Boolean>> releaseInterceptAreas = new ArrayList<>();


    // endregion init

    protected BaniraScreen(ITextComponent textComponent) {
        super(textComponent);
    }

    protected BaniraScreen(Component component) {
        super(component.toTextComponent(LanguageHelper.getClientLanguage()));
    }

    public void initScrollBar(ScrollBar scrollBar) {
        this.initScrollBar("default", scrollBar);
    }

    public void initScrollBar(String name, ScrollBar scrollBar) {
        if (!this.scrollBars.containsKey(name)) {
            this.scrollBars.put(name, scrollBar);
        }
    }

    public void addScrollBar(ScrollBar scrollBar) {
        this.addScrollBar("default", scrollBar);
    }

    public void addScrollBar(String name, ScrollBar scrollBar) {
        this.scrollBars.put(name, scrollBar);
    }

    public ScrollBar scrollBar() {
        return this.scrollBar("default");
    }

    public ScrollBar scrollBar(String name) {
        return this.scrollBars.get(name);
    }

    /**
     * 注册点击拦截区域
     *
     * @param interceptArea 拦截区域函数, mouseX:mouseY mouseButton:isClicked
     */
    protected void registerClickInterceptArea(Function<MouseArgs, Boolean> interceptArea) {
        this.clickInterceptAreas.add(interceptArea);
    }

    /**
     * 注册释放拦截区域
     *
     * @param interceptArea 拦截区域函数, mouseX:mouseY mouseButton:isClicked
     */
    protected void registerReleaseInterceptArea(Function<MouseArgs, Boolean> interceptArea) {
        this.releaseInterceptAreas.add(interceptArea);
    }

    /**
     * 注册拦截区域
     *
     * @param interceptArea 拦截区域函数, mouseX:mouseY mouseButton:isClicked
     */
    protected void registerInterceptArea(Function<MouseArgs, Boolean> interceptArea) {
        this.registerClickInterceptArea(interceptArea);
        this.registerReleaseInterceptArea(interceptArea);
    }

    /**
     * 清除所有拦截区域
     */
    protected void clearInterceptAreas() {
        this.clickInterceptAreas.clear();
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseArgs {
        private final double mouseX;
        private final double mouseY;
        private final int button;
        private final boolean clicked; // true -> click, false -> release
    }

    public void renderButtons(MatrixStack stack, float partialTicks) {
        this.renderButtons(stack, mouseHelper.mouseX(), mouseHelper.mouseY(), partialTicks);
    }

    public void renderButtons(MatrixStack stack, double mouseX, double mouseY, float partialTicks) {
        this.buttons.forEach(button -> button.render(stack, (int) mouseX, (int) mouseY, partialTicks));
    }

    @Override
    protected void init() {
        this.cursor = MouseCursor.init();
        this.popupOption = PopupOption.init(super.font);

        initEvent();

        super.init();
    }

    protected abstract void initEvent();

    protected abstract void updateLayout();


    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderCount++;
        this.mouseHelper.tick(mouseX, mouseY);
        this.keyManager.tick();

        this.renderEvent(stack, partialTicks);

        // 绘制弹出选项
        this.popupOption.render(stack, keyManager, mouseHelper);
        // 绘制鼠标光标
        this.cursor.draw(stack, mouseX, mouseY);
    }

    protected abstract void renderEvent(MatrixStack stack, float partialTicks);


    @Override
    public void removed() {
        this.cursor.removed();

        this.removedEvent();

        super.removed();
    }

    protected abstract void removedEvent();


    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseClickedHandleArgs {
        private boolean consumed;
        private boolean layout;
        private double mouseX;
        private double mouseY;
        private int button;
        private boolean intercepted;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.cursor.mouseClicked(mouseX, mouseY, button);
        this.mouseHelper.mouseClicked(mouseX, mouseY, button);
        this.scrollBars.values().forEach(scrollBar -> scrollBar.mouseClicked(mouseX, mouseY, button));

        // 检查是否在拦截区域内
        boolean shouldIntercept = false;
        MouseArgs mouseArgs = new MouseArgs(mouseX, mouseY, button, true);
        for (Function<MouseArgs, Boolean> interceptArea : this.clickInterceptAreas) {
            if (interceptArea.apply(mouseArgs)) {
                shouldIntercept = true;
                break;
            }
        }

        MouseClickedHandleArgs args = new MouseClickedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button)
                .intercepted(shouldIntercept);

        if (!this.popupOption.isHovered()) {
            // 清空弹出选项
            this.popupOption.clear();

            mouseClickedEvent(args);
        }

        if (args.layout()) this.updateLayout();

        return args.intercepted() || args.consumed() || super.mouseClicked(mouseX, mouseY, button);
    }

    protected abstract void mouseClickedEvent(MouseClickedHandleArgs eventArgs);


    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseReleasedHandleArgs {
        private boolean consumed;
        private boolean layout;
        private boolean clearPopup = true;
        private double mouseX;
        private double mouseY;
        private int button;
        private boolean intercepted;
    }

    /**
     * 检测鼠标松开事件
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.cursor.mouseReleased(mouseX, mouseY, button);
        this.keyManager.tick();
        this.scrollBars.values().forEach(scrollBar -> scrollBar.mouseReleased(mouseX, mouseY, button));

        // 检查是否在拦截区域内
        boolean shouldIntercept = false;
        MouseArgs mouseArgs = new MouseArgs(mouseX, mouseY, button, false);
        for (Function<MouseArgs, Boolean> interceptArea : this.releaseInterceptAreas) {
            if (interceptArea.apply(mouseArgs)) {
                shouldIntercept = true;
                break;
            }
        }

        MouseReleasedHandleArgs args = new MouseReleasedHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .button(button)
                .intercepted(shouldIntercept);

        if (!this.mouseHelper.isMoved()) {
            if (this.popupOption.isHovered()) {
                this.handlePopupOption(args);
                if (args.clearPopup()) this.popupOption.clear();
            } else {
                this.mouseReleasedEvent(args);
            }
        } else {
            this.mouseReleasedEvent(args);
        }

        this.mouseHelper.mouseReleased(mouseX, mouseY, button);
        if (args.layout()) this.updateLayout();

        return args.intercepted() || args.consumed() || super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 处理弹出选项
     */
    protected abstract void handlePopupOption(MouseReleasedHandleArgs eventArgs);

    protected abstract void mouseReleasedEvent(MouseReleasedHandleArgs eventArgs);


    /**
     * 鼠标移动事件
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseHelper.mouseMoved(mouseX, mouseY);
        this.scrollBars.values().forEach(scrollBar -> scrollBar.mouseMoved(mouseX, mouseY));

        this.mouseMovedEvent();

        super.mouseMoved(mouseX, mouseY);
    }

    protected abstract void mouseMovedEvent();


    @Data
    @Accessors(chain = true, fluent = true)
    public static class MouseScoredHandleArgs {
        private boolean consumed = false;
        private boolean layout = false;
        double mouseX;
        double mouseY;
        private double delta;
    }

    /**
     * 鼠标滚动事件
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.cursor.mouseScrolled(mouseX, mouseY, delta);
        this.mouseHelper.mouseScrolled(mouseX, mouseY, delta);
        // TODO 判断滚动条焦点
        this.scrollBars.values().forEach(scrollBar -> scrollBar.mouseScrolled(delta));

        MouseScoredHandleArgs args = new MouseScoredHandleArgs()
                .mouseX(mouseX)
                .mouseY(mouseY)
                .delta(delta);

        this.mouseScrolledEvent(args);

        if (args.layout()) this.updateLayout();
        return args.consumed() || super.mouseScrolled(mouseX, mouseY, delta);
    }

    protected abstract void mouseScrolledEvent(MouseScoredHandleArgs eventArgs);


    @Data
    @Accessors(chain = true, fluent = true)
    public static class KeyPressedHandleArgs {
        private boolean consumed = false;
        private boolean layout = false;
        private int key;
        private int scan;
        private int modifiers;
    }

    /**
     * 键盘按下事件
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.keyManager.keyPressed(keyCode);
        KeyPressedHandleArgs args = new KeyPressedHandleArgs()
                .key(keyCode)
                .scan(scanCode)
                .modifiers(modifiers);

        this.keyPressedEvent(args);

        if (args.layout()) this.updateLayout();
        return args.consumed() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected abstract void keyPressedEvent(KeyPressedHandleArgs eventArgs);


    @Data
    @Accessors(chain = true, fluent = true)
    public static class KeyReleasedHandleArgs {
        private boolean consumed = false;
        private boolean layout = false;
        private int key;
        private int scan;
        private int modifiers;
    }

    /**
     * 键盘松开事件
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        KeyReleasedHandleArgs args = new KeyReleasedHandleArgs()
                .key(keyCode)
                .scan(scanCode)
                .modifiers(modifiers);

        this.keyReleasedEvent(args);

        this.keyManager.keyReleased(keyCode);
        if (args.layout()) this.updateLayout();
        return args.consumed() || super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected abstract void keyReleasedEvent(KeyReleasedHandleArgs eventArgs);

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

    protected abstract void closeEvent();
}
