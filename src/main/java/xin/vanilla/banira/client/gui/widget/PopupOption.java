package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import static xin.vanilla.banira.client.data.BaniraColorToken.*;

/**
 * 弹出层选项框。支持链式调用，默认 FINE 模式绘制。
 * 推荐用法：
 * <pre>
 * popupOption.clear()
 *     .addOptionWithId("opt_a", "选项 A", "提示文本", e -> handleA(e))
 *     .addOptionWithId("opt_b", "选项 B")
 *     .onSelect(e -> handle(e.id(), e.text()))  // 全局回调，选项无单独回调时触发
 *     .showAt(mouseX, mouseY);
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
@Accessors(chain = true)
public class PopupOption extends BaseWidget {

    /**
     * 选项选中事件
     */
    @Getter
    @RequiredArgsConstructor
    @Accessors(chain = true, fluent = true)
    public static class SelectEvent {
        /**
         * 选项序号，保留用于兼容
         */
        private final int index;
        /**
         * 选项字符串 ID，未指定时使用序号字符串
         */
        @Nonnull
        private final String id;
        @Nonnull
        private final String text;
    }

    private static final int PAD_TOP = 2, PAD_BOTTOM = 2, PAD_LEFT = 5, PAD_RIGHT = 5, MARGIN = 2;

    private final List<Text> optionList = new ArrayList<>();
    private final List<String> optionIds = new ArrayList<>();
    @Getter
    private final List<Text> renderList = new ArrayList<>();
    private final Map<Integer, Integer> relationMap = new HashMap<>();
    private final Map<Integer, Text> tipsMap = new HashMap<>();
    private final List<Consumer<SelectEvent>> optionCallbacks = new ArrayList<>();

    @Setter
    private int radius = 2;
    @Getter
    private String id;
    @Setter
    private Font font;

    private int width = -1, height = -1;
    private int adjustedX = -1, adjustedY = -1;
    private int maxWidth = -1, maxHeight = -1;
    private int selectedIndex = -1;
    private int scrollOffset, maxLines;
    private int pressedOptionIndex = -1;

    @Setter
    private String tipsKeyNames;
    @Setter
    private Consumer<PopupOption> beforeRender;
    @Setter
    private Consumer<PopupOption> afterRender;

    @Nullable
    private Consumer<SelectEvent> onSelect;

    private boolean built;

    public PopupOption(BaniraScreen screen) {
        super(screen, new ScreenCoordinate(0, 0, 0, 0));
        this.font = screen.getFont();
        this.renderDepth(EnumRenderDepth.TOOLTIP);
    }

    public static PopupOption init(BaniraScreen screen) {
        return new PopupOption(screen);
    }


    // region Deprecated

    /**
     * 添加选项
     */
    @Deprecated
    public PopupOption addOption(@Nonnull String text) {
        return addOptionInternal(null, text, null, null);
    }

    /**
     * 添加选项
     */
    @Deprecated
    public PopupOption addOption(@Nonnull String text, @Nullable String tip) {
        return addOptionInternal(null, text, tip, null);
    }

    /**
     * 添加选项，可单独设置点击回调
     */
    @Deprecated
    public PopupOption addOption(@Nonnull String text, @Nullable String tip, @Nullable Consumer<SelectEvent> onClick) {
        return addOptionInternal(null, text, tip, onClick);
    }

    /**
     * 添加选项
     */
    @Deprecated
    public PopupOption addOption(@Nonnull Text text) {
        return addOptionInternal(null, text, null, null);
    }

    /**
     * 添加选项
     */
    @Deprecated
    public PopupOption addOption(@Nonnull Text text, @Nullable Text tip) {
        return addOptionInternal(null, text, tip, null);
    }

    /**
     * 添加选项，可单独设置点击回调
     */
    @Deprecated
    public PopupOption addOption(@Nonnull Text text, @Nullable Text tip, @Nullable Consumer<SelectEvent> onClick) {
        return addOptionInternal(null, text, tip, onClick);
    }

    // endregion Deprecated


    /**
     * 添加选项并指定字符串 ID
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull String text) {
        return addOptionInternal(id, text, null, null);
    }

    /**
     * 添加选项并指定字符串 ID
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull String text, @Nullable String tip) {
        return addOptionInternal(id, text, tip, null);
    }

    /**
     * 添加选项并指定字符串 ID 和点击回调
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull String text, @Nullable String tip, @Nullable Consumer<SelectEvent> onClick) {
        return addOptionInternal(id, text, tip, onClick);
    }

    /**
     * 添加选项并指定字符串 ID
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull Text text) {
        return addOptionInternal(id, text, null, null);
    }

    /**
     * 添加选项并指定字符串 ID
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull Text text, @Nullable Text tip) {
        return addOptionInternal(id, text, tip, null);
    }

    /**
     * 添加选项并指定字符串 ID 和点击回调
     */
    public PopupOption addOptionWithId(@Nullable String id, @Nonnull Text text, @Nullable Text tip, @Nullable Consumer<SelectEvent> onClick) {
        return addOptionInternal(id, text, tip, onClick);
    }

    private PopupOption addOptionInternal(@Nullable String id, @Nonnull String text, @Nullable String tip, @Nullable Consumer<SelectEvent> onClick) {
        ensureNotBuilt();
        String resolvedId = !StringUtils.isNullOrEmptyEx(id) ? id : String.valueOf(optionList.size());
        Text literal = Text.literal(text);
        List<Text> lines = Arrays.stream(StringUtils.replaceLineBreak(text).split("\n"))
                .map(Text::literal)
                .toList();
        for (int i = 0; i < lines.size(); i++) {
            relationMap.put(renderList.size() + i, optionList.size());
        }
        optionList.add(literal);
        optionIds.add(resolvedId);
        renderList.addAll(lines);
        optionCallbacks.add(onClick);
        if (tip != null) {
            tipsMap.put(optionList.size() - 1, Text.literal(tip));
        }
        return this;
    }

    private PopupOption addOptionInternal(@Nullable String id, @Nonnull Text text, @Nullable Text tip, @Nullable Consumer<SelectEvent> onClick) {
        ensureNotBuilt();
        String resolvedId = !StringUtils.isNullOrEmptyEx(id) ? id : String.valueOf(optionList.size());
        List<Text> lines = Arrays.stream(StringUtils.replaceLineBreak(text.content()).split("\n"))
                .map(s -> text.clone().text(s).hoverText(s).withStyle(text))
                .toList();
        for (int i = 0; i < lines.size(); i++) {
            relationMap.put(renderList.size() + i, optionList.size());
        }
        optionList.add(text);
        optionIds.add(resolvedId);
        renderList.addAll(lines);
        optionCallbacks.add(onClick);
        if (tip != null) {
            tipsMap.put(optionList.size() - 1, tip);
        }
        return this;
    }

    /**
     * 设置选中回调，左键点击选项时触发
     */
    public PopupOption onSelect(@Nullable Consumer<SelectEvent> callback) {
        this.onSelect = callback;
        return this;
    }

    /**
     * 在指定位置显示，自动使用 screen 的 font
     */
    public PopupOption showAt(double x, double y) {
        return showAt(x, y, "popup_" + System.currentTimeMillis());
    }

    /**
     * 在指定位置显示，指定 id
     */
    public PopupOption showAt(double x, double y, String id) {
        if (CollectionUtils.isNullOrEmpty(optionList)) {
            throw new IllegalStateException("addOption must be called before showAt");
        }
        this.font = font != null ? font : screen.getFont();
        this.id = id;
        built = true;
        layout((int) x, (int) y);
        return this;
    }

    /**
     * @deprecated 使用 {@link #showAt(double, double)} 替代
     */
    @Deprecated
    public PopupOption build(Font font, double x, double y, String id) {
        this.font = font;
        return showAt(x, y, id);
    }

    /**
     * 清空并重置，可再次 addOption + showAt
     */
    public PopupOption clear() {
        optionList.clear();
        optionIds.clear();
        renderList.clear();
        tipsMap.clear();
        relationMap.clear();
        optionCallbacks.clear();
        width = height = -1;
        adjustedX = adjustedY = -1;
        maxWidth = maxHeight = -1;
        selectedIndex = -1;
        pressedOptionIndex = -1;
        scrollOffset = maxLines = 0;
        built = false;
        bounds(new ScreenCoordinate(0, 0, 0, 0));
        return this;
    }

    public boolean isEmpty() {
        return CollectionUtils.isNullOrEmpty(optionList);
    }

    public boolean isHovered() {
        return built && !optionList.isEmpty() && relationMap.getOrDefault(selectedIndex, -1) >= 0;
    }

    public int getSelectedIndex() {
        return CollectionUtils.isNullOrEmpty(optionList) ? -1 : relationMap.getOrDefault(selectedIndex, -1);
    }

    @Nonnull
    public String getSelectedString() {
        int idx = getSelectedIndex();
        return (idx >= 0 && idx < optionList.size()) ? optionList.get(idx).content() : "";
    }

    /**
     * 获取当前选中选项的字符串 ID
     */
    @Nonnull
    public String getSelectedId() {
        int idx = getSelectedIndex();
        return (idx >= 0 && idx < optionIds.size()) ? optionIds.get(idx) : "";
    }

    /**
     * 获取指定坐标下的悬停选项索引
     */
    public int getHoveredIndexAt(double mouseX, double mouseY) {
        return findHoveredIndex(mouseX, mouseY);
    }

    /**
     * 按下时记录选项索引，供抬起时判断是否在同一控件上。
     *
     * @return 是否消费了此次按下事件（在选项上按下时返回 true）
     */
    public boolean tryHandleOptionPress(MouseEvent event) {
        if (event == null || event.button() != 0 || !built || optionList.isEmpty()) return false;
        int idx = findHoveredIndex(event.mouseX(), event.mouseY());
        if (idx < 0 || relationMap.getOrDefault(idx, -1) < 0) return false;
        pressedOptionIndex = relationMap.get(idx);
        return true;
    }

    /**
     * 抬起时处理选项选择。仅当按下与抬起都在同一选项上时才触发回调。
     * 优先调用选项单独设置的回调，若无则调用全局 onSelect。
     * 若按下时在选项上，抬起时无论是否在同一选项都会消费事件，避免误触下方控件。
     */
    public boolean tryHandleOptionRelease(MouseEvent event) {
        if (event == null || event.button() != 0 || pressedOptionIndex < 0) return false;
        int releaseIdx = findHoveredIndex(event.mouseX(), event.mouseY());
        int releaseOptionIdx = releaseIdx >= 0 ? relationMap.getOrDefault(releaseIdx, -1) : -1;
        boolean sameOption = releaseOptionIdx == pressedOptionIndex;
        int idx = pressedOptionIndex;
        pressedOptionIndex = -1;
        if (!sameOption) {
            return true; // 消费事件，但不触发回调
        }
        String text = (idx >= 0 && idx < optionList.size()) ? optionList.get(idx).content() : "";
        String optId = (idx >= 0 && idx < optionIds.size()) ? optionIds.get(idx) : "";
        SelectEvent selectEvent = new SelectEvent(idx, optId, text);
        Consumer<SelectEvent> cb = (idx >= 0 && idx < optionCallbacks.size()) ? optionCallbacks.get(idx) : null;
        if (cb == null) cb = onSelect;
        clear();
        if (cb != null) {
            cb.accept(selectEvent);
        }
        return true;
    }

    /**
     * 尝试处理选项点击。优先调用选项单独设置的回调，若无则调用全局 onSelect。
     *
     * @deprecated 已改为按下记录、抬起触发，请使用 {@link #tryHandleOptionPress} 和 {@link #tryHandleOptionRelease}
     */
    @Deprecated
    public boolean tryHandleOptionClick(MouseEvent event) {
        if (event != null && tryHandleOptionPress(event)) {
            return tryHandleOptionRelease(event);
        }
        return false;
    }

    public PopupOption setMaxWidth(int maxWidth) {
        ensureNotBuilt();
        this.maxWidth = maxWidth + PAD_LEFT + PAD_RIGHT;
        return this;
    }

    public PopupOption setMaxHeight(int maxHeight) {
        ensureNotBuilt();
        this.maxHeight = maxHeight;
        return this;
    }

    public PopupOption setMaxLines(int maxLines) {
        ensureNotBuilt();
        this.maxHeight = maxLines * (font.lineHeight + 1) + PAD_TOP + PAD_BOTTOM - 1;
        return this;
    }

    public boolean addScrollOffset(double delta) {
        if (!isHovered()) return false;
        int unit = renderList.size() / maxLines > 25
                ? Math.max(1, maxLines - 1)
                : Math.min(Math.max(1, (int) Math.ceil(Math.sqrt(renderList.size() / (double) maxLines))), Math.max(1, maxLines - 1));
        if (delta > 0) {
            scrollOffset = Math.max(scrollOffset - unit, 0);
            return true;
        } else if (delta < 0) {
            scrollOffset = Math.min(scrollOffset + unit, Math.max(0, renderList.size() - maxLines));
            return true;
        }
        return false;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (beforeRender != null) beforeRender.accept(this);
        if (CollectionUtils.isNullOrEmpty(optionList) || Minecraft.getInstance().screen == null) {
            if (afterRender != null) afterRender.accept(this);
            return;
        }

        InputStateManager inputState = screen.inputState();
        selectedIndex = findHoveredIndex(inputState.mouseX(), inputState.mouseY());

        BaniraColorConfig theme = screen.getEffectiveTheme();
        int popupBg = theme.color(POPUP_BG);
        int popupBorder = theme.color(POPUP_BORDER);
        int popupItemHover = theme.color(POPUP_ITEM_HOVER);
        int textColorUnselected = theme.color(POPUP_ITEM_TEXT);
        int textColorSelected = theme.color(POPUP_ITEM_TEXT_SELECTED);

        AbstractGuiUtils.renderByDepth(stack, renderDepth(), s -> {
            ShapeDrawArgs fillArgs = ShapeDrawArgs.rect(s, adjustedX, adjustedY, width, height, popupBg);
            fillArgs.rect().radius(radius).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(fillArgs);
            ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(s, adjustedX, adjustedY, width, height, popupBorder);
            borderArgs.rect().radius(radius).border(1).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(borderArgs);
            int lineOffset = 0;
            for (int i = 0; i < maxLines; i++) {
                int index = i + scrollOffset;
                if (index >= 0 && index < renderList.size()) {
                    Text text = renderList.get(index);
                    boolean isSelected = selectedIndex == index;
                    if (isSelected) {
                        int lineH = font.lineHeight * StringUtils.getLineCount(text.content());
                        AbstractGuiUtils.fill(stack, adjustedX + 1, adjustedY + PAD_TOP + lineOffset * (font.lineHeight + 1), width - 2, lineH, popupItemHover);
                    }
                    int textColor = isSelected ? textColorSelected : textColorUnselected;
                    FontDrawArgs args = FontDrawArgs.of(text.stack(stack).font(font).color(textColor))
                            .x(adjustedX + PAD_LEFT).y(adjustedY + PAD_TOP + i * (font.lineHeight + 1));
                    if (maxWidth > 0) args.maxWidth(maxWidth).position(EnumEllipsisPosition.MIDDLE);
                    LabelWidget.drawLimitedText(args);
                    lineOffset += StringUtils.getLineCount(text.content());
                }
            }
        });

        if (StringUtils.isNullOrEmptyEx(tipsKeyNames) || inputState.isKeyPressed(tipsKeyNames)) {
            renderOptionTip(stack, inputState);
        }
        if (afterRender != null) afterRender.accept(this);
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        return event != null && addScrollOffset(event.delta());
    }

    /**
     * 供 BaniraScreen 直接调用渲染
     */
    public void render(PoseStack stack, InputStateManager inputState) {
        render(stack, 0);
    }

    private void ensureNotBuilt() {
        if (built) {
            throw new IllegalStateException("Cannot add options after showAt/build. Call clear() first.");
        }
    }

    private void layout(int px, int py) {
        Objects.requireNonNull(Minecraft.getInstance().screen);
        int screenWidth = Minecraft.getInstance().screen.width;
        int screenHeight = Minecraft.getInstance().screen.height;
        if (maxWidth <= 0) maxWidth = screenWidth - MARGIN * 2;
        if (maxHeight <= 0) maxHeight = screenHeight - MARGIN * 2;

        width = Math.min(AbstractGuiUtils.getTextWidth(font, renderList) + PAD_LEFT + PAD_RIGHT, maxWidth);
        height = Math.min(AbstractGuiUtils.getTextHeight(font, renderList) + renderList.size() - 1 + PAD_TOP + PAD_BOTTOM, maxHeight);
        maxLines = ((height - PAD_TOP - PAD_BOTTOM) + 1) / (font.lineHeight + 1);

        adjustedX = px + MARGIN;
        adjustedY = py + MARGIN;

        if (adjustedY + height + MARGIN > screenHeight) adjustedY = py - height - MARGIN + 1;
        if (adjustedX + width + MARGIN > screenWidth) adjustedX = px - width - MARGIN + 1;
        adjustedX = Math.max(MARGIN, Math.min(adjustedX, screenWidth - width - MARGIN));
        adjustedY = Math.max(MARGIN, Math.min(adjustedY, screenHeight - height - MARGIN));

        bounds(new ScreenCoordinate(adjustedX, adjustedY, width, height));
    }

    private void renderOptionTip(PoseStack stack, InputStateManager inputState) {
        int optIdx = getSelectedIndex();
        if (optIdx < 0) return;
        Text tip = tipsMap.get(optIdx);
        if (tip == null || StringUtils.isNullOrEmptyEx(tip.content())) return;

        BaniraColorConfig theme = screen.getEffectiveTheme();
        FontDrawArgs args = FontDrawArgs.of(tip.stack(stack).font(font))
                .margin(MARGIN)
                .x(inputState.mouseX()).y(inputState.mouseY())
                .inScreen(true)
                .popupUseTexture(theme.tooltipUseTexture());
        TooltipWidget.drawPopupMessage(stack, args, theme, screen.season());
    }

    private int findHoveredIndex(double mouseX, double mouseY) {
        if (adjustedY < 0 || width < 0) return -1;
        if (mouseX < adjustedX || mouseX >= adjustedX + width - 1) return -1;
        int relativeY = (int) (mouseY - adjustedY - PAD_TOP);
        if (relativeY < 0 || relativeY >= height - PAD_TOP - PAD_BOTTOM) return -1;

        int lines = 0;
        for (int i = 0; i < maxLines && scrollOffset + i < renderList.size(); i++) {
            Text t = renderList.get(scrollOffset + i);
            int curLines = StringUtils.getLineCount(t.content());
            if (relativeY >= lines * (font.lineHeight + 1) && relativeY < (lines + curLines) * (font.lineHeight + 1) - 1) {
                return scrollOffset + i;
            }
            lines += curLines;
        }
        return -1;
    }
}
