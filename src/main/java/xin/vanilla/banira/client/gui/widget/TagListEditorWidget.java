package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 可折叠标签列表编辑控件。
 * <p>
 * 折叠时仅显示标题栏；展开后以标签形式列出所有内容，每项可点击删除按钮移除。
 * 标题栏右侧提供清空与添加按钮。点击添加后根据 {@link #itemType} 显示对应输入控件：
 * 文本用 {@link InputWidget}，数值用 {@link NumericInputWidget}，枚举与布尔用 {@link DropdownSelectWidget}。
 * 展开后的列表高度固定，带滚动条。
 * </p>
 */
@Accessors(chain = true, fluent = true)
public class TagListEditorWidget extends BaseWidget implements ITextWidget {

    /**
     * 列表项类型，决定添加时使用的输入控件
     */
    public enum ItemType {
        /**
         * 文本，使用 InputWidget
         */
        TEXT,
        /**
         * 数值，使用 NumericInputWidget
         */
        NUMBER,
        /**
         * 枚举，使用 DropdownSelectWidget，选项由 enumOptions 提供
         */
        ENUM,
        /**
         * 布尔，使用 DropdownSelectWidget，选项为 true/false
         */
        BOOLEAN
    }

    public static final int HEADER_HEIGHT = 20;
    public static final int CONTENT_HEIGHT = 100;

    /**
     * 展开时的默认高度，用于布局计算
     */
    public static final int DEFAULT_EXPANDED_HEIGHT = HEADER_HEIGHT + CONTENT_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLL_GAP = 2;
    private static final int TAG_HEIGHT = 18;
    private static final int TAG_PAD = 4;
    private static final int TAG_CLOSE_SIZE = 10;
    private static final int BTN_SIZE = 14;
    private static final int ARROW_SIZE = 8;

    @Getter
    private Text text = Text.empty();

    @Getter
    @Setter
    private boolean expanded = false;

    @Getter
    @Setter
    @Nullable
    private Consumer<TagListEditorWidget> onExpandChanged;

    @Getter
    @Setter
    private ItemType itemType = ItemType.TEXT;

    /**
     * 枚举类型的选项列表，itemType 为 ENUM 时生效
     */
    private List<String> enumOptions = new ArrayList<>();

    public List<String> enumOptions() {
        return enumOptions;
    }

    @Getter
    @Setter
    @Nullable
    private Consumer<List<Object>> onListChanged;

    @Getter
    private final List<Object> items = new ArrayList<>();

    private double listScrollOffset = 0;
    private boolean addingMode = false;
    private int hoveredDeleteIndex = -1;
    private int pressedDeleteIndex = -1;

    /**
     * 清空后是否处于可撤销状态
     */
    private boolean undoMode = false;
    /**
     * 清空前保存的列表，用于撤销
     */
    private List<Object> lastClearedItems = new ArrayList<>();

    private ButtonWidget clearButton;
    private TooltipWidget clearButtonTooltip;
    private ButtonWidget addButton;
    private ScrollbarWidget scrollbar;
    @Nullable
    private IWidget addInputWidget;
    @Nullable
    private ButtonWidget addConfirmButton;

    /**
     * 子组件处理点击时，应获得焦点的目标（供 getFocusTarget 使用）
     */
    @Nullable
    private IWidget lastClickFocusTarget;

    public TagListEditorWidget(BaniraScreen screen) {
        super(screen);
    }

    public TagListEditorWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public double effectiveHeight() {
        ScreenCoordinate b = bounds();
        if (b == null) return 0;
        return b.height();
    }

    private void ensureChildren() {
        if (clearButton != null) return;
        updateBoundsHeight();
        double w = width();
        double contentH = CONTENT_HEIGHT;

        clearButton = new ButtonWidget(screen);
        clearButton.presetStyle(ButtonWidget.PresetStyle.MINUS)
                .bounds(new ScreenCoordinate(w - BTN_SIZE * 2 - 4, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));
        clearButton.onClick(b -> onClearOrUndoClicked());
        clearButtonTooltip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, BTN_SIZE));
        clearButtonTooltip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_clear_tooltip"));
        clearButton.addChild(clearButtonTooltip);
        addChild(clearButton);

        addButton = new ButtonWidget(screen);
        addButton.presetStyle(ButtonWidget.PresetStyle.PLUS)
                .bounds(new ScreenCoordinate(w - BTN_SIZE - 2, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));
        addButton.onClick(b -> enterAddingMode());
        TooltipWidget addTip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, BTN_SIZE));
        addTip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_add_tooltip"));
        addButton.addChild(addTip);
        addChild(addButton);

        scrollbar = new ScrollbarWidget(screen)
                .orientation(EnumOrientation.VERTICAL)
                .minValue(0)
                .maxValue(1)
                .value(0)
                .visibleSize(contentH)
                .onValueChanged(v -> listScrollOffset = v);
        addChild(scrollbar);
    }

    private static final int ADD_INPUT_HEIGHT = 22;

    private void enterAddingMode() {
        addingMode = true;
        updateHeightForAddingMode(true);
        createAddInputWidget();
    }

    private void exitAddingMode() {
        addingMode = false;
        updateHeightForAddingMode(false);
        if (addInputWidget != null) {
            if (addInputWidget instanceof DropdownSelectWidget) {
                ((DropdownSelectWidget) addInputWidget).closeDropdown();
            }
            removeChild(addInputWidget);
            addInputWidget = null;
        }
        if (addConfirmButton != null) {
            removeChild(addConfirmButton);
            addConfirmButton = null;
        }
    }

    private void updateHeightForAddingMode(boolean adding) {
        updateBoundsHeight();
    }

    private void updateBoundsHeight() {
        ScreenCoordinate b = bounds();
        if (b != null) {
            double h = expanded
                    ? (addingMode ? HEADER_HEIGHT + CONTENT_HEIGHT + 2 + ADD_INPUT_HEIGHT : HEADER_HEIGHT + CONTENT_HEIGHT)
                    : HEADER_HEIGHT;
            b.height(h);
            invalidateAbsCache();
        }
    }

    /**
     * 根据当前展开状态刷新 bounds 高度，供外部在布局前调用以确保 effectiveHeight 正确。
     */
    public void refreshBounds() {
        updateBoundsHeight();
    }

    private void createAddInputWidget() {
        if (addInputWidget != null) {
            if (addInputWidget instanceof DropdownSelectWidget) {
                ((DropdownSelectWidget) addInputWidget).closeDropdown();
            }
            removeChild(addInputWidget);
            addInputWidget = null;
        }
        if (addConfirmButton != null) {
            removeChild(addConfirmButton);
            addConfirmButton = null;
        }
        double w = width();
        double inputY = HEADER_HEIGHT + 2;
        double confirmBtnX = w - BTN_SIZE - 2;

        switch (itemType) {
            case TEXT:
                InputWidget input = new InputWidget(screen);
                double inputW = confirmBtnX - 2;
                input.bounds(new ScreenCoordinate(0, inputY, inputW, ADD_INPUT_HEIGHT));
                input.maxLength(64);
                input.text(Text.literal("输入后按 Enter 或点击添加"));
                input.value("");
                addInputWidget = input;
                break;
            case NUMBER:
                NumericInputWidget numInput = new NumericInputWidget(screen);
                numInput.bounds(new ScreenCoordinate(0, inputY, confirmBtnX - 2, ADD_INPUT_HEIGHT));
                numInput.value("0");
                addInputWidget = numInput;
                break;
            case ENUM:
            case BOOLEAN:
                DropdownSelectWidget dropdown = new DropdownSelectWidget(screen)
                        .multiSelect(true)
                        .bounds(new ScreenCoordinate(0, inputY, confirmBtnX - 2, ADD_INPUT_HEIGHT));
                if (itemType == ItemType.BOOLEAN) {
                    dropdown.options(Arrays.asList("true", "false"));
                    dropdown.selectedValues(Collections.emptyList());
                } else {
                    dropdown.options(enumOptions.isEmpty() ? Collections.singletonList("") : enumOptions);
                    dropdown.selectedValues(Collections.emptyList());
                }
                addInputWidget = dropdown;
                break;
            default:
                return;
        }
        addChild(addInputWidget);

        addConfirmButton = new ButtonWidget(screen);
        addConfirmButton.presetStyle(ButtonWidget.PresetStyle.PLUS)
                .bounds(new ScreenCoordinate(confirmBtnX, inputY, BTN_SIZE, ADD_INPUT_HEIGHT));
        addConfirmButton.onClick(b -> confirmAddFromInput());
        TooltipWidget confirmTip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, ADD_INPUT_HEIGHT));
        confirmTip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_confirm_add"));
        addConfirmButton.addChild(confirmTip);
        if (screen != null) {
            addConfirmButton.applyTheme(screen.getEffectiveTheme());
        }
        addChild(addConfirmButton);

        if (addInputWidget instanceof DropdownSelectWidget) {
            DropdownSelectWidget dd = (DropdownSelectWidget) addInputWidget;
            dd.excludedCloseAreasSupplier(() -> {
                if (addConfirmButton == null) return Collections.emptyList();
                return Collections.singletonList(new ScreenCoordinate(
                        addConfirmButton.absoluteX(), addConfirmButton.absoluteY(),
                        addConfirmButton.width(), addConfirmButton.height()));
            });
            dd.openDropdown();
        }

        if (screen != null && addInputWidget != null) {
            screen.requestFocus(addInputWidget.getFocusTarget());
        }
    }

    private void confirmAddFromInput() {
        if (addInputWidget == null) return;
        boolean added = false;
        if (addInputWidget instanceof DropdownSelectWidget) {
            DropdownSelectWidget dropdown = (DropdownSelectWidget) addInputWidget;
            List<String> sel = dropdown.getSelectedValues();
            for (String v : sel) {
                if (v == null || v.trim().isEmpty()) continue;
                Object value = itemType == ItemType.BOOLEAN ? Boolean.parseBoolean(v) : v;
                items.add(value);
                added = true;
            }
            if (added) dropdown.selectedValues(Collections.emptyList());
        } else if (addInputWidget instanceof InputWidget && !(addInputWidget instanceof NumericInputWidget)) {
            String v = ((InputWidget) addInputWidget).value();
            if (itemType == ItemType.TEXT && (v == null || v.trim().isEmpty())) return;
            Object value = v != null ? v.trim() : "";
            if (value != null) {
                items.add(value);
                added = true;
                ((InputWidget) addInputWidget).value("");
            }
        } else if (addInputWidget instanceof NumericInputWidget) {
            NumericInputWidget numInput = (NumericInputWidget) addInputWidget;
            String v = numInput.value();
            if (v == null || v.trim().isEmpty()) return;
            items.add(numInput.getParsedValue());
            added = true;
            numInput.value("0");
        }
        if (added) {
            exitUndoModeIfNeeded();
            syncScrollbar();
            fireListChanged();
        }
    }

    private void fireListChanged() {
        if (onListChanged != null) {
            onListChanged.accept(new ArrayList<>(items));
        }
    }

    /**
     * 列表变更时若处于撤销模式则退出（新增/删除会覆盖撤销状态）
     */
    private void exitUndoModeIfNeeded() {
        if (undoMode) {
            undoMode = false;
            lastClearedItems.clear();
            clearButton.presetStyle(ButtonWidget.PresetStyle.MINUS);
            if (clearButtonTooltip != null) {
                clearButtonTooltip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_clear_tooltip"));
            }
        }
    }

    private void onClearOrUndoClicked() {
        if (undoMode) {
            items.clear();
            items.addAll(lastClearedItems);
            lastClearedItems.clear();
            undoMode = false;
            clearButton.presetStyle(ButtonWidget.PresetStyle.MINUS);
            if (clearButtonTooltip != null) {
                clearButtonTooltip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_clear_tooltip"));
            }
        } else {
            lastClearedItems.clear();
            lastClearedItems.addAll(items);
            items.clear();
            undoMode = true;
            clearButton.presetStyle(ButtonWidget.PresetStyle.ARROW_LEFT);
            if (clearButtonTooltip != null) {
                clearButtonTooltip.text(Component.transClientAuto(BaniraCodex.MODID, "tag_list_undo_tooltip"));
            }
        }
        listScrollOffset = 0;
        syncScrollbar();
        fireListChanged();
    }

    public TagListEditorWidget items(List<Object> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        listScrollOffset = 0;
        syncScrollbar();
        return this;
    }

    public TagListEditorWidget enumOptions(String... options) {
        this.enumOptions = new ArrayList<>(Arrays.asList(options));
        return this;
    }

    public TagListEditorWidget enumOptionsList(List<String> options) {
        this.enumOptions = options != null ? new ArrayList<>(options) : new ArrayList<>();
        return this;
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        if (theme == null) return;
        ensureChildren();
        if (clearButton != null) clearButton.applyTheme(theme);
        if (addButton != null) addButton.applyTheme(theme);
        if (scrollbar != null) scrollbar.applyTheme(theme);
        if (addConfirmButton != null) addConfirmButton.applyTheme(theme);
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) return;
        ensureChildren();

        double ox = x();
        double oy = y();
        double w = width();
        double absX = absoluteX();
        double absY = absoluteY();

        stack.pushPose();
        stack.translate(ox, oy, 0);

        // region 绘制标题栏
        int headerBg = mouseInside ? BaniraColorConfig.winter().bgTertiary() : BaniraColorConfig.winter().bgSecondary();
        AbstractGuiUtils.fill(stack, 0, 0, (int) w, HEADER_HEIGHT, headerBg);

        int arrowX = 4;
        int arrowY = (HEADER_HEIGHT - ARROW_SIZE) / 2;
        if (expanded) {
            drawArrowDown(stack, arrowX, arrowY, ARROW_SIZE, 0xFF333333);
        } else {
            drawArrowRight(stack, arrowX, arrowY, ARROW_SIZE, 0xFF333333);
        }

        int textX = 4 + ARROW_SIZE + 4;
        int textY = (HEADER_HEIGHT - 9) / 2;
        String baseTitle = text != null ? text.content() : "";
        String displayTitle = baseTitle.isEmpty() ? "(" + items.size() + ")" : baseTitle + " (" + items.size() + ")";
        FontDrawArgs args = FontDrawArgs.of(
                Text.literal(displayTitle).stack(stack).font(screen != null ? screen.getFont() : AbstractGuiUtils.getFont()).color(0xFF333333));
        args.x(textX).y(textY).maxWidth((int) Math.max(0, w - textX - BTN_SIZE * 2 - 16)).wrap(false).inScreen(false);
        LabelWidget.drawLimitedText(args);

        stack.popPose();
        // endregion 标题栏

        if (!expanded) {
            clearButton.visible(false);
            addButton.visible(false);
            scrollbar.visible(false);
            if (addInputWidget != null) addInputWidget.visible(false);
            if (addConfirmButton != null) addConfirmButton.visible(false);
            return;
        }

        clearButton.visible(true);
        addButton.visible(true);

        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listAreaHeight = CONTENT_HEIGHT - (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listW = w - SCROLLBAR_WIDTH - SCROLL_GAP;

        clearButton.bounds(new ScreenCoordinate(w - BTN_SIZE * 2 - 4, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));
        addButton.bounds(new ScreenCoordinate(w - BTN_SIZE - 2, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));

        if (addInputWidget != null) {
            addInputWidget.visible(true);
        }
        if (addConfirmButton != null) {
            addConfirmButton.bounds(new ScreenCoordinate(w - BTN_SIZE - 2, HEADER_HEIGHT + 2, BTN_SIZE, ADD_INPUT_HEIGHT));
            addConfirmButton.visible(true);
        }

        scrollbar.bounds(new ScreenCoordinate(listW + SCROLL_GAP, listContentTop, SCROLLBAR_WIDTH, listAreaHeight));
        syncScrollbar();
        scrollbar.visible(true);

        double listAbsX = absX;
        double listAbsY = absY + listContentTop;
        scrollbar.scrollingCoordinates(new ArrayList<>());
        scrollbar.addScrollHoverArea(new ScreenCoordinate(listAbsX, listAbsY, listW, listAreaHeight));

        // region 绘制标签列表
        BaniraColorConfig theme = screen != null ? screen.getEffectiveTheme() : BaniraColorConfig.winter();
        int tagBg = theme.popupItemSelected();
        int tagBorder = theme.popupItemSelectedBorder();
        int textColor = theme.listItemText();

        int listAreaAbsX = (int) listAbsX;
        int listAreaAbsY = (int) listAbsY;
        int listAreaW = (int) listW;
        int listAreaH = (int) listAreaHeight;
        AbstractGuiUtils.pushScissor(listAreaAbsX, listAreaAbsY, listAreaW, listAreaH);

        stack.pushPose();
        stack.translate(ox, oy + listContentTop - listScrollOffset, 0);

        FontRenderer font = Minecraft.getInstance().font;
        int tagW = (int) listW;
        int textMaxW = tagW - TAG_PAD * 2 - TAG_CLOSE_SIZE - TAG_PAD;
        int closeX = tagW - TAG_PAD - TAG_CLOSE_SIZE;
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            String label = formatItemLabel(item);
            String display = font.plainSubstrByWidth(label, textMaxW);
            double tagY = i * (TAG_HEIGHT + 2);

            int closeY = (int) (tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2);
            boolean closeHovered = hoveredDeleteIndex == i;
            boolean closePressed = pressedDeleteIndex == i;

            AbstractGuiUtils.fill(stack, 0, (int) tagY, tagW, TAG_HEIGHT, tagBg);
            AbstractGuiUtils.fill(stack, 0, (int) tagY, 2, TAG_HEIGHT, tagBorder);
            font.draw(stack, display, TAG_PAD, (float) (tagY + (TAG_HEIGHT - font.lineHeight) / 2), textColor);

            int closeColor = closePressed ? 0xFFE53935 : (closeHovered ? 0xFFE53935 : 0xFF999999);
            AbstractGuiUtils.fill(stack, closeX, closeY, TAG_CLOSE_SIZE, TAG_CLOSE_SIZE, closeColor);
            float r = 2f;
            int cx = closeX + TAG_CLOSE_SIZE / 2;
            int cy = closeY + TAG_CLOSE_SIZE / 2;
            AbstractGuiUtils.drawLine(stack, cx - r, cy - r, cx + r, cy + r, 1f, 0xFFFFFFFF);
            AbstractGuiUtils.drawLine(stack, cx + r, cy - r, cx - r, cy + r, 1f, 0xFFFFFFFF);
        }

        stack.popPose();
        AbstractGuiUtils.popScissor();
        // endregion 标签列表

        renderChildren(stack, partialTicks);
    }

    private String formatItemLabel(Object item) {
        if (item == null) return "";
        if (item instanceof Boolean) return ((Boolean) item) ? "true" : "false";
        if (item instanceof BigDecimal) return ((BigDecimal) item).toPlainString();
        if (item instanceof Number) {
            double d = ((Number) item).doubleValue();
            if (d == (long) d) return String.valueOf((long) d);
            return BigDecimal.valueOf(d).toPlainString();
        }
        return String.valueOf(item);
    }

    private void drawArrowDown(MatrixStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 90, color);
    }

    private void drawArrowRight(MatrixStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 0, color);
    }

    private void syncScrollbar() {
        if (scrollbar == null) return;
        double listAreaH = CONTENT_HEIGHT - (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double totalH = items.size() * (TAG_HEIGHT + 2);
        double maxScroll = Math.max(0, totalH - listAreaH);
        scrollbar.maxValue(maxScroll);
        scrollbar.visibleSize(listAreaH);
        scrollbar.setValue(Math.min(listScrollOffset, maxScroll));
        listScrollOffset = scrollbar.value();
    }

    @Override
    public void update() {
        if (!visible || !enabled) return;
        ensureChildren();
        if (screen != null) {
            updateMouseHover(screen.inputState().mouseX(), screen.inputState().mouseY());
        }
        double absX = absoluteX();
        double absY = absoluteY();
        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        int closeX = getDeleteButtonX();
        hoveredDeleteIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            double tagY = listContentTop - listScrollOffset + i * (TAG_HEIGHT + 2);
            double delX = absX + closeX;
            double delY = absY + tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2.0;
            double mx = screen != null ? screen.inputState().mouseX() : 0;
            double my = screen != null ? screen.inputState().mouseY() : 0;
            if (mx >= delX && mx < delX + TAG_CLOSE_SIZE && my >= delY && my < delY + TAG_CLOSE_SIZE) {
                hoveredDeleteIndex = i;
                break;
            }
        }

        if (expanded) {
            for (IWidget child : children) {
                if (child != null && child.visible() && child.enabled() && child.needsUpdate()) {
                    child.update();
                }
            }
        }

        if (addingMode && addInputWidget != null && addConfirmButton != null) {
            boolean inputFocused = addInputWidget instanceof BaseWidget && ((BaseWidget) addInputWidget).focused();
            boolean confirmFocused = addConfirmButton.focused();
            boolean dropdownOpen = addInputWidget instanceof DropdownSelectWidget
                    && ((DropdownSelectWidget) addInputWidget).dropdownOpen();
            boolean previewExpanded = addInputWidget instanceof DropdownSelectWidget
                    && ((DropdownSelectWidget) addInputWidget).previewExpanded();
            if (!inputFocused && !confirmFocused && !dropdownOpen && !previewExpanded) {
                exitAddingMode();
            }
        }
    }

    private int getDeleteButtonX() {
        double listW = width() - SCROLLBAR_WIDTH - SCROLL_GAP;
        return (int) listW - TAG_PAD - TAG_CLOSE_SIZE;
    }

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        if (!visible || !enabled || event == null) return false;
        if (!isMouseInside(event.mouseX(), event.mouseY())) return false;

        ensureChildren();
        lastClickFocusTarget = null;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        double relY = mouseY - absY;

        if (expanded) {
            for (int i = children.size() - 1; i >= 0; i--) {
                IWidget child = children.get(i);
                if (child != null && child.visible() && child.enabled()) {
                    if (child.handleMouseClick(event)) {
                        if (child == addInputWidget || child == addConfirmButton) {
                            lastClickFocusTarget = child == addConfirmButton ? addConfirmButton : addInputWidget.getFocusTarget();
                        }
                        return true;
                    }
                }
            }
        }

        if (relY < HEADER_HEIGHT && event.button() == 0) {
            expanded = !expanded;
            updateBoundsHeight();
            if (onExpandChanged != null) onExpandChanged.accept(this);
            return true;
        }

        if (!expanded) return false;

        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listAreaHeight = CONTENT_HEIGHT - (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        if (relY >= listContentTop && relY < listContentTop + listAreaHeight && event.button() == 0) {
            for (int i = 0; i < items.size(); i++) {
                int closeX = getDeleteButtonX();
                double tagY = listContentTop - listScrollOffset + i * (TAG_HEIGHT + 2);
                double delX = absX + closeX;
                double delY = absY + tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2.0;
                if (mouseX >= delX && mouseX < delX + TAG_CLOSE_SIZE && mouseY >= delY && mouseY < delY + TAG_CLOSE_SIZE) {
                    pressedDeleteIndex = i;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean handleMouseRelease(MouseEvent event) {
        if (!visible || !enabled || event == null) return false;

        if (expanded) {
            for (int i = children.size() - 1; i >= 0; i--) {
                IWidget child = children.get(i);
                if (child != null && child.visible() && child.enabled()) {
                    if (child.handleMouseRelease(event)) return true;
                }
            }
        }

        if (pressedDeleteIndex >= 0 && event.button() == 0) {
            double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
            double absX = absoluteX();
            double absY = absoluteY();
            int idx = pressedDeleteIndex;
            int closeX = getDeleteButtonX();
            double tagY = listContentTop - listScrollOffset + idx * (TAG_HEIGHT + 2);
            double delX = absX + closeX;
            double delY = absY + tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2.0;
            double mouseX = event.mouseX();
            double mouseY = event.mouseY();
            if (mouseX >= delX && mouseX < delX + TAG_CLOSE_SIZE && mouseY >= delY && mouseY < delY + TAG_CLOSE_SIZE) {
                if (idx >= 0 && idx < items.size()) {
                    items.remove(idx);
                    exitUndoModeIfNeeded();
                    syncScrollbar();
                    fireListChanged();
                }
            }
            pressedDeleteIndex = -1;
            return true;
        }

        return false;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (!visible || !enabled || event == null) return false;
        if (expanded) {
            for (int i = children.size() - 1; i >= 0; i--) {
                IWidget child = children.get(i);
                if (child != null && child.visible() && child.enabled()) {
                    if (child.handleMouseScroll(event)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (addInputWidget != null && addingMode) {
            if (keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
                exitAddingMode();
                return true;
            }
            if ((keyCode == GLFWKey.GLFW_KEY_ENTER || keyCode == GLFWKey.GLFW_KEY_KP_ENTER)
                    && (addInputWidget instanceof InputWidget || addInputWidget instanceof NumericInputWidget)) {
                confirmAddFromInput();
                return true;
            }
        }
        if (expanded) {
            for (int i = children.size() - 1; i >= 0; i--) {
                IWidget child = children.get(i);
                if (child != null && child.visible() && child.enabled()) {
                    if (child.handleKeyPress(keyCode, scanCode, modifiers)) return true;
                }
            }
        }
        return false;
    }

    public TagListEditorWidget text(String text) {
        this.text = Text.literal(text);
        return this;
    }

    public TagListEditorWidget text(Text text) {
        this.text = text != null ? text : Text.empty();
        return this;
    }

    @Override
    public TagListEditorWidget text(Component text) {
        this.text = text != null ? Text.from(text) : Text.empty();
        return this;
    }

    @Override
    public IWidget getFocusTarget() {
        IWidget target = lastClickFocusTarget != null ? lastClickFocusTarget.getFocusTarget() : this;
        lastClickFocusTarget = null;
        return target;
    }
}
