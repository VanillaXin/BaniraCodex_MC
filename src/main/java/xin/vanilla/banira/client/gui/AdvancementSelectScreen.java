package xin.vanilla.banira.client.gui;

import xin.vanilla.banira.BaniraComponent;
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
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.network.data.AdvancementData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 成就选择界面。支持搜索、显示模式切换（可显示/全部）、列表滚动选择。
 */
public class AdvancementSelectScreen extends BaniraScreen {

    // region 常量与字段
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_H = 16;
    private static final int OP_BTN_SIZE = AbstractGuiUtils.ITEM_ICON_SIZE + 2;
    private static final int OP_BTN_GAP = 2;
    private static final int ITEM_SPACING = 1;
    /**
     * 列表项高度
     */
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_LINES = 5;
    private static final int BTN_H = 20;
    private static final int PANEL_MARGIN = 6;
    private static final int SCROLL_W = 5;

    private final Args args;

    private static final Component TITLE = BaniraComponent.get().literal("AdvancementSelectScreen");

    private String inputFieldText = "";
    private final List<AdvancementData> advancementList = new ArrayList<>();
    @Nullable
    private InputWidget searchInputWidget;
    private final List<ButtonWidget> advancementButtonWidgets = new ArrayList<>();
    @Nullable
    private ScrollbarWidget scrollbarWidget;
    private ResourceLocation currentAdvancement;
    private boolean displayMode = true;

    private BaseWidget selectedAdvancementWidget;
    private ItemWidget typeButtonItemWidget;
    private ItemWidget advancementButtonItemWidget;
    private TooltipWidget typeTooltip;
    private TooltipWidget advancementTooltip;

    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;

    private boolean wasLoading = false;

    @Getter
    @Accessors(fluent = true)
    enum ButtonType {
        TYPE(1),
        ADVANCEMENT(2),
        ;

        final int code;

        ButtonType(int code) {
            this.code = code;
        }

        static ButtonType valueOf(int code) {
            return Arrays.stream(values()).filter(v -> v.code() == code).findFirst().orElse(null);
        }
    }

    // endregion 常量与字段

    public AdvancementSelectScreen(Args args) {
        super(TITLE.toVanilla());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.currentAdvancement = args.defaultAdvancement();
        BaniraScreen.inheritThemeAndSeason(this, args.parentScreen(), args.theme(), args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        private Screen parentScreen;
        private ResourceLocation defaultAdvancement = Identifier.id().empty();
        private Consumer<ResourceLocation> onDataReceived1;
        private Function<ResourceLocation, String> onDataReceived2;
        private Supplier<Boolean> shouldClose;
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
    protected void onInit() {
        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            Minecraft.getInstance().setScreen(args.parentScreen());

        AdvancementUtils.ensureAdvancementData();
        this.wasLoading = AdvancementUtils.isLoading();
        this.updateSearchResults();
    }

    @Override
    protected void initWidgets() {
        boolean useSeasonTooltip = true;

        int w = width;
        int h = height;
        int listH = MAX_LINES * ROW_HEIGHT;
        int listW = 200;
        int scrollGap = 2;

        int panelW = PANEL_MARGIN + listW + scrollGap + SCROLL_W + PANEL_MARGIN;
        int panelH = PANEL_MARGIN + INPUT_H + 4 + listH + 4 + BTN_H + PANEL_MARGIN;

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
        int listItemW = listW - 2;
        int scrollX = listX + listW + scrollGap;
        int btnY = listY + listH + 4;
        int btnAreaW = this.panelW - PANEL_MARGIN * 2 - SCROLL_W - scrollGap;
        int btnW = (btnAreaW - PANEL_MARGIN) / 2;
        int cancelX = this.panelLeft + PANEL_MARGIN;
        int submitX = this.panelLeft + PANEL_MARGIN + btnW + PANEL_MARGIN;


        // region 输入框
        searchInputWidget = new InputWidget(this);
        searchInputWidget.id("search_input");
        searchInputWidget.bounds(new ScreenCoordinate(inputX, inputY, inputW, INPUT_H));
        searchInputWidget.value(this.inputFieldText);
        searchInputWidget.text(Text.transAuto(BaniraCodex.MODID, "search_advancement"));
        searchInputWidget.onTextChanged(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        addWidget(searchInputWidget);
        // endregion 输入框

        // region 滚动条
        scrollbarWidget = new ScrollbarWidget(this);
        scrollbarWidget.id("scroll");
        scrollbarWidget.bounds(new ScreenCoordinate(scrollX, listY, SCROLL_W, listH));
        scrollbarWidget.orientation(EnumOrientation.VERTICAL);
        scrollbarWidget.minValue(0);
        scrollbarWidget.maxValue(0);
        scrollbarWidget.visibleSize(MAX_LINES);
        scrollbarWidget.scrollStep(1.0);
        scrollbarWidget.onValueChanged(v -> refreshAdvancementButtons());
        scrollbarWidget.addScrollHoverArea(new ScreenCoordinate(listX, listY, listW, listH));
        addWidget(scrollbarWidget);
        // endregion 滚动条

        // region 操作按钮
        int opBtnX = this.panelLeft - OP_BTN_SIZE - OP_BTN_GAP;
        int opBtnY = this.panelTop;
        String[] btnIds = {"type", "advancement"};
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
                iconWidget.itemStack(new net.minecraft.item.ItemStack(this.displayMode ? Items.MAP : Items.BOOK));
                iconWidget.enableTooltip(false);
                typeTooltip = new TooltipWidget(this, tooltipBounds);
                typeTooltip.seasonTooltip(useSeasonTooltip);
                typeTooltip.text(Text.transAuto(BaniraCodex.MODID,
                        (this.displayMode ? "advancement_display_mode_icon" : "advancement_display_mode_all"),
                        (this.displayMode ? AdvancementUtils.getDisplayableAdvancements().size() : AdvancementUtils.getAllAdvancements().size())));
                btn.addChild(typeTooltip);
            } else {
                advancementButtonItemWidget = iconWidget;
                AdvancementData sel = findAdvancementData(currentAdvancement);
                iconWidget.itemStack(sel != null ? sel.displayInfo().getIcon().copy() : new net.minecraft.item.ItemStack(Items.END_CRYSTAL));
                iconWidget.enableTooltip(false);
                advancementTooltip = new TooltipWidget(this, tooltipBounds);
                advancementTooltip.seasonTooltip(useSeasonTooltip);
                advancementTooltip.text(Text.empty());
                btn.addChild(advancementTooltip);
            }
            btn.addChild(iconWidget);
            btn.onClick(b -> handleOperationInternal(opCode));
            addWidget(btn);
        }
        // endregion 操作按钮

        // region 进度列表按钮
        advancementButtonWidgets.clear();
        int iconW = AbstractGuiUtils.ITEM_ICON_SIZE + 4;
        int textMaxW = listItemW - iconW - 4;
        for (int i = 0; i < MAX_LINES; i++) {
            ButtonWidget btn = new ButtonWidget(this);
            btn.id("advancement_btn_" + i);
            btn.bounds(new ScreenCoordinate(listX, listY + i * (ROW_HEIGHT + ITEM_SPACING), listItemW, ROW_HEIGHT - 2));
            btn.text(Text.empty());
            btn.paddingLeft(iconW).paddingRight(4);
            btn.visible(false);

            ItemWidget iconWidget = new ItemWidget(this, new ScreenCoordinate(2, 2, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
            iconWidget.showCountText(false);
            iconWidget.enableTooltip(false);
            iconWidget.seasonTooltip(useSeasonTooltip);
            iconWidget.visible(false);
            btn.addChild(iconWidget);

            LabelWidget labelWidget = new LabelWidget(this, new ScreenCoordinate(iconW, 2, textMaxW, ROW_HEIGHT - 4));
            labelWidget.text(Text.empty());
            labelWidget.textWrap(false).textEllipsisPosition(EnumEllipsisPosition.MIDDLE).textVerticalAlign(EnumAlignment.CENTER);
            labelWidget.visible(false);
            btn.addChild(labelWidget);

            TooltipWidget itemTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, listItemW, ROW_HEIGHT - 2));
            itemTooltip.text(Text.empty());
            itemTooltip.seasonTooltip(useSeasonTooltip);
            itemTooltip.visible(false);
            btn.addChild(itemTooltip);

            btn.onClick(b -> {
                Object advId = b.property("advancementId");
                if (advId instanceof String) {
                    handleAdvancement((String) advId);
                    if (selectedAdvancementWidget != null) selectedAdvancementWidget.focused(false);
                    selectedAdvancementWidget = btn;
                }
            });

            advancementButtonWidgets.add(btn);
            addWidget(btn);
        }
        // endregion 进度列表按钮

        // region 确认与取消按钮
        ButtonWidget cancelButtonWidget = new ButtonWidget(this);
        cancelButtonWidget.id("cancel");
        cancelButtonWidget.bounds(new ScreenCoordinate(cancelX, btnY, btnW, BTN_H));
        cancelButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "cancel"));
        cancelButtonWidget.onClick(b -> Minecraft.getInstance().setScreen(args.parentScreen()));
        addWidget(cancelButtonWidget);

        ButtonWidget submitButtonWidget = new ButtonWidget(this);
        submitButtonWidget.id("submit");
        submitButtonWidget.bounds(new ScreenCoordinate(submitX, btnY, btnW, BTN_H));
        submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "submit"));
        submitButtonWidget.onClick(b -> {
            if (this.currentAdvancement == null) {
                Minecraft.getInstance().setScreen(args.parentScreen());
            } else {
                ResourceLocation location = this.currentAdvancement;
                if (args.onDataReceived1() != null) {
                    args.onDataReceived1().accept(location);
                    LOGGER.debug("Advancement selected: {}", location);
                    Minecraft.getInstance().setScreen(args.parentScreen());
                } else if (args.onDataReceived2() != null) {
                    String result = args.onDataReceived2().apply(location);
                    if (StringUtils.isNullOrEmpty(result)) {
                        LOGGER.debug("Advancement selected: {}", location);
                        Minecraft.getInstance().setScreen(args.parentScreen());
                    } else {
                        LOGGER.debug("Advancement validation failed: {}", result);
                    }
                }
            }
        });
        addWidget(submitButtonWidget);
        // endregion 确认与取消按钮

        updateSearchResults();
    }

    @Nullable
    private AdvancementData findAdvancementData(ResourceLocation id) {
        if (id == null) return null;
        String idStr = id.toString();
        if (StringUtils.isNullOrEmpty(idStr) || ":".equals(idStr)) return null;
        try {
            for (AdvancementData d : advancementList) {
                if (idStr.equals(d.id().toString())) return d;
            }
            for (AdvancementData d : AdvancementUtils.getAllAdvancements()) {
                if (idStr.equals(d.id().toString())) return d;
            }
        } catch (Exception e) {
            LOGGER.debug("findAdvancementData failed for id={}", idStr, e);
        }
        return null;
    }

    @Override
    public void onRender(MatrixStack stack, float partialTicks) {
        ShapeDrawArgs panelBg = ShapeDrawArgs.rect(stack, panelLeft, panelTop, panelW, panelH, getEffectiveTheme().panelBg());
        panelBg.rect().radius(5).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panelBg);

        if (selectedAdvancementWidget != null) selectedAdvancementWidget.focused(true);
        super.renderWidgets(stack, partialTicks);

        if (searchInputWidget != null) {
            this.inputFieldText = searchInputWidget.value();
        }

        boolean isLoading = AdvancementUtils.isLoading();
        if (this.wasLoading && !isLoading) {
            this.updateSearchResults();
            LOGGER.debug("Advancement data loaded, updating search results");
        }
        this.wasLoading = isLoading;

        refreshAdvancementButtons();
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        AtomicBoolean flag = new AtomicBoolean(false);
        if (inputState.isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_4)) {
            Minecraft.getInstance().setScreen(args.parentScreen());
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
            Minecraft.getInstance().setScreen(args.parentScreen());
            eventArgs.consumed(true);
        } else if (super.inputState.isEnterPressed() && searchInputWidget != null && searchInputWidget.focused()) {
            this.updateSearchResults();
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        updateSearchResults();
    }

    private void refreshAdvancementButtons() {
        if (advancementButtonWidgets.isEmpty()) return;

        int scrollOffset = scrollbarWidget != null ? (int) scrollbarWidget.value() : 0;

        boolean found = false;
        for (int i = 0; i < advancementButtonWidgets.size(); i++) {
            ButtonWidget buttonWidget = advancementButtonWidgets.get(i);
            int index = scrollOffset + i;
            ItemWidget iw = buttonWidget.findChildByType(ItemWidget.class);
            LabelWidget lw = buttonWidget.findChildByType(LabelWidget.class);
            TooltipWidget tw = buttonWidget.findChildByType(TooltipWidget.class);
            if (iw == null || lw == null) continue;

            if (index >= 0 && index < advancementList.size()) {
                AdvancementData advancementData = advancementList.get(index);
                String advancementId = advancementData.id().toString();
                String displayName = AdvancementUtils.getDisplayName(advancementData);
                String description = AdvancementUtils.getDescription(advancementData);
                iw.itemStack(advancementData.displayInfo().getIcon().copy());
                iw.visible(true);
                lw.text(Text.literal(displayName).color(Color.argb(getEffectiveTheme().listItemText())));
                lw.visible(true);
                if (tw != null) {
                    String tip = buildDeduplicatedTooltip(displayName, description, advancementId);
                    tw.text(Text.literal(tip));
                    tw.visible(true);
                }
                buttonWidget.property("advancementId", advancementId);
                buttonWidget.visible(true);
                buttonWidget.enabled(true);
            } else {
                iw.itemStack(new ItemStack(Items.AIR));
                iw.visible(false);
                lw.text(Text.empty());
                lw.visible(false);
                if (tw != null) {
                    tw.text(Text.empty());
                    tw.visible(false);
                }
                buttonWidget.property("advancementId", "");
                buttonWidget.visible(false);
                buttonWidget.enabled(false);
            }

            if (selectedAdvancementWidget != null) selectedAdvancementWidget.focused(false);
            if (currentAdvancement != null && currentAdvancement.toString().equals(buttonWidget.property("advancementId"))) {
                selectedAdvancementWidget = buttonWidget;
                found = true;
            }
        }
        if (!found) selectedAdvancementWidget = null;

        if (typeTooltip != null) {
            typeTooltip.text(Text.transAuto(BaniraCodex.MODID,
                    (this.displayMode ? "advancement_display_mode_icon" : "advancement_display_mode_all"),
                    (this.displayMode ? AdvancementUtils.getDisplayableAdvancements().size() : AdvancementUtils.getAllAdvancements().size())));
        }
        if (typeButtonItemWidget != null) {
            typeButtonItemWidget.itemStack(new ItemStack(this.displayMode ? Items.MAP : Items.BOOK));
        }
        if (advancementButtonItemWidget != null) {
            AdvancementData sel = findAdvancementData(currentAdvancement);
            boolean hasSelection = sel != null;
            advancementButtonItemWidget.visible(hasSelection);
            advancementButtonItemWidget.itemStack(hasSelection ? sel.displayInfo().getIcon().copy() : new ItemStack(Items.AIR));
        }
        if (advancementTooltip != null) {
            AdvancementData sel = findAdvancementData(currentAdvancement);
            if (sel != null) {
                String displayName = AdvancementUtils.getDisplayName(sel);
                String description = AdvancementUtils.getDescription(sel);
                String advancementId = sel.id().toString();
                advancementTooltip.text(Text.literal(buildDeduplicatedTooltip(displayName, description, advancementId)));
            } else {
                advancementTooltip.text(Text.transAuto(BaniraCodex.MODID, "advancement_select_advancement"));
            }
        }
    }

    private void updateSearchResults() {
        String s = this.searchInputWidget != null ? this.searchInputWidget.value() : this.inputFieldText;
        this.advancementList.clear();

        AdvancementUtils.ensureAdvancementData();

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

        if (scrollbarWidget != null) {
            scrollbarWidget.maxValue(Math.max(0, advancementList.size() - MAX_LINES));
            if (scrollbarWidget.value() > scrollbarWidget.maxValue()) {
                scrollbarWidget.setValue(0);
            }
        }

        refreshAdvancementButtons();
        LOGGER.debug("Search results updated: count={}, query={}", advancementList.size(), s);
    }

    private void handleAdvancement(String advancementId) {
        if (StringUtils.isNotNullOrEmpty(advancementId)) {
            try {
                ResourceLocation location = Identifier.id().parse(advancementId);
                this.currentAdvancement = location;
                LOGGER.debug("Select advancement: {}", location);
                refreshAdvancementButtons();
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Invalid advancement id format: {}", advancementId);
            } catch (Exception e) {
                LOGGER.debug("Unexpected error handling advancement click", e);
            }
        }
    }

    private static String buildDeduplicatedTooltip(String displayName, String description, String advancementId) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotNullOrEmpty(displayName)) parts.add(displayName);
        if (StringUtils.isNotNullOrEmpty(description) && !description.equals(displayName)) parts.add(description);
        if (StringUtils.isNotNullOrEmpty(advancementId)
                && !advancementId.equals(displayName)
                && !advancementId.equals(description)) {
            parts.add(advancementId);
        }
        return new LinkedHashSet<>(parts).stream().collect(Collectors.joining("\n"));
    }

    private int parseOperationButtonType(String buttonId) {
        if ("type".equals(buttonId) || "1".equals(buttonId)) return ButtonType.TYPE.code();
        if ("advancement".equals(buttonId) || "2".equals(buttonId)) return ButtonType.ADVANCEMENT.code();
        try {
            return Integer.parseInt(buttonId);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unknown operation button id: {}", buttonId);
            return 0;
        }
    }

    private void handleOperationInternal(int operationCode) {
        if (operationCode == ButtonType.TYPE.code()) {
            this.displayMode = !this.displayMode;
            this.updateSearchResults();
        } else if (operationCode == ButtonType.ADVANCEMENT.code()) {
            String effectString = this.currentAdvancement != null ? this.currentAdvancement.toString() : "";
            StringInputScreen.Args inputArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.transAuto(BaniraCodex.MODID, "enter_advancement_id"))
                            .hint(Text.transAuto(BaniraCodex.MODID, "enter_something"))
                            .defaultValue(effectString)
                            .validator((input) -> {
                                try {
                                    Identifier.id().parse(input.value());
                                } catch (IllegalArgumentException e) {
                                    return BaniraComponent.get().transClientAuto("enter_advancement_id_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        String id = input.firstValue();
                        try {
                            this.currentAdvancement = Identifier.id().parse(id);
                        } catch (IllegalArgumentException e) {
                            LOGGER.debug("Invalid advancement id format: {}", id);
                        } catch (Exception e) {
                            LOGGER.debug("Unexpected error parsing advancement id: {}", id, e);
                        }
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(inputArgs));
        }
    }
}
