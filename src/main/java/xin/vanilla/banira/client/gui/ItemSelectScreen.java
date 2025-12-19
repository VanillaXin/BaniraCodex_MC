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
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.KeyEventManager;
import xin.vanilla.banira.common.data.ArraySet;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ItemSelectScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private final KeyEventManager keyManager = new KeyEventManager();

    private final Args args;

    private final static Component TITLE = Component.literal("ItemSelectScreen");

    // 每行显示数量
    private final int itemPerLine = 9;
    // 每页显示行数
    private final int maxLine = 5;

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
    private final double margin = 3;
    private double itemBgX = this.bgX + margin;
    private double itemBgY = this.bgY + 20;

    /**
     * 滚动条组件
     */
    private final ScrollBar scrollBar = new ScrollBar();

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
    protected void init() {
        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            Minecraft.getInstance().setScreen(args.parentScreen());
        this.updateSearchResults();
        this.updateLayout();
        // 创建文本输入框
        this.inputField = new BaniraTextField(this.font, bgX, bgY, 180, 12, Component.empty().toTextComponent())
                .hint("搜索物品...");
        this.inputField.setValue(this.inputFieldText);
        // 添加文本变化监听
        this.inputField.setResponder(text -> {
            this.inputFieldText = text;
            this.updateSearchResults();
        });
        this.addButton(this.inputField);

        int buttonY = (int) (this.bgY + (20 + (AbstractGuiUtils.ITEM_ICON_SIZE + 3) * 5 + margin));
        int buttonWidth = (int) (90 - this.margin * 2);

        // 创建提交按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + 90 + this.margin),
                buttonY,
                buttonWidth,
                20,
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
                (int) (this.bgX + this.margin),
                buttonY,
                buttonWidth,
                20,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(args.parentScreen())
        ));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        keyManager.refresh(mouseX, mouseY);
        // 绘制背景
        this.renderBackground(matrixStack);
        AbstractGuiUtils.fill(matrixStack, (int) (this.bgX - this.margin), (int) (this.bgY - this.margin), (int) (180 + this.margin * 2), (int) (20 + (AbstractGuiUtils.ITEM_ICON_SIZE + 3) * 5 + 20 + margin * 2 + 5), 0xCCC6C6C6, 2);
        AbstractGuiUtils.fillOutLine(matrixStack, (int) (this.itemBgX - this.margin), (int) (this.itemBgY - this.margin), (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + this.margin) * this.itemPerLine + this.margin), (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + this.margin) * this.maxLine + this.margin), 1, 0xFF000000, 1);
        super.render(matrixStack, mouseX, mouseY, delta);
        // 保存输入框的文本, 防止窗口重绘时输入框内容丢失
        this.inputFieldText = this.inputField.getValue();

        this.renderButton(matrixStack);
        // 更新并渲染滚动条
        int totalRows = (int) Math.ceil((double) itemList.size() / itemPerLine);
        scrollBar.updateScrollParams(totalRows, maxLine);
        scrollBar.render(matrixStack);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        keyManager.mouseScrolled(delta, mouseX, mouseY);
        if (scrollBar.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        keyManager.mouseClicked(button, mouseX, mouseY);
        AtomicBoolean flag = new AtomicBoolean(false);
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_4) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            flag.set(true);
        } else if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT || button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            // 检查滚动条
            if (scrollBar.mouseClicked(mouseX, mouseY, button)) {
                flag.set(true);
            } else {
                OP_BUTTONS.forEach((key, value) -> {
                    if (value.isHovered()) {
                        value.setPressed(true);
                    }
                });
                // 物品按钮
                ITEM_BUTTONS.forEach(bt -> bt.setPressed(bt.isHovered()));
            }
        }
        return flag.get() ? flag.get() : super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        keyManager.refresh(mouseX, mouseY);
        AtomicBoolean flag = new AtomicBoolean(false);
        AtomicBoolean updateSearchResults = new AtomicBoolean(false);
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT || button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            // 检查滚动条
            if (scrollBar.mouseReleased(mouseX, mouseY, button)) {
                flag.set(true);
            } else {
                // 控制按钮
                OP_BUTTONS.forEach((key, value) -> {
                    if (value.isHovered() && value.isPressed()) {
                        this.handleOperation(value, button, flag, updateSearchResults);
                    }
                    value.setPressed(false);
                });
                // 物品按钮
                ITEM_BUTTONS.forEach(bt -> {
                    if (bt.isHovered() && bt.isPressed()) {
                        this.handleItem(bt, button, flag);
                    }
                    bt.setPressed(false);
                });
            }
            if (updateSearchResults.get()) {
                this.updateSearchResults();
            }
        }
        keyManager.mouseReleased(button, mouseX, mouseY);
        return flag.get() ? flag.get() : super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        keyManager.mouseMoved(mouseX, mouseY);
        // 更新滚动条
        scrollBar.mouseMoved(mouseX, mouseY);
        // 控制按钮
        OP_BUTTONS.forEach((key, value) -> value.setHovered(value.isMouseOverEx(mouseX, mouseY)));
        // 物品按钮
        ITEM_BUTTONS.forEach(bt -> bt.setHovered(bt.isMouseOverEx(mouseX, mouseY)));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        keyManager.keyPressed(keyCode);
        if (keyCode == GLFWKey.GLFW_KEY_ESCAPE || (keyCode == GLFWKey.GLFW_KEY_BACKSPACE && !this.inputField.isFocused())) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        keyManager.keyReleased(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateLayout() {
        this.bgX = this.width / 2 - 92;
        this.bgY = this.height / 2 - 65;
        this.itemBgX = this.bgX + margin;
        this.itemBgY = this.bgY + 20;

        // 初始化操作按钮
        this.OP_BUTTONS.put(OperationButtonType.TYPE.getCode(), new OperationButton(OperationButtonType.TYPE.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            ItemStack itemStack = new ItemStack(this.inventoryMode ? Items.CHEST : Items.COMPASS);
            this.itemRenderer.renderGuiItem(itemStack, (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, (this.inventoryMode ? "item_select_list_inventory_mode" : "item_select_list_all_mode"), (this.inventoryMode ? ItemUtils.getAllPlayerItems().size() : ItemUtils.getAllItems().size()));
            context.button.setTooltip(text);
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));
        this.OP_BUTTONS.put(OperationButtonType.ITEM.getCode(), new OperationButton(OperationButtonType.ITEM.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            this.itemRenderer.renderGuiItem((this.currentItem), (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            context.button.setTooltip(Text.fromTextComponent(((ItemStack) (this.currentItem)).getHoverName().copy()));
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin + AbstractGuiUtils.ITEM_ICON_SIZE + 4 + 1).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));
        this.OP_BUTTONS.put(OperationButtonType.COUNT.getCode(), new OperationButton(OperationButtonType.COUNT.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            ItemStack itemStack = new ItemStack(Items.WRITABLE_BOOK);
            this.itemRenderer.renderGuiItem(itemStack, (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "set_count_s", this.currentItem.getCount());
            context.button.setTooltip(text);
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin + (AbstractGuiUtils.ITEM_ICON_SIZE + 4 + 1) * 2).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));
        this.OP_BUTTONS.put(OperationButtonType.NBT.getCode(), new OperationButton(OperationButtonType.NBT.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            ItemStack itemStack = new ItemStack(Items.NAME_TAG);
            this.itemRenderer.renderGuiItem(itemStack, (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            Text text = Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "edit_nbt");
            context.button.setTooltip(text);
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin + (AbstractGuiUtils.ITEM_ICON_SIZE + 4 + 1) * 3).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));

        // 初始化滚动条位置
        double bgWidth = (AbstractGuiUtils.ITEM_ICON_SIZE + margin) * itemPerLine;
        double bgHeight = (AbstractGuiUtils.ITEM_ICON_SIZE + margin) * maxLine - margin;
        scrollBar.x(itemBgX + bgWidth + 2)
                .y(itemBgY - margin + 1)
                .width(5)
                .height((int) (bgHeight + margin + 1));

        // 物品列表
        this.ITEM_BUTTONS.clear();
        for (int i = 0; i < maxLine; i++) {
            for (int j = 0; j < itemPerLine; j++) {
                ITEM_BUTTONS.add(new OperationButton(itemPerLine * i + j, context -> {
                    int i1 = context.button.getOperation() / itemPerLine;
                    int j1 = context.button.getOperation() % itemPerLine;
                    int scrollRow = itemList.size() > itemPerLine * maxLine ? scrollBar.scrollOffset() : 0;
                    int index = (scrollRow + i1) * itemPerLine + j1;
                    if (index >= 0 && index < itemList.size()) {
                        ItemStack itemStack = itemList.get(index);
                        // 物品图标在弹出层中的 x 位置
                        double itemX = itemBgX + j1 * (AbstractGuiUtils.ITEM_ICON_SIZE + margin);
                        // 物品图标在弹出层中的 y 位置
                        double itemY = itemBgY + i1 * (AbstractGuiUtils.ITEM_ICON_SIZE + margin);
                        // 绘制背景
                        int bgColor;
                        if (context.button.isHovered() || ItemUtils.serializeItemStack(itemStack).equalsIgnoreCase(this.getSelectedItemId())) {
                            bgColor = 0xEE7CAB7C;
                        } else {
                            bgColor = 0xEE707070;
                        }
                        context.button.setX(itemX - 1).setY(itemY - 1).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 2).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 2)
                                .setId(ItemUtils.serializeItemStack(itemStack));

                        AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), bgColor);
                        AbstractGuiUtils.renderItem(this.itemRenderer, itemStack, (int) context.button.getX() + 1, (int) context.button.getY() + 1, false);

                        // 绘制物品详情悬浮窗
                        if (context.button.isHovered()) {
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
                                            .x(context.keyManager.getMouseX())
                                            .y(context.keyManager.getMouseY())
                                            .padding(0)
                            );
                        }
                    } else {
                        context.button.setX(0).setY(0).setWidth(0).setHeight(0).setId("");
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
        scrollBar.setScrollOffset(0);
    }

    /**
     * 绘制按钮
     */
    private void renderButton(MatrixStack matrixStack) {
        for (OperationButton button : OP_BUTTONS.values()) button.render(matrixStack, keyManager);
        for (OperationButton button : ITEM_BUTTONS) button.render(matrixStack, keyManager);
    }

    private void handleItem(OperationButton bt, int button, AtomicBoolean flag) {
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            this.selectedItemId = bt.getId();
            if (StringUtils.isNotNullOrEmpty(this.selectedItemId)) {
                ItemStack itemStack = ItemUtils.deserializeItemStack(selectedItemId);
                itemStack.setCount(1);
                this.currentItem = itemStack.copy();
                LOGGER.debug("Select item: {}", ItemUtils.getItemRegistryString(itemStack));
                flag.set(true);

            }
        }
    }

    private void handleOperation(OperationButton bt, int button, AtomicBoolean flag, AtomicBoolean updateSearchResults) {
        if (bt.getOperation() == OperationButtonType.TYPE.getCode()) {
            this.inventoryMode = !this.inventoryMode;
            updateSearchResults.set(true);
            flag.set(true);
        }
        // 编辑数量
        else if (bt.getOperation() == OperationButtonType.COUNT.getCode()) {
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
        else if (bt.getOperation() == OperationButtonType.NBT.getCode()) {
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
