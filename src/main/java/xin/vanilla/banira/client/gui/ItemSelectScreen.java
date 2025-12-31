package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.component.*;
import xin.vanilla.banira.client.gui.helper.LayoutConfig;
import xin.vanilla.banira.client.gui.helper.OperationButtonRender;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.ArraySet;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ItemSelectScreen extends BaniraScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Args args;

    private final static Component TITLE = Component.literal("ItemSelectScreen");

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
    private final ArraySet<ItemStack> itemList = new ArraySet<>();
    /**
     * 操作按钮
     */
    private final Map<Integer, OperationButton> OP_BUTTONS = new HashMap<>();
    /**
     * 物品按钮
     */
    private final List<OperationButton> ITEM_BUTTONS = new ArrayList<>();
    /**
     * 当前选择的物品 ID
     */
    @Getter
    private String selectedItemId;
    /**
     * 当前选择的物品
     */
    private ItemStack currentItem;
    /**
     * 背包模式
     */
    private boolean inventoryMode = false;

    private int bgX;
    private int bgY;
    private double itemBgX;
    private double itemBgY;

    /**
     * 操作按钮类型
     */
    @Getter
    enum OperationButtonType {
        TYPE(1),
        ITEM(2),
        COUNT(3),
        NBT(4),
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

    public ItemSelectScreen(Args args) {
        super(TITLE.toTextComponent());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.currentItem = args.defaultItem();
        this.selectedItemId = ItemUtils.serializeItemStack(args.defaultItem());
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
        private ItemStack defaultItem = ItemStack.EMPTY;
        /**
         * 输入数据回调
         */
        private Consumer<ItemStack> onDataReceived1;
        /**
         * 输入数据回调
         */
        private Function<ItemStack, String> onDataReceived2;
        /**
         * 是否要显示该界面, 若为false则直接关闭当前界面并返回到调用者的 Screen
         */
        private Supplier<Boolean> shouldClose;

        public Args onDataReceived(Consumer<ItemStack> onDataReceived) {
            this.onDataReceived1 = onDataReceived;
            return this;
        }

        public Args onDataReceived(Function<ItemStack, String> onDataReceived) {
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
        this.updateSearchResults();
        this.updateLayout();
        // 创建文本输入框
        this.inputField = new BaniraTextField(
                this.font,
                bgX,
                bgY,
                LayoutConfig.ItemSelect.PANEL_WIDTH,
                LayoutConfig.ItemSelect.INPUT_FIELD_HEIGHT,
                Component.empty().toTextComponent()
        ).hint("搜索物品...");
        this.inputField.setValue(this.inputFieldText);
        // 添加文本变化监听
        this.inputField.setResponder(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        this.addButton(this.inputField);

        int buttonY = (int) (this.bgY + (LayoutConfig.ItemSelect.PANEL_HEIGHT_OFFSET +
                (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.MAX_LINES +
                LayoutConfig.ItemSelect.MARGIN));
        int buttonWidth = (int) (LayoutConfig.ItemSelect.BUTTON_WIDTH - LayoutConfig.ItemSelect.MARGIN * 2);

        // 创建提交按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + LayoutConfig.ItemSelect.BUTTON_WIDTH + LayoutConfig.ItemSelect.MARGIN),
                buttonY,
                buttonWidth,
                LayoutConfig.ItemSelect.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "submit").toTextComponent(),
                button -> {
                    if (this.currentItem == null || this.currentItem.isEmpty()) {
                        Minecraft.getInstance().setScreen(args.parentScreen());
                        return;
                    }
                    // 获取选择的数据，并执行回调
                    ItemStack reward = this.currentItem.copy();
                    if (args.onDataReceived1() != null) {
                        args.onDataReceived1().accept(reward);
                        Minecraft.getInstance().setScreen(args.parentScreen());
                    } else if (args.onDataReceived2() != null) {
                        String result = args.onDataReceived2().apply(reward);
                        if (StringUtils.isNullOrEmptyEx(result)) {
                            Minecraft.getInstance().setScreen(args.parentScreen());
                        }
                    }
                }
        ));

        // 创建取消按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + LayoutConfig.ItemSelect.MARGIN),
                buttonY,
                buttonWidth,
                LayoutConfig.ItemSelect.BUTTON_HEIGHT,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(args.parentScreen())
        ));
    }

    @Override
    public void renderEvent(MatrixStack matrixStack, float partialTicks) {
        super.keyManager.tick();
        // 绘制背景
        this.renderBackground(matrixStack);
        int panelWidth = LayoutConfig.ItemSelect.PANEL_WIDTH;
        int panelHeight = LayoutConfig.ItemSelect.PANEL_HEIGHT_OFFSET +
                (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.MAX_LINES) +
                LayoutConfig.ItemSelect.BUTTON_HEIGHT +
                (int) (LayoutConfig.ItemSelect.MARGIN * 2) + 5;
        AbstractGuiUtils.drawRoundedRect(
                matrixStack,
                (int) (this.bgX - LayoutConfig.ItemSelect.MARGIN),
                (int) (this.bgY - LayoutConfig.ItemSelect.MARGIN),
                (int) (panelWidth + LayoutConfig.ItemSelect.MARGIN * 2),
                panelHeight,
                LayoutConfig.Colors.BACKGROUND,
                2
        );
        int itemAreaWidth = (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.ITEMS_PER_LINE + LayoutConfig.ItemSelect.MARGIN);
        int itemAreaHeight = (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.MAX_LINES + LayoutConfig.ItemSelect.MARGIN);
        AbstractGuiUtils.drawRoundedRectOutLineRough(
                matrixStack,
                (int) (this.itemBgX - LayoutConfig.ItemSelect.MARGIN),
                (int) (this.itemBgY - LayoutConfig.ItemSelect.MARGIN),
                itemAreaWidth,
                itemAreaHeight,
                1,
                LayoutConfig.Colors.BORDER,
                1
        );
        super.renderButtons(matrixStack, partialTicks);
        // 保存输入框的文本, 防止窗口重绘时输入框内容丢失
        this.inputFieldText = this.inputField.getValue();

        this.renderButton(matrixStack);
        // 更新并渲染滚动条
        int totalRows = (int) Math.ceil((double) itemList.size() / LayoutConfig.ItemSelect.ITEMS_PER_LINE);
        super.scrollBar().updateScrollParams(totalRows, LayoutConfig.ItemSelect.MAX_LINES);
        super.scrollBar().render(matrixStack);
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
            // 物品按钮
            ITEM_BUTTONS.forEach(bt -> bt.pressed(bt.hovered()));

        }
        eventArgs.consumed(flag.get());
    }

    @Override
    protected void handlePopupOption(MouseReleasedHandleArgs eventArgs) {
    }

    @Override
    public void mouseReleasedEvent(MouseReleasedHandleArgs eventArgs) {
        super.keyManager.tick();
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
            // 物品按钮
            ITEM_BUTTONS.forEach(bt -> {
                if (bt.hovered() && bt.pressed()) {
                    this.handleItem(bt, flag);
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
        // 物品按钮
        ITEM_BUTTONS.forEach(bt -> bt.hovered(bt.isMouseOverEx(mouseHelper)));
    }

    @Override
    public void mouseScrolledEvent(MouseScoredHandleArgs eventArgs) {
    }

    @Override
    public void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
        if (super.keyManager.isEscapePressed() || (super.keyManager.isBackspacePressed() && !this.inputField.isFocused())) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            eventArgs.consumed(true);
        }
    }

    @Override
    public void keyReleasedEvent(KeyReleasedHandleArgs eventArgs) {
    }

    @Override
    protected void closeEvent() {

    }

    public void updateLayout() {
        calculatePositions();
        initOperationButtons();
        initScrollBar();
        initItemButtons();
    }

    /**
     * 计算位置
     */
    private void calculatePositions() {
        this.bgX = this.width / 2 - LayoutConfig.ItemSelect.BG_X_OFFSET;
        this.bgY = this.height / 2 - LayoutConfig.ItemSelect.BG_Y_OFFSET;
        this.itemBgX = this.bgX + LayoutConfig.ItemSelect.MARGIN;
        this.itemBgY = this.bgY + LayoutConfig.ItemSelect.PANEL_HEIGHT_OFFSET;
    }

    /**
     * 初始化操作按钮
     */
    private void initOperationButtons() {
        int buttonSize = AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.Button.OPERATION_BUTTON_SIZE_OFFSET;
        int buttonX = this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - (int) LayoutConfig.ItemSelect.MARGIN - LayoutConfig.Button.OPERATION_BUTTON_X_OFFSET;
        int baseY = this.bgY + (int) LayoutConfig.ItemSelect.MARGIN;
        int buttonSpacing = buttonSize + LayoutConfig.Button.OPERATION_BUTTON_SPACING;

        this.OP_BUTTONS.put(OperationButtonType.TYPE.getCode(), createTypeButton(buttonX, baseY, buttonSize));
        this.OP_BUTTONS.put(OperationButtonType.ITEM.getCode(), createItemButton(buttonX, baseY + buttonSpacing, buttonSize));
        this.OP_BUTTONS.put(OperationButtonType.COUNT.getCode(), createCountButton(buttonX, baseY + buttonSpacing * 2, buttonSize));
        this.OP_BUTTONS.put(OperationButtonType.NBT.getCode(), createNbtButton(buttonX, baseY + buttonSpacing * 3, buttonSize));
    }

    /**
     * 创建类型切换按钮
     */
    private OperationButton createTypeButton(int x, int y, int size) {
        return new OperationButton(OperationButtonType.TYPE.getCode(), context -> {
            OperationButtonRender.renderOperationButtonBackground(context);
            ItemStack itemStack = new ItemStack(this.inventoryMode ? Items.CHEST : Items.COMPASS);
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, itemStack);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS,
                    (this.inventoryMode ? "item_select_list_inventory_mode" : "item_select_list_all_mode"),
                    (this.inventoryMode ? ItemUtils.getAllPlayerItems().size() : ItemUtils.getAllItems().size()));
            OperationButtonRender.setOperationButtonTooltip(context, text);
        }).x(x).y(y).width(size).height(size);
    }

    /**
     * 创建物品显示按钮
     */
    private OperationButton createItemButton(int x, int y, int size) {
        return new OperationButton(OperationButtonType.ITEM.getCode(), context -> {
            OperationButtonRender.renderOperationButtonBackground(context);
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, this.currentItem);
            OperationButtonRender.setOperationButtonTooltip(context,
                    Text.fromTextComponent(this.currentItem.getHoverName().copy()));
        }).x(x).y(y).width(size).height(size);
    }

    /**
     * 创建数量编辑按钮
     */
    private OperationButton createCountButton(int x, int y, int size) {
        return new OperationButton(OperationButtonType.COUNT.getCode(), context -> {
            OperationButtonRender.renderOperationButtonBackground(context);
            ItemStack itemStack = new ItemStack(Items.WRITABLE_BOOK);
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, itemStack);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "set_count_s", this.currentItem.getCount());
            OperationButtonRender.setOperationButtonTooltip(context, text);
        }).x(x).y(y).width(size).height(size);
    }

    /**
     * 创建NBT编辑按钮
     */
    private OperationButton createNbtButton(int x, int y, int size) {
        return new OperationButton(OperationButtonType.NBT.getCode(), context -> {
            OperationButtonRender.renderOperationButtonBackground(context);
            ItemStack itemStack = new ItemStack(Items.NAME_TAG);
            OperationButtonRender.renderOperationButtonIcon(context, this.itemRenderer, itemStack);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "edit_nbt");
            OperationButtonRender.setOperationButtonTooltip(context, text);
        }).x(x).y(y).width(size).height(size);
    }

    /**
     * 初始化滚动条
     */
    private void initScrollBar() {
        double bgWidth = (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.ITEMS_PER_LINE;
        double bgHeight = (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN) * LayoutConfig.ItemSelect.MAX_LINES - LayoutConfig.ItemSelect.MARGIN;
        super.scrollBar().x(itemBgX + bgWidth + 2)
                .y(itemBgY - LayoutConfig.ItemSelect.MARGIN + 1)
                .width(5)
                .height((int) (bgHeight + LayoutConfig.ItemSelect.MARGIN + 1));
    }

    /**
     * 初始化物品按钮列表
     */
    private void initItemButtons() {
        this.ITEM_BUTTONS.clear();
        for (int i = 0; i < LayoutConfig.ItemSelect.MAX_LINES; i++) {
            for (int j = 0; j < LayoutConfig.ItemSelect.ITEMS_PER_LINE; j++) {
                ITEM_BUTTONS.add(new OperationButton(LayoutConfig.ItemSelect.ITEMS_PER_LINE * i + j, context -> {
                    int i1 = context.button.operation() / LayoutConfig.ItemSelect.ITEMS_PER_LINE;
                    int j1 = context.button.operation() % LayoutConfig.ItemSelect.ITEMS_PER_LINE;
                    int maxVisibleItems = LayoutConfig.ItemSelect.ITEMS_PER_LINE * LayoutConfig.ItemSelect.MAX_LINES;
                    int scrollRow = itemList.size() > maxVisibleItems ? super.scrollBar().scrollOffset() : 0;
                    int index = (scrollRow + i1) * LayoutConfig.ItemSelect.ITEMS_PER_LINE + j1;
                    if (index >= 0 && index < itemList.size()) {
                        ItemStack itemStack = itemList.get(index);
                        // 物品图标在弹出层中的 x 位置
                        double itemX = itemBgX + j1 * (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN);
                        // 物品图标在弹出层中的 y 位置
                        double itemY = itemBgY + i1 * (AbstractGuiUtils.ITEM_ICON_SIZE + LayoutConfig.ItemSelect.MARGIN);
                        // 绘制背景
                        int bgColor;
                        if (context.button.hovered() || ItemUtils.serializeItemStack(itemStack).equalsIgnoreCase(this.getSelectedItemId())) {
                            bgColor = LayoutConfig.Colors.SELECTED_BACKGROUND;
                        } else {
                            bgColor = LayoutConfig.Colors.BUTTON_BACKGROUND;
                        }
                        context.button.x(itemX - 1).y(itemY - 1).width(AbstractGuiUtils.ITEM_ICON_SIZE + 2).height(AbstractGuiUtils.ITEM_ICON_SIZE + 2)
                                .id(ItemUtils.serializeItemStack(itemStack));

                        AbstractGuiUtils.fill(context.matrixStack, (int) context.button.x(), (int) context.button.y(), (int) context.button.width(), (int) context.button.height(), bgColor);
                        AbstractGuiUtils.renderItem(this.itemRenderer, itemStack, (int) context.button.x() + 1, (int) context.button.y() + 1, false);

                        // 绘制物品详情悬浮窗
                        if (context.button.hovered()) {
                            boolean advanced = Screen.hasShiftDown();
                            List<Component> tooltipList = ItemUtils.getItemTooltip(itemStack, Minecraft.getInstance().player, advanced);

                            Component tooltipComponent = Component.empty();
                            for (int idx = 0; idx < tooltipList.size(); idx++) {
                                Component component = tooltipList.get(idx);
                                if (idx > 0) {
                                    tooltipComponent = tooltipComponent.append("\n");
                                }
                                tooltipComponent = tooltipComponent.append(component);
                            }
                            Text tooltipText = new Text(tooltipComponent);

                            FontRenderer font = itemStack.getItem().getFontRenderer(itemStack);
                            AbstractGuiUtils.drawPopupMessageWithSeason(
                                    FontDrawArgs.ofPopo(tooltipText.stack(context.matrixStack).font(font == null ? this.font : font))
                                            .x(context.mouseX())
                                            .y(context.mouseY())
                                            .padding(0)
                            );
                        }
                    } else {
                        context.button.x(0).y(0).width(0).height(0).id("");
                    }
                }));
            }
        }
    }

    /**
     * 更新搜索结果
     */
    private void updateSearchResults() {
        String s = this.inputField == null ? null : this.inputField.getValue();
        this.itemList.clear();
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (this.inventoryMode && player != null) {
            this.itemList.addAll(ItemUtils.searchPlayerItems(player, s));
        } else {
            this.itemList.addAll(ItemUtils.searchItems(s));
        }
        super.scrollBar().setScrollOffset(0);
    }

    /**
     * 绘制按钮
     */
    private void renderButton(MatrixStack matrixStack) {
        for (OperationButton button : OP_BUTTONS.values()) button.render(matrixStack, super.keyManager, mouseHelper);
        for (OperationButton button : ITEM_BUTTONS) button.render(matrixStack, super.keyManager, mouseHelper);
    }

    private void handleItem(OperationButton bt, AtomicBoolean flag) {
        if (mouseHelper.isPressedLeft()) {
            this.selectedItemId = bt.id();
            if (StringUtils.isNotNullOrEmpty(this.selectedItemId)) {
                ItemStack itemStack = ItemUtils.deserializeItemStack(selectedItemId);
                itemStack.setCount(1);
                this.currentItem = itemStack.copy();
                LOGGER.debug("Select item: {}", ItemUtils.getItemRegistryString(itemStack));
                flag.set(true);

            }
        }
    }

    private void handleOperation(OperationButton bt, AtomicBoolean flag, AtomicBoolean updateSearchResults) {
        if (bt.operation() == OperationButtonType.TYPE.getCode()) {
            this.inventoryMode = !this.inventoryMode;
            updateSearchResults.set(true);
            flag.set(true);
        }
        // 编辑数量
        else if (bt.operation() == OperationButtonType.COUNT.getCode()) {
            StringInputScreen.Args args = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_item_count").shadow(true))
                            .regex("\\d{0,4}")
                            .defaultValue(String.valueOf(this.currentItem.getCount()))
                            .validator((input) -> {
                                int count = NumberUtils.toInt(input.value());
                                if (count <= 0 || count > 64 * 9 * 5) {
                                    return Component.translatableClient(EnumI18nType.TIPS, "item_count_s_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        int count = NumberUtils.toInt(input.firstValue());
                        ItemStack itemStack = (this.currentItem);
                        itemStack.setCount(count);
                        this.currentItem = itemStack.copy();
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(args));
        }
        // 编辑NBT
        else if (bt.operation() == OperationButtonType.NBT.getCode()) {
            String itemNbtJsonString = ItemUtils.serializeItemStackTag((this.currentItem));
            StringInputScreen.Args args = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_item_nbt").shadow(true))
                            .defaultValue(itemNbtJsonString)
                            .validator((input) -> {
                                try {
                                    if (!ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(this.currentItem.getItem()) + input.value()).hasTag()) {
                                        throw new RuntimeException();
                                    }
                                } catch (Exception e) {
                                    return Component.translatableClient(EnumI18nType.TIPS, "item_nbt_s_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        try {
                            ItemStack itemStack = ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(this.currentItem.getItem()) + input.firstValue());
                            itemStack.setCount(this.currentItem.getCount());
                            this.currentItem = itemStack;
                            this.selectedItemId = ItemUtils.serializeItemStack(itemStack);
                        } catch (Exception e) {
                            input.runningResult(e);
                        }
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(args));
        }
    }
}
