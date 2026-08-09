package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.enums.IEnumDropdownIcon;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static xin.vanilla.banira.client.data.BaniraColorToken.*;

/**
 * 下拉选择 Widget。拥有 InputWidget 的输入特性，支持：
 * <ul>
 *   <li>点击时在上方或下方显示下拉选项</li>
 *   <li>下拉展开时输入框可输入内容进行智能过滤</li>
 *   <li>点击选择项时不会使下方控件响应</li>
 *   <li>选项过多时支持滚轮滚动</li>
 *   <li>可配置单选/多选，多选用逗号分隔</li>
 *   <li>多选时点击标签项（排除删除按钮）可打开预览列表；点击下拉箭头或外部区域关闭</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * DropdownSelectWidget dropdown = new DropdownSelectWidget(this);
 * dropdown.bounds(new ScreenCoordinate(10, 10, 200, 24));
 * dropdown.options(Arrays.asList("选项A", "选项B", "选项C"));
 * // 或带图标与提示：dropdown.optionEntries(List.of(
 * //     new DropdownOption("a", new ItemStack(Items.APPLE), BaniraComponent.get().transClientAuto("tip_a"))));
 * // 或从枚举：dropdown.optionsEnum(MyEnum.class);  // 枚举可实现 IEnumDescribable / IEnumDropdownIcon
 * dropdown.multiSelect(true);  // 可选，默认单选
 * dropdown.selectedValues(Collections.singletonList("选项A"));
 * dropdown.onSelectionChanged(values -> { ... });
 * addWidget(dropdown);
 * }</pre>
 */
@Accessors(chain = true, fluent = true)
public class DropdownSelectWidget extends InputWidget {

    private static final String MULTI_SEPARATOR = ", ";
    private static final int DROPDOWN_ARROW_WIDTH = 16;
    private static final int CLEAR_BUTTON_SIZE = 10;
    private static final int CLEAR_BUTTON_RADIUS = 4;
    private static final int DROPDOWN_MAX_HEIGHT = 150;
    private static final int DROPDOWN_ITEM_HEIGHT = 20;
    private static final int DROPDOWN_PAD = 4;
    private static final int TAG_PAD = 4;
    private static final int TAG_GAP = 4;
    private static final int TAG_CLOSE_SIZE = 10;
    private static final int TAG_MIN_HEIGHT = 16;
    private static final int TAG_RIGHT_MARGIN = 4;
    /**
     * 下拉项左侧图标区
     */
    public static final int DROPDOWN_ICON_INSET = 2;
    /**
     * 下拉项左侧图标绘制边长
     */
    public static final int DROPDOWN_ICON_DRAW_SIZE = 16;
    /**
     * 图标右缘与文字左缘之间的间距
     */
    public static final int DROPDOWN_ICON_TEXT_GAP = 4;
    /**
     * 下拉项左侧图标列总宽度（含内边距与文字间距）
     */
    public static final int DROPDOWN_ICON_COLUMN =
            DROPDOWN_ICON_INSET + DROPDOWN_ICON_DRAW_SIZE + DROPDOWN_ICON_TEXT_GAP;

    private List<DropdownOption> optionEntries = new ArrayList<>();
    private List<DropdownOption> cachedFilteredOptionEntries = new ArrayList<>();
    @Nullable
    private String cachedFilterText;
    private boolean hasDropdownIconCache;

    @Getter
    @Setter
    private boolean multiSelect = false;

    /** 默认严格选择；EDITABLE 模式允许提交预设列表之外的单个值。 */
    @Getter
    @Setter
    private DropdownInputMode inputMode = DropdownInputMode.SELECTION_ONLY;

    @Getter
    @Setter
    @Nullable
    private Consumer<List<String>> onSelectionChanged;

    @Getter
    private boolean dropdownOpen = false;

    @Getter
    private int dropdownScrollOffset = 0;

    private List<String> selectedValues = new ArrayList<>();
    private boolean customValueDirty;

    @Nullable
    private DropdownPreviewOverlayWidget previewOverlayWidget;
    private boolean previewExpanded = false;
    private int tagScrollOffset = 0;

    /**
     * 关闭下拉时排除的点击区域（如相邻的添加按钮），点击这些区域时不关闭且不消费事件
     */
    @Nullable
    private Supplier<List<ScreenCoordinate>> excludedCloseAreasSupplier;

    boolean previewExpanded() {
        return previewExpanded;
    }

    void closePreview() {
        if (!previewExpanded) return;
        previewExpanded = false;
        previewScrollOffset = 0;
        if (previewOverlayWidget != null && screen != null) {
            screen.removeOverlayWidget(previewOverlayWidget);
            previewOverlayWidget = null;
        }
    }

    private int previewScrollOffset = 0;
    private final List<TagLayout> cachedTagLayouts = new ArrayList<>();
    private boolean tagLayoutDirty = true;
    private Font cachedTagLayoutFont;
    private int cachedTagTotalWidth;

    int getPreviewScrollOffset() {
        return previewScrollOffset;
    }

    void setPreviewScrollOffset(int offset) {
        this.previewScrollOffset = Math.max(0, offset);
    }

    void removeSelectedValueAt(int index) {
        if (index >= 0 && index < selectedValues.size()) {
            selectedValues.remove(index);
            invalidateTagLayoutCache();
            clampTagScrollOffset();
            updateDisplayValue();
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(getSelectedValues());
            }
            if (selectedValues.isEmpty()) {
                closePreview();
            }
        }
    }

    private void clampTagScrollOffset() {
        int cw = getTagContentWidth();
        if (cw <= 0) {
            tagScrollOffset = 0;
            return;
        }
        Font font = AbstractGuiUtils.getFont();
        int maxScroll = Math.max(0, tagTotalWidth(font) - cw);
        tagScrollOffset = Math.max(0, Math.min(maxScroll, tagScrollOffset));
    }

    private static final int PREVIEW_GAP = 2;
    private static final int PREVIEW_ITEM_HEIGHT = 24;
    private static final int PREVIEW_MAX_HEIGHT = 200;
    /**
     * 下拉/预览列表与屏幕边缘的最小间距
     */
    private static final int SCREEN_EDGE_MARGIN = 4;

    /**
     * 获取预览浮层边界。智能选择上方或下方（哪边距屏幕边缘更远选哪边），
     * 且严格限制在可用范围内：上方时为屏幕顶部至输入框顶部，下方时为输入框底部至屏幕底部，不遮挡输入框。
     * 与屏幕边缘保留 {@link #SCREEN_EDGE_MARGIN} 间距。
     */
    ScreenCoordinate getPreviewBounds() {
        if (renderCoordinate == null || screen == null) return null;
        KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getScreenSize();
        int sw = screenSize.key();
        int sh = screenSize.val();

        double absX = absoluteX();
        double absY = absoluteY();
        double inputBottom = absY + renderCoordinate.height();
        int w = (int) Math.max(renderCoordinate.width(), 120);
        int itemCount = Math.min(selectedValues.size(), 8);
        int desiredH = Math.min(PREVIEW_MAX_HEIGHT, itemCount * PREVIEW_ITEM_HEIGHT + 12);

        double spaceAbove = absY - SCREEN_EDGE_MARGIN;
        double spaceBelow = sh - inputBottom - SCREEN_EDGE_MARGIN;
        boolean showAbove = spaceAbove >= spaceBelow;
        int availableH = (int) (showAbove ? spaceAbove - PREVIEW_GAP : spaceBelow - PREVIEW_GAP);
        int h = Math.min(desiredH, Math.max(0, availableH));

        double previewY = showAbove ? absY - h - PREVIEW_GAP : inputBottom + PREVIEW_GAP;
        double previewX = Math.max(SCREEN_EDGE_MARGIN, Math.min(absX, sw - w - SCREEN_EDGE_MARGIN));
        return new ScreenCoordinate(previewX, previewY, w, h);
    }

    /**
     * 设置下拉滚动偏移
     */
    void setDropdownScrollOffset(int offset) {
        int maxScroll = getMaxDropdownScroll();
        this.dropdownScrollOffset = Math.max(0, Math.min(maxScroll, offset));
    }

    /**
     * 获取下拉最大滚动偏移
     */
    int getMaxDropdownScroll() {
        ScreenCoordinate db = getDropdownBounds();
        if (db == null) return 0;
        int contentHeight = filteredOptionEntriesView().size() * DROPDOWN_ITEM_HEIGHT;
        int visibleHeight = (int) db.height() - DROPDOWN_PAD * 2;
        return Math.max(0, contentHeight - visibleHeight);
    }

    @Nullable
    private DropdownOverlayWidget overlayWidget;
    /**
     * 按下时的区域：0=无，1=清空，2=箭头，3=输入区。抬起时需在同一区域才触发
     */
    private int pressedArea = 0;

    public DropdownSelectWidget(BaniraScreen screen) {
        super(screen);
        showClearButton(false);
    }

    public DropdownSelectWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
        showClearButton(false);
    }

    /**
     * 当前全部选项条目（值、图标、提示）。
     */
    public List<DropdownOption> optionEntries() {
        return new ArrayList<>(optionEntries);
    }

    /**
     * 设置选项条目（含图标与悬浮提示）。
     */
    public DropdownSelectWidget optionEntries(List<DropdownOption> entries) {
        this.optionEntries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        rebuildOptionCaches();
        return this;
    }

    /**
     * 仅字符串选项，无图标与提示。
     */
    public DropdownSelectWidget options(List<String> strings) {
        if (strings == null) {
            this.optionEntries = new ArrayList<>();
        } else {
            this.optionEntries = strings.stream().map(DropdownOption::new).collect(Collectors.toList());
        }
        rebuildOptionCaches();
        return this;
    }

    /**
     * 与 {@link #options(List)} 对应的纯文本值列表。
     */
    public List<String> options() {
        return optionEntries.stream().map(DropdownOption::value).collect(Collectors.toList());
    }

    /**
     * 从枚举构建选项：{@code name()} 为值；若实现 {@link IEnumDropdownIcon} 则绘制左侧图标；
     * 若实现 {@link IEnumDescribable} 则列表与输入框显示其描述文案，且仍设置 {@link DropdownOption#tooltip()}，
     * 下拉浮层悬停时会显示与原先一致的悬浮提示。
     */
    public DropdownSelectWidget optionsEnum(Class<? extends Enum<?>> clazz) {
        Enum<?>[] constants = clazz.getEnumConstants();
        if (constants == null) {
            this.optionEntries = new ArrayList<>();
            rebuildOptionCaches();
            return this;
        }
        List<DropdownOption> list = new ArrayList<>();
        for (Enum<?> e : constants) {
            ItemStack icon = ItemStack.EMPTY;
            Texture tex = null;
            if (e instanceof IEnumDropdownIcon id) {
                ResourceLocation rl = id.dropdownTextureLocation();
                if (rl != null) {
                    tex = Texture.of(rl);
                }
                icon = id.dropdownIcon();
            }
            Texture[] textures = tex != null ? new Texture[]{tex} : null;
            Component dEnum = null;
            if (e instanceof IEnumDescribable id) {
                dEnum = id.enumDescription();
            }
            String dispLabel = null;
            if (dEnum != null && !dEnum.isEmpty()) {
                dispLabel = dEnum.getString(Translator.getClientLanguage());
            }
            Component tooltip = (dEnum != null && !dEnum.isEmpty()) ? dEnum : null;
            list.add(new DropdownOption(e.name(), dispLabel, icon, textures, tooltip));
        }
        this.optionEntries = list;
        rebuildOptionCaches();
        return this;
    }

    /**
     * 是否存在任意带图标的选项（用于为整列预留图标宽度）。
     */
    public boolean hasAnyDropdownIcon() {
        return hasDropdownIconCache;
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
    }

    /**
     * 设置选中的值。单选传入单元素列表，多选传入多个。
     */
    public DropdownSelectWidget selectedValues(List<String> values) {
        this.selectedValues = values != null ? new ArrayList<>(values) : new ArrayList<>();
        invalidateTagLayoutCache();
        this.customValueDirty = false;
        updateDisplayValue();
        return this;
    }

    /**
     * 获取当前选中的值列表
     */
    public List<String> getSelectedValues() {
        return new ArrayList<>(selectedValues);
    }

    List<String> selectedValuesView() {
        return selectedValues;
    }

    /**
     * 获取过滤后的选项列表（根据当前输入内容）
     */
    public List<String> getFilteredOptions() {
        return filteredOptionEntriesView().stream().map(DropdownOption::value).collect(Collectors.toList());
    }

    /**
     * 过滤后的选项条目（与 {@link #getFilteredOptions()} 顺序一致）。
     */
    public List<DropdownOption> getFilteredOptionEntries() {
        return new ArrayList<>(filteredOptionEntriesView());
    }

    /**
     * 内部浮层只读使用，避免每帧重复分配过滤列表拷贝。
     */
    List<DropdownOption> filteredOptionEntriesView() {
        String filter = value().toLowerCase(Locale.ROOT).trim();
        if (filter.equals(cachedFilterText)) {
            return cachedFilteredOptionEntries;
        }
        cachedFilterText = filter;
        if (StringUtils.isNullOrEmpty(filter)) {
            cachedFilteredOptionEntries = new ArrayList<>(optionEntries);
            return cachedFilteredOptionEntries;
        }
        cachedFilteredOptionEntries = optionEntries.stream()
                .filter(opt -> opt.displayLabelLowerCase().contains(filter)
                        || opt.valueLowerCase().contains(filter))
                .collect(Collectors.toList());
        return cachedFilteredOptionEntries;
    }

    private void rebuildOptionCaches() {
        cachedFilterText = null;
        cachedFilteredOptionEntries = new ArrayList<>();
        hasDropdownIconCache = optionEntries.stream().anyMatch(o -> o.hasTexture() || !o.icon().isEmpty());
        invalidateTagLayoutCache();
        dropdownScrollOffset = Math.min(dropdownScrollOffset, getMaxDropdownScroll());
    }

    String displayLabelForValue(String storedValue) {
        for (DropdownOption o : optionEntries) {
            if (o.value().equals(storedValue)) {
                return o.displayLabel();
            }
        }
        return storedValue;
    }

    private void invalidateTagLayoutCache() {
        tagLayoutDirty = true;
    }

    /**
     * 多选标签布局缓存。统一使用显示文本测宽，避免 render/命中/滚轮各算一遍。
     */
    private List<TagLayout> tagLayouts(Font font) {
        if (!tagLayoutDirty && cachedTagLayoutFont == font) {
            return cachedTagLayouts;
        }
        cachedTagLayouts.clear();
        cachedTagLayoutFont = font;
        cachedTagTotalWidth = 0;
        for (int i = 0; i < selectedValues.size(); i++) {
            String value = selectedValues.get(i);
            String label = displayLabelForValue(value);
            int textWidth = font.width(label);
            int tagWidth = TAG_PAD + textWidth + TAG_PAD + TAG_CLOSE_SIZE + TAG_PAD;
            cachedTagLayouts.add(new TagLayout(i, label, tagWidth));
            cachedTagTotalWidth += tagWidth + TAG_GAP;
        }
        if (!cachedTagLayouts.isEmpty()) {
            cachedTagTotalWidth -= TAG_GAP;
        }
        tagLayoutDirty = false;
        return cachedTagLayouts;
    }

    private int tagTotalWidth(Font font) {
        tagLayouts(font);
        return cachedTagTotalWidth;
    }

    private static final class TagLayout {
        final int index;
        final String label;
        final int tagWidth;

        private TagLayout(int index, String label, int tagWidth) {
            this.index = index;
            this.label = label;
            this.tagWidth = tagWidth;
        }
    }

    private static final int DROPDOWN_GAP = 2;

    /**
     * 获取下拉框的屏幕坐标。智能选择上方或下方（哪边距屏幕边缘更远选哪边），
     * 且严格限制在可用范围内：上方时为屏幕顶部至输入框顶部，下方时为输入框底部至屏幕底部，不遮挡输入框。
     * 与屏幕边缘保留 {@link #SCREEN_EDGE_MARGIN} 间距。
     */
    public ScreenCoordinate getDropdownBounds() {
        if (renderCoordinate == null || screen == null) return null;
        KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getScreenSize();
        int sw = screenSize.key();
        int sh = screenSize.val();

        double absX = absoluteX();
        double absY = absoluteY();
        double inputBottom = absY + renderCoordinate.height();
        int w = (int) Math.max(renderCoordinate.width(), 100);
        int itemCount = Math.min(filteredOptionEntriesView().size(), 10);
        int desiredH = Math.min(DROPDOWN_MAX_HEIGHT, itemCount * DROPDOWN_ITEM_HEIGHT + DROPDOWN_PAD * 2);

        double spaceAbove = absY - SCREEN_EDGE_MARGIN;
        double spaceBelow = sh - inputBottom - SCREEN_EDGE_MARGIN;
        boolean showAbove = spaceAbove >= spaceBelow;
        int availableH = (int) (showAbove ? spaceAbove - DROPDOWN_GAP : spaceBelow - DROPDOWN_GAP);
        int h = Math.min(desiredH, Math.max(0, availableH));

        double dropY = showAbove ? absY - h - DROPDOWN_GAP : inputBottom + DROPDOWN_GAP;
        double dropX = Math.max(SCREEN_EDGE_MARGIN, Math.min(absX, sw - w - SCREEN_EDGE_MARGIN));
        return new ScreenCoordinate(dropX, dropY, w, h);
    }

    /**
     * 获取输入框区域（不含下拉箭头）的屏幕坐标
     */
    public ScreenCoordinate getInputBounds() {
        if (renderCoordinate == null) return null;
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width() - DROPDOWN_ARROW_WIDTH;
        int h = (int) renderCoordinate.height();
        return new ScreenCoordinate(absX, absY, w, h);
    }

    /**
     * 获取下拉箭头区域的屏幕坐标
     */
    public ScreenCoordinate getDropdownArrowBounds() {
        if (renderCoordinate == null) return null;
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        return new ScreenCoordinate(absX + w - DROPDOWN_ARROW_WIDTH, absY, DROPDOWN_ARROW_WIDTH, h);
    }

    /**
     * 打开下拉框
     */
    public void openDropdown() {
        if (dropdownOpen) return;
        dropdownOpen = true;
        if (inputMode == DropdownInputMode.SELECTION_ONLY) {
            value("");
        }
        dropdownScrollOffset = 0;
        if (screen != null) {
            overlayWidget = new DropdownOverlayWidget(screen, this);
            screen.addOverlayWidget(overlayWidget);
            screen.requestFocus(this);
        }
    }

    /**
     * 关闭下拉框
     */
    public void closeDropdown() {
        if (!dropdownOpen) return;
        if (inputMode == DropdownInputMode.EDITABLE && customValueDirty) {
            syncCustomValue();
        } else {
            updateDisplayValue();
        }
        dropdownOpen = false;
        if (overlayWidget != null && screen != null) {
            screen.removeOverlayWidget(overlayWidget);
            overlayWidget = null;
        }
        if (screen != null) {
            screen.requestFocus(this);
        }
    }

    /**
     * 检查选项是否已选中
     */
    public boolean isOptionSelected(String option) {
        return selectedValues.contains(option);
    }

    /**
     * 选择某个选项（由 Overlay 调用）。多选时不实时更新输入框，避免与过滤冲突，通过高亮已选项体现选择状态。
     * 单选时再次点击当前选择项会清空选择。
     */
    public void selectOption(String option) {
        if (multiSelect) {
            if (selectedValues.contains(option)) {
                selectedValues.remove(option);
            } else {
                selectedValues.add(option);
            }
            invalidateTagLayoutCache();
            clampTagScrollOffset();
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(getSelectedValues());
            }
        } else {
            customValueDirty = false;
            if (selectedValues.contains(option)) {
                selectedValues.clear();
            } else {
                selectedValues.clear();
                selectedValues.add(option);
            }
            invalidateTagLayoutCache();
            updateDisplayValue();
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(getSelectedValues());
            }
            closeDropdown();
        }
    }

    /**
     * 处理下拉区域的滚动
     */
    public boolean handleDropdownScroll(double mouseX, double mouseY, double delta) {
        if (!dropdownOpen || overlayWidget == null) return false;
        ScreenCoordinate db = getDropdownBounds();
        if (db == null) return false;
        if (mouseX < db.x() || mouseX >= db.x() + db.width() ||
                mouseY < db.y() || mouseY >= db.y() + db.height()) {
            return false;
        }
        int contentHeight = filteredOptionEntriesView().size() * DROPDOWN_ITEM_HEIGHT;
        int visibleHeight = (int) db.height() - DROPDOWN_PAD * 2;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        int step = DROPDOWN_ITEM_HEIGHT;
        if (delta > 0) {
            dropdownScrollOffset = Math.max(0, dropdownScrollOffset - step);
        } else {
            dropdownScrollOffset = Math.min(maxScroll, dropdownScrollOffset + step);
        }
        return true;
    }

    /**
     * 设置关闭下拉时排除的点击区域，点击这些区域时不关闭且不消费事件
     */
    public DropdownSelectWidget excludedCloseAreasSupplier(@Nullable Supplier<List<ScreenCoordinate>> supplier) {
        this.excludedCloseAreasSupplier = supplier;
        return this;
    }

    /**
     * 检查点击是否在排除区域内
     */
    boolean isInExcludedCloseArea(double mouseX, double mouseY) {
        if (excludedCloseAreasSupplier == null) return false;
        List<ScreenCoordinate> areas = excludedCloseAreasSupplier.get();
        if (areas == null) return false;
        for (ScreenCoordinate area : areas) {
            if (mouseX >= area.x() && mouseX < area.x() + area.width()
                    && mouseY >= area.y() && mouseY < area.y() + area.height()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查点击是否应关闭下拉（多选时点击箭头或外部）
     */
    public boolean shouldCloseOnClick(double mouseX, double mouseY) {
        if (isInExcludedCloseArea(mouseX, mouseY)) return false;
        ScreenCoordinate inputBounds = getInputBounds();
        ScreenCoordinate arrowBounds = getDropdownArrowBounds();
        ScreenCoordinate dropdownBounds = getDropdownBounds();
        boolean inInput = inputBounds != null && isInBounds(mouseX, mouseY, inputBounds);
        boolean inArrow = arrowBounds != null && isInBounds(mouseX, mouseY, arrowBounds);
        boolean inDropdown = dropdownBounds != null && isInBounds(mouseX, mouseY, dropdownBounds);
        if (multiSelect) {
            return inArrow || (!inInput && !inDropdown);
        } else {
            return !inInput && !inDropdown;
        }
    }

    /**
     * 检查点击是否在下拉选项区域
     */
    public boolean isInDropdownOptions(double mouseX, double mouseY) {
        ScreenCoordinate db = getDropdownBounds();
        return db != null && isInBounds(mouseX, mouseY, db);
    }

    private boolean isInBounds(double mx, double my, ScreenCoordinate b) {
        return mx >= b.x() && mx < b.x() + b.width() && my >= b.y() && my < b.y() + b.height();
    }

    private void updateDisplayValue() {
        invalidateTagLayoutCache();
        if (selectedValues.isEmpty()) {
            tagScrollOffset = 0;
            value("");
            return;
        }
        clampTagScrollOffset();
        Font font = AbstractGuiUtils.getFont();
        StringBuilder builder = new StringBuilder();
        for (TagLayout layout : tagLayouts(font)) {
            if (builder.length() > 0) {
                builder.append(MULTI_SEPARATOR);
            }
            builder.append(layout.label);
        }
        value(builder.toString());
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) return;
        if (renderCoordinate == null) return;

        boolean tagMode = multiSelect && !dropdownOpen && !selectedValues.isEmpty();
        int extraRight = (!value().isEmpty() || tagMode ? CLEAR_BUTTON_SIZE + 2 : 0);
        paddingRight(DROPDOWN_ARROW_WIDTH + extraRight + 5);

        if (tagMode) {
            skipTextContentForRendering = true;
        }
        super.render(stack, partialTicks);
        skipTextContentForRendering = false;
        paddingRight(5);

        int drawX = (int) (x() + marginLeft());
        int drawY = (int) (y() + marginTop());
        int drawWidth = (int) width() - marginLeft() - marginRight();
        int drawHeight = (int) height() - marginTop() - marginBottom();
        int rightEdge = drawX + drawWidth;
        int contentLeft = drawX + paddingLeft();
        int contentWidth = tagMode ? getTagContentWidth() : (drawWidth - paddingLeft() - paddingRight());

        if (tagMode) {
            renderTags(stack, contentLeft, drawY, contentWidth, drawHeight);
        }

        if (!value().isEmpty() || tagMode) {
            int clearCenterX = rightEdge - DROPDOWN_ARROW_WIDTH - CLEAR_BUTTON_SIZE / 2 - 1;
            int clearCenterY = drawY + drawHeight / 2;
            AbstractGuiUtils.drawCircle(stack, clearCenterX, clearCenterY, CLEAR_BUTTON_RADIUS, 0xFFE53935);
            drawClearIcon(stack, clearCenterX, clearCenterY, 0xFFFFFFFF);
        }

        int arrowX = rightEdge - DROPDOWN_ARROW_WIDTH;
        int arrowCenterX = arrowX + DROPDOWN_ARROW_WIDTH / 2;
        int arrowCenterY = drawY + drawHeight / 2;
        int arrowColor = enabled ? textColor() : uneditableTextColor();
        if (dropdownOpen) {
            AbstractGuiUtils.fill(stack, arrowCenterX - 1, arrowCenterY - 2, 2, 1, arrowColor);
            AbstractGuiUtils.fill(stack, arrowCenterX - 2, arrowCenterY, 4, 1, arrowColor);
            AbstractGuiUtils.fill(stack, arrowCenterX - 3, arrowCenterY + 2, 6, 1, arrowColor);
        } else {
            AbstractGuiUtils.fill(stack, arrowCenterX - 3, arrowCenterY - 2, 6, 1, arrowColor);
            AbstractGuiUtils.fill(stack, arrowCenterX - 2, arrowCenterY, 4, 1, arrowColor);
            AbstractGuiUtils.fill(stack, arrowCenterX - 1, arrowCenterY + 2, 2, 1, arrowColor);
        }

        if (!value().isEmpty() && isMouseOverClearButton() && screen != null) {
            drawTooltipAtScreenCoords(stack, screen.inputState().mouseX(), screen.inputState().mouseY(), Text.literal("清空"));
        }

        if (screen != null && !isMouseOverClearButton() && !isMouseOverArrow()) {
            if (tagMode && isMouseOverInputArea()) {
                String fullContent = value();
                if (!fullContent.isEmpty()) {
                    drawTooltipAtScreenCoords(stack, screen.inputState().mouseX(), screen.inputState().mouseY(), Text.literal(fullContent));
                }
            } else if (!value().isEmpty() && !dropdownOpen && isMouseOverInputArea()) {
                String fullContent = value();
                if (!fullContent.isEmpty()) {
                    drawTooltipAtScreenCoords(stack, screen.inputState().mouseX(), screen.inputState().mouseY(), Text.literal(fullContent));
                }
            }
        }
    }

    private boolean isMouseOverArrow() {
        if (renderCoordinate == null || screen == null) return false;
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        return mx >= absX + w - DROPDOWN_ARROW_WIDTH && mx < absX + w
                && my >= absY && my < absY + h;
    }

    private boolean isMouseOverClearButton() {
        if (renderCoordinate == null || screen == null) return false;
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        return mx >= absX + w - DROPDOWN_ARROW_WIDTH - CLEAR_BUTTON_SIZE - 2 && mx < absX + w - DROPDOWN_ARROW_WIDTH
                && my >= absY && my < absY + h;
    }

    private static void drawClearIcon(PoseStack stack, int centerX, int centerY, int color) {
        float r = CLEAR_BUTTON_RADIUS * 0.4f; // x 略小于圆的 1/2
        AbstractGuiUtils.drawLine(stack, centerX - r, centerY - r, centerX + r, centerY + r, 1f, color);
        AbstractGuiUtils.drawLine(stack, centerX + r, centerY - r, centerX - r, centerY + r, 1f, color);
    }

    private boolean isMouseOverInputArea() {
        if (renderCoordinate == null || screen == null) return false;
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width() - DROPDOWN_ARROW_WIDTH;
        if (multiSelect && !selectedValues.isEmpty()) {
            w -= CLEAR_BUTTON_SIZE + 2;
        }
        return mx >= absX && mx < absX + w && my >= absY && my < absY + renderCoordinate.height();
    }

    private void renderTags(PoseStack stack, int contentLeft, int drawY, int contentWidth, int drawHeight) {
        if (selectedValues.isEmpty()) return;
        if (screen == null) return;

        Font font = AbstractGuiUtils.getFont();
        BaniraColorConfig theme = screen.getEffectiveTheme();
        int tagBg = theme.color(POPUP_ITEM_SELECTED);
        int tagBorder = theme.color(POPUP_ITEM_SELECTED_BORDER);
        int textColor = theme.color(LIST_ITEM_TEXT);

        int tagY = drawY + (drawHeight - TAG_MIN_HEIGHT) / 2;
        int currentX = contentLeft - tagScrollOffset;

        List<TagLayout> layouts = tagLayouts(font);
        int maxScroll = Math.max(0, cachedTagTotalWidth - contentWidth);
        boolean canScrollRight = maxScroll > 0 && tagScrollOffset < maxScroll;
        boolean canScrollLeft = tagScrollOffset > 0;

        int scissorX = (int) (absoluteX() + contentLeft - x());
        int scissorY = (int) (absoluteY() + drawY - y() + 1);
        // 使用 push/pop，避免 disableScissor 关掉外层界面的全局裁剪
        AbstractGuiUtils.pushScissor(scissorX, scissorY, contentWidth, drawHeight - 2);

        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();

        try {
            for (TagLayout layout : layouts) {
                if (currentX + layout.tagWidth < contentLeft || currentX > contentLeft + contentWidth) {
                    currentX += layout.tagWidth + TAG_GAP;
                    continue;
                }

                int closeX = currentX + layout.tagWidth - TAG_PAD - TAG_CLOSE_SIZE;
                int closeY = tagY + (TAG_MIN_HEIGHT - TAG_CLOSE_SIZE) / 2;
                boolean closeHovered = mx >= closeX && mx < closeX + TAG_CLOSE_SIZE && my >= closeY && my < closeY + TAG_CLOSE_SIZE;

                AbstractGuiUtils.fill(stack, currentX, tagY, layout.tagWidth, TAG_MIN_HEIGHT, tagBg);
                AbstractGuiUtils.fill(stack, currentX, tagY, 2, TAG_MIN_HEIGHT, tagBorder);
                font.draw(stack, layout.label, currentX + TAG_PAD, tagY + (TAG_MIN_HEIGHT - font.lineHeight) / 2f, textColor);

                int clearColor = closeHovered ? 0xFFE53935 : 0xFF999999;
                AbstractGuiUtils.fill(stack, closeX, closeY, TAG_CLOSE_SIZE, TAG_CLOSE_SIZE, clearColor);
                float r = 2f;
                int cx = closeX + TAG_CLOSE_SIZE / 2;
                int cy = closeY + TAG_CLOSE_SIZE / 2;
                AbstractGuiUtils.drawLine(stack, cx - r, cy - r, cx + r, cy + r, 1f, 0xFFFFFFFF);
                AbstractGuiUtils.drawLine(stack, cx + r, cy - r, cx - r, cy + r, 1f, 0xFFFFFFFF);

                currentX += layout.tagWidth + TAG_GAP;
            }
        } finally {
            AbstractGuiUtils.popScissor();
        }

        int centerY = drawY + drawHeight / 2;
        int dotColor = textColor;
        if (canScrollLeft) {
            int dotX = contentLeft - 2;
            AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX - 1, centerY, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
        }
        if (canScrollRight) {
            int dotX = contentLeft + contentWidth + 3;
            AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX + 1, centerY, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
        }
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        if (!visible || renderCoordinate == null || event == null || event.button() != 0) return false;

        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();

        if (mouseX >= absX && mouseX < absX + w && mouseY >= absY && mouseY < absY + h) {
            boolean inArrow = mouseX >= absX + w - DROPDOWN_ARROW_WIDTH;
            boolean inClear = !value().isEmpty() && mouseX >= absX + w - DROPDOWN_ARROW_WIDTH - CLEAR_BUTTON_SIZE - 2 && mouseX < absX + w - DROPDOWN_ARROW_WIDTH;
            if (inClear) {
                pressedArea = 1;
            } else if (inArrow) {
                pressedArea = 2;
            } else {
                pressedArea = 3;
            }
            return true;
        }
        pressedArea = 0;
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        if (!visible || renderCoordinate == null || event == null || event.button() != 0 || pressedArea == 0) {
            pressedArea = 0;
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        boolean inArrow = mouseX >= absX + w - DROPDOWN_ARROW_WIDTH;
        boolean inClear = !value().isEmpty() && mouseX >= absX + w - DROPDOWN_ARROW_WIDTH - CLEAR_BUTTON_SIZE - 2 && mouseX < absX + w - DROPDOWN_ARROW_WIDTH;
        boolean inInput = mouseX >= absX && mouseX < absX + w && mouseY >= absY && mouseY < absY + h;

        int releaseArea = inClear ? 1 : (inArrow ? 2 : (inInput ? 3 : 0));
        boolean sameArea = releaseArea == pressedArea && inside;
        int area = pressedArea;
        pressedArea = 0;

        if (!sameArea) return area != 0;

        if (area == 1) {
            selectedValues.clear();
            invalidateTagLayoutCache();
            tagScrollOffset = 0;
            customValueDirty = false;
            value("");
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(getSelectedValues());
            }
            return true;
        }
        if (area == 2) {
            if (dropdownOpen) {
                closeDropdown();
            } else {
                openDropdown();
            }
            return true;
        }
        if (area == 3) {
            if (multiSelect && !dropdownOpen && !selectedValues.isEmpty()) {
                int closeIdx = findTagCloseIndexAt(mouseX, mouseY);
                if (closeIdx >= 0) {
                    removeSelectedValueAt(closeIdx);
                    return true;
                }
                int tagIdx = findTagIndexAt(mouseX, mouseY);
                if (tagIdx >= 0) {
                    openPreview();
                    return true;
                }
                openDropdown();
                return true;
            }
            if (!dropdownOpen) {
                openDropdown();
            }
            pressedArea = 2;
            return super.onMouseRelease(event, inside);
        }
        return false;
    }

    private void openPreview() {
        if (previewExpanded || selectedValues.isEmpty()) return;
        previewExpanded = true;
        if (screen != null) {
            previewOverlayWidget = new DropdownPreviewOverlayWidget(screen, this);
            screen.addOverlayWidget(previewOverlayWidget);
        }
    }

    private int getTagContentWidth() {
        if (renderCoordinate == null) return 0;
        int rightReserved = DROPDOWN_ARROW_WIDTH + CLEAR_BUTTON_SIZE + 2 + 5;
        return (int) renderCoordinate.width() - marginLeft() - marginRight() - paddingLeft() - rightReserved - TAG_RIGHT_MARGIN;
    }

    private int findTagIndexAt(double mouseX, double mouseY) {
        if (renderCoordinate == null) return -1;
        double absX = absoluteX();
        double absY = absoluteY();
        int contentLeft = (int) absX + marginLeft() + paddingLeft();
        int drawHeight = (int) height() - marginTop() - marginBottom();
        int tagY = (int) absY + marginTop() + (drawHeight - TAG_MIN_HEIGHT) / 2;
        if (mouseY < tagY || mouseY >= tagY + TAG_MIN_HEIGHT) return -1;
        int currentX = contentLeft - tagScrollOffset;
        Font font = AbstractGuiUtils.getFont();
        for (TagLayout layout : tagLayouts(font)) {
            if (mouseX >= currentX && mouseX < currentX + layout.tagWidth) return layout.index;
            currentX += layout.tagWidth + TAG_GAP;
        }
        return -1;
    }

    private int findTagCloseIndexAt(double mouseX, double mouseY) {
        int idx = findTagIndexAt(mouseX, mouseY);
        if (idx < 0) return -1;
        if (renderCoordinate == null) return -1;
        double absX = absoluteX();
        double absY = absoluteY();
        int contentLeft = (int) absX + marginLeft() + paddingLeft();
        int drawHeight = (int) height() - marginTop() - marginBottom();
        int tagY = (int) absY + marginTop() + (drawHeight - TAG_MIN_HEIGHT) / 2;
        int currentX = contentLeft - tagScrollOffset;
        Font font = AbstractGuiUtils.getFont();
        for (TagLayout layout : tagLayouts(font)) {
            int closeX = currentX + layout.tagWidth - TAG_PAD - TAG_CLOSE_SIZE;
            int closeY = tagY + (TAG_MIN_HEIGHT - TAG_CLOSE_SIZE) / 2;
            if (mouseX >= closeX && mouseX < closeX + TAG_CLOSE_SIZE && mouseY >= closeY && mouseY < closeY + TAG_CLOSE_SIZE) {
                return layout.index;
            }
            currentX += layout.tagWidth + TAG_GAP;
        }
        return -1;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (event == null) {
            return false;
        }
        double mx = event.mouseX();
        double my = event.mouseY();
        if (previewExpanded && previewOverlayWidget != null) {
            ScreenCoordinate pb = getPreviewBounds();
            if (pb != null && mx >= pb.x() && mx < pb.x() + pb.width() && my >= pb.y() && my < pb.y() + pb.height()) {
                return previewOverlayWidget.handleMouseScroll(event);
            }
        }
        if (multiSelect && !dropdownOpen && !selectedValues.isEmpty() && isMouseInside(mx, my)) {
            Font font = AbstractGuiUtils.getFont();
            int contentWidth = getTagContentWidth();
            int maxScroll = Math.max(0, tagTotalWidth(font) - contentWidth);
            if (maxScroll > 0) {
                int step = 40;
                if (event.delta() > 0) {
                    tagScrollOffset = Math.max(0, tagScrollOffset - step);
                } else {
                    tagScrollOffset = Math.min(maxScroll, tagScrollOffset + step);
                }
                return true;
            }
        }
        if (previewExpanded) {
            return false;
        }
        return super.handleMouseScroll(event);
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        int keyCode = event.keyCode();
        if (dropdownOpen && keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
            closeDropdown();
            return true;
        }
        if (previewExpanded && keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
            closePreview();
            return true;
        }
        if ((previewExpanded || (multiSelect && !dropdownOpen && !selectedValues.isEmpty() && focused()))
                && (keyCode == GLFWKey.GLFW_KEY_BACKSPACE || keyCode == GLFWKey.GLFW_KEY_DELETE)) {
            if (!selectedValues.isEmpty()) {
                removeSelectedValueAt(selectedValues.size() - 1);
                return true;
            }
        }
        String previousValue = value();
        boolean consumed = super.onKeyPress(event);
        if (consumed && inputMode == DropdownInputMode.EDITABLE
                && !multiSelect && !previousValue.equals(value())) {
            customValueDirty = true;
            syncCustomValue();
        }
        return consumed;
    }

    @Override
    protected boolean onCharTyped(xin.vanilla.banira.client.gui.event.CharInputEvent event) {
        String previousValue = value();
        boolean consumed = super.onCharTyped(event);
        if (consumed && inputMode == DropdownInputMode.EDITABLE
                && !multiSelect && !previousValue.equals(value())) {
            customValueDirty = true;
            syncCustomValue();
        }
        return consumed;
    }

    private void syncCustomValue() {
        selectedValues.clear();
        if (!value().isEmpty()) {
            selectedValues.add(value());
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(getSelectedValues());
        }
    }
}
