package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/** 玩家自定义快捷入口的列表与持久化入口。 */
public final class CustomQuickActionConfigScreen extends BaniraScreen {
    private static final int MARGIN = 18;
    private static final int PAD = 10;
    private static final int TITLE_H = 16;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;
    private static final int BOTTOM_H = 22;
    private static final int SCROLL_W = 5;

    private final Screen parent;
    private final List<CustomQuickActionDefinition> definitions = new ArrayList<>();
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private final List<ButtonWidget> deleteButtons = new ArrayList<>();
    private final List<TooltipWidget> rowTooltips = new ArrayList<>();
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

    public CustomQuickActionConfigScreen(Screen parent) {
        super(BaniraComponent.get().transClientAuto("custom_quick_action_title").toVanilla());
        this.parent = parent;
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
    }

    @Override
    protected void onInit() {
        definitions.clear();
        for (CustomQuickActionDefinition definition : CustomQuickActionManager.get().definitions()) {
            definitions.add(CustomQuickActionEditor.copy(definition));
        }
    }

    @Override
    protected void initWidgets() {
        panelX = MARGIN;
        panelY = MARGIN;
        panelW = width - MARGIN * 2;
        panelH = height - MARGIN * 2;
        listX = panelX + PAD;
        listY = panelY + PAD + TITLE_H;
        listW = panelW - PAD * 2 - SCROLL_W - 3;
        int listH = panelH - PAD * 3 - TITLE_H - BOTTOM_H;
        visibleRows = Math.max(1, listH / (ROW_H + ROW_GAP));

        rowButtons.clear();
        deleteButtons.clear();
        rowTooltips.clear();
        for (int i = 0; i < definitions.size(); i++) {
            final int index = i;
            ButtonWidget row = new ButtonWidget(this);
            row.id("action_" + i);
            row.onClick(button -> openEditor(index));
            TooltipWidget tooltip = new TooltipWidget(this);
            tooltip.id("entry_tooltip_" + i);
            tooltip.popupAtScreenCoords(true);
            row.addChild(tooltip);
            rowButtons.add(row);
            rowTooltips.add(tooltip);
            addWidget(row);

            ButtonWidget delete = QuickActionWidgets.deleteButton(this, "delete_" + i, ROW_H,
                    button -> delete(index));
            deleteButtons.add(delete);
            addWidget(delete);
        }

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0)
                .maxValue(Math.max(0, definitions.size() - visibleRows)).visibleSize(visibleRows)
                .scrollStep(1).onValueChanged(value -> {
                    scrollIndex = (int) Math.round(value);
                    updateRows();
                });
        addWidget(scrollbar);

        int gap = 8;
        int buttonW = Math.min(150, (panelW - PAD * 2 - gap) / 2);
        int y = panelY + panelH - PAD - BOTTOM_H;
        ButtonWidget add = new ButtonWidget(this);
        add.id("add");
        add.bounds(new ScreenCoordinate(panelX + PAD, y, buttonW, BOTTOM_H));
        add.text(CustomQuickActionEditor.t("add"));
        add.onClick(button -> add());
        addWidget(add);
        ButtonWidget close = new ButtonWidget(this);
        close.id("close");
        close.bounds(new ScreenCoordinate(panelX + panelW - PAD - buttonW, y, buttonW, BOTTOM_H));
        close.text(CustomQuickActionEditor.t("close"));
        close.onClick(button -> onClose());
        addWidget(close);
        updateRows();
    }

    private void add() {
        CustomQuickActionDefinition draft = new CustomQuickActionDefinition()
                .setId(CustomQuickActionManager.nextDefinitionId());
        Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(this, draft, -1));
    }

    private void openEditor(int index) {
        if (index < 0 || index >= definitions.size()) return;
        Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(this, definitions.get(index), index));
    }

    static void openEditor(Screen parent, String definitionId) {
        CustomQuickActionConfigScreen screen = new CustomQuickActionConfigScreen(parent);
        List<CustomQuickActionDefinition> values = CustomQuickActionManager.get().definitions();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getId().equalsIgnoreCase(definitionId)) {
                Minecraft.getInstance().setScreen(new CustomQuickActionStepsScreen(screen, values.get(i), i));
                return;
            }
        }
        Minecraft.getInstance().setScreen(screen);
    }

    private void delete(int index) {
        if (index < 0 || index >= definitions.size()) return;
        definitions.remove(index);
        persist();
        Minecraft.getInstance().setScreen(new CustomQuickActionConfigScreen(parent));
    }

    void saveDefinition(int originalIndex, CustomQuickActionDefinition definition) {
        List<CustomQuickActionDefinition> existing = CustomQuickActionManager.get().definitions();
        List<CustomQuickActionDefinition> values = new ArrayList<>();
        boolean replaced = false;
        for (int i = 0; i < existing.size(); i++) {
            if (i == originalIndex) {
                values.add(definition);
                replaced = true;
            } else if (!existing.get(i).getId().equalsIgnoreCase(definition.getId())) {
                values.add(existing.get(i));
            }
        }
        if (!replaced) values.add(definition);
        CustomQuickActionManager.get().replaceDefinitions(values);
        Minecraft.getInstance().setScreen(new CustomQuickActionConfigScreen(parent));
    }

    private void persist() {
        CustomQuickActionManager.get().replaceDefinitions(definitions);
    }

    private void updateRows() {
        int deleteW = ROW_H;
        for (int i = 0; i < rowButtons.size(); i++) {
            int visibleIndex = i - scrollIndex;
            boolean visible = visibleIndex >= 0 && visibleIndex < visibleRows;
            int y = listY + visibleIndex * (ROW_H + ROW_GAP);
            CustomQuickActionDefinition definition = definitions.get(i);
            ButtonWidget row = rowButtons.get(i);
            row.visible(visible);
            row.bounds(new ScreenCoordinate(listX, y, listW - deleteW - 3, ROW_H));
            String fullText = (definition.isEnabled() ? "" : "[x] ") + definition.getLabel()
                    + (definition.getKeyChord().isEmpty() ? "" : "  [" + definition.getKeyChord() + "]");
            int maxTextWidth = Math.max(0, (int) row.bounds().width() - 12);
            String displayText = QuickActionTextLayout.ellipsize(fullText, maxTextWidth, font::width);
            row.text(displayText);
            row.textMaxWidth(maxTextWidth);
            TooltipWidget tooltip = rowTooltips.get(i);
            tooltip.bounds(new ScreenCoordinate(0, 0, row.bounds().width(), ROW_H));
            tooltip.text(QuickActionTextLayout.wrap(fullText,
                    Math.max(40, Math.min(320, width - 24)), font::width));
            tooltip.visible(visible && !displayText.equals(fullText));
            ButtonWidget delete = deleteButtons.get(i);
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
    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack stack = graphics.pose();
        BaniraColorConfig theme = getEffectiveTheme();
        ShapeDrawArgs panel = ShapeDrawArgs.rect(stack, panelX, panelY, panelW, panelH,
                ColorUtils.applyAlphaToArgb(theme.panelBg(), 0xFF));
        panel.rect().radius(8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panel);
        String title = CustomQuickActionEditor.t("custom_quick_action_title").content();
        graphics.drawString(font, title, panelX + (panelW - font.width(title)) / 2f, panelY + PAD,
                theme.textPrimary(), false);
        if (definitions.isEmpty()) {
            String empty = CustomQuickActionEditor.t("custom_quick_action_empty").content();
            graphics.drawString(font, empty, panelX + (panelW - font.width(empty)) / 2f,
                    listY + 16, theme.textSecondary(), false);
        }
        super.renderWidgets(graphics, partialTicks);
    }

    @Override
    protected ScreenCoordinate closeableWindowBounds() {
        return new ScreenCoordinate(panelX, panelY, panelW, panelH);
    }
}
