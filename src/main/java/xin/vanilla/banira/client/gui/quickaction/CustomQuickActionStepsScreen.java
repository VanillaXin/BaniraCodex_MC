package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
import xin.vanilla.banira.api.quickaction.CustomQuickActionMenuItem;
import xin.vanilla.banira.api.quickaction.CustomQuickActionStep;
import xin.vanilla.banira.api.quickaction.QuickActionExecutionMode;
import xin.vanilla.banira.api.quickaction.QuickActionIconType;
import xin.vanilla.banira.api.quickaction.QuickActionStepCondition;
import xin.vanilla.banira.api.quickaction.QuickActionStepType;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.EffectSelectScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.ItemSelectScreen;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 编辑主入口或右键菜单项的动作步骤。 */
final class CustomQuickActionStepsScreen extends BaniraScreen {
    private static final int MARGIN = 16;
    private static final int PAD = 8;
    private static final int TOP_H = 22;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;
    private static final int BOTTOM_H = 20;
    private static final int SCROLL_W = 5;

    private final CustomQuickActionConfigScreen rootParent;
    private final CustomQuickActionStepsScreen sequenceParent;
    private final int originalIndex;
    private final int menuItemIndex;
    private CustomQuickActionDefinition definition;
    private CustomQuickActionMenuItem menuItem;
    private final List<ButtonWidget> rows = new ArrayList<>();
    private final List<ButtonWidget> deletes = new ArrayList<>();
    private final List<TooltipWidget> rowTooltips = new ArrayList<>();
    private ButtonWidget iconButton;
    private ScrollbarWidget scrollbar;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int visibleRows;
    private int scrollIndex;

    CustomQuickActionStepsScreen(CustomQuickActionConfigScreen parent,
                                 CustomQuickActionDefinition definition, int originalIndex) {
        super(BaniraComponent.get().transClientAuto("custom_quick_action_step_title").toVanilla());
        this.rootParent = parent;
        this.sequenceParent = null;
        this.definition = CustomQuickActionEditor.copy(definition);
        this.menuItem = null;
        this.originalIndex = originalIndex;
        this.menuItemIndex = -1;
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
    }

    private CustomQuickActionStepsScreen(CustomQuickActionStepsScreen parent,
                                         CustomQuickActionMenuItem menuItem, int menuItemIndex) {
        super(BaniraComponent.get().transClientAuto("custom_quick_action_menu_step_title").toVanilla());
        this.rootParent = null;
        this.sequenceParent = parent;
        this.definition = null;
        this.menuItem = CustomQuickActionEditor.copy(menuItem);
        this.originalIndex = -1;
        this.menuItemIndex = menuItemIndex;
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
    }

    void openDefinitionEditor() {
        if (isMenuItem()) {
            CustomQuickActionEditor.openMenuItem(this, menuItem, value -> menuItem = value);
        } else {
            CustomQuickActionEditor.openDefinition(this, definition, value -> definition = value);
        }
    }

    @Override
    protected void initWidgets() {
        panelX = MARGIN;
        panelY = MARGIN;
        panelW = width - MARGIN * 2;
        panelH = height - MARGIN * 2;
        listX = panelX + PAD;
        listY = panelY + PAD + TOP_H + 4;
        listW = panelW - PAD * 2 - SCROLL_W - 3;
        int bottomRows = isMenuItem() ? 1 : 2;
        int listH = panelH - PAD * 3 - TOP_H - (BOTTOM_H * bottomRows + (bottomRows - 1) * 5);
        visibleRows = Math.max(1, listH / (ROW_H + ROW_GAP));

        int topButtonW = (listW - 6) / 3;
        addWidget(button("details", listX, panelY + PAD, topButtonW, TOP_H,
                isMenuItem() ? "custom_quick_action_menu_details" : "custom_quick_action_edit_details",
                this::openDefinitionEditor));
        if (isMenuItem()) {
            addWidget(button("add_command", listX + topButtonW + 3, panelY + PAD, topButtonW, TOP_H,
                    "custom_quick_action_add_command", () -> addStep(QuickActionStepType.COMMAND)));
            addWidget(button("add_screen", listX + (topButtonW + 3) * 2, panelY + PAD, topButtonW, TOP_H,
                    "custom_quick_action_add_screen", () -> addStep(QuickActionStepType.SCREEN)));
        } else {
            iconButton = button("icon", listX + topButtonW + 3, panelY + PAD, topButtonW, TOP_H,
                    "custom_quick_action_select_icon", this::selectIcon);
            iconButton.leadingIconRenderer((stack, x, y, size) ->
                    CustomQuickActionManager.resolveIcon(definition)
                            .render(stack, x, y, size));
            addWidget(iconButton);
            addWidget(button("add_command", listX + (topButtonW + 3) * 2, panelY + PAD,
                    topButtonW, TOP_H, "custom_quick_action_add_command",
                    () -> addStep(QuickActionStepType.COMMAND)));
        }

        rows.clear();
        deletes.clear();
        rowTooltips.clear();
        for (int i = 0; i < rowCount(); i++) {
            final int index = i;
            ButtonWidget row = new ButtonWidget(this);
            row.id("entry_" + i);
            row.onClick(button -> editRow(index));
            TooltipWidget tooltip = new TooltipWidget(this);
            tooltip.id("entry_tooltip_" + i);
            tooltip.popupAtScreenCoords(true);
            row.addChild(tooltip);
            rows.add(row);
            rowTooltips.add(tooltip);
            addWidget(row);
            ButtonWidget delete = QuickActionWidgets.deleteButton(this, "delete_" + i, ROW_H,
                    button -> deleteRow(index));
            deletes.add(delete);
            addWidget(delete);
        }

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0)
                .maxValue(Math.max(0, rowCount() - visibleRows)).visibleSize(visibleRows)
                .scrollStep(1).onValueChanged(value -> {
                    scrollIndex = (int) Math.round(value);
                    updateRows();
                });
        addWidget(scrollbar);

        int gap = 5;
        int saveY = panelY + panelH - PAD - BOTTOM_H;
        int half = (listW - gap) / 2;
        if (!isMenuItem()) {
            int addY = saveY - BOTTOM_H - gap;
            addWidget(button("add_screen", listX, addY, half, BOTTOM_H,
                    "custom_quick_action_add_screen", () -> addStep(QuickActionStepType.SCREEN)));
            addWidget(button("add_menu", listX + half + gap, addY, half, BOTTOM_H,
                    "custom_quick_action_add_menu", this::addMenuItem));
        }
        addWidget(button("save", listX, saveY, half, BOTTOM_H, "save", this::save));
        addWidget(button("cancel", listX + half + gap, saveY, half, BOTTOM_H, "cancel", this::onClose));
        updateRows();
    }

    private boolean isMenuItem() {
        return menuItem != null;
    }

    private List<CustomQuickActionStep> currentSteps() {
        return isMenuItem() ? menuItem.getSteps() : definition.getSteps();
    }

    private QuickActionExecutionMode currentMode() {
        return isMenuItem() ? menuItem.getExecutionMode() : definition.getExecutionMode();
    }

    private void forceChainedForConditionalStep(CustomQuickActionStep step) {
        if (step.getCondition() == QuickActionStepCondition.ALWAYS) return;
        if (isMenuItem()) menuItem.setExecutionMode(QuickActionExecutionMode.CHAINED);
        else definition.setExecutionMode(QuickActionExecutionMode.CHAINED);
    }

    private int rowCount() {
        return currentSteps().size() + (isMenuItem() ? 0 : definition.getContextMenuItems().size());
    }

    private void addStep(QuickActionStepType type) {
        CustomQuickActionStep step = new CustomQuickActionStep().setType(type);
        CustomQuickActionEditor.openStep(this, step, currentMode(), value -> {
            forceChainedForConditionalStep(value);
            currentSteps().add(value);
        });
    }

    private void editRow(int index) {
        if (index < 0 || index >= rowCount()) return;
        if (index < currentSteps().size()) {
            CustomQuickActionEditor.openStep(this, currentSteps().get(index), currentMode(), value -> {
                forceChainedForConditionalStep(value);
                currentSteps().set(index, value);
            });
            return;
        }
        int menuIndex = index - currentSteps().size();
        Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(
                this, definition.getContextMenuItems().get(menuIndex), menuIndex));
    }

    private void deleteRow(int index) {
        if (index < 0 || index >= rowCount()) return;
        if (index < currentSteps().size()) currentSteps().remove(index);
        else definition.getContextMenuItems().remove(index - currentSteps().size());
        reopen();
    }

    private void addMenuItem() {
        CustomQuickActionMenuItem item = new CustomQuickActionMenuItem()
                .setLabel(CustomQuickActionEditor.t("custom_quick_action_new_menu").content());
        Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(this, item, -1));
    }

    private void replaceMenuItem(int index, CustomQuickActionMenuItem value) {
        if (index >= 0 && index < definition.getContextMenuItems().size()) {
            definition.getContextMenuItems().set(index, value);
        } else {
            definition.getContextMenuItems().add(value);
        }
        Minecraft.getInstance().setScreen(this);
    }

    private void save() {
        if (isMenuItem()) {
            if (menuItem.getLabel().trim().isEmpty()) {
                openDefinitionEditor();
                return;
            }
            sequenceParent.replaceMenuItem(menuItemIndex, CustomQuickActionEditor.copy(menuItem));
            return;
        }
        if (definition.getId().trim().isEmpty() || definition.getLabel().trim().isEmpty()) {
            openDefinitionEditor();
            return;
        }
        rootParent.saveDefinition(originalIndex, definition);
    }

    private void reopen() {
        if (isMenuItem()) {
            Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(
                    sequenceParent, menuItem, menuItemIndex));
        } else {
            Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(
                    rootParent, definition, originalIndex));
        }
    }

    /** 先在当前页面选择图标来源，再进入对应选择器。 */
    private void selectIcon() {
        popupOption.clear();
        for (QuickActionIconType type : QuickActionIconType.values()) {
            popupOption.addOptionWithId(type.name(), CustomQuickActionEditor.t(
                    "custom_quick_action_icon_" + type.name().toLowerCase()), null,
                    event -> openIconPicker(type));
        }
        popupOption.showAt(inputState.mouseX(), inputState.mouseY(), "quick_action_icon_type");
    }

    private void openIconPicker(QuickActionIconType type) {
        definition.setIconType(type);
        switch (type) {
            case ITEM:
                Item item = optionalItem(definition.getIcon());
                Minecraft.getInstance().setScreen(new ItemSelectScreen(new ItemSelectScreen.Args()
                        .parentScreen(this).defaultItem(new ItemStack(item))
                        .onDataReceived((java.util.function.Consumer<ItemStack>) stack -> {
                            definition.setIconType(QuickActionIconType.ITEM);
                            definition.setIcon(String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
                        })));
                break;
            case EFFECT:
                MobEffect effect = optionalEffect(definition.getIcon());
                Minecraft.getInstance().setScreen(new EffectSelectScreen(new EffectSelectScreen.Args()
                        .parentScreen(this).defaultEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), 200, 0))
                        .onDataReceived((java.util.function.Consumer<MobEffectInstance>) value -> {
                            definition.setIconType(QuickActionIconType.EFFECT);
                            definition.setIcon(String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(value.getEffect().value())));
                        })));
                break;
            case EXTERNAL_FILE:
                openIconInput(InputFormScreen.WidgetType.FILE, null);
                break;
            case RESOURCE:
            default:
                openIconInput(InputFormScreen.WidgetType.DROPDOWN, Arrays.asList(
                        new DropdownOption("banira_codex:gui/quick_icon.png"),
                        new DropdownOption("banira_codex:gui/sakura_cat.png"),
                        new DropdownOption("banira_codex:gui/aotake_cat.png"),
                        new DropdownOption("banira_codex:gui/narcissus_cat.png"),
                        new DropdownOption("banira_codex:gui/snowflake_cat.png")));
                break;
        }
    }

    private void openIconInput(InputFormScreen.WidgetType type, List<DropdownOption> options) {
        InputFormScreen.Widget icon = new InputFormScreen.Widget().name("icon")
                .title(CustomQuickActionEditor.t("custom_quick_action_icon_value"))
                .type(type).defaultValue(definition.getIcon()).allowEmpty(false);
        if (type == InputFormScreen.WidgetType.DROPDOWN) {
            icon.dropdownOptionEntries(options).dropdownInputMode(DropdownInputMode.EDITABLE);
        }
        Minecraft.getInstance().setScreen(new InputFormScreen(new InputFormScreen.Args()
                .setParentScreen(this).setHeaderTitle(CustomQuickActionEditor.t("custom_quick_action_select_icon"))
                .addWidget(icon).setCallback(result -> definition.setIcon(result.firstValue().trim()))));
    }

    private static Item optionalItem(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value == null ? "" : value);
        return id == null ? Items.PAPER : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.PAPER);
    }

    private static MobEffect optionalEffect(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value == null ? "" : value);
        return id == null ? MobEffects.LUCK.value() : BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(MobEffects.LUCK.value());
    }

    private void updateRows() {
        int deleteW = ROW_H;
        for (int i = 0; i < rows.size(); i++) {
            int visibleIndex = i - scrollIndex;
            boolean visible = visibleIndex >= 0 && visibleIndex < visibleRows;
            int y = listY + visibleIndex * (ROW_H + ROW_GAP);
            ButtonWidget row = rows.get(i);
            row.visible(visible);
            row.bounds(new ScreenCoordinate(listX, y, listW - deleteW - 3, ROW_H));
            String fullText = rowText(i);
            int maxTextWidth = Math.max(0, (int) row.bounds().width() - 12);
            String displayText = QuickActionTextLayout.ellipsize(fullText, maxTextWidth, font::width);
            row.text(displayText);
            row.textMaxWidth(maxTextWidth);
            TooltipWidget tooltip = rowTooltips.get(i);
            tooltip.bounds(new ScreenCoordinate(0, 0, row.bounds().width(), ROW_H));
            tooltip.text(QuickActionTextLayout.wrap(fullText,
                    Math.max(40, Math.min(320, width - 24)), font::width));
            tooltip.visible(visible && !displayText.equals(fullText));
            ButtonWidget delete = deletes.get(i);
            delete.visible(visible);
            delete.bounds(new ScreenCoordinate(listX + listW - deleteW, y, deleteW, ROW_H));
        }
        if (scrollbar != null) {
            scrollbar.bounds(new ScreenCoordinate(listX + listW + 3, listY, SCROLL_W,
                    visibleRows * (ROW_H + ROW_GAP) - ROW_GAP));
        }
    }

    private String rowText(int index) {
        if (index >= currentSteps().size()) {
            CustomQuickActionMenuItem item = definition.getContextMenuItems().get(index - currentSteps().size());
            return CustomQuickActionEditor.t("custom_quick_action_menu_prefix").content() + " · " + item.getLabel();
        }
        CustomQuickActionStep step = currentSteps().get(index);
        String value = step.getValue() == null ? "" : step.getValue();
        return (index + 1) + ". " + CustomQuickActionEditor.t("custom_quick_action_step_type_"
                + step.getType().name().toLowerCase()).content() + " · "
                + CustomQuickActionEditor.t("custom_quick_action_condition_"
                + step.getCondition().name().toLowerCase()).content() + " · " + value;
    }

    @Override
    protected void onMouseScrolled(MouseScrolledHandleArgs eventArgs) {
        if (scrollbar == null || scrollbar.maxValue() <= 0) return;
        scrollbar.setValue(Math.max(0, Math.min(scrollbar.maxValue(),
                scrollbar.value() - eventArgs.delta())));
        eventArgs.consumed(true);
    }

    @Override
    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack stack = graphics.pose();
        BaniraColorConfig theme = getEffectiveTheme();
        ShapeDrawArgs panel = ShapeDrawArgs.rect(stack, panelX, panelY, panelW, panelH,
                ColorUtils.applyAlphaToArgb(theme.panelBg(), 0xFF));
        panel.rect().radius(8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panel);
        if (rowCount() == 0) {
            String empty = CustomQuickActionEditor.t("custom_quick_action_step_empty").content();
            graphics.drawString(font, empty, (int) (panelX + (panelW - font.width(empty)) / 2f),
                    listY + 12, theme.textSecondary(), false);
        }
        super.renderWidgets(graphics, partialTicks);
    }

    @Override
    protected ScreenCoordinate closeableWindowBounds() {
        return new ScreenCoordinate(panelX, panelY, panelW, panelH);
    }

    private ButtonWidget button(String id, int x, int y, int width, int height,
                                String textKey, Runnable action) {
        ButtonWidget button = new ButtonWidget(this);
        button.id(id);
        button.bounds(new ScreenCoordinate(x, y, width, height));
        button.text(CustomQuickActionEditor.t(textKey));
        button.onClick(value -> action.run());
        return button;
    }
}
