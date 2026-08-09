package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.ArraySet;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 物品选择界面。支持搜索、背包/全部模式切换、数量与 NBT 编辑、网格滚动选择。
 */
public class ItemSelectScreen extends BaniraScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_H = 16;
    private static final int OP_BTN_SIZE = AbstractGuiUtils.ITEM_ICON_SIZE + 2;
    private static final int OP_BTN_GAP = 2;
    private static final int ITEM_BTN_SIZE = AbstractGuiUtils.ITEM_ICON_SIZE + 2;
    private static final int BTN_H = 20;
    /**
     * 面板内边距
     */
    private static final int PANEL_MARGIN = 6;
    /**
     * 滚动条宽度
     */
    private static final int SCROLL_W = 5;

    private final Args args;

    private static final Component TITLE = BaniraComponent.get().literal("ItemSelectScreen");

    private String inputFieldText = "";
    private final ArraySet<ItemStack> itemList = new ArraySet<>();
    @Getter
    private String selectedItemId;
    private ItemStack selectedItem;
    private boolean inventoryMode = false;
    private static final int ITEM_ROWS = 5;
    private static final int ITEM_COLS = 9;
    private static final int ITEM_SPACING = 1;
    private final List<BaseWidget> itemWidgets = new ArrayList<>();
    private final List<ItemCell> itemCells = new ArrayList<>();
    private BaseWidget selectedItemWidget;
    private InputWidget searchInputWidget;
    private ScrollbarWidget scrollbarWidget;
    private TooltipWidget inventoryModeTooltip;
    private TooltipWidget countTooltip;
    private ItemWidget typeButtonItemWidget;
    private ItemWidget itemButtonItemWidget;
    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;

    @Getter
    @Accessors(fluent = true)
    enum ButtonType {
        TYPE(1),
        ITEM(2),
        COUNT(3),
        NBT(4),
        ;

        final int code;

        ButtonType(int code) {
            this.code = code;
        }

        static ButtonType valueOf(int code) {
            return Arrays.stream(values()).filter(v -> v.code() == code).findFirst().orElse(null);
        }
    }

    public ItemSelectScreen(Args args) {
        super(TITLE.toVanilla());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.selectedItem = args.defaultItem() != null ? args.defaultItem() : new ItemStack(Items.AIR);
        this.selectedItemId = ItemUtils.serializeItemStack(this.selectedItem);
        BaniraScreen.inheritThemeAndSeason(this, args.parentScreen(), args.theme(), args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        private net.minecraft.client.gui.screens.Screen parentScreen;
        @Nullable
        private ItemStack defaultItem;
        private Consumer<ItemStack> onDataReceived1;
        private Function<ItemStack, String> onDataReceived2;
        private Supplier<Boolean> shouldClose;
        /** 提交成功后是否自动返回父界面，多步表单可交由回调接管导航。 */
        private boolean closeAfterSubmit = true;
        /**
         * 季节主题，null 时从父界面继承
         */
        @Nullable
        private EnumSeason season;
        /**
         * 自定义主题配置，null 时从父界面继承
         */
        @Nullable
        private BaniraColorConfig theme;

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
    protected void onInit() {
        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            BaniraClientRuntime.setScreen(args.parentScreen());

        updateSearchResults();
    }

    @Override
    protected void initWidgets() {
        boolean useSeasonTooltip = true;

        int w = width;
        int h = height;
        int listH = ITEM_ROWS * (ITEM_BTN_SIZE + ITEM_SPACING) - ITEM_SPACING;
        int listW = ITEM_COLS * (ITEM_BTN_SIZE + ITEM_SPACING) - ITEM_SPACING;
        int scrollGap = 2;

        int panelW = PANEL_MARGIN + listW + scrollGap + SCROLL_W + PANEL_MARGIN;
        int panelH = PANEL_MARGIN + INPUT_H + 4 + listH + 4 + BTN_H + PANEL_MARGIN;

        // 整体居中
        int totalW = OP_BTN_SIZE + OP_BTN_GAP + panelW;
        this.panelLeft = (w - totalW) / 2 + OP_BTN_SIZE + OP_BTN_GAP;
        this.panelTop = (h - panelH) / 2;
        this.panelW = panelW;
        this.panelH = panelH;

        int inputY = this.panelTop + PANEL_MARGIN;
        int inputX = this.panelLeft + PANEL_MARGIN;
        int inputW = this.panelW - PANEL_MARGIN * 2 - SCROLL_W - scrollGap;
        int listY = inputY + INPUT_H + 4;
        int listX = this.panelLeft + PANEL_MARGIN;
        int scrollX = listX + listW + scrollGap;
        int btnY = listY + listH + 4;
        int btnAreaW = this.panelW - PANEL_MARGIN * 2 - SCROLL_W - scrollGap;
        int btnW = (btnAreaW - PANEL_MARGIN) / 2;
        int cancelX = this.panelLeft + PANEL_MARGIN;
        int submitX = this.panelLeft + PANEL_MARGIN + btnW + PANEL_MARGIN;

        searchInputWidget = new InputWidget(this);
        searchInputWidget.id("search_input");
        searchInputWidget.bounds(new ScreenCoordinate(inputX, inputY, inputW, INPUT_H));
        searchInputWidget.value(this.inputFieldText);
        searchInputWidget.text(Text.transAuto(Banira.MOD_ID, "search_item"));
        searchInputWidget.onTextChanged(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        addWidget(searchInputWidget);

        int opBtnX = this.panelLeft - OP_BTN_SIZE - OP_BTN_GAP;
        int opBtnY = this.panelTop;
        String[] btnIds = {"type", "item", "count", "nbt"};
        for (int i = 0; i < btnIds.length; i++) {
            String btnId = btnIds[i];
            ButtonWidget btn = new ButtonWidget(this);
            btn.id(btnId);
            btn.bounds(new ScreenCoordinate(opBtnX, opBtnY + i * (OP_BTN_SIZE + OP_BTN_GAP), OP_BTN_SIZE, OP_BTN_SIZE));
            btn.text(Text.empty());
            btn.paddingLeft(0).paddingRight(0).paddingTop(0).paddingBottom(0);

            int opCode = parseOperationButtonType(btnId);
            ItemWidget iconWidget = new ItemWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
            iconWidget.showCountText(false);
            ScreenCoordinate tooltipBounds = new ScreenCoordinate(0, 0, OP_BTN_SIZE, OP_BTN_SIZE);
            if (opCode == ButtonType.TYPE.code()) {
                typeButtonItemWidget = iconWidget;
                iconWidget.itemStack(new ItemStack(this.inventoryMode ? Items.CHEST : Items.COMPASS));
                iconWidget.enableTooltip(false);
                inventoryModeTooltip = new TooltipWidget(this, tooltipBounds);
                inventoryModeTooltip.seasonTooltip(useSeasonTooltip);
                inventoryModeTooltip.text(Text.transAuto(Banira.MOD_ID,
                        (this.inventoryMode ? "item_display_mode_inventory" : "item_display_mode_all"),
                        (this.inventoryMode ? ItemUtils.getAllPlayerItems().size() : ItemUtils.getAllItems().size())));
                btn.addChild(inventoryModeTooltip);
            } else if (opCode == ButtonType.ITEM.code()) {
                itemButtonItemWidget = iconWidget;
                iconWidget.itemStack(this.selectedItem != null ? this.selectedItem.copy() : new ItemStack(Items.AIR));
            } else if (opCode == ButtonType.COUNT.code()) {
                iconWidget.itemStack(new ItemStack(Items.WRITABLE_BOOK));
                iconWidget.enableTooltip(false);
                countTooltip = new TooltipWidget(this, tooltipBounds);
                countTooltip.seasonTooltip(useSeasonTooltip);
                countTooltip.text(Text.transAuto(Banira.MOD_ID, "set_quantity", this.selectedItem != null ? this.selectedItem.getCount() : 0));
                btn.addChild(countTooltip);
            } else {
                iconWidget.itemStack(new ItemStack(Items.NAME_TAG));
                iconWidget.enableTooltip(false);
                TooltipWidget tip = new TooltipWidget(this, tooltipBounds);
                tip.seasonTooltip(useSeasonTooltip);
                tip.text(Text.transAuto(Banira.MOD_ID, "edit_nbt"));
                btn.addChild(tip);
            }
            btn.addChild(iconWidget);
            btn.onClick(b -> handleOperationInternal(opCode));
            addWidget(btn);
        }

        itemWidgets.clear();
        itemCells.clear();
        int expectedCount = ITEM_ROWS * ITEM_COLS;
        for (int i = 0; i < expectedCount; i++) {
            int row = i / ITEM_COLS;
            int col = i % ITEM_COLS;
            int btnX = listX + col * (ITEM_BTN_SIZE + ITEM_SPACING);
            int btnYPos = listY + row * (ITEM_BTN_SIZE + ITEM_SPACING);

            ButtonWidget btn = new ButtonWidget(this);
            btn.id("item_btn_" + i);
            btn.bounds(new ScreenCoordinate(btnX, btnYPos, ITEM_BTN_SIZE, ITEM_BTN_SIZE));
            btn.text(Text.empty());

            ItemWidget itemWidget = new ItemWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
            itemWidget.showCountText(false);
            itemWidget.visible(false);
            itemWidget.seasonTooltip(useSeasonTooltip);
            btn.addChild(itemWidget);

            btn.onClick(b -> {
                Object itemId = b.property("itemId");
                if (itemId instanceof String id) {
                    handleItem(id);
                    if (selectedItemWidget != null) selectedItemWidget.focused(false);
                    selectedItemWidget = btn;
                }
            });

            itemWidgets.add(btn);
            itemCells.add(new ItemCell(btn, itemWidget));
            addWidget(btn);
        }

        scrollbarWidget = new ScrollbarWidget(this);
        scrollbarWidget.id("scroll");
        scrollbarWidget.bounds(new ScreenCoordinate(scrollX, listY, SCROLL_W, listH));
        scrollbarWidget.orientation(EnumOrientation.VERTICAL);
        scrollbarWidget.minValue(0);
        scrollbarWidget.maxValue(0);
        scrollbarWidget.visibleSize(ITEM_ROWS);
        scrollbarWidget.scrollStep(1.0);
        scrollbarWidget.onValueChanged(v -> refreshButtons());
        scrollbarWidget.addScrollHoverArea(new ScreenCoordinate(listX, listY, listW, listH));
        addWidget(scrollbarWidget);

        ButtonWidget cancelButtonWidget = new ButtonWidget(this);
        cancelButtonWidget.id("cancel");
        cancelButtonWidget.bounds(new ScreenCoordinate(cancelX, btnY, btnW, BTN_H));
        cancelButtonWidget.text(Text.transAuto(Banira.MOD_ID, "cancel"));
        cancelButtonWidget.onClick(b -> BaniraClientRuntime.setScreen(args.parentScreen()));
        addWidget(cancelButtonWidget);

        ButtonWidget submitButtonWidget = new ButtonWidget(this);
        submitButtonWidget.id("submit");
        submitButtonWidget.bounds(new ScreenCoordinate(submitX, btnY, btnW, BTN_H));
        submitButtonWidget.text(Text.transAuto(Banira.MOD_ID, "submit"));
        submitButtonWidget.onClick(b -> {
            if (this.selectedItem == null || this.selectedItem.isEmpty()) {
                BaniraClientRuntime.setScreen(args.parentScreen());
            } else {
                ItemStack itemStack = this.selectedItem;
                if (args.onDataReceived1() != null) {
                    args.onDataReceived1().accept(itemStack);
                    LOGGER.debug("Item selected via callback1: {}", ItemUtils.getItemRegistryString(itemStack));
                    closeAfterSubmit(args, () -> BaniraClientRuntime.setScreen(args.parentScreen()));
                } else if (args.onDataReceived2() != null) {
                    String result = args.onDataReceived2().apply(itemStack);
                    if (StringUtils.isNullOrEmpty(result)) {
                        LOGGER.debug("Item selected via callback2: {}", ItemUtils.getItemRegistryString(itemStack));
                        closeAfterSubmit(args, () -> BaniraClientRuntime.setScreen(args.parentScreen()));
                    }
                }
            }
        });
        addWidget(submitButtonWidget);

        updateSearchResults();
    }

    static void closeAfterSubmit(Args args, Runnable closeAction) {
        if (args.closeAfterSubmit()) {
            closeAction.run();
        }
    }

    @Override
    public void onRender(PoseStack stack, float partialTicks) {
        ShapeDrawArgs panelBg = ShapeDrawArgs.rect(stack, panelLeft, panelTop, panelW, panelH, getEffectiveTheme().panelBg());
        panelBg.rect().radius(5).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panelBg);

        if (selectedItemWidget != null) selectedItemWidget.focused(true);
        super.renderWidgets(stack, partialTicks);

        if (searchInputWidget != null) {
            this.inputFieldText = searchInputWidget.value();
        }
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        AtomicBoolean flag = new AtomicBoolean(false);
        if (inputState.isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_4)) {
            BaniraClientRuntime.setScreen(args.parentScreen());
            flag.set(true);
        }
        eventArgs.consumed(flag.get());

        super.onMouseClicked(eventArgs);

        if (searchInputWidget != null && searchInputWidget.focused()) {
            BaseWidget inputBase = searchInputWidget;
            double mouseX = eventArgs.mouseX();
            double mouseY = eventArgs.mouseY();
            if (!inputBase.isMouseInside(mouseX, mouseY)) {
                this.unfocusWidget(searchInputWidget);
            }
        }
    }

    @Override
    public void onKeyPressed(KeyPressedHandleArgs eventArgs) {
        super.onKeyPressed(eventArgs);
        if (eventArgs.consumed()) {
            return;
        }

        if (super.inputState.isEscapePressed() ||
                (super.inputState.isBackspacePressed() &&
                        (searchInputWidget == null || !searchInputWidget.focused()))) {
            BaniraClientRuntime.setScreen(args.parentScreen());
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        updateSearchResults();
    }

    private void updateSearchResults() {
        String s = this.searchInputWidget != null ? this.searchInputWidget.value() : this.inputFieldText;
        this.itemList.clear();
        LocalPlayer player = BaniraClientRuntime.localPlayer();
        if (this.inventoryMode && player != null) {
            this.itemList.addAll(ItemUtils.searchPlayerItems(player, s));
        } else {
            this.itemList.addAll(ItemUtils.searchItems(s));
        }

        if (scrollbarWidget != null) {
            int totalRows = (int) Math.ceil((double) itemList.size() / ITEM_COLS);
            scrollbarWidget.visibleSize(ITEM_ROWS);
            scrollbarWidget.maxValue(Math.max(0, totalRows - ITEM_ROWS));
            if (scrollbarWidget.value() > scrollbarWidget.maxValue()) {
                scrollbarWidget.setValue(0);
            }
        }

        refreshButtons();
        LOGGER.debug("Search results updated: count={}, query={}", itemList.size(), s);
    }

    private void refreshButtons() {
        if (itemCells.isEmpty()) {
            return;
        }

        int scrollRow = 0;
        if (scrollbarWidget != null) {
            scrollRow = (int) scrollbarWidget.value();
        }

        boolean f = false;
        for (int i = 0; i < itemCells.size(); i++) {
            ItemCell cell = itemCells.get(i);
            BaseWidget buttonWidget = cell.button;
            int index = scrollRow * ITEM_COLS + i;
            ItemWidget iw = cell.item;
            if (index >= 0 && index < itemList.size()) {
                ItemStack itemStack = itemList.get(index);
                String itemId = ItemUtils.serializeItemStack(itemStack);
                iw.itemId(itemId);
                iw.visible(true);
                buttonWidget.property("itemId", itemId);
                buttonWidget.visible(true);
            } else {
                iw.itemId(null);
                iw.visible(false);
                buttonWidget.property("itemId", "");
                buttonWidget.visible(false);
            }
            if (this.selectedItemWidget != null) this.selectedItemWidget.focused(false);
            if (this.selectedItemId.equals(buttonWidget.property("itemId"))) {
                this.selectedItemWidget = buttonWidget;
                f = true;
            }
        }
        if (!f) {
            this.selectedItemWidget = null;
        }

        if (this.inventoryModeTooltip != null) {
            this.inventoryModeTooltip.text(Text.transAuto(Banira.MOD_ID,
                    (this.inventoryMode ? "item_display_mode_inventory" : "item_display_mode_all"),
                    (this.inventoryMode ? ItemUtils.getAllPlayerItems().size() : ItemUtils.getAllItems().size()))
            );
        }

        if (this.countTooltip != null) {
            this.countTooltip.text(Text.transAuto(Banira.MOD_ID, "set_quantity",
                    this.selectedItem != null ? this.selectedItem.getCount() : 0));
        }

        if (this.typeButtonItemWidget != null) {
            this.typeButtonItemWidget.itemStack(new ItemStack(this.inventoryMode ? Items.CHEST : Items.COMPASS));
        }
        if (this.itemButtonItemWidget != null && this.selectedItem != null) {
            this.itemButtonItemWidget.itemStack(this.selectedItem.copy());
        }
    }

    /**
     * 物品格子在 initWidgets 中固定，刷新时直接复用引用，避免滚动时遍历子树。
     */
    private static final class ItemCell {
        private final BaseWidget button;
        private final ItemWidget item;

        private ItemCell(BaseWidget button, ItemWidget item) {
            this.button = button;
            this.item = item;
        }
    }

    private void handleItem(String itemId) {
        if (handleItemInternal(itemId)) {
            refreshButtons();
        }
    }

    private boolean handleItemInternal(String itemId) {
        this.selectedItemId = itemId;
        if (StringUtils.isNotNullOrEmpty(this.selectedItemId)) {
            ItemStack itemStack = ItemUtils.deserializeItemStack(selectedItemId);
            itemStack.setCount(1);
            this.selectedItem = itemStack.copy();
            LOGGER.debug("Select item: {}", ItemUtils.getItemRegistryString(itemStack));
            return true;
        }
        return false;
    }

    private int parseOperationButtonType(String buttonId) {
        if ("type".equals(buttonId) || "1".equals(buttonId)) {
            return ButtonType.TYPE.code();
        } else if ("item".equals(buttonId) || "2".equals(buttonId)) {
            return ButtonType.ITEM.code();
        } else if ("count".equals(buttonId) || "3".equals(buttonId)) {
            return ButtonType.COUNT.code();
        } else if ("nbt".equals(buttonId) || "4".equals(buttonId)) {
            return ButtonType.NBT.code();
        }
        try {
            return Integer.parseInt(buttonId);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unknown operation button id: {}", buttonId);
            return 0;
        }
    }

    private void handleOperationInternal(int operationCode) {
        if (operationCode == ButtonType.TYPE.code()) {
            this.inventoryMode = !this.inventoryMode;
            this.updateSearchResults();
        } else if (operationCode == ButtonType.ITEM.code()) {
            InputFormScreen.Args args = new InputFormScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new InputFormScreen.Widget()
                            .title(Text.transAuto(Banira.MOD_ID, "enter_item_id"))
                            .defaultValue(ItemUtils.getItemRegistryString(this.selectedItem))
                            .validator((input) -> {
                                Item item = ItemUtils.getItemFromRegistry(input.value());
                                if (item == null) {
                                    return BaniraComponent.get().transClientAuto("enter_item_id_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        int count = this.selectedItem.getCount();
                        ItemStack itemStack = ItemUtils.deserializeItemStack(input.firstValue(), ItemUtils.serializeItemStackTag((this.selectedItem)));
                        itemStack.setCount(count);
                        this.selectedItem = itemStack.copy();
                        this.selectedItemId = ItemUtils.serializeItemStack(this.selectedItem);
                        refreshButtons();
                    });
            BaniraClientRuntime.setScreen(new InputFormScreen(args));
        } else if (operationCode == ButtonType.COUNT.code()) {
            InputFormScreen.Args args = new InputFormScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new InputFormScreen.Widget()
                            .title(Text.transAuto(Banira.MOD_ID, "enter_item_quantity"))
                            .regex("\\d{0,4}")
                            .defaultValue(String.valueOf(this.selectedItem.getCount()))
                            .validator((input) -> {
                                int count = NumberUtils.toInt(input.value());
                                if (count <= 0 || count > 64 * 9 * 5) {
                                    return BaniraComponent.get().transClientAuto("enter_item_quantity_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        int count = NumberUtils.toInt(input.firstValue());
                        ItemStack itemStack = this.selectedItem.copy();
                        itemStack.setCount(count);
                        this.selectedItem = itemStack;
                        this.selectedItemId = ItemUtils.serializeItemStack(this.selectedItem);
                        refreshButtons();
                    });
            BaniraClientRuntime.setScreen(new InputFormScreen(args));
        } else if (operationCode == ButtonType.NBT.code()) {
            String itemNbtJsonString = ItemUtils.serializeItemStackTag((this.selectedItem));
            InputFormScreen.Args args = new InputFormScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new InputFormScreen.Widget()
                            .title(Text.transAuto(Banira.MOD_ID, "enter_item_nbt"))
                            .defaultValue(itemNbtJsonString)
                            .validator((input) -> {
                                try {
                                    if (!ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(this.selectedItem.getItem()) + input.value()).hasTag()) {
                                        throw new RuntimeException();
                                    }
                                } catch (Exception e) {
                                    return BaniraComponent.get().transClientAuto("enter_item_nbt_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        try {
                            ItemStack itemStack = ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(this.selectedItem.getItem()) + input.firstValue());
                            itemStack.setCount(this.selectedItem.getCount());
                            this.selectedItem = itemStack;
                            this.selectedItemId = ItemUtils.serializeItemStack(itemStack);
                            refreshButtons();
                        } catch (Exception e) {
                            input.runningResult(e);
                        }
                    });
            BaniraClientRuntime.setScreen(new InputFormScreen(args));
        }
    }
}
