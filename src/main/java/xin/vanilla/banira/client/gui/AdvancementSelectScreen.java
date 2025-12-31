package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.component.*;
import xin.vanilla.banira.client.gui.helper.LayoutConfig;
import xin.vanilla.banira.client.gui.helper.OperationButtonRender;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.network.data.AdvancementData;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class AdvancementSelectScreen extends BaniraScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Args args;

    private static final Component TITLE = Component.literal("AdvancementSelectScreen");

    /**
     * 输入框
     */
    private BaniraTextField inputField;
    /**
     * 输入框文本
     */
    private String inputFieldText = "";
    /**
     * 搜索结果
     */
    private final List<AdvancementData> advancementList = new ArrayList<>();
    /**
     * 操作按钮
     */
    private final Map<Integer, OperationButton> OP_BUTTONS = new HashMap<>();
    /**
     * 进度按钮
     */
    private final List<OperationButton> ADVANCEMENT_BUTTONS = new ArrayList<>();
    /**
     * 当前选择的进度
     */
    private ResourceLocation currentAdvancement;
    /**
     * 显示模式
     */
    private boolean displayMode = true;

    private int bgX;
    private int bgY;
    private double effectBgX;
    private double effectBgY;

    /**
     * 上一次的加载状态，用于检测加载完成
     */
    private boolean wasLoading = false;

    /**
     * 操作按钮类型
     */
    @Getter
    enum OperationButtonType {
        TYPE(1),
        ADVANCEMENT(2),
        PROBABILITY(6),
        ;

        final int code;

        OperationButtonType(int code) {
            this.code = code;
        }

        static OperationButtonType valueOf(int code) {
            return Arrays.stream(values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
        }
    }

    public AdvancementSelectScreen(Args args) {
        super(TITLE.toTextComponent());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.currentAdvancement = args.defaultAdvancement();
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        /**
         * 父级 Screen
         */
        private Screen parentScreen;
        /**
         * 默认值
         */
        private ResourceLocation defaultAdvancement = BaniraCodex.resourceFactory().empty();
        /**
         * 输入数据回调
         */
        private Consumer<ResourceLocation> onDataReceived1;
        /**
         * 输入数据回调
         */
        private Function<ResourceLocation, String> onDataReceived2;
        /**
         * 是否要显示该界面, 若为false则直接关闭当前界面并返回到调用者的 Screen
         */
        private Supplier<Boolean> shouldClose;

        public Args onDataReceived(Consumer<ResourceLocation> onDataReceived) {
            this.onDataReceived1 = onDataReceived;
            return this;
        }

        public Args onDataReceived(Function<ResourceLocation, String> onDataReceived) {
            this.onDataReceived2 = onDataReceived;
            return this;
        }

        public void validate() {
            Objects.requireNonNull(this.parentScreen());
            if (this.onDataReceived1() == null)
                Objects.requireNonNull(this.onDataReceived2());
            if (this.onDataReceived2() == null)
                Objects.requireNonNull(this.onDataReceived1());
        }

    }

    @Override
    protected void initEvent() {
        super.initScrollBar(new ScrollBar());

        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            Minecraft.getInstance().setScreen(args.parentScreen());

        // 确保本地有进度数据
        AdvancementUtils.ensureAdvancementData();

        // 初始化加载状态跟踪
        this.wasLoading = AdvancementUtils.isLoading();

        this.updateSearchResults();
        this.updateLayout();
        // 创建文本输入框
        this.inputField = AbstractGuiUtils.newTextFieldWidget(
                this.font,
                bgX,
                bgY,
                LayoutConfig.AdvancementSelect.PANEL_WIDTH,
                LayoutConfig.AdvancementSelect.INPUT_FIELD_HEIGHT,
                Component.empty()
        ).hint("搜索进度...");
        this.inputField.setResponder(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        this.inputField.setValue(this.inputFieldText);
        this.addButton(this.inputField);

        int buttonY = (int) (this.bgY + (LayoutConfig.AdvancementSelect.PANEL_HEIGHT_OFFSET +
                (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN) * LayoutConfig.AdvancementSelect.MAX_LINES +
                LayoutConfig.AdvancementSelect.MARGIN));
        int buttonWidth = (int) (LayoutConfig.AdvancementSelect.BUTTON_WIDTH - LayoutConfig.AdvancementSelect.MARGIN * 2);

        // 创建提交按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + LayoutConfig.AdvancementSelect.BUTTON_WIDTH + LayoutConfig.AdvancementSelect.MARGIN),
                buttonY,
                buttonWidth,
                LayoutConfig.AdvancementSelect.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "submit").toTextComponent(),
                button -> {
                    if (this.currentAdvancement == null) {
                        Minecraft.getInstance().setScreen(args.parentScreen());
                    } else {
                        // 获取选择的数据，并执行回调
                        ResourceLocation resourceLocation = this.currentAdvancement;
                        if (args.onDataReceived1() != null) {
                            args.onDataReceived1().accept(resourceLocation);
                            Minecraft.getInstance().setScreen(args.parentScreen());
                        } else if (args.onDataReceived2() != null) {
                            String result = args.onDataReceived2().apply(resourceLocation);
                            if (StringUtils.isNotNullOrEmpty(result)) {
                                // this.errorText = Text.literal(result).setColorArgb(0xFFFF0000);
                            } else {
                                Minecraft.getInstance().setScreen(args.parentScreen());
                            }
                        }
                    }
                }
        ));

        // 创建取消按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + LayoutConfig.AdvancementSelect.MARGIN),
                buttonY,
                buttonWidth,
                LayoutConfig.AdvancementSelect.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(args.parentScreen())
        ));
    }

    @Override
    public void renderEvent(MatrixStack matrixStack, float partialTicks) {
        super.keyManager.tick();
        // 绘制背景
        this.renderBackground(matrixStack);
        int panelWidth = LayoutConfig.AdvancementSelect.PANEL_WIDTH;
        int panelHeight = LayoutConfig.AdvancementSelect.PANEL_HEIGHT_OFFSET +
                (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN) * LayoutConfig.AdvancementSelect.MAX_LINES) +
                LayoutConfig.AdvancementSelect.BUTTON_HEIGHT +
                (int) (LayoutConfig.AdvancementSelect.MARGIN * 2) + 5;
        AbstractGuiUtils.drawRoundedRect(
                matrixStack,
                (int) (this.bgX - LayoutConfig.AdvancementSelect.MARGIN),
                (int) (this.bgY - LayoutConfig.AdvancementSelect.MARGIN),
                (int) (panelWidth + LayoutConfig.AdvancementSelect.MARGIN * 2),
                panelHeight,
                LayoutConfig.Colors.BACKGROUND,
                2
        );
        int listAreaHeight = (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN) * LayoutConfig.AdvancementSelect.MAX_LINES + LayoutConfig.AdvancementSelect.MARGIN);
        AbstractGuiUtils.drawRoundedRectOutLineRough(
                matrixStack,
                (int) (this.effectBgX - LayoutConfig.AdvancementSelect.MARGIN),
                (int) (this.effectBgY - LayoutConfig.AdvancementSelect.MARGIN),
                LayoutConfig.AdvancementSelect.LIST_WIDTH,
                listAreaHeight,
                1,
                LayoutConfig.Colors.BORDER,
                1
        );

        super.renderButtons(matrixStack, partialTicks);

        // 保存输入框的文本, 防止窗口重绘时输入框内容丢失
        this.inputFieldText = this.inputField.getValue();

        // 检测加载状态变化
        boolean isLoading = AdvancementUtils.isLoading();
        if (this.wasLoading && !isLoading) {
            this.updateSearchResults();
        }
        this.wasLoading = isLoading;

        if (isLoading) {
            String loadingText = "加载中...";
            int textWidth = this.font.width(loadingText);
            int textX = this.width / 2 - textWidth / 2;
            int textY = (int) (this.effectBgY + (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN) * LayoutConfig.AdvancementSelect.MAX_LINES / 2.0 - this.font.lineHeight / 2.0);
            this.font.drawShadow(matrixStack, loadingText, textX, textY, 0xFFFFFFFF);
        }

        this.renderButton(matrixStack);
    }

    @Override
    protected void removedEvent() {
    }


    @Override
    public void mouseClickedEvent(MouseClickedHandleArgs eventArgs) {
        AtomicBoolean flag = new AtomicBoolean(false);
        if (mouseHelper.isPressed(GLFWKey.GLFW_MOUSE_BUTTON_4)) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            flag.set(true);
        } else if (mouseHelper.onlyPressedLeft() || mouseHelper.onlyPressedRight()) {
            OP_BUTTONS.forEach((key, value) -> {
                if (value.hovered()) {
                    value.pressed(true);
                }
            });
            // 进度按钮
            ADVANCEMENT_BUTTONS.forEach(bt -> bt.pressed(bt.hovered()));
        }
        eventArgs.consumed(flag.get());
    }

    @Override
    protected void handlePopupOption(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    public void mouseReleasedEvent(MouseReleasedHandleArgs eventArgs) {
        AtomicBoolean flag = new AtomicBoolean(false);
        AtomicBoolean updateSearchResults = new AtomicBoolean(false);
        if (mouseHelper.onlyPressedLeft() || mouseHelper.onlyPressedRight()) {
            // 控制按钮
            OP_BUTTONS.forEach((key, value) -> {
                if (value.hovered() && value.pressed()) {
                    this.handleOperation(value, flag, updateSearchResults);
                }
                value.pressed(false);
            });
            // 进度按钮
            ADVANCEMENT_BUTTONS.forEach(bt -> {
                if (bt.hovered() && bt.pressed()) {
                    this.handleAdvancement(bt, flag);
                }
                bt.pressed(false);
            });
            if (updateSearchResults.get()) {
                this.updateSearchResults();
            }
        }
        eventArgs.consumed(flag.get());
    }

    @Override
    public void mouseMovedEvent() {
        // 控制按钮
        OP_BUTTONS.forEach((key, value) -> value.hovered(value.isMouseOverEx(mouseHelper)));
        // 进度按钮
        ADVANCEMENT_BUTTONS.forEach(bt -> bt.hovered(bt.isMouseOverEx(mouseHelper)));
    }

    @Override
    protected void mouseScrolledEvent(MouseScoredHandleArgs eventArgs) {
    }

    @Override
    public void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
        if (super.keyManager.onlyEscapePressed() || (super.keyManager.onlyBackspacePressed() && !this.inputField.isFocused())) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            eventArgs.consumed(true);
        } else if (super.keyManager.onlyEnterPressed() && this.inputField.isFocused()) {
            this.updateSearchResults();
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void keyReleasedEvent(KeyReleasedHandleArgs eventArgs) {
    }

    @Override
    protected void closeEvent() {
    }

    public void updateLayout() {
        this.bgX = this.width / 2 - LayoutConfig.AdvancementSelect.BG_X_OFFSET;
        this.bgY = this.height / 2 - LayoutConfig.AdvancementSelect.BG_Y_OFFSET;
        this.effectBgX = this.bgX + LayoutConfig.AdvancementSelect.MARGIN;
        this.effectBgY = this.bgY + LayoutConfig.AdvancementSelect.PANEL_HEIGHT_OFFSET;

        // 初始化操作按钮
        this.OP_BUTTONS.put(OperationButtonType.TYPE.getCode(), new OperationButton(OperationButtonType.TYPE.getCode(), context -> {
            // 绘制背景
            OperationButtonRender.renderOperationButtonBackground(context);
            ItemStack itemStack = new ItemStack(this.displayMode ? Items.CHEST : Items.COMPASS);
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, itemStack);
            int total = AdvancementUtils.getAllAdvancements().size();
            int displayableCount = AdvancementUtils.getDisplayableAdvancements().size();
            Text text = Text.translatable(BaniraCodex.MODID,
                    EnumI18nType.TIPS,
                    (this.displayMode ? "advancement_select_list_icon_mode" : "advancement_select_list_all_mode"),
                    (this.displayMode ? displayableCount : total));
            OperationButtonRender.setOperationButtonTooltip(context, text);
        }).x(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - (int) LayoutConfig.AdvancementSelect.MARGIN - LayoutConfig.Button.OPERATION_BUTTON_X_OFFSET)
                .y(this.bgY + (int) LayoutConfig.AdvancementSelect.MARGIN)
                .width(AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET)
                .height(AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET));
        this.OP_BUTTONS.put(OperationButtonType.ADVANCEMENT.getCode(), new OperationButton(OperationButtonType.ADVANCEMENT.getCode(), context -> {
            // 绘制背景
            OperationButtonRender.renderOperationButtonBackground(context);
            ItemStack iconStack = new ItemStack(Items.BARRIER);
            String title = "-";
            if (this.currentAdvancement != null) {
                Optional<AdvancementData> dataOpt = AdvancementUtils.findAdvancementByRegistry(this.currentAdvancement.toString());
                if (dataOpt.isPresent()) {
                    AdvancementData data = dataOpt.get();
                    iconStack = data.displayInfo().getIcon();
                    title = data.displayInfo().getTitle().getString();
                }
            }
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, iconStack);
            OperationButtonRender.setOperationButtonTooltip(context, Text.literal(title));
        }).x(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - (int) LayoutConfig.AdvancementSelect.MARGIN - LayoutConfig.Button.OPERATION_BUTTON_X_OFFSET)
                .y(this.bgY + (int) LayoutConfig.AdvancementSelect.MARGIN + AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET + LayoutConfig.Button.OPERATION_BUTTON_SPACING)
                .width(AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET)
                .height(AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET));

        // 初始化滚动条位置
        double bgHeight = (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN) * LayoutConfig.AdvancementSelect.MAX_LINES - LayoutConfig.AdvancementSelect.MARGIN;
        super.scrollBar().x(effectBgX + LayoutConfig.AdvancementSelect.LIST_WIDTH + 2)
                .y(effectBgY - LayoutConfig.AdvancementSelect.MARGIN + 1)
                .width(5)
                .height((int) (bgHeight + LayoutConfig.AdvancementSelect.MARGIN + 1));

        // 进度列表
        this.ADVANCEMENT_BUTTONS.clear();
        for (int i = 0; i < LayoutConfig.AdvancementSelect.MAX_LINES; i++) {
            ADVANCEMENT_BUTTONS.add(new OperationButton(i, context -> {
                int i1 = context.button.operation();
                int scrollRow = advancementList.size() > LayoutConfig.AdvancementSelect.MAX_LINES ? super.scrollBar().scrollOffset() : 0;
                int index = scrollRow + i1;
                if (index >= 0 && index < advancementList.size()) {
                    AdvancementData advancementData = advancementList.get(index);
                    double effectX = effectBgX;
                    double effectY = effectBgY + i1 * (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.AdvancementSelect.MARGIN);
                    // 绘制背景
                    int bgColor;
                    if (context.button.hovered() || (advancementData.id().equals(this.currentAdvancement))) {
                        bgColor = LayoutConfig.Colors.SELECTED_BACKGROUND;
                    } else {
                        bgColor = LayoutConfig.Colors.BUTTON_BACKGROUND;
                    }
                    context.button.x(effectX - 1).y(effectY - 1).width(100).height(AbstractGuiUtils.ITEM_ICON_SIZE + 2)
                            .id(advancementData.id().toString());

                    AbstractGuiUtils.fill(context.matrixStack, (int) context.button.x(), (int) context.button.y(), (int) context.button.width(), (int) context.button.height(), bgColor);
                    FontDrawArgs drawArgs = FontDrawArgs.of(Text.literal(advancementData.displayInfo().getTitle().getString()).stack(context.matrixStack).font(this.font))
                            .x(context.button.x() + AbstractGuiUtils.ITEM_ICON_SIZE + (int) LayoutConfig.AdvancementSelect.MARGIN * 2)
                            .y(context.button.y() + (AbstractGuiUtils.ITEM_ICON_SIZE + 4 - this.font.lineHeight) / 2.0)
                            .maxWidth((int) context.button.width() - AbstractGuiUtils.ITEM_ICON_SIZE - 4)
                            .wrap(false);
                    AbstractGuiUtils.drawLimitedText(drawArgs);
                    this.itemRenderer.renderGuiItem(advancementData.displayInfo().getIcon(), (int) (context.button.x() + LayoutConfig.AdvancementSelect.MARGIN), (int) context.button.y());
                    context.button.tooltip(Text.literal(advancementData.displayInfo().getTitle().getString() + "\n" + advancementData.displayInfo().getDescription().getString()));
                } else {
                    context.button.x(0).y(0).width(0).height(0).id("");
                }
            }));
        }
    }

    /**
     * 更新搜索结果
     */
    private void updateSearchResults() {
        String s = this.inputField == null ? null : this.inputField.getValue();
        this.advancementList.clear();

        AdvancementUtils.ensureAdvancementData();

        // 根据显示模式和搜索关键字获取进度列表
        if (StringUtils.isNotNullOrEmpty(s)) {
            if (this.displayMode) {
                this.advancementList.addAll(AdvancementUtils.searchDisplayableAdvancements(s));
            } else {
                this.advancementList.addAll(AdvancementUtils.searchAdvancements(s));
            }
        } else {
            if (this.displayMode) {
                this.advancementList.addAll(AdvancementUtils.getDisplayableAdvancements());
            } else {
                this.advancementList.addAll(AdvancementUtils.getAllAdvancements());
            }
        }
        super.scrollBar().setScrollOffset(0);
    }

    /**
     * 绘制按钮
     */
    private void renderButton(MatrixStack matrixStack) {
        for (OperationButton button : OP_BUTTONS.values()) button.render(matrixStack, super.keyManager, mouseHelper);
        for (OperationButton button : ADVANCEMENT_BUTTONS) button.render(matrixStack, super.keyManager, mouseHelper);

        // 更新并渲染滚动条
        int totalRows = advancementList.size();
        super.scrollBar().updateScrollParams(totalRows, LayoutConfig.AdvancementSelect.MAX_LINES);
        super.scrollBar().render(matrixStack);
    }

    private void handleAdvancement(OperationButton bt, AtomicBoolean flag) {
        if (mouseHelper.isPressedLeft()) {
            if (StringUtils.isNotNullOrEmpty(bt.id())) {
                try {
                    ResourceLocation resourceLocation = new ResourceLocation(bt.id());
                    this.currentAdvancement = resourceLocation;
                    LOGGER.debug("Select advancement: {}", resourceLocation);
                    flag.set(true);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid advancement id format: {}", bt.id());
                } catch (Exception e) {
                    LOGGER.error("Unexpected error handling advancement click", e);
                }
            }
        }
    }

    private void handleOperation(OperationButton bt, AtomicBoolean flag, AtomicBoolean updateSearchResults) {
        if (bt.operation() == OperationButtonType.TYPE.getCode()) {
            this.displayMode = !this.displayMode;
            updateSearchResults.set(true);
            flag.set(true);
        }
        // 编辑进度ID
        else if (bt.operation() == OperationButtonType.ADVANCEMENT.getCode()) {
            String effectString = this.currentAdvancement != null ? this.currentAdvancement.toString() : "";
            StringInputScreen.Args args = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_advancement_json").shadow(true))
                            .message(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_something"))
                            .defaultValue(effectString)
                            .validator((input) -> {
                                try {
                                    new ResourceLocation(input.value());
                                } catch (IllegalArgumentException e) {
                                    return Component.translatableClient(EnumI18nType.TIPS, "advancement_json_s_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        String id = input.firstValue();
                        try {
                            this.currentAdvancement = new ResourceLocation(id);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid advancement id format: {}", id);
                        } catch (Exception e) {
                            LOGGER.error("Unexpected error parsing advancement id: {}", id, e);
                        }
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(args));
        }
    }

}
