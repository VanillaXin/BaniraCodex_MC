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
import xin.vanilla.banira.client.gui.component.BaniraButton;
import xin.vanilla.banira.client.gui.component.BaniraTextFieldWidget;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.component.TextList;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.DialogUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 内容输入 Screen
 */
public class StringInputScreen extends Screen {
    private static final int INPUT_FIELD_HEIGHT = 12;
    private static final int INPUT_FIELD_SPACING = 25;
    private static final int TITLE_HEIGHT = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 95;
    private static final int SCROLL_SPEED = 10;

    private final Args args;
    private final List<InputField> inputFields = new ArrayList<>();
    private final TextList errorText = new TextList();
    private final Map<Integer, Text> errorTextMap = new HashMap<>();
    private Text runningErrorText = Text.empty();

    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int contentHeight = 0;
    private int startY = 0;

    private BaniraButton submitButton;
    private BaniraButton cancelButton;

    public StringInputScreen(Args args) {
        super(args.getTitle() != null ? args.getTitle().toComponent().toTextComponent() : Component.literal("StringInputScreen").toTextComponent());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class InputField {
        private BaniraTextFieldWidget input;
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
    protected void init() {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(args.getParentScreen());
            return;
        }

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

            int fieldY = startY + TITLE_HEIGHT + INPUT_FIELD_SPACING * i + scrollOffset;
            int inputWidth = (widget.type() == WidgetType.FILE || widget.type() == WidgetType.COLOR) ? 175 : 200;

            BaniraTextFieldWidget input = new BaniraTextFieldWidget(
                    this.font,
                    this.width / 2 - 100,
                    fieldY,
                    inputWidth,
                    INPUT_FIELD_HEIGHT,
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

            this.addButton(input);
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
                        INPUT_FIELD_HEIGHT,
                        Component.literal("...").toTextComponent(),
                        button -> {
                            try {
                                if (widget.type() == WidgetType.FILE) {
                                    input.setValue(DialogUtils.chooseFileString("", widget.fileFilter()));
                                } else if (widget.type() == WidgetType.COLOR) {
                                    input.setValue(DialogUtils.chooseRgbHex(""));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                );
                this.addButton(fileButton);
                inputField.button(fileButton);
            }

            this.inputFields.add(inputField);
        }

        // 创建按钮
        int buttonY = startY + contentHeight - BUTTON_HEIGHT - 5 + scrollOffset;

        this.submitButton = new BaniraButton(
                this.width / 2 + 5,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> handleSubmit()
        );
        this.addButton(this.submitButton);

        this.cancelButton = new BaniraButton(
                this.width / 2 - 100,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(args.getParentScreen())
        );
        this.addButton(this.cancelButton);
    }

    private void calculateLayout() {
        contentHeight = args.getWidgets().size() * INPUT_FIELD_SPACING + TITLE_HEIGHT + BUTTON_HEIGHT + 15;
        startY = Math.max(10, (this.height - Math.min(contentHeight, this.height - 40)) / 2);
        maxScrollOffset = Math.max(0, contentHeight - (this.height - 40));
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
    }

    private void handleSubmit() {
        Results results = new Results();
        for (int i = 0; i < this.args.getWidgets().size(); i++) {
            results.value(args.getWidgets().get(i).name(), i, this.inputFields.get(i).input().getValue());
        }

        if (results.isEmpty() || this.submitButton.getMessage().getString().equals(BaniraCodex.languager().getTranslationClient(EnumI18nType.OPTION, "cancel"))) {
            Minecraft.getInstance().setScreen(args.getParentScreen());
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
                BaniraTextFieldWidget input = inputFields.get(i).input();
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
                Minecraft.getInstance().setScreen(args.getParentScreen());
            } else {
                this.runningErrorText = Text.literal(results.runningResult()).color(Color.argb(0xFFFF0000));
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack stack, int mouseX, int mouseY, float delta) {
        if (args.invisible != null && Boolean.TRUE.equals(args.invisible.get())) {
            Minecraft.getInstance().setScreen(args.getParentScreen());
            return;
        }

        // 更新输入框位置
        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            int fieldY = startY + TITLE_HEIGHT + INPUT_FIELD_SPACING * i + scrollOffset;
            field.input().y = fieldY;
            field.y(fieldY);
            field.value(field.input().getValue());

            // 更新按钮位置
            if (field.button() != null) {
                field.button().y = fieldY;
            }
        }

        // 更新按钮位置
        int buttonY = startY + contentHeight - BUTTON_HEIGHT - 5 + scrollOffset;
        this.submitButton.y = buttonY;
        this.cancelButton.y = buttonY;

        this.renderBackground(stack);
        super.render(stack, mouseX, mouseY, delta);

        // 绘制标题
        for (int i = 0; i < args.getWidgets().size(); i++) {
            Widget widget = args.getWidgets().get(i);
            int titleY = startY + INPUT_FIELD_SPACING * i + scrollOffset + 2;
            AbstractGuiUtils.drawLimitedText(FontDrawArgs.of(widget.title().stack(stack))
                    .x(this.width / 2.0f - 100)
                    .y(titleY));
        }

        // 绘制错误提示
        for (int i = 0; i < inputFields.size(); i++) {
            InputField field = inputFields.get(i);
            BaniraTextFieldWidget input = field.input();
            if (input.error() && input.isMouseOver(mouseX, mouseY)) {
                Text errorTextItem = this.errorTextMap.get(i).stack(stack);
                if (errorTextItem != null) {
                    AbstractGuiUtils.drawPopupMessageWithSeason(FontDrawArgs.ofPopo(errorTextItem)
                            .x(mouseX + 5)
                            .y(mouseY + 5)
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
            DialogUtils.openMessageBox("Something Error!", runningErrorTextContent, DialogUtils.DialogIconType.error, DialogUtils.DialogButtonType.ok);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_4) {
            Minecraft.getInstance().setScreen(args.getParentScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollOffset = (int) (this.scrollOffset - delta * SCROLL_SPEED);
        this.scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFWKey.GLFW_KEY_ESCAPE
                || (keyCode == GLFWKey.GLFW_KEY_BACKSPACE && this.inputFields.stream().noneMatch(w -> w.input().isFocused()))) {
            Minecraft.getInstance().setScreen(args.getParentScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
