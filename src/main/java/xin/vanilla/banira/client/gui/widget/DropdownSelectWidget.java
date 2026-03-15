package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 下拉选择 Widget。拥有 InputWidget 的输入特性，支持：
 * <ul>
 *   <li>点击时在上方或下方显示下拉选项</li>
 *   <li>下拉展开时输入框可输入内容进行智能过滤</li>
 *   <li>点击选择项时不会使下方控件响应</li>
 *   <li>选项过多时支持滚轮滚动</li>
 *   <li>可配置单选/多选，多选用逗号分隔</li>
 *   <li>多选时需点击下拉箭头或外部区域关闭；单选时点击选项或外部区域关闭</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * DropdownSelectWidget dropdown = new DropdownSelectWidget(this);
 * dropdown.renderCoordinate(new ScreenCoordinate(10, 10, 200, 24));
 * dropdown.options(Arrays.asList("选项A", "选项B", "选项C"));
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

    @Getter
    @Setter
    private List<String> options = new ArrayList<>();

    @Getter
    @Setter
    private boolean multiSelect = false;

    @Getter
    @Setter
    @Nullable
    private Consumer<List<String>> onSelectionChanged;

    @Getter
    private boolean dropdownOpen = false;

    @Getter
    private int dropdownScrollOffset = 0;

    private List<String> selectedValues = new ArrayList<>();

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
        List<String> filtered = getFilteredOptions();
        int contentHeight = filtered.size() * DROPDOWN_ITEM_HEIGHT;
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

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
    }

    /**
     * 设置选中的值。单选传入单元素列表，多选传入多个。
     */
    public DropdownSelectWidget selectedValues(List<String> values) {
        this.selectedValues = values != null ? new ArrayList<>(values) : new ArrayList<>();
        updateDisplayValue();
        return this;
    }

    /**
     * 获取当前选中的值列表
     */
    public List<String> getSelectedValues() {
        return new ArrayList<>(selectedValues);
    }

    /**
     * 获取过滤后的选项列表（根据当前输入内容）
     */
    public List<String> getFilteredOptions() {
        String filter = value().toLowerCase().trim();
        if (StringUtils.isNullOrEmpty(filter)) {
            return new ArrayList<>(options);
        }
        return options.stream()
                .filter(opt -> opt.toLowerCase().contains(filter))
                .collect(Collectors.toList());
    }

    /**
     * 获取下拉框的屏幕坐标（上方或下方，根据空间自动选择）
     */
    public ScreenCoordinate getDropdownBounds() {
        if (renderCoordinate == null || screen == null) return null;
        int sw = Minecraft.getInstance().screen != null ? Minecraft.getInstance().screen.width : 400;
        int sh = Minecraft.getInstance().screen != null ? Minecraft.getInstance().screen.height : 300;

        double absX = absoluteX();
        double absY = absoluteY();
        int w = (int) Math.max(renderCoordinate.width(), 100);
        int itemCount = Math.min(getFilteredOptions().size(), 10);
        int h = Math.min(DROPDOWN_MAX_HEIGHT, itemCount * DROPDOWN_ITEM_HEIGHT + DROPDOWN_PAD * 2);

        double dropY;
        if (absY + renderCoordinate.height() + h + 4 <= sh) {
            dropY = absY + renderCoordinate.height() + 2;
        } else if (absY - h - 2 >= 0) {
            dropY = absY - h - 2;
        } else {
            dropY = Math.max(2, sh - h - 2);
        }

        double dropX = Math.max(2, Math.min(absX, sw - w - 2));
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
        value("");
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
        dropdownOpen = false;
        value(selectedValues.isEmpty() ? "" : String.join(MULTI_SEPARATOR, selectedValues));
        if (overlayWidget != null && screen != null) {
            screen.removeOverlayWidget(overlayWidget);
            overlayWidget = null;
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
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(getSelectedValues());
            }
        } else {
            if (selectedValues.contains(option)) {
                selectedValues.clear();
            } else {
                selectedValues.clear();
                selectedValues.add(option);
            }
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
        List<String> filtered = getFilteredOptions();
        int contentHeight = filtered.size() * DROPDOWN_ITEM_HEIGHT;
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
     * 检查点击是否应关闭下拉（多选时点击箭头或外部）
     */
    public boolean shouldCloseOnClick(double mouseX, double mouseY) {
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
        value(selectedValues.isEmpty() ? "" : String.join(MULTI_SEPARATOR, selectedValues));
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) return;
        if (renderCoordinate == null) return;

        int extraRight = (!value().isEmpty() ? CLEAR_BUTTON_SIZE + 2 : 0);
        paddingRight(DROPDOWN_ARROW_WIDTH + extraRight + 5);
        super.render(stack, partialTicks);
        paddingRight(5);

        int drawX = (int) (x() + marginLeft());
        int drawY = (int) (y() + marginTop());
        int drawWidth = (int) width() - marginLeft() - marginRight();
        int drawHeight = (int) height() - marginTop() - marginBottom();
        int rightEdge = drawX + drawWidth;

        if (!value().isEmpty()) {
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
            double mx = screen.inputState().mouseX();
            double my = screen.inputState().mouseY();
            TooltipWidget.drawPopupMessage(stack, FontDrawArgs.ofPopo(Text.literal("清空").stack(stack)).x((int) mx).y((int) my),
                    screen.getEffectiveTheme(), screen.season());
        }
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

    private static void drawClearIcon(MatrixStack stack, int centerX, int centerY, int color) {
        float r = CLEAR_BUTTON_RADIUS * 0.4f; // x 略小于圆的 1/2
        AbstractGuiUtils.drawLine(stack, centerX - r, centerY - r, centerX + r, centerY + r, 1f, color);
        AbstractGuiUtils.drawLine(stack, centerX + r, centerY - r, centerX - r, centerY + r, 1f, color);
    }

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (!visible || renderCoordinate == null || mouseButton != 0) return false;

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
    protected boolean onMouseRelease(double mouseX, double mouseY, int mouseButton, boolean inside) {
        if (!visible || renderCoordinate == null || mouseButton != 0 || pressedArea == 0) {
            pressedArea = 0;
            return false;
        }
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
            if (!dropdownOpen) {
                openDropdown();
            }
            pressedArea = 2;
            return super.onMouseRelease(mouseX, mouseY, mouseButton, inside);
        }
        return false;
    }

    @Override
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (dropdownOpen && keyCode == GLFWKey.GLFW_KEY_ESCAPE) {
            closeDropdown();
            return true;
        }
        return super.onKeyPress(keyCode, scanCode, modifiers);
    }
}
