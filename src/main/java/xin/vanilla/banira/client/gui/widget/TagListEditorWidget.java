package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;

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
 * 展开后列表区域高度随标签项数量增加，不超过 {@link #CONTENT_HEIGHT}；超出部分滚动显示，无溢出时可隐藏滚动条以加宽列表。
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

    /**
     * 列表区域最大高度
     */
    public static final int CONTENT_HEIGHT = 100;

    /**
     * 展开且列表占满 {@link #CONTENT_HEIGHT} 时的总高度，供外部初始布局参考（实际高度可能更小）。
     */
    public static final int DEFAULT_EXPANDED_HEIGHT = HEADER_HEIGHT + CONTENT_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
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

    /**
     * 当根据列表项数量、展开状态、添加行等重新计算后的 bounds 高度与之前不同时调用，供父级面板同步布局。
     */
    @Getter
    @Setter
    @Nullable
    private Consumer<TagListEditorWidget> onBoundsHeightChanged;

    @Getter
    @Setter
    private ItemType itemType = ItemType.TEXT;

    /**
     * {@link ItemType#NUMBER} 时：是否仅允许整数（用于 {@code List<Integer>} / {@code List<Long>} 等）。
     */
    @Getter
    @Setter
    private boolean listNumberIntegerOnly = false;

    /**
     * {@link ItemType#NUMBER} 且非整数模式时的小数位，与 {@link NumericInputWidget#decimalPlaces()} 一致。
     */
    @Getter
    @Setter
    private int listNumberDecimalPlaces = 2;

    /**
     * {@link ItemType#NUMBER} 时可选的最小值，null 表示不限制。
     */
    @Getter
    @Setter
    @Nullable
    private Double listNumberMin;

    /**
     * {@link ItemType#NUMBER} 时可选的最大值，null 表示不限制。
     */
    @Getter
    @Setter
    @Nullable
    private Double listNumberMax;

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
    private final VisibleTagRange cachedVisibleTagRange = new VisibleTagRange();
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
    private final List<Object> lastClearedItems = new ArrayList<>();

    private ButtonWidget clearButton;
    private TooltipWidget clearButtonTooltip;
    private ButtonWidget addButton;
    private ScrollbarWidget scrollbar;
    @Nullable
    private IWidget addInputWidget;
    @Nullable
    private ButtonWidget addConfirmButton;

    /**
     * 行内编辑中的输入控件（双击列表项进入）
     */
    @Nullable
    private IWidget editWidget;
    private int editingIndex = -1;

    /**
     * 行内编辑框是否曾获得过焦点；用于避免首帧尚未完成 requestFocus 时误判失焦而关闭编辑。
     */
    private boolean inlineEditHadFocus;

    private int hoveredTagBodyIndex = -1;

    /**
     * 标题栏「+」按钮的 X（相对本控件），添加行右侧确认按钮与之对齐
     */
    private double headerAddButtonX;

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

    // region 列表高度与滚动条布局

    private static int tagRowStride() {
        return TAG_HEIGHT + 2;
    }

    private double tagsContentHeight() {
        return items.size() * (double) tagRowStride();
    }

    /**
     * 列表视口允许的最大高度（添加行开启时从 {@link #CONTENT_HEIGHT} 中扣除添加区）。
     */
    private double maxListViewportHeight() {
        return CONTENT_HEIGHT - (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
    }

    /**
     * 当前列表视口实际高度：标签总高与上限的较小值。
     */
    private double visibleListViewportHeight() {
        return Math.min(tagsContentHeight(), maxListViewportHeight());
    }

    private boolean listShowsScrollbar() {
        return tagsContentHeight() > maxListViewportHeight() + 1e-6 && maxListViewportHeight() > 1e-6;
    }

    private int firstVisibleTagIndex(double viewportHeight) {
        if (items.isEmpty()) {
            return 0;
        }
        return Math.max(0, (int) Math.floor(listScrollOffset / tagRowStride()));
    }

    private int lastVisibleTagIndex(double viewportHeight) {
        if (items.isEmpty()) {
            return -1;
        }
        double bottom = listScrollOffset + Math.max(0, viewportHeight);
        return Math.min(items.size() - 1, (int) Math.ceil(bottom / tagRowStride()));
    }

    /**
     * 当前滚动窗口内的标签行范围。渲染、hover 和删除命中共用，避免一帧内分散计算。
     */
    private VisibleTagRange visibleTagRange(double viewportHeight) {
        cachedVisibleTagRange.set(firstVisibleTagIndex(viewportHeight), lastVisibleTagIndex(viewportHeight));
        return cachedVisibleTagRange;
    }

    private static final class VisibleTagRange {
        int first;
        int last;

        private void set(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    private double listInnerWidth(double widgetWidth) {
        return listShowsScrollbar() ? widgetWidth - SCROLLBAR_WIDTH - SCROLL_GAP : widgetWidth;
    }

    // endregion 列表高度与滚动条布局

    // region 标题栏工具按钮布局

    /**
     * 将清空/添加按钮置于控件右内缘
     *
     * @return 靠左一侧按钮的 X
     */
    private double layoutHeaderToolbar(double w) {
        if (clearButton == null || addButton == null) {
            return w;
        }
        final double pad = 2;
        final double gap = 6;
        double y = (HEADER_HEIGHT - BTN_SIZE) / 2.0;
        double addX = Math.max(pad, w - pad - BTN_SIZE);
        double clearX = addX - gap - BTN_SIZE;
        if (clearX < pad) {
            clearX = pad;
            addX = clearX + BTN_SIZE + gap;
            if (addX + BTN_SIZE > w - pad) {
                addX = Math.max(pad, w - pad - BTN_SIZE);
            }
        }
        headerAddButtonX = addX;
        clearButton.bounds(new ScreenCoordinate(clearX, y, BTN_SIZE, BTN_SIZE));
        addButton.bounds(new ScreenCoordinate(addX, y, BTN_SIZE, BTN_SIZE));
        return Math.min(clearX, addX);
    }

    // endregion 标题栏工具按钮布局

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
                .padding(2)
                .bounds(new ScreenCoordinate(w - BTN_SIZE * 2 - 4, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));
        clearButton.onClick(b -> onClearOrUndoClicked());
        clearButtonTooltip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, BTN_SIZE));
        clearButtonTooltip.text(BaniraComponent.get().transClientAuto("tag_list_clear_tooltip"));
        clearButtonTooltip.popupAtScreenCoords(true);
        clearButton.addChild(clearButtonTooltip);
        addChild(clearButton);

        addButton = new ButtonWidget(screen);
        addButton.presetStyle(ButtonWidget.PresetStyle.PLUS)
                .padding(2)
                .bounds(new ScreenCoordinate(w - BTN_SIZE - 2, (HEADER_HEIGHT - BTN_SIZE) / 2.0, BTN_SIZE, BTN_SIZE));
        addButton.onClick(b -> enterAddingMode());
        TooltipWidget addTip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, BTN_SIZE));
        addTip.text(BaniraComponent.get().transClientAuto("tag_list_add_tooltip"));
        addTip.popupAtScreenCoords(true);
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

        layoutHeaderToolbar(w);
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
            if (addInputWidget instanceof DropdownSelectWidget selectWidget) {
                selectWidget.closeDropdown();
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
        if (b == null) {
            return;
        }
        double listH = expanded ? visibleListViewportHeight() : 0;
        double h = expanded
                ? HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0) + listH
                : HEADER_HEIGHT;
        double oldH = b.height();
        b.height(h);
        invalidateAbsCache();
        if (onBoundsHeightChanged != null && Math.abs(oldH - h) > 1e-6) {
            onBoundsHeightChanged.accept(this);
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
            if (addInputWidget instanceof DropdownSelectWidget selectWidget) {
                selectWidget.closeDropdown();
            }
            removeChild(addInputWidget);
            addInputWidget = null;
        }
        if (addConfirmButton != null) {
            removeChild(addConfirmButton);
            addConfirmButton = null;
        }
        double w = width();
        layoutHeaderToolbar(w);
        double inputY = HEADER_HEIGHT + 2;
        double confirmBtnX = headerAddButtonX;

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
                numInput.integerOnly(listNumberIntegerOnly);
                numInput.decimalPlaces(listNumberIntegerOnly ? 0 : listNumberDecimalPlaces);
                if (listNumberMin != null) {
                    numInput.minValue(listNumberMin);
                }
                if (listNumberMax != null) {
                    numInput.maxValue(listNumberMax);
                }
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
                .padding(2)
                .bounds(new ScreenCoordinate(confirmBtnX, inputY, BTN_SIZE, ADD_INPUT_HEIGHT));
        addConfirmButton.onClick(b -> confirmAddFromInput());
        TooltipWidget confirmTip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, BTN_SIZE, ADD_INPUT_HEIGHT));
        confirmTip.text(BaniraComponent.get().transClientAuto("tag_list_confirm_add"));
        confirmTip.popupAtScreenCoords(true);
        addConfirmButton.addChild(confirmTip);
        if (screen != null) {
            addConfirmButton.applyTheme(screen.getEffectiveTheme());
        }
        addChild(addConfirmButton);

        if (addInputWidget instanceof DropdownSelectWidget dd) {
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
        if (addInputWidget instanceof DropdownSelectWidget dropdown) {
            List<String> sel = dropdown.getSelectedValues();
            for (String v : sel) {
                if (v == null || v.trim().isEmpty()) continue;
                Object value = itemType == ItemType.BOOLEAN ? Boolean.parseBoolean(v) : v;
                items.add(value);
                added = true;
            }
            if (added) dropdown.selectedValues(Collections.emptyList());
        } else if (addInputWidget instanceof InputWidget inputWidget && !(addInputWidget instanceof NumericInputWidget)) {
            String v = inputWidget.value();
            if (itemType == ItemType.TEXT && (v == null || v.trim().isEmpty())) return;
            Object value = v != null ? v.trim() : "";
            if (value != null) {
                items.add(value);
                added = true;
                inputWidget.value("");
            }
        } else if (addInputWidget instanceof NumericInputWidget numInput) {
            String v = numInput.value();
            if (v == null || v.trim().isEmpty()) return;
            items.add(numInput.getParsedValue());
            added = true;
            numInput.value("0");
        }
        if (added) {
            exitUndoModeIfNeeded();
            syncScrollbar();
            updateBoundsHeight();
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
                clearButtonTooltip.text(BaniraComponent.get().transClientAuto("tag_list_clear_tooltip"));
            }
        }
    }

    private void cancelInlineEdit() {
        if (editWidget != null) {
            if (screen != null && editWidget instanceof BaseWidget baseWidget && baseWidget.focused()) {
                screen.unfocusWidget(editWidget);
            }
            removeChild(editWidget);
            editWidget = null;
        }
        editingIndex = -1;
        inlineEditHadFocus = false;
    }

    private void scrollRowIntoView(int index) {
        if (index < 0 || index >= items.size()) return;
        double rowTop = index * tagRowStride();
        double rowBottom = rowTop + TAG_HEIGHT;
        double listAreaH = visibleListViewportHeight();
        if (rowTop < listScrollOffset) {
            listScrollOffset = rowTop;
        }
        if (rowBottom > listScrollOffset + listAreaH) {
            listScrollOffset = rowBottom - listAreaH;
        }
        syncScrollbar();
    }

    private void startInlineEdit(int index) {
        if (index < 0 || index >= items.size()) return;
        cancelInlineEdit();
        scrollRowIntoView(index);
        editingIndex = index;
        double listW = listInnerWidth(width());
        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double rowY = listContentTop + index * tagRowStride() - listScrollOffset;
        String label = formatItemLabel(items.get(index));
        switch (itemType) {
            case TEXT:
            case ENUM:
            case BOOLEAN: {
                InputWidget input = new InputWidget(screen);
                input.bounds(new ScreenCoordinate(0, rowY, listW, TAG_HEIGHT));
                input.maxLength(itemType == ItemType.TEXT ? 64 : 128);
                input.value(label);
                editWidget = input;
                break;
            }
            case NUMBER: {
                NumericInputWidget num = new NumericInputWidget(screen);
                num.bounds(new ScreenCoordinate(0, rowY, listW, TAG_HEIGHT));
                num.integerOnly(listNumberIntegerOnly);
                num.decimalPlaces(listNumberIntegerOnly ? 0 : listNumberDecimalPlaces);
                if (listNumberMin != null) {
                    num.minValue(listNumberMin);
                }
                if (listNumberMax != null) {
                    num.maxValue(listNumberMax);
                }
                num.value(label.isEmpty() ? "0" : label);
                editWidget = num;
                break;
            }
            default:
                editingIndex = -1;
                return;
        }
        if (screen != null && editWidget != null) {
            editWidget.applyTheme(screen.getEffectiveTheme());
        }
        addChild(editWidget);
        if (screen != null) {
            screen.requestFocus(editWidget.getFocusTarget());
        }
    }

    private void commitInlineEdit() {
        if (editingIndex < 0 || editWidget == null || editingIndex >= items.size()) {
            cancelInlineEdit();
            return;
        }
        Object parsed = null;
        switch (itemType) {
            case TEXT: {
                String v = ((InputWidget) editWidget).value();
                if (v == null) v = "";
                parsed = v.trim();
                break;
            }
            case ENUM: {
                String v = ((InputWidget) editWidget).value();
                if (v == null || v.trim().isEmpty()) {
                    cancelInlineEdit();
                    return;
                }
                parsed = v.trim();
                break;
            }
            case BOOLEAN: {
                String v = ((InputWidget) editWidget).value();
                if (v == null || v.trim().isEmpty()) {
                    cancelInlineEdit();
                    return;
                }
                parsed = Boolean.parseBoolean(v.trim());
                break;
            }
            case NUMBER: {
                NumericInputWidget num = (NumericInputWidget) editWidget;
                String v = num.value();
                if (v == null || v.trim().isEmpty()) {
                    cancelInlineEdit();
                    return;
                }
                parsed = num.getParsedValue();
                break;
            }
            default:
                cancelInlineEdit();
                return;
        }
        items.set(editingIndex, parsed);
        exitUndoModeIfNeeded();
        syncScrollbar();
        fireListChanged();
        cancelInlineEdit();
    }

    private void copyRowToClipboard(int index) {
        if (index < 0 || index >= items.size()) return;
        String s = formatItemLabel(items.get(index));
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
        Notification n = Notification.ofComponent(BaniraComponent.get().transClientAuto("tag_list_copied"));
        NotificationManager.get().addNotification(n);
    }

    /**
     * @return 命中列表项正文区域（不含删除按钮）的索引，否则 -1
     */
    private int hitTagBodyIndex(double mouseX, double mouseY) {
        double absX = absoluteX();
        double absY = absoluteY();
        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listAreaHeight = visibleListViewportHeight();
        if (mouseY < absY + listContentTop || mouseY >= absY + listContentTop + listAreaHeight) {
            return -1;
        }
        int closeX = getDeleteButtonX();
        int bodyRight = (int) (absX + closeX - TAG_PAD);
        if (mouseX < absX + TAG_PAD || mouseX >= bodyRight) {
            return -1;
        }
        double relY = mouseY - (absY + listContentTop - listScrollOffset);
        int i = (int) (relY / tagRowStride());
        if (i < 0 || i >= items.size()) {
            return -1;
        }
        double tagTop = absY + listContentTop + i * tagRowStride() - listScrollOffset;
        if (mouseY < tagTop || mouseY >= tagTop + TAG_HEIGHT) {
            return -1;
        }
        return i;
    }

    private void onClearOrUndoClicked() {
        cancelInlineEdit();
        if (undoMode) {
            items.clear();
            items.addAll(lastClearedItems);
            lastClearedItems.clear();
            undoMode = false;
            clearButton.presetStyle(ButtonWidget.PresetStyle.MINUS);
            if (clearButtonTooltip != null) {
                clearButtonTooltip.text(BaniraComponent.get().transClientAuto("tag_list_clear_tooltip"));
            }
        } else {
            lastClearedItems.clear();
            lastClearedItems.addAll(items);
            items.clear();
            undoMode = true;
            clearButton.presetStyle(ButtonWidget.PresetStyle.ARROW_LEFT);
            if (clearButtonTooltip != null) {
                clearButtonTooltip.text(BaniraComponent.get().transClientAuto("tag_list_undo_tooltip"));
            }
        }
        listScrollOffset = 0;
        syncScrollbar();
        updateBoundsHeight();
        fireListChanged();
    }

    public TagListEditorWidget items(List<Object> items) {
        cancelInlineEdit();
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        listScrollOffset = 0;
        syncScrollbar();
        updateBoundsHeight();
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
        if (editWidget != null) editWidget.applyTheme(theme);
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) return;
        ensureChildren();

        BaniraColorConfig theme = screen != null ? screen.getEffectiveTheme() : BaniraColorConfig.forSeason(EnumSeason.AUTO);

        double ox = x();
        double oy = y();
        double w = width();
        double absX = absoluteX();
        double absY = absoluteY();

        double titleBtnLeft = layoutHeaderToolbar(w);

        stack.pushPose();
        stack.translate(ox, oy, 0);

        // region 绘制标题栏
        int headerBg = mouseInside ? theme.bgTertiary() : theme.bgSecondary();
        AbstractGuiUtils.fill(stack, 0, 0, (int) w, HEADER_HEIGHT, headerBg);

        int headerTextColor = theme.textPrimary();
        int arrowX = 4;
        int arrowY = (HEADER_HEIGHT - ARROW_SIZE) / 2;
        if (expanded) {
            drawArrowDown(stack, arrowX, arrowY, ARROW_SIZE, headerTextColor);
        } else {
            drawArrowRight(stack, arrowX, arrowY, ARROW_SIZE, headerTextColor);
        }

        int textX = 4 + ARROW_SIZE + 4;
        int textY = (HEADER_HEIGHT - 9) / 2;
        String baseTitle = text != null ? text.content() : "";
        String displayTitle = baseTitle.isEmpty() ? "(" + items.size() + ")" : baseTitle + " (" + items.size() + ")";
        FontDrawArgs args = FontDrawArgs.of(
                Text.literal(displayTitle).stack(stack).font(screen != null ? screen.getFont() : AbstractGuiUtils.getFont()).color(headerTextColor));
        args.x(textX).y(textY).maxWidth((int) Math.max(0, titleBtnLeft - textX - 4)).wrap(false).inScreen(false);
        LabelWidget.drawLimitedText(args);

        stack.popPose();
        // endregion 标题栏

        if (!expanded) {
            clearButton.visible(false);
            addButton.visible(false);
            scrollbar.visible(false);
            if (addInputWidget != null) addInputWidget.visible(false);
            if (addConfirmButton != null) addConfirmButton.visible(false);
            if (editWidget != null) editWidget.visible(false);
            return;
        }

        clearButton.visible(true);
        addButton.visible(true);

        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listAreaHeight = visibleListViewportHeight();
        boolean showScrollbar = listShowsScrollbar();
        double listW = listInnerWidth(w);

        if (addInputWidget != null) {
            addInputWidget.visible(true);
        }
        if (addConfirmButton != null) {
            addConfirmButton.bounds(new ScreenCoordinate(headerAddButtonX, HEADER_HEIGHT + 2, BTN_SIZE, ADD_INPUT_HEIGHT));
            addConfirmButton.visible(true);
        }

        syncScrollbar();
        scrollbar.visible(showScrollbar);
        if (showScrollbar) {
            scrollbar.bounds(new ScreenCoordinate(listW + SCROLL_GAP, listContentTop, SCROLLBAR_WIDTH, listAreaHeight));
        } else {
            scrollbar.bounds(new ScreenCoordinate(0, listContentTop, 0, 0));
        }

        double listAbsX = absX;
        double listAbsY = absY + listContentTop;
        scrollbar.clearScrollHoverAreas();
        if (showScrollbar) {
            scrollbar.addScrollHoverArea(new ScreenCoordinate(listAbsX, listAbsY, listW, listAreaHeight));
        }

        // region 绘制标签列表
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

        Font font = Minecraft.getInstance().font;
        int tagW = (int) listW;
        int textMaxW = tagW - TAG_PAD * 2 - TAG_CLOSE_SIZE - TAG_PAD;
        int closeX = tagW - TAG_PAD - TAG_CLOSE_SIZE;
        VisibleTagRange visibleRange = visibleTagRange(listAreaHeight);
        for (int i = visibleRange.first; i <= visibleRange.last; i++) {
            Object item = items.get(i);
            String label = formatItemLabel(item);
            String display = font.plainSubstrByWidth(label, textMaxW);
            double tagY = i * tagRowStride();

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

        if (editWidget != null && expanded) {
            double rowY = listContentTop + editingIndex * tagRowStride() - listScrollOffset;
            ((BaseWidget) editWidget).bounds(new ScreenCoordinate(0, rowY, listW, TAG_HEIGHT));
            editWidget.visible(true);
        }

        renderChildren(stack, partialTicks);

        if (expanded && editingIndex < 0 && hoveredTagBodyIndex >= 0 && screen != null
                && !(screen instanceof BaniraScreen && screen.isAnyDropdownSelectOpen())) {
            int mx = (int) screen.inputState().mouseX();
            int my = (int) screen.inputState().mouseY();
            final EnumSeason rowSeason = screen.season() != null ? screen.season() : EnumSeason.AUTO;
            Text rowHint = Text.from(BaniraComponent.get().transClientAuto("tag_list_row_hint"));
            final BaniraColorConfig tooltipTheme = theme;
            screen.addDeferredTooltipRender(s -> {
                s.pushPose();
                s.last().pose().setIdentity();
                TooltipWidget.drawPopupMessage(s, FontDrawArgs.ofPopo(rowHint.stack(s)).x(mx).y(my).popupUseTexture(tooltipTheme.tooltipUseTexture()), tooltipTheme, rowSeason);
                s.popPose();
            });
        }
    }

    private String formatItemLabel(Object item) {
        if (item == null) return "";
        if (item instanceof Boolean b) return b ? "true" : "false";
        if (item instanceof BigDecimal bd) return bd.toPlainString();
        if (item instanceof Number n) {
            double d = n.doubleValue();
            if (d == (long) d) return String.valueOf((long) d);
            return BigDecimal.valueOf(d).toPlainString();
        }
        return String.valueOf(item);
    }

    private void drawArrowDown(PoseStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 90, color);
    }

    private void drawArrowRight(PoseStack stack, int x, int y, int size, int color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.35f;
        AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 0, color);
    }

    private void syncScrollbar() {
        if (scrollbar == null) return;
        double listAreaH = visibleListViewportHeight();
        double totalH = tagsContentHeight();
        if (!listShowsScrollbar()) {
            listScrollOffset = 0;
        }
        double maxScroll = Math.max(0, totalH - listAreaH);
        scrollbar.maxValue(maxScroll);
        scrollbar.visibleSize(Math.max(1, listAreaH));
        scrollbar.setValue(Math.min(listScrollOffset, maxScroll));
        listScrollOffset = scrollbar.value();
    }

    @Override
    public void update() {
        if (!visible || !enabled) return;
        ensureChildren();
        updateBoundsHeight();
        updateInteractiveState();
        double absX = absoluteX();
        double absY = absoluteY();
        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        int closeX = getDeleteButtonX();
        double listAreaHeight = visibleListViewportHeight();
        hoveredDeleteIndex = -1;
        hoveredTagBodyIndex = -1;
        double mx = screen != null ? screen.inputState().mouseX() : 0;
        double my = screen != null ? screen.inputState().mouseY() : 0;
        if (expanded && editingIndex < 0 && screen != null) {
            int bodyIdx = hitTagBodyIndex(mx, my);
            if (bodyIdx >= 0) {
                hoveredTagBodyIndex = bodyIdx;
            }
        }
        VisibleTagRange visibleRange = visibleTagRange(listAreaHeight);
        for (int i = visibleRange.first; i <= visibleRange.last; i++) {
            double tagY = listContentTop - listScrollOffset + i * tagRowStride();
            double delX = absX + closeX;
            double delY = absY + tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2.0;
            if (mx >= delX && mx < delX + TAG_CLOSE_SIZE && my >= delY && my < delY + TAG_CLOSE_SIZE) {
                hoveredDeleteIndex = i;
                break;
            }
        }

        if (expanded) {
            updateChildren();
        }

        // region 行内编辑失焦则取消（恢复原列表显示，等同 Esc，不提交）
        if (expanded && editingIndex >= 0 && editWidget instanceof BaseWidget eb) {
            if (eb.focused()) {
                inlineEditHadFocus = true;
            } else if (inlineEditHadFocus) {
                cancelInlineEdit();
            }
        }
        // endregion 行内编辑失焦则取消

        if (addingMode && addInputWidget != null && addConfirmButton != null) {
            boolean inputFocused = addInputWidget instanceof BaseWidget baseWidget && baseWidget.focused();
            boolean confirmFocused = addConfirmButton.focused();
            boolean dropdownOpen = addInputWidget instanceof DropdownSelectWidget selectWidget
                    && selectWidget.dropdownOpen();
            boolean previewExpanded = addInputWidget instanceof DropdownSelectWidget selectWidget
                    && selectWidget.previewExpanded();
            boolean inlineEditing = editingIndex >= 0 && editWidget instanceof BaseWidget baseWidget && baseWidget.focused();
            if (!inputFocused && !confirmFocused && !dropdownOpen && !previewExpanded && !inlineEditing) {
                exitAddingMode();
            }
        }
    }

    private int getDeleteButtonX() {
        double listW = listInnerWidth(width());
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
            IWidget handlingChild = findHandlingChild(child -> child.handleMouseClick(event));
            if (handlingChild != null) {
                if (handlingChild == addInputWidget || handlingChild == addConfirmButton) {
                    lastClickFocusTarget = handlingChild == addConfirmButton ? addConfirmButton : addInputWidget.getFocusTarget();
                } else if (handlingChild == editWidget) {
                    lastClickFocusTarget = editWidget.getFocusTarget();
                }
                return true;
            }
        }

        if (relY < HEADER_HEIGHT && event.button() == 0) {
            cancelInlineEdit();
            expanded = !expanded;
            updateBoundsHeight();
            if (onExpandChanged != null) onExpandChanged.accept(this);
            return true;
        }

        if (!expanded) return false;

        double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
        double listAreaHeight = visibleListViewportHeight();
        if (relY >= listContentTop && relY < listContentTop + listAreaHeight) {
            int bodyIdx = hitTagBodyIndex(mouseX, mouseY);
            if (event.button() == 1 && bodyIdx >= 0) {
                copyRowToClipboard(bodyIdx);
                return true;
            }
            if (event.button() == 0 && bodyIdx >= 0) {
                if (isDoubleClick(event)) {
                    startInlineEdit(bodyIdx);
                    // BaniraScreen 在 handleMouseClick 返回后会对「被点击的根 widget」再 requestFocus(getFocusTarget())；
                    // 若不设置 lastClickFocusTarget，getFocusTarget() 会落到本控件自身，抢走行内输入框焦点。
                    if (editWidget != null) {
                        lastClickFocusTarget = editWidget.getFocusTarget();
                    }
                }
                return true;
            }
        }
        if (relY >= listContentTop && relY < listContentTop + listAreaHeight && event.button() == 0) {
            VisibleTagRange visibleRange = visibleTagRange(listAreaHeight);
            int closeX = getDeleteButtonX();
            for (int i = visibleRange.first; i <= visibleRange.last; i++) {
                double tagY = listContentTop - listScrollOffset + i * tagRowStride();
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
            if (findHandlingChild(child -> child.handleMouseRelease(event)) != null) {
                return true;
            }
        }

        if (pressedDeleteIndex >= 0 && event.button() == 0) {
            double listContentTop = HEADER_HEIGHT + (addingMode ? ADD_INPUT_HEIGHT + 4 : 0);
            double absX = absoluteX();
            double absY = absoluteY();
            int idx = pressedDeleteIndex;
            int closeX = getDeleteButtonX();
            double tagY = listContentTop - listScrollOffset + idx * tagRowStride();
            double delX = absX + closeX;
            double delY = absY + tagY + (TAG_HEIGHT - TAG_CLOSE_SIZE) / 2.0;
            double mouseX = event.mouseX();
            double mouseY = event.mouseY();
            if (mouseX >= delX && mouseX < delX + TAG_CLOSE_SIZE && mouseY >= delY && mouseY < delY + TAG_CLOSE_SIZE) {
                if (idx >= 0 && idx < items.size()) {
                    items.remove(idx);
                    exitUndoModeIfNeeded();
                    syncScrollbar();
                    updateBoundsHeight();
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
            if (findHandlingChild(child -> child.handleMouseScroll(event)) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(KeyEvent event) {
        if (!visible || !enabled || event == null) return false;
        int keyCode = event.keyCode();
        if (editingIndex >= 0 && editWidget != null && editWidget instanceof BaseWidget baseWidget && baseWidget.focused()) {
            if (keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
                cancelInlineEdit();
                return true;
            }
            if (keyCode == GLFWKey.GLFW_KEY_ENTER || keyCode == GLFWKey.GLFW_KEY_KP_ENTER) {
                commitInlineEdit();
                return true;
            }
        }
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
            if (findHandlingChild(child -> child.handleKeyPress(event)) != null) {
                return true;
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
