package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.util.List;

/**
 * 下拉选择框的浮层，用于渲染下拉列表并拦截点击/滚动，防止穿透到下方控件。
 */
class DropdownOverlayWidget extends BaseWidget {

    private static final int ITEM_HEIGHT = 20;
    private static final int PAD = 4;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 2;

    private final DropdownSelectWidget parent;
    private boolean scrollbarDragging;
    private double scrollbarDragStartY;
    private int scrollbarDragStartOffset;
    private int pressedOptionIndex = -1;
    private boolean pressedInCloseArea = false;

    DropdownOverlayWidget(BaniraScreen screen, DropdownSelectWidget parent) {
        super(screen, createFullScreenBounds());
        this.parent = parent;
        this.renderDepth(EnumRenderDepth.TOOLTIP);
    }

    private static ScreenCoordinate createFullScreenBounds() {
        Screen mcScreen = Minecraft.getInstance().screen;
        int w = mcScreen != null ? mcScreen.width : 400;
        int h = mcScreen != null ? mcScreen.height : 300;
        return new ScreenCoordinate(0, 0, w, h);
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!parent.dropdownOpen()) return;

        ScreenCoordinate db = parent.getDropdownBounds();
        if (db == null) return;

        List<DropdownOption> options = parent.getFilteredOptionEntries();
        if (options.isEmpty()) return;

        FontRenderer font = Minecraft.getInstance().font;
        BaniraScreen scr = screen;
        if (scr == null) return;
        BaniraColorConfig theme = scr.getEffectiveTheme();
        int popupBg = theme.popupBg();
        int popupBorder = theme.popupBorder();
        int popupSelected = theme.popupItemSelected();
        int popupSelectedBorder = theme.popupItemSelectedBorder();
        int textColorUnselected = theme.popupItemText();
        int textColorSelected = theme.popupItemTextSelected();
        int scrollbarBg = theme.scrollbarBg();
        int scrollbarThumb = theme.scrollbarThumb();
        int scrollbarThumbHover = theme.scrollbarThumbHover();

        double mouseX = scr.inputState().mouseX();
        double mouseY = scr.inputState().mouseY();

        int contentHeight = options.size() * ITEM_HEIGHT;
        int visibleHeight = (int) db.height() - PAD * 2;
        boolean scrollable = contentHeight > visibleHeight;
        int iconCol = parent.hasAnyDropdownIcon() ? DropdownSelectWidget.DROPDOWN_ICON_COLUMN : 0;
        int contentWidth = scrollable ? (int) db.width() - PAD * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN : (int) db.width() - 2;

        int hoveredIndex = findHoveredOptionIndex(db, options, mouseX, mouseY, contentWidth);
        boolean scrollbarThumbHovered = scrollable && isMouseOverScrollbarThumb(db, mouseX, mouseY);

        AbstractGuiUtils.renderByDepth(stack, renderDepth(), s -> {
            ShapeDrawArgs bgArgs = ShapeDrawArgs.rect(s, (int) db.x(), (int) db.y(), (int) db.width(), (int) db.height(), popupBg);
            bgArgs.rect().radius(2).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(bgArgs);

            ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(s, (int) db.x(), (int) db.y(), (int) db.width(), (int) db.height(), popupBorder);
            borderArgs.rect().radius(2).border(1).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(borderArgs);

            AbstractGuiUtils.enableScissor((int) db.x() + 1, (int) db.y() + 1, contentWidth, (int) db.height() - 2);

            int scrollOffset = parent.dropdownScrollOffset();
            int visibleCount = visibleHeight / ITEM_HEIGHT;
            int startIdx = scrollOffset / ITEM_HEIGHT;
            int endIdx = Math.min(startIdx + visibleCount + 1, options.size());
            int leftBorderWidth = 3;

            for (int i = startIdx; i < endIdx; i++) {
                int itemY = (int) db.y() + PAD + i * ITEM_HEIGHT - scrollOffset;
                if (itemY + ITEM_HEIGHT < db.y() || itemY >= db.y() + db.height()) continue;

                DropdownOption optEntry = options.get(i);
                String opt = optEntry.displayLabel();
                boolean selected = parent.isOptionSelected(optEntry.value());
                boolean hovered = hoveredIndex == i;
                int itemBg = hovered ? popupSelected : (selected ? popupSelected : 0);
                if (itemBg != 0) {
                    AbstractGuiUtils.fill(s, (int) db.x() + 1, itemY, contentWidth, ITEM_HEIGHT, itemBg);
                }
                boolean multi = parent.multiSelect();
                if (selected && multi) {
                    AbstractGuiUtils.fill(s, (int) db.x() + 1, itemY, leftBorderWidth, ITEM_HEIGHT, popupSelectedBorder);
                }

                int leftOffset = multi ? leftBorderWidth : 0;
                int textX = (int) (db.x() + PAD + leftOffset + iconCol);
                int textMaxWidth = contentWidth - PAD * 2 - 4 - leftOffset - iconCol;
                if (optEntry.hasTexture()) {
                    for (Texture t : optEntry.texture()) {
                        if (t != null) {
                            int iconX = (int) (db.x() + PAD + leftOffset + DropdownSelectWidget.DROPDOWN_ICON_INSET);
                            int sz = DropdownSelectWidget.DROPDOWN_ICON_DRAW_SIZE;
                            int iconY = itemY + (ITEM_HEIGHT - sz) / 2;
                            ImageWidget.blitBlend(s, t, iconX, iconY, sz, sz);
                        }
                    }
                } else if (!optEntry.icon().isEmpty()) {
                    int iconX = (int) (db.x() + PAD + leftOffset + DropdownSelectWidget.DROPDOWN_ICON_INSET);
                    int sz = DropdownSelectWidget.DROPDOWN_ICON_DRAW_SIZE;
                    int iconY = itemY + (ITEM_HEIGHT - sz) / 2;
                    ItemWidget.renderGuiItemFlatBlit(s, Minecraft.getInstance(), optEntry.icon(), iconX, iconY, sz);
                }
                String display = font.plainSubstrByWidth(opt, textMaxWidth);
                int textColor = (selected || hovered) ? textColorSelected : textColorUnselected;
                font.draw(s, display, textX, itemY + (ITEM_HEIGHT - font.lineHeight) / 2f, textColor);
            }

            AbstractGuiUtils.disableScissor();

            if (scrollable) {
                drawScrollbar(s, db, scrollOffset, contentHeight, visibleHeight, scrollbarBg,
                        scrollbarDragging || scrollbarThumbHovered ? scrollbarThumbHover : scrollbarThumb);
            }
        });

        if (hoveredIndex >= 0 && hoveredIndex < options.size()) {
            Component tip = options.get(hoveredIndex).tooltip();
            if (tip != null && !tip.isEmpty()) {
                boolean useTexture = theme.tooltipUseTexture();
                Text textToDraw = new Text(tip.clone());
                int tipX = (int) scr.inputState().mouseX();
                int tipY = (int) scr.inputState().mouseY();
                EnumSeason season = scr.season();
                scr.addDeferredTooltipRender(t -> {
                    t.pushPose();
                    t.last().pose().setIdentity();
                    TooltipWidget.drawPopupMessage(t, FontDrawArgs.ofPopo(textToDraw.stack(t)).x(tipX).y(tipY).popupUseTexture(useTexture), theme, season);
                    t.popPose();
                });
            }
        }
    }

    private int findHoveredOptionIndex(ScreenCoordinate db, List<DropdownOption> options, double mouseX, double mouseY, int contentWidth) {
        if (mouseX < db.x() || mouseX >= db.x() + contentWidth) return -1;
        if (mouseY < db.y() + PAD || mouseY >= db.y() + db.height() - PAD) return -1;

        int scrollOffset = parent.dropdownScrollOffset();
        int relY = (int) (mouseY - db.y() - PAD + scrollOffset);
        int idx = relY / ITEM_HEIGHT;
        if (idx >= 0 && idx < options.size()) return idx;
        return -1;
    }

    private void drawScrollbar(MatrixStack s, ScreenCoordinate db, int scrollOffset, int contentHeight, int visibleHeight,
                               int trackColor, int thumbColor) {
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) return;

        int trackX = (int) (db.x() + db.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) db.y() + PAD;
        int trackH = visibleHeight;

        AbstractGuiUtils.fill(s, trackX, trackY, SCROLLBAR_WIDTH, trackH, trackColor);

        double thumbRatio = (double) visibleHeight / contentHeight;
        int thumbH = Math.max(8, (int) (thumbRatio * trackH));
        double scrollRatio = (double) scrollOffset / maxScroll;
        int thumbY = trackY + (int) ((trackH - thumbH) * scrollRatio);
        AbstractGuiUtils.fill(s, trackX, thumbY, SCROLLBAR_WIDTH, thumbH, thumbColor);
    }

    private boolean isMouseOverScrollbarTrack(ScreenCoordinate db, double mouseX, double mouseY) {
        int trackX = (int) (db.x() + db.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) db.y() + PAD;
        int trackH = (int) db.height() - PAD * 2;
        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH
                && mouseY >= trackY && mouseY < trackY + trackH;
    }

    private boolean isMouseOverScrollbarThumb(ScreenCoordinate db, double mouseX, double mouseY) {
        List<DropdownOption> options = parent.getFilteredOptionEntries();
        int contentHeight = options.size() * ITEM_HEIGHT;
        int visibleHeight = (int) db.height() - PAD * 2;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) return false;

        int trackX = (int) (db.x() + db.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) db.y() + PAD;
        int trackH = visibleHeight;
        double thumbRatio = (double) visibleHeight / contentHeight;
        int thumbH = Math.max(8, (int) (thumbRatio * trackH));
        double scrollRatio = (double) parent.dropdownScrollOffset() / maxScroll;
        int thumbY = trackY + (int) ((trackH - thumbH) * scrollRatio);

        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        if (event == null || event.button() != 0) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();

        if (parent.isInExcludedCloseArea(mouseX, mouseY)) {
            return false;
        }
        if (parent.shouldCloseOnClick(mouseX, mouseY)) {
            pressedInCloseArea = true;
            pressedOptionIndex = -1;
            return true;
        }

        if (parent.isInDropdownOptions(mouseX, mouseY)) {
            ScreenCoordinate db = parent.getDropdownBounds();
            if (db != null) {
                List<DropdownOption> options = parent.getFilteredOptionEntries();
                int contentHeight = options.size() * ITEM_HEIGHT;
                int visibleHeight = (int) db.height() - PAD * 2;
                boolean scrollable = contentHeight > visibleHeight;
                int contentWidth = scrollable ? (int) db.width() - PAD * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN : (int) db.width();

                if (scrollable && isMouseOverScrollbarTrack(db, mouseX, mouseY)) {
                    int trackY = (int) db.y() + PAD;
                    int trackH = visibleHeight;
                    int maxScroll = parent.getMaxDropdownScroll();
                    double thumbRatio = (double) visibleHeight / contentHeight;
                    int thumbH = Math.max(8, (int) (thumbRatio * trackH));
                    double relY = mouseY - trackY;
                    int newOffset = (int) ((relY - thumbH / 2.0) / Math.max(1, trackH - thumbH) * maxScroll);
                    parent.setDropdownScrollOffset(Math.max(0, Math.min(maxScroll, newOffset)));
                    scrollbarDragging = true;
                    scrollbarDragStartY = mouseY;
                    scrollbarDragStartOffset = parent.dropdownScrollOffset();
                    return true;
                }

                int idx = findHoveredOptionIndex(db, options, mouseX, mouseY, scrollable ? contentWidth : (int) db.width());
                if (idx >= 0 && idx < options.size()) {
                    pressedOptionIndex = idx;
                    pressedInCloseArea = false;
                    return true;
                }
            }
        }

        pressedOptionIndex = -1;
        pressedInCloseArea = false;
        if (parent.getInputBounds() != null && isInBounds(mouseX, mouseY, parent.getInputBounds())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean handleMouseRelease(MouseEvent event) {
        if (event == null) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        if (event.button() == 0 && scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (event.button() == 0 && pressedInCloseArea) {
            if (parent.shouldCloseOnClick(mouseX, mouseY)) {
                parent.closeDropdown();
            }
            pressedInCloseArea = false;
            return true;
        }
        if (event.button() == 0 && pressedOptionIndex >= 0) {
            ScreenCoordinate db = parent.getDropdownBounds();
            if (db != null) {
                List<DropdownOption> options = parent.getFilteredOptionEntries();
                int contentHeight = options.size() * ITEM_HEIGHT;
                int visibleHeight = (int) db.height() - PAD * 2;
                boolean scrollable = contentHeight > visibleHeight;
                int contentWidth = scrollable ? (int) db.width() - PAD * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN : (int) db.width();
                int releaseIdx = findHoveredOptionIndex(db, options, mouseX, mouseY, contentWidth);
                if (releaseIdx == pressedOptionIndex && releaseIdx >= 0 && releaseIdx < options.size()) {
                    parent.selectOption(options.get(releaseIdx).value());
                }
            }
            pressedOptionIndex = -1;
            return true;
        }
        pressedOptionIndex = -1;
        pressedInCloseArea = false;
        return super.handleMouseRelease(event);
    }

    @Override
    public boolean handleMouseDrag(MouseDragEvent event) {
        if (event == null) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        if (scrollbarDragging && event.button() == 0) {
            ScreenCoordinate db = parent.getDropdownBounds();
            if (db != null) {
                List<String> options = parent.getFilteredOptions();
                int contentHeight = options.size() * ITEM_HEIGHT;
                int visibleHeight = (int) db.height() - PAD * 2;
                int maxScroll = parent.getMaxDropdownScroll();
                if (maxScroll > 0) {
                    int trackH = visibleHeight;
                    double thumbRatio = (double) visibleHeight / contentHeight;
                    int thumbH = Math.max(8, (int) (thumbRatio * trackH));
                    double deltaY = mouseY - scrollbarDragStartY;
                    int deltaScroll = (int) (deltaY / Math.max(1, trackH - thumbH) * maxScroll);
                    int newOffset = Math.max(0, Math.min(maxScroll, scrollbarDragStartOffset + deltaScroll));
                    parent.setDropdownScrollOffset(newOffset);
                    scrollbarDragStartY = mouseY;
                    scrollbarDragStartOffset = newOffset;
                }
            }
            return true;
        }
        return super.handleMouseDrag(event);
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (event != null && parent.handleDropdownScroll(event.mouseX(), event.mouseY(), event.delta())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseInside(double mouseX, double mouseY) {
        return true;
    }

    private boolean isInBounds(double mx, double my, ScreenCoordinate b) {
        return mx >= b.x() && mx < b.x() + b.width() && my >= b.y() && my < b.y() + b.height();
    }
}
