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
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumStringInputRegex;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.DialogUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 字符串输入Screen
 */
public class StringInputScreen extends BaniraScreen {
    private final Args args;
    private final List<InputField> inputFields = new ArrayList<>();
    private final Map<Integer, Text> errorTextMap = new HashMap<>();
    private Text runningErrorText = Text.empty();
    private final List<String> inputValues = new ArrayList<>();

    @Nullable
    private ButtonWidget submitButtonWidget;
    @Nullable
    private ButtonWidget cancelButtonWidget;
    @Nullable
    private ScrollbarWidget scrollbarWidget;

    public StringInputScreen(Args args) {
        super(args.getTitle() != null ? args.getTitle().toComponent() : Component.literal("StringInputScreen"));
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.previousScreen(args.getParentScreen());
        BaniraScreen.inheritThemeAndSeason(this, args.getParentScreen(), null, null);
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class InputField {
        @Nullable
        private LabelWidget titleLabel;
        @Nullable
        private InputWidget input;
        @Nullable
        private ButtonWidget button;
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
        private Text hint = Text.transAuto(BaniraCodex.MODID, "enter_something");
        private String regex = EnumStringInputRegex.NONE.regex();
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

        public Widget type(WidgetType type) {
            this.type = type;
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
    protected void onInit() {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        this.inputFields.clear();
    }

    private static final int TOP_MARGIN = 12;
    private static final int BOTTOM_MARGIN = 12;
    private static final int LIST_BTN_GAP = 12;
    private static final int TITLE_TOP_MARGIN = 2;
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_INPUT_GAP = 2;
    private static final int INPUT_H = 17;
    /**
     * 每个输入框组之间的间距
     */
    private static final int ITEM_GAP = 8;
    /**
     * 每个输入项总高度：标题上边距 + 标题 + 间距 + 输入框 + 组间距
     */
    private static final int ITEM_HEIGHT = TITLE_TOP_MARGIN + TITLE_HEIGHT + TITLE_INPUT_GAP + INPUT_H + ITEM_GAP;
    private static final int BTN_H = 24;
    private static final int CONTENT_MAX_WIDTH = 260;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 3;
    private static final int PICKER_BTN_W = 28;
    private static final int PICKER_BTN_GAP = 2;

    /**
     * 布局状态：是否处于滚动模式（内容超出屏幕）
     */
    private boolean scrollMode;
    /**
     * 内容区域左边界
     */
    private int contentLeft;
    /**
     * 输入框宽度（不含滚动条）
     */
    private int inputW;
    /**
     * 内容总宽度（含滚动条）
     */
    private int contentTotalWidth;
    /**
     * 输入框列表顶部 Y（用于居中时；滚动模式下为 listAreaTop）
     */
    private int listTop;
    /**
     * 列表可视区域高度（滚动模式下有效）
     */
    private int listAreaHeight;
    /**
     * 按钮区域 Y
     */
    private int btnY;

    @Override
    protected void initWidgets() {
        inputFields.clear();
        int w = width;
        int h = height;
        int listHeight = args.getWidgets().size() * ITEM_HEIGHT;
        int contentHeight = TOP_MARGIN + listHeight + LIST_BTN_GAP + BTN_H + BOTTOM_MARGIN;

        contentTotalWidth = Math.min(w - TOP_MARGIN * 2, CONTENT_MAX_WIDTH + SCROLLBAR_WIDTH + SCROLLBAR_GAP);
        inputW = contentTotalWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        contentLeft = (w - contentTotalWidth) / 2;

        scrollMode = contentHeight > h;
        if (scrollMode) {
            btnY = h - BOTTOM_MARGIN - BTN_H;
            listTop = TOP_MARGIN;
            listAreaHeight = Math.max(0, btnY - listTop - LIST_BTN_GAP);
        } else {
            int contentTop = (h - contentHeight) / 2 + TOP_MARGIN;
            listTop = contentTop;
            btnY = contentTop + listHeight + LIST_BTN_GAP;
            listAreaHeight = listHeight;
        }

        submitButtonWidget = new ButtonWidget(this);
        submitButtonWidget.id("submit");
        submitButtonWidget.renderCoordinate(new ScreenCoordinate(contentLeft + inputW - 80, btnY, 80, BTN_H));
        submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "submit"));
        submitButtonWidget.onClick(b -> handleSubmit());
        addWidget(submitButtonWidget);

        cancelButtonWidget = new ButtonWidget(this);
        cancelButtonWidget.id("cancel");
        cancelButtonWidget.renderCoordinate(new ScreenCoordinate(contentLeft, btnY, 80, BTN_H));
        cancelButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "cancel"));
        cancelButtonWidget.onClick(b -> Minecraft.getInstance().setScreen(this.previousScreen()));
        addWidget(cancelButtonWidget);

        for (int i = 0; i < args.getWidgets().size(); i++) {
            Widget widget = args.getWidgets().get(i);
            String defaultValue = StringUtils.isNullOrEmptyEx(widget.defaultValue()) ? "" : widget.defaultValue();
            while (inputValues.size() <= i) {
                inputValues.add(defaultValue);
            }
            String currentValue = inputValues.get(i);

            InputField inputField = new InputField();
            inputField.title(widget.title());
            inputField.type(widget.type());
            inputField.fileFilter(widget.fileFilter());
            inputField.value(currentValue);

            int itemTop = listTop + i * ITEM_HEIGHT;
            int titleY = itemTop + TITLE_TOP_MARGIN;
            int inputY = titleY + TITLE_HEIGHT + TITLE_INPUT_GAP;

            // 标题
            LabelWidget titleLabel = new LabelWidget(this);
            titleLabel.id("title_" + i);
            titleLabel.text(widget.title());
            titleLabel.renderCoordinate(new ScreenCoordinate(contentLeft, titleY, inputW, TITLE_HEIGHT));
            titleLabel.textWrap(false);
            inputField.titleLabel(titleLabel);
            addWidget(titleLabel);

            if (widget.type() == WidgetType.TEXT) {
                InputWidget inputWidget = new InputWidget(this);
                inputWidget.id("input_" + i);
                inputWidget.renderCoordinate(new ScreenCoordinate(contentLeft, inputY, inputW, INPUT_H));
                inputWidget.value(currentValue);
                inputWidget.text(widget.hint());
                inputWidget.enabled(!widget.disabled());

                int finalI = i;
                inputWidget.onTextChanged(text -> {
                    if (finalI < inputValues.size()) {
                        inputValues.set(finalI, text);
                    } else {
                        while (inputValues.size() <= finalI) inputValues.add("");
                        inputValues.set(finalI, text);
                    }
                    validateAndUpdateError(widget, finalI, text, inputWidget);
                });

                inputField.input(inputWidget);
                addWidget(inputWidget);
            } else if (widget.type() == WidgetType.FILE || widget.type() == WidgetType.COLOR) {
                int pickerInputW = inputW - PICKER_BTN_W - PICKER_BTN_GAP;
                InputWidget pickerInput = new InputWidget(this);
                pickerInput.id("input_" + i);
                pickerInput.renderCoordinate(new ScreenCoordinate(contentLeft, inputY, pickerInputW, INPUT_H));
                pickerInput.value(currentValue);
                Text pickerHint = widget.type() == WidgetType.FILE
                        ? Text.transAuto(BaniraCodex.MODID, "enter_file_path")
                        : Text.transAuto(BaniraCodex.MODID, "enter_color_hex");
                pickerInput.text(pickerHint);
                pickerInput.enabled(!widget.disabled());
                pickerInput.editable(true);

                int finalI = i;
                pickerInput.onTextChanged(text -> {
                    if (finalI < inputValues.size()) {
                        inputValues.set(finalI, text);
                    } else {
                        while (inputValues.size() <= finalI) inputValues.add("");
                        inputValues.set(finalI, text);
                    }
                    validateAndUpdateError(widget, finalI, text, pickerInput);
                });

                ButtonWidget pickerBtn = new ButtonWidget(this);
                pickerBtn.id("btn_" + i);
                pickerBtn.renderCoordinate(new ScreenCoordinate(contentLeft + pickerInputW + PICKER_BTN_GAP, inputY, PICKER_BTN_W, INPUT_H));
                pickerBtn.text(Text.literal("..."));
                pickerBtn.onClick(b -> {
                    if (widget.type() == WidgetType.FILE) {
                        String[] exts = new String[0];
                        if (StringUtils.isNotNullOrEmpty(widget.fileFilter())) {
                            String[] parts = widget.fileFilter().split("[,;\\s]+");
                            exts = new String[parts.length];
                            for (int j = 0; j < parts.length; j++) {
                                String p = parts[j].trim();
                                exts[j] = p.startsWith("*") ? p : "*." + p;
                            }
                        }
                        DialogUtils.chooseFileString(widget.title().content(), path -> {
                            if (path != null) {
                                pickerInput.value(path);
                            }
                        }, exts);
                    } else if (widget.type() == WidgetType.COLOR) {
                        DialogUtils.chooseRgbHex(widget.title().content(), color -> {
                            if (color != null && !color.isEmpty()) {
                                pickerInput.value(color);
                            }
                        });
                    }
                });

                inputField.input(pickerInput);
                inputField.button(pickerBtn);
                addWidget(pickerInput);
                addWidget(pickerBtn);
            }

            inputFields.add(inputField);
        }

        // 滚动条
        int scrollH = scrollMode ? listAreaHeight : 0;
        int maxScroll = Math.max(0, listHeight - listAreaHeight);
        scrollbarWidget = new ScrollbarWidget(this);
        scrollbarWidget.id("scroll");
        scrollbarWidget.renderCoordinate(new ScreenCoordinate(contentLeft + inputW + SCROLLBAR_GAP, listTop, SCROLLBAR_WIDTH, scrollH));
        scrollbarWidget.orientation(ScrollbarWidget.Orientation.VERTICAL);
        scrollbarWidget.minValue(0);
        scrollbarWidget.maxValue(maxScroll);
        scrollbarWidget.visibleSize(listAreaHeight);
        scrollbarWidget.scrollStep(ITEM_HEIGHT);
        scrollbarWidget.onValueChanged(v -> updateInputFieldsPosition());
        scrollbarWidget.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop, inputW + SCROLLBAR_GAP + SCROLLBAR_WIDTH, listAreaHeight));
        scrollbarWidget.visible(scrollMode && maxScroll > 0);
        addWidget(scrollbarWidget);
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        updateInputFieldsPosition();
    }

    @Override
    protected boolean shouldWidgetReceiveClick(IWidget widget, double mouseX, double mouseY, int button) {
        if (!scrollMode) return true;
        boolean isListWidget = false;
        for (InputField field : inputFields) {
            if (widget == field.titleLabel() || widget == field.input() || widget == field.button()) {
                isListWidget = true;
                break;
            }
        }
        if (!isListWidget) return true;
        // 点击须在可见列表区域内
        return mouseX >= contentLeft && mouseX < contentLeft + inputW + SCROLLBAR_GAP + SCROLLBAR_WIDTH
                && mouseY >= listTop && mouseY < listTop + listAreaHeight;
    }

    private int getScrollOffset() {
        return scrollbarWidget != null ? (int) scrollbarWidget.value() : 0;
    }

    /**
     * 构建包含所有输入框当前值的 Results，用于校验（支持跨字段校验）
     */
    private Results buildResultsFromInputs(int overrideIndex, String overrideValue) {
        Results r = new Results();
        for (int i = 0; i < inputFields.size() && i < args.getWidgets().size(); i++) {
            InputField field = inputFields.get(i);
            String val = (i == overrideIndex && overrideValue != null) ? overrideValue
                    : (field.input() != null ? field.input().value() : "");
            r.value(args.getWidgets().get(i).name(), i, val);
        }
        return r;
    }

    /**
     * 校验并更新错误状态。
     * 使用 buildResultsFromInputs 支持跨字段校验。
     */
    private void validateAndUpdateError(Widget widget, int index, String text, InputWidget inputWidget) {
        Results r = buildResultsFromInputs(index, text);
        r.curIndex(index).curName(widget.name());
        String error = widget.validator().apply(r);
        boolean hasError = StringUtils.isNotNullOrEmpty(error);
        if (inputWidget != null) {
            inputWidget.error(hasError);
        }
        if (hasError) {
            errorTextMap.put(index, Text.literal(error).color(Color.argb(0xFFFF0000)));
        } else {
            errorTextMap.remove(index);
        }
        if (!hasError && widget.changed() != null) {
            widget.changed().accept(new Inputs().curIndex(index).curName(widget.name()));
        }
    }

    private void updateInputFieldsPosition() {
        int scrollOffset = scrollMode ? getScrollOffset() : 0;
        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            int itemTop = listTop + ITEM_HEIGHT * i - scrollOffset;
            int titleY = itemTop + TITLE_TOP_MARGIN;
            int inputY = titleY + TITLE_HEIGHT + TITLE_INPUT_GAP;

            if (field.titleLabel() != null) {
                field.titleLabel().renderCoordinate(new ScreenCoordinate(contentLeft, titleY, inputW, TITLE_HEIGHT));
            }
            if (field.input() != null && field.button() != null) {
                int pickerInputW = inputW - PICKER_BTN_W - PICKER_BTN_GAP;
                field.input().renderCoordinate(new ScreenCoordinate(contentLeft, inputY, pickerInputW, INPUT_H));
                field.button().renderCoordinate(new ScreenCoordinate(contentLeft + pickerInputW + PICKER_BTN_GAP, inputY, PICKER_BTN_W, INPUT_H));
            } else if (field.input() != null) {
                field.input().renderCoordinate(new ScreenCoordinate(contentLeft, inputY, inputW, INPUT_H));
            } else if (field.button() != null) {
                field.button().renderCoordinate(new ScreenCoordinate(contentLeft, inputY, inputW, INPUT_H));
            }
        }
    }

    private void handleSubmit() {
        Results results = new Results();
        for (int i = 0; i < this.args.getWidgets().size() && i < this.inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            if (field.input() != null) {
                results.value(args.getWidgets().get(i).name(), i, field.input().value());
            }
        }

        if (results.isEmpty() || (submitButtonWidget != null && submitButtonWidget.text().content().equals(BaniraCodex.languager().getTranslationClient(EnumI18nType.WORD, "cancel")))) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        // 验证输入
        this.errorTextMap.clear();
        for (int i = 0; i < args.getWidgets().size() && i < inputFields.size(); i++) {
            Widget widget = args.getWidgets().get(i);
            InputField field = inputFields.get(i);
            results.curIndex(i).curName(widget.name());
            String error = widget.validator().apply(results);
            // 设置输入框的错误状态
            if (field.input() != null) {
                boolean hasError = StringUtils.isNotNullOrEmpty(error);
                field.input().error(hasError);
                if (hasError) {
                    Text errorTextItem = Text.literal(error).color(Color.argb(0xAAFF0000));
                    this.errorTextMap.put(i, errorTextItem);
                } else {
                    this.errorTextMap.remove(i);
                }
            }
        }

        if (this.errorTextMap.isEmpty()) {
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
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        if (scrollMode) {
            // 滚动模式：对输入框列表区域启用裁剪，避免与底部按钮重叠
            AbstractGuiUtils.enableScissor(contentLeft, listTop, inputW + SCROLLBAR_GAP + SCROLLBAR_WIDTH, listAreaHeight);
        }

        for (IWidget widget : widgets()) {
            if (widget == scrollbarWidget || widget == submitButtonWidget || widget == cancelButtonWidget) continue;
            if (widget.visible() && widget.parent() == null) {
                if (widget.enabled()) {
                    widget.update();
                }
                widget.render(stack, partialTicks);
            }
        }

        if (scrollMode) {
            AbstractGuiUtils.disableScissor();
        }

        // 渲染确认、取消按钮
        if (submitButtonWidget != null && submitButtonWidget.visible()) {
            if (submitButtonWidget.enabled()) submitButtonWidget.update();
            submitButtonWidget.render(stack, partialTicks);
        }
        if (cancelButtonWidget != null && cancelButtonWidget.visible()) {
            if (cancelButtonWidget.enabled()) cancelButtonWidget.update();
            cancelButtonWidget.render(stack, partialTicks);
        }

        // 滚动条最后渲染，确保在最上层
        if (scrollbarWidget != null && scrollbarWidget.visible()) {
            scrollbarWidget.renderCoordinate(new ScreenCoordinate(contentLeft + inputW + SCROLLBAR_GAP, listTop, SCROLLBAR_WIDTH, listAreaHeight));
            if (scrollbarWidget.enabled()) {
                scrollbarWidget.update();
            }
            scrollbarWidget.render(stack, partialTicks);
        }
    }

    @Override
    protected void renderEvent(MatrixStack stack, float partialTicks) {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            return;
        }

        renderWidgets(stack, partialTicks);

        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            if (field.input() != null) {
                while (inputValues.size() <= i) inputValues.add("");
                inputValues.set(i, field.input().value());
            }
        }

        if (widgets().isEmpty()) {
            this.renderBackground(stack);
        }

        super.renderButtons(stack, partialTicks);

        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            if (field.input() != null && field.input().error() && field.input().hoveringCoordinates() != null && !field.input().hoveringCoordinates().isEmpty()) {
                Text errorTextItem = this.errorTextMap.get(i);
                if (errorTextItem != null) {
                    TooltipWidget.drawPopupMessageWithSeason(stack, FontDrawArgs.of(errorTextItem.stack(stack))
                            .x(inputState.mouseX() + 5)
                            .y(inputState.mouseY() + 5)
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
        if (submitButtonWidget != null) {
            boolean allValid = this.args.getWidgets().stream().allMatch(wi -> {
                int index = args.getWidgets().indexOf(wi);
                if (index >= 0 && index < inputFields.size()) {
                    InputField field = inputFields.get(index);
                    return wi.allowEmpty() || (field.input() != null && StringUtils.isNotNullOrEmpty(field.input().value()));
                }
                return true;
            });
            if (allValid) {
                submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "submit"));
            } else {
                submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "cancel"));
            }
        }
    }

    @Override
    protected void mouseClickedEvent(MouseClickedHandleArgs eventArgs) {
        if (eventArgs.button() == GLFWKey.GLFW_MOUSE_BUTTON_4) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            eventArgs.consumed(true);
        }
        // 若当前点击未被消费且未被拦截，则取消所有已聚焦的Widget
        super.mouseClickedEvent(eventArgs);
    }

    @Override
    protected void mouseScrolledEvent(MouseScoredHandleArgs eventArgs) {
        if (scrollbarWidget != null && scrollbarWidget.maxValue() > 0) {
            double currentValue = scrollbarWidget.value();
            double targetValue = Math.max(0, Math.min(scrollbarWidget.maxValue(), currentValue - eventArgs.delta() * 20));
            scrollbarWidget.setValue(targetValue);
            updateInputFieldsPosition();
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
        super.keyPressedEvent(eventArgs);
        if (eventArgs.consumed()) {
            return;
        }
        if (eventArgs.key() == GLFWKey.GLFW_KEY_ESCAPE
                || (eventArgs.key() == GLFWKey.GLFW_KEY_BACKSPACE && this.inputFields.stream().noneMatch(w -> w.input() != null && w.input().focused()))) {
            Minecraft.getInstance().setScreen(this.previousScreen());
            eventArgs.consumed(true);
        }
    }

}
