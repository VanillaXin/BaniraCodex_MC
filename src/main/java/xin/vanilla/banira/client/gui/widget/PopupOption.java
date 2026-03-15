package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
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
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 弹出层选项框。支持链式调用，默认 FINE 模式绘制。
 * 推荐用法：
 * <pre>
 * popupOption.clear()
 *     .addOption("选项 A", "提示文本", e -> handleA(e))
 *     .addOption("选项 B")
 *     .onSelect(e -> handle(e.index(), e.text()))  // 全局回调，选项无单独回调时触发
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
    public static class SelectEvent {
        private final int index;
        @NonNull
        private final String text;
    }

    private static final int PAD_TOP = 2, PAD_BOTTOM = 2, PAD_LEFT = 5, PAD_RIGHT = 5, MARGIN = 2;

    private final List<Text> optionList = new ArrayList<>();
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
    private FontRenderer font;

    private int width = -1, height = -1;
    private int screenWidth, screenHeight;
    private int x = -1, y = -1;
    private int adjustedX = -1, adjustedY = -1;
    private int maxWidth = -1, maxHeight = -1;
    private int selectedIndex = -1;
    private int scrollOffset, maxLines;

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

    /**
     * 添加选项
     */
    public PopupOption addOption(@NonNull String text) {
        return addOption(text, null, null);
    }

    /**
     * 添加选项
     */
    public PopupOption addOption(@NonNull String text, @Nullable String tip) {
        return addOption(text, tip, null);
    }

    /**
     * 添加选项，可单独设置点击回调
     */
    public PopupOption addOption(@NonNull String text, @Nullable String tip, @Nullable Consumer<SelectEvent> onClick) {
        ensureNotBuilt();
        Text literal = Text.literal(text);
        List<Text> lines = Arrays.stream(StringUtils.replaceLineBreak(text).split("\n"))
                .map(Text::literal)
                .collect(Collectors.toList());
        for (int i = 0; i < lines.size(); i++) {
            relationMap.put(renderList.size() + i, optionList.size());
        }
        optionList.add(literal);
        renderList.addAll(lines);
        optionCallbacks.add(onClick);
        if (tip != null) {
            tipsMap.put(optionList.size() - 1, Text.literal(tip));
        }
        return this;
    }

    /**
     * 添加选项
     */
    public PopupOption addOption(@NonNull Text text) {
        return addOption(text, null, null);
    }

    /**
     * 添加选项
     */
    public PopupOption addOption(@NonNull Text text, @Nullable Text tip) {
        return addOption(text, tip, null);
    }

    /**
     * 添加选项，可单独设置点击回调
     */
    public PopupOption addOption(@NonNull Text text, @Nullable Text tip, @Nullable Consumer<SelectEvent> onClick) {
        ensureNotBuilt();
        List<Text> lines = Arrays.stream(StringUtils.replaceLineBreak(text.content()).split("\n"))
                .map(s -> text.clone().text(s).hoverText(s).withStyle(text))
                .collect(Collectors.toList());
        for (int i = 0; i < lines.size(); i++) {
            relationMap.put(renderList.size() + i, optionList.size());
        }
        optionList.add(text);
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
    public PopupOption build(FontRenderer font, double x, double y, String id) {
        this.font = font;
        return showAt(x, y, id);
    }

    /**
     * 清空并重置，可再次 addOption + showAt
     */
    public PopupOption clear() {
        optionList.clear();
        renderList.clear();
        tipsMap.clear();
        relationMap.clear();
        optionCallbacks.clear();
        width = height = -1;
        x = y = -1;
        adjustedX = adjustedY = -1;
        maxWidth = maxHeight = -1;
        selectedIndex = -1;
        scrollOffset = maxLines = 0;
        built = false;
        renderCoordinate(new ScreenCoordinate(0, 0, 0, 0));
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

    @NonNull
    public String getSelectedString() {
        int idx = getSelectedIndex();
        return (idx >= 0 && idx < optionList.size()) ? optionList.get(idx).content() : "";
    }

    /**
     * 尝试处理选项点击。优先调用选项单独设置的回调，若无则调用全局 onSelect。
     */
    public boolean tryHandleOptionClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered()) return false;
        int idx = getSelectedIndex();
        if (idx < 0) return false;
        String text = getSelectedString();
        SelectEvent event = new SelectEvent(idx, text);
        Consumer<SelectEvent> cb = (idx >= 0 && idx < optionCallbacks.size()) ? optionCallbacks.get(idx) : null;
        if (cb == null) cb = onSelect;
        clear();
        if (cb != null) {
            cb.accept(event);
        }
        return true;
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
    public void render(MatrixStack matrixStack, float partialTicks) {
        if (beforeRender != null) beforeRender.accept(this);
        if (CollectionUtils.isNullOrEmpty(optionList) || Minecraft.getInstance().screen == null) {
            if (afterRender != null) afterRender.accept(this);
            return;
        }

        InputStateManager inputState = screen.inputState();
        selectedIndex = findHoveredIndex(inputState.mouseX(), inputState.mouseY());

        BaniraColorConfig theme = screen.getEffectiveTheme();
        int popupBg = theme.popupBg();
        int popupBorder = theme.popupBorder();
        int popupItemHover = theme.popupItemHover();
        int textColorUnselected = theme.popupItemText();
        int textColorSelected = theme.popupItemTextSelected();

        AbstractGuiUtils.renderByDepth(matrixStack, renderDepth(), stack -> {
            ShapeDrawArgs fillArgs = ShapeDrawArgs.rect(stack, adjustedX, adjustedY, width, height, popupBg);
            fillArgs.rect().radius(radius).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(fillArgs);
            ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(stack, adjustedX, adjustedY, width, height, popupBorder);
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
            renderOptionTip(matrixStack, inputState);
        }
        if (afterRender != null) afterRender.accept(this);
    }

    @Override
    protected boolean onMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        return addScrollOffset(scrollDelta);
    }

    /**
     * 供 BaniraScreen 直接调用渲染
     */
    public void render(MatrixStack matrixStack, InputStateManager inputState) {
        render(matrixStack, 0);
    }

    private void ensureNotBuilt() {
        if (built) {
            throw new IllegalStateException("Cannot add options after showAt/build. Call clear() first.");
        }
    }

    private void layout(int px, int py) {
        Objects.requireNonNull(Minecraft.getInstance().screen);
        screenWidth = Minecraft.getInstance().screen.width;
        screenHeight = Minecraft.getInstance().screen.height;
        if (maxWidth <= 0) maxWidth = screenWidth - MARGIN * 2;
        if (maxHeight <= 0) maxHeight = screenHeight - MARGIN * 2;

        width = Math.min(AbstractGuiUtils.getTextWidth(font, renderList) + PAD_LEFT + PAD_RIGHT, maxWidth);
        height = Math.min(AbstractGuiUtils.getTextHeight(font, renderList) + renderList.size() - 1 + PAD_TOP + PAD_BOTTOM, maxHeight);
        maxLines = ((height - PAD_TOP - PAD_BOTTOM) + 1) / (font.lineHeight + 1);

        x = px;
        y = py;
        adjustedX = px + MARGIN;
        adjustedY = py + MARGIN;

        if (adjustedY + height + MARGIN > screenHeight) adjustedY = py - height - MARGIN + 1;
        if (adjustedX + width + MARGIN > screenWidth) adjustedX = px - width - MARGIN + 1;
        adjustedX = Math.max(MARGIN, Math.min(adjustedX, screenWidth - width - MARGIN));
        adjustedY = Math.max(MARGIN, Math.min(adjustedY, screenHeight - height - MARGIN));

        renderCoordinate(new ScreenCoordinate(adjustedX, adjustedY, width, height));
    }

    private void renderOptionTip(MatrixStack matrixStack, InputStateManager inputState) {
        int optIdx = getSelectedIndex();
        if (optIdx < 0) return;
        Text tip = tipsMap.get(optIdx);
        if (tip == null || StringUtils.isNullOrEmptyEx(tip.content())) return;

        FontDrawArgs args = FontDrawArgs.of(tip.stack(matrixStack).font(font))
                .padding(4).margin(MARGIN)
                .x(inputState.mouseX()).y(inputState.mouseY())
                .inScreen(true);
        TooltipWidget.drawPopupMessage(matrixStack, args, screen.getEffectiveTheme(), screen.season());
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
