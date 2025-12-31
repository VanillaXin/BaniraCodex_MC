package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumStringInputRegex;
import xin.vanilla.banira.client.gui.component.*;
import xin.vanilla.banira.client.gui.helper.LayoutConfig;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.DialogUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 内容输入 Screen
 */
public class StringInputScreen extends BaniraScreen {

    private final Args args;
    private final List<InputField> inputFields = new ArrayList<>();
    private final TextList errorText = new TextList();
    private final Map<Integer, Text> errorTextMap = new HashMap<>();
    private Text runningErrorText = Text.empty();

    private int contentAreaTop = 0;
    private int contentAreaBottom = 0;
    private int scrollableContentHeight = 0;
    private int visibleContentHeight = 0;
    private int buttonY = 0;

    private BaniraButton submitButton;
    private BaniraButton cancelButton;

    public StringInputScreen(Args args) {
        super(args.getTitle() != null ? args.getTitle().toComponent() : Component.literal("StringInputScreen"));
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.previousScreen(args.getParentScreen());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class InputField {
        private BaniraTextField input;
        private BaniraButton button;
        private Text title;
        private String value = "";
        private int y;
        private WidgetType type;
        private String fileFilter;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Widget {
        private String name = "";
        private Text title;
        private Text message = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_something");
        private String regex = EnumStringInputRegex.NONE.getRegex();
        private String defaultValue = "";
        private boolean allowEmpty;
        private boolean disabled;
        private WidgetType type = WidgetType.TEXT;
        private String fileFilter = "";
        private Function<Results, String> validator = s -> "";
        private Consumer<Inputs> changed;

        public Widget title(Text title) {
            this.title = title;
            if (StringUtils.isNullOrEmptyEx(this.name)) {
                this.name = title.content();
            }
            return this;
        }
    }

    public enum WidgetType {
        TEXT,
        FILE,
        COLOR,
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Inputs {
        private Map<String, TextFieldWidget> nameMap = new HashMap<>();
        private Map<Integer, TextFieldWidget> indexMap = new HashMap<>();
        private String curName = "";
        private int curIndex = -1;

        public TextFieldWidget value() {
            if (!StringUtils.isNullOrEmptyEx(this.curName)) {
                return this.nameMap.get(this.curName);
            } else if (this.curIndex >= 0) {
                return this.indexMap.get(this.curIndex);
            } else if (this.nameMap.size() == 1) {
                return this.nameMap.values().iterator().next();
            } else if (this.indexMap.size() == 1) {
                return this.indexMap.values().iterator().next();
            } else {
                return null;
            }
        }

        public TextFieldWidget value(String name) {
            return this.nameMap.getOrDefault(name, null);
        }

        public TextFieldWidget value(int index) {
            return this.indexMap.getOrDefault(index, null);
        }

        public Inputs value(String name, int index, TextFieldWidget value) {
            if (!StringUtils.isNullOrEmptyEx(name)) {
                this.nameMap.put(name, value);
            }
            this.indexMap.put(index, value);
            return this;
        }

        public boolean isEmpty() {
            return this.nameMap.isEmpty() && this.indexMap.isEmpty();
        }

        public Inputs clear() {
            this.nameMap.clear();
            this.indexMap.clear();
            return this;
        }
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Results {
        private Map<String, String> nameMap = new HashMap<>();
        private Map<Integer, String> indexMap = new HashMap<>();
        private String curName = "";
        private int curIndex = -1;
        private String runningResult;

        public String value() {
            if (!StringUtils.isNullOrEmptyEx(this.curName)) {
                return this.nameMap.get(this.curName);
            } else if (this.curIndex >= 0) {
                return this.indexMap.get(this.curIndex);
            } else if (this.nameMap.size() == 1) {
                return this.nameMap.values().iterator().next();
            } else if (this.indexMap.size() == 1) {
                return this.indexMap.values().iterator().next();
            } else {
                return null;
            }
        }

        public String value(String name) {
            return this.nameMap.getOrDefault(name, null);
        }

        public String value(int index) {
            return this.indexMap.getOrDefault(index, null);
        }

        public String value(String name, int index) {
            if (this.nameMap.containsKey(name)) {
                return this.nameMap.get(name);
            } else {
                return this.indexMap.getOrDefault(index, null);
            }
        }

        public String firstValue() {
            return this.indexMap.getOrDefault(0, null);
        }

        public String lastValue() {
            return this.indexMap.getOrDefault(this.indexMap.size() - 1, null);
        }

        public Results value(String name, int index, String value) {
            if (!StringUtils.isNullOrEmptyEx(name)) {
                this.nameMap.put(name, value);
            }
            this.indexMap.put(index, value);
            return this;
        }

        public Results runningResult(String s) {
            this.runningResult = s;
            return this;
        }

        public Results runningResult(Exception e) {
            this.runningResult = e.toString();
            return this;
        }

        public boolean isEmpty() {
            return this.nameMap.isEmpty() && this.indexMap.isEmpty();
        }
    }

    @Data
    @Accessors(chain = true)
    public static final class Args {
        private Screen parentScreen;
        private Text title;
        private List<Widget> widgets = new ArrayList<>();
        private Consumer<Results> callback;
        private Supplier<Boolean> invisible = () -> false;

        public Args addWidget(Widget widget) {
            this.getWidgets().add(widget);
            return this;
        }

        public void validate() {
            Objects.requireNonNull(this.getParentScreen());
            Objects.requireNonNull(this.getWidgets());
            Objects.requireNonNull(this.getCallback());
            if (this.getWidgets().isEmpty()) {
                throw new RuntimeException("Widgets list cannot be empty");
            }
        }
    }

    @Override
    protected void initEvent() {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        // 初始化滚动条
        super.initScrollBar(new ScrollBar());

        // 计算布局
        calculateLayout();

        // 清空之前的输入框
        this.inputFields.clear();
        this.children.clear();

        // 创建输入框
        Inputs inputs = new Inputs();
        for (int i = 0; i < args.getWidgets().size(); i++) {
            Widget widget = args.getWidgets().get(i);
            InputField inputField = new InputField();

            int fieldY = contentAreaTop + LayoutConfig.StringInput.TITLE_HEIGHT + LayoutConfig.StringInput.INPUT_FIELD_SPACING * i - getScrollOffset();
            int inputWidth = (widget.type() == WidgetType.FILE || widget.type() == WidgetType.COLOR) ? 175 : 200;

            BaniraTextField input = new BaniraTextField(
                    this.font,
                    this.width / 2 - 100,
                    fieldY,
                    inputWidth,
                    LayoutConfig.StringInput.INPUT_FIELD_HEIGHT,
                    widget.message().toComponent().toTextComponent()
            );

            input.setEditable(!widget.disabled());
            input.setMaxLength(Integer.MAX_VALUE);
            if (StringUtils.isNotNullOrEmpty(widget.regex())) {
                input.setFilter(s -> s.matches(widget.regex()));
            }

            // 恢复之前的值或使用默认值
            String defaultValue = StringUtils.isNullOrEmptyEx(inputField.value())
                    ? widget.defaultValue() : inputField.value();
            input.setValue(defaultValue);
            inputField.value(defaultValue);

            inputs.value(widget.name(), i, input);

            // 设置变化回调
            if (widget.changed() != null) {
                int finalI = i;
                input.setResponder(s -> {
                    // 验证输入并更新错误状态
                    String error = widget.validator().apply(new Results().value(widget.name(), finalI, input.getValue()));
                    boolean hasError = StringUtils.isNotNullOrEmpty(error);
                    input.error(hasError);
                    // 更新错误文本映射
                    if (hasError) {
                        errorTextMap.put(finalI, Text.literal(error).color(Color.argb(0xFFFF0000)));
                    } else {
                        errorTextMap.remove(finalI);
                    }
                    if (StringUtils.isNullOrEmptyEx(error)) {
                        widget.changed().accept(inputs.curIndex(finalI).curName(widget.name()));
                    }
                });
            }

            super.addButton(input);
            inputField.input(input);
            inputField.title(widget.title());
            inputField.y(fieldY);
            inputField.type(widget.type());
            inputField.fileFilter(widget.fileFilter());

            // 为FILE和COLOR类型创建按钮
            if (widget.type() == WidgetType.FILE || widget.type() == WidgetType.COLOR) {
                BaniraButton fileButton = new BaniraButton(
                        this.width / 2 + 78,
                        fieldY,
                        20,
                        LayoutConfig.StringInput.INPUT_FIELD_HEIGHT,
                        Component.literal("...").toTextComponent(),
                        button -> {
                            try {
                                if (widget.type() == WidgetType.FILE) {
                                    DialogUtils.chooseFileString("", input::setValue);
                                } else if (widget.type() == WidgetType.COLOR) {
                                    DialogUtils.chooseRgbHex("", input::setValue);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                );
                super.addButton(fileButton);
                inputField.button(fileButton);
            }

            this.inputFields.add(inputField);
        }

        this.submitButton = new BaniraButton(
                this.width / 2 + 5,
                this.buttonY,
                LayoutConfig.StringInput.BUTTON_WIDTH,
                LayoutConfig.StringInput.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> handleSubmit()
        );
        super.addButton(this.submitButton);

        this.cancelButton = new BaniraButton(
                this.width / 2 - 100,
                this.buttonY,
                LayoutConfig.StringInput.BUTTON_WIDTH,
                LayoutConfig.StringInput.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(this.previousScreen())
        );
        super.addButton(this.cancelButton);

        // 注册按钮区域为点击拦截区域
        super.clearInterceptAreas();
        super.registerInterceptArea((args) -> {
            if (this.submitButton != null && this.submitButton.isMouseOver(args.mouseX(), args.mouseY())) {
                // 使提交按钮处理点击
                if (args.clicked() && this.submitButton.mouseClicked(args.mouseX(), args.mouseY(), args.button())) {
                    return true;
                } else if (this.submitButton.mouseReleased(args.mouseX(), args.mouseY(), args.button())) {
                    return true;
                }
            }
            if (this.cancelButton != null && this.cancelButton.isMouseOver(args.mouseX(), args.mouseY())) {
                // 使取消按钮处理点击
                if (args.clicked() && this.cancelButton.mouseClicked(args.mouseX(), args.mouseY(), args.button())) {
                    return true;
                } else if (this.cancelButton.mouseReleased(args.mouseX(), args.mouseY(), args.button())) {
                    return true;
                }
            }
            return (this.submitButton != null && this.submitButton.isMouseOver(args.mouseX(), args.mouseY())) ||
                    (this.cancelButton != null && this.cancelButton.isMouseOver(args.mouseX(), args.mouseY()));
        });

        initScrollBar();
    }

    @Override
    protected void updateLayout() {
        calculateLayout();
        initScrollBar();
        updateInputFieldsPosition();
    }

    private void calculateLayout() {
        // 计算按钮区域
        this.buttonY = this.height - LayoutConfig.StringInput.BUTTON_HEIGHT - LayoutConfig.StringInput.BUTTON_MARGIN;

        // 计算内容区域底部
        this.contentAreaBottom = this.height - LayoutConfig.StringInput.BUTTON_HEIGHT - LayoutConfig.StringInput.BUTTON_MARGIN - LayoutConfig.StringInput.BUTTON_MARGIN;

        // 计算内容区域顶部
        this.contentAreaTop = LayoutConfig.StringInput.BUTTON_HEIGHT;

        // 计算可见内容高度
        this.visibleContentHeight = this.contentAreaBottom - this.contentAreaTop;

        if (args.getWidgets().isEmpty()) {
            this.scrollableContentHeight = 0;
        } else {
            this.scrollableContentHeight = args.getWidgets().size() * LayoutConfig.StringInput.INPUT_FIELD_SPACING;
        }

        // 更新滚动条参数
        if (super.scrollBar() != null) {
            super.scrollBar().updateScrollParams(this.scrollableContentHeight, this.visibleContentHeight);
        }
    }

    /**
     * 初始化滚动条位置和尺寸
     */
    private void initScrollBar() {
        if (super.scrollBar() == null) return;

        int scrollBarX = this.width / 2 + 100 + LayoutConfig.StringInput.SCROLL_BAR_MARGIN;
        int scrollBarY = this.contentAreaTop;
        int scrollBarHeight = this.visibleContentHeight;

        super.scrollBar()
                .x(scrollBarX)
                .y(scrollBarY)
                .width(LayoutConfig.StringInput.SCROLL_BAR_WIDTH)
                .height(scrollBarHeight);

        // 设置滚动变化回调
        super.scrollBar().onScrollChanged(offset -> {
            updateInputFieldsPosition();
        });
    }

    /**
     * 获取当前滚动偏移量（像素）
     */
    private int getScrollOffset() {
        return super.scrollBar() != null ? super.scrollBar().scrollOffset() : 0;
    }

    /**
     * 更新输入框位置
     */
    private void updateInputFieldsPosition() {
        int scrollOffset = getScrollOffset();
        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            int fieldY = contentAreaTop + LayoutConfig.StringInput.TITLE_HEIGHT + LayoutConfig.StringInput.INPUT_FIELD_SPACING * i - scrollOffset;
            field.input().y = fieldY;
            field.y(fieldY);

            // 更新按钮位置
            if (field.button() != null) {
                field.button().y = fieldY;
            }
        }
    }

    private void handleSubmit() {
        Results results = new Results();
        for (int i = 0; i < this.args.getWidgets().size(); i++) {
            results.value(args.getWidgets().get(i).name(), i, this.inputFields.get(i).input().getValue());
        }

        if (results.isEmpty() || this.submitButton.getMessage().getString().equals(BaniraCodex.languager().getTranslationClient(EnumI18nType.OPTION, "cancel"))) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        // 验证输入
        this.errorText.clear();
        this.errorTextMap.clear();
        for (int i = 0; i < args.getWidgets().size(); i++) {
            Widget widget = args.getWidgets().get(i);
            results.curIndex(i).curName(widget.name());
            String error = widget.validator().apply(results);
            // 设置输入框的错误状态
            if (i < inputFields.size()) {
                BaniraTextField input = inputFields.get(i).input();
                boolean hasError = StringUtils.isNotNullOrEmpty(error);
                input.error(hasError);
                if (hasError) {
                    Text errorTextItem = Text.literal(error).color(Color.argb(0xAAFF0000));
                    this.errorText.add(errorTextItem);
                    this.errorTextMap.put(i, errorTextItem);
                } else {
                    this.errorTextMap.remove(i);
                }
            }
        }

        if (this.errorText.isEmptyEx()) {
            try {
                args.getCallback().accept(results);
            } catch (Exception e) {
                results.runningResult(e);
            }

            if (StringUtils.isNullOrEmptyEx(results.runningResult())) {
                Minecraft.getInstance().setScreen(this.previousScreen());
            } else {
                this.runningErrorText = Text.literal(results.runningResult()).color(Color.argb(0xFFFF0000));
            }
        }
    }

    @Override
    protected void renderEvent(MatrixStack stack, float partialTicks) {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        // 更新输入框位置
        updateInputFieldsPosition();

        if (this.submitButton != null) {
            this.submitButton.y = this.buttonY;
        }
        if (this.cancelButton != null) {
            this.cancelButton.y = this.buttonY;
        }

        this.renderBackground(stack);

        // 绘制标题
        int scrollOffset = getScrollOffset();
        for (int i = 0; i < args.getWidgets().size(); i++) {
            Widget widget = args.getWidgets().get(i);
            int titleY = contentAreaTop + LayoutConfig.StringInput.INPUT_FIELD_SPACING * i - scrollOffset + 2;
            AbstractGuiUtils.drawLimitedText(FontDrawArgs.of(widget.title().stack(stack))
                    .x(this.width / 2.0f - 100)
                    .y(titleY));
        }

        // 渲染按钮
        super.renderButtons(stack, partialTicks);

        // 更新并渲染滚动条
        if (super.scrollBar() != null) {
            super.scrollBar().updateScrollParams(this.scrollableContentHeight, this.visibleContentHeight);
            super.scrollBar().render(stack);
        }

        // 绘制错误提示
        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            BaniraTextField input = field.input();
            if (input.error() && input.isMouseOver((int) mouseHelper.mouseX(), (int) mouseHelper.mouseY())) {
                Text errorTextItem = this.errorTextMap.get(i).stack(stack);
                if (errorTextItem != null) {
                    AbstractGuiUtils.drawPopupMessageWithSeason(FontDrawArgs.ofPopo(errorTextItem)
                            .x(mouseHelper.mouseX() + 5)
                            .y(mouseHelper.mouseY() + 5)
                            .padding(0)
                            .maxWidth(200)
                            .position(EnumEllipsisPosition.MIDDLE)
                    );
                }
            }
        }

        // 显示执行错误
        String runningErrorTextContent = this.runningErrorText.content();
        if (StringUtils.isNotNullOrEmpty(runningErrorTextContent)) {
            this.runningErrorText = Text.empty();
            DialogUtils.openMessageBox("Something Error!", runningErrorTextContent, DialogUtils.DialogIconType.error, DialogUtils.DialogButtonType.ok, result -> {
            });
        }

        // 更新提交按钮文本
        boolean allValid = this.args.getWidgets().stream().allMatch(wi ->
                wi.allowEmpty() || StringUtils.isNotNullOrEmpty(this.inputFields.get(args.getWidgets().indexOf(wi)).input().getValue()));
        if (allValid) {
            this.submitButton.setMessage(Component.translatableClient(EnumI18nType.OPTION, "submit").toTextComponent());
        } else {
            this.submitButton.setMessage(Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent());
        }
    }

    @Override
    protected void mouseClickedEvent(MouseClickedHandleArgs eventArgs) {
        if (eventArgs.button() == GLFWKey.GLFW_MOUSE_BUTTON_4) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void handlePopupOption(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    protected void mouseReleasedEvent(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    protected void mouseMovedEvent() {
    }

    @Override
    protected void mouseScrolledEvent(MouseScoredHandleArgs eventArgs) {
        if (super.scrollBar() != null && this.scrollableContentHeight > this.visibleContentHeight) {
            int currentOffset = super.scrollBar().scrollOffset();
            int targetOffset = currentOffset - (int) (eventArgs.delta() * (LayoutConfig.StringInput.SCROLL_SPEED - 1));
            super.scrollBar().setScrollOffset(targetOffset);
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
        if (eventArgs.key() == GLFWKey.GLFW_KEY_ESCAPE
                || (eventArgs.key() == GLFWKey.GLFW_KEY_BACKSPACE && this.inputFields.stream().noneMatch(w -> w.input().isFocused()))) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void keyReleasedEvent(KeyReleasedHandleArgs eventArgs) {
    }

    @Override
    protected void removedEvent() {
    }

    @Override
    protected void closeEvent() {
    }
}
