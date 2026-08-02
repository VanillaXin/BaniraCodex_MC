package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.quickaction.*;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/** 编辑快捷入口的执行步骤；动作基础信息和图标选择保持为独立流程。 */
final class CustomQuickActionStepsScreen extends BaniraScreen {
    private static final int MARGIN = 16;
    private static final int PAD = 8;
    private static final int TOP_H = 22;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;
    private static final int BOTTOM_H = 20;
    private static final int SCROLL_W = 5;

    private final CustomQuickActionConfigScreen parent;
    private final int originalIndex;
    private CustomQuickActionDefinition definition;
    private final List<ButtonWidget> rows = new ArrayList<>();
    private final List<ButtonWidget> deletes = new ArrayList<>();
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
        this.parent = parent;
        this.definition = CustomQuickActionEditor.copy(definition);
        this.originalIndex = originalIndex;
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
    }

    void openDefinitionEditor() {
        CustomQuickActionEditor.openDefinition(this, definition, value -> definition = value);
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
        int bottomArea = BOTTOM_H * 2 + 5;
        int listH = panelH - PAD * 3 - TOP_H - bottomArea;
        visibleRows = Math.max(1, listH / (ROW_H + ROW_GAP));

        int topButtonW = (listW - 6) / 3;
        addWidget(button("details", listX, panelY + PAD, topButtonW, TOP_H,
                "custom_quick_action_edit_details", this::openDefinitionEditor));
        addWidget(button("icon", listX + topButtonW + 3, panelY + PAD, topButtonW, TOP_H,
                "custom_quick_action_select_icon", this::selectIcon));
        addWidget(button("add_command", listX + (topButtonW + 3) * 2, panelY + PAD,
                topButtonW, TOP_H, "custom_quick_action_add_command",
                () -> addStep(QuickActionStepType.COMMAND)));

        rows.clear();
        deletes.clear();
        for (int i = 0; i < definition.getSteps().size(); i++) {
            final int index = i;
            ButtonWidget row = new ButtonWidget(this);
            row.id("step_" + i);
            row.onClick(button -> editStep(index));
            rows.add(row);
            addWidget(row);
            ButtonWidget delete = new ButtonWidget(this);
            delete.id("delete_" + i);
            delete.text("-");
            delete.onClick(button -> deleteStep(index));
            deletes.add(delete);
            addWidget(delete);
        }

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0)
                .maxValue(Math.max(0, definition.getSteps().size() - visibleRows)).visibleSize(visibleRows)
                .scrollStep(1).onValueChanged(value -> {
                    scrollIndex = (int) Math.round(value);
                    updateRows();
                });
        addWidget(scrollbar);

        int gap = 5;
        int y1 = panelY + panelH - PAD - BOTTOM_H * 2 - gap;
        int y2 = y1 + BOTTOM_H + gap;
        int half = (listW - gap) / 2;
        addWidget(button("add_screen", listX, y1, listW, BOTTOM_H,
                "custom_quick_action_add_screen", () -> addStep(QuickActionStepType.SCREEN)));
        addWidget(button("save", listX, y2, half, BOTTOM_H, "save", this::save));
        addWidget(button("cancel", listX + half + gap, y2, half, BOTTOM_H, "cancel", this::onClose));
        updateRows();
    }

    private void addStep(QuickActionStepType type) {
        CustomQuickActionStep step = new CustomQuickActionStep().setType(type);
        CustomQuickActionEditor.openStep(this, step, definition.getExecutionMode(), value -> {
            definition.getSteps().add(value);
        });
    }

    private void editStep(int index) {
        if (index < 0 || index >= definition.getSteps().size()) return;
        CustomQuickActionEditor.openStep(this, definition.getSteps().get(index),
                definition.getExecutionMode(), value -> {
                    definition.getSteps().set(index, value);
                });
    }

    private void deleteStep(int index) {
        if (index < 0 || index >= definition.getSteps().size()) return;
        definition.getSteps().remove(index);
        rebuild();
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(parent, definition, originalIndex));
    }

    private void save() {
        if (definition.getId().trim().isEmpty() || definition.getLabel().trim().isEmpty()) {
            openDefinitionEditor();
            return;
        }
        parent.saveDefinition(originalIndex, definition);
    }

    private void selectIcon() {
        switch (definition.getIconType()) {
            case ITEM:
                Item item = optionalItem(definition.getIcon());
                Minecraft.getInstance().setScreen(new ItemSelectScreen(new ItemSelectScreen.Args()
                        .parentScreen(this).defaultItem(new ItemStack(item))
                        .onDataReceived((java.util.function.Consumer<ItemStack>) stack ->
                                definition.setIcon(String.valueOf(Registry.ITEM.getKey(stack.getItem()))))));
                break;
            case EFFECT:
                Effect effect = optionalEffect(definition.getIcon());
                Minecraft.getInstance().setScreen(new EffectSelectScreen(new EffectSelectScreen.Args()
                        .parentScreen(this).defaultEffect(new EffectInstance(effect, 200, 0))
                        .onDataReceived((java.util.function.Consumer<EffectInstance>) value ->
                                definition.setIcon(String.valueOf(Registry.MOB_EFFECT.getKey(value.getEffect()))))));
                break;
            case EXTERNAL_FILE:
                openIconInput(InputFormScreen.WidgetType.FILE);
                break;
            case RESOURCE:
            default:
                openIconInput(InputFormScreen.WidgetType.TEXT);
                break;
        }
    }

    private void openIconInput(InputFormScreen.WidgetType type) {
        InputFormScreen.Widget icon = new InputFormScreen.Widget().name("icon")
                .title(CustomQuickActionEditor.t("custom_quick_action_icon_value"))
                .type(type).defaultValue(definition.getIcon()).allowEmpty(false);
        Minecraft.getInstance().setScreen(new InputFormScreen(new InputFormScreen.Args()
                .setParentScreen(this).setHeaderTitle(CustomQuickActionEditor.t("custom_quick_action_select_icon"))
                .addWidget(icon).setCallback(result -> definition.setIcon(result.firstValue().trim()))));
    }

    private static Item optionalItem(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value == null ? "" : value);
        return id == null ? Items.PAPER : Registry.ITEM.getOptional(id).orElse(Items.PAPER);
    }

    private static Effect optionalEffect(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value == null ? "" : value);
        return id == null ? Effects.LUCK : Registry.MOB_EFFECT.getOptional(id).orElse(Effects.LUCK);
    }

    private void updateRows() {
        int deleteW = ROW_H;
        for (int i = 0; i < rows.size(); i++) {
            int visibleIndex = i - scrollIndex;
            boolean visible = visibleIndex >= 0 && visibleIndex < visibleRows;
            int y = listY + visibleIndex * (ROW_H + ROW_GAP);
            CustomQuickActionStep step = definition.getSteps().get(i);
            String value = step.getValue() == null ? "" : step.getValue();
            ButtonWidget row = rows.get(i);
            row.visible(visible);
            row.bounds(new ScreenCoordinate(listX, y, listW - deleteW - 3, ROW_H));
            row.text((i + 1) + ". " + CustomQuickActionEditor.t("custom_quick_action_step_type_"
                    + step.getType().name().toLowerCase()).content() + " · "
                    + CustomQuickActionEditor.t("custom_quick_action_condition_"
                    + step.getCondition().name().toLowerCase()).content() + " · " + value);
            row.textMaxWidth(listW - deleteW - 12);
            ButtonWidget delete = deletes.get(i);
            delete.visible(visible);
            delete.bounds(new ScreenCoordinate(listX + listW - deleteW, y, deleteW, ROW_H));
        }
        if (scrollbar != null) {
            scrollbar.bounds(new ScreenCoordinate(listX + listW + 3, listY, SCROLL_W,
                    visibleRows * (ROW_H + ROW_GAP) - ROW_GAP));
        }
    }

    @Override
    protected void onMouseScrolled(MouseScrolledHandleArgs eventArgs) {
        if (scrollbar == null || scrollbar.maxValue() <= 0) return;
        scrollbar.setValue(Math.max(0, Math.min(scrollbar.maxValue(),
                scrollbar.value() - eventArgs.delta())));
        eventArgs.consumed(true);
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        ShapeDrawArgs panel = ShapeDrawArgs.rect(stack, panelX, panelY, panelW, panelH,
                ColorUtils.applyAlphaToArgb(theme.panelBg(), 0xFF));
        panel.rect().radius(8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panel);
        if (definition.getSteps().isEmpty()) {
            String empty = CustomQuickActionEditor.t("custom_quick_action_step_empty").content();
            font.draw(stack, empty, panelX + (panelW - font.width(empty)) / 2f,
                    listY + 12, theme.textSecondary());
        }
        super.renderWidgets(stack, partialTicks);
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
