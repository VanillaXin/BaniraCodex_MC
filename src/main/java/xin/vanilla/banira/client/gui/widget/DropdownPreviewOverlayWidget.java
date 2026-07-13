package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.internal.client.BaniraClientAccess;

import java.util.List;


class DropdownPreviewOverlayWidget extends BaseWidget {

    private static final int ITEM_HEIGHT = 24;
    private static final int PAD = 6;
    private static final int TAG_CLOSE_SIZE = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 2;

    private final DropdownSelectWidget parent;
    private boolean scrollbarDragging = false;
    private double scrollbarDragStartY = 0;
    private int scrollbarDragStartOffset = 0;

    DropdownPreviewOverlayWidget(BaniraScreen screen, DropdownSelectWidget parent) {
        super(screen, createFullScreenBounds());
        this.parent = parent;
        // 在 InputFormScreen 等带列表裁剪的界面中延后绘制，避免整层被裁掉
        this.renderDepth(EnumRenderDepth.TOOLTIP);
    }

    private static ScreenCoordinate createFullScreenBounds() {
        return new ScreenCoordinate(0, 0, AbstractGuiUtils.getScreenSize().key(), AbstractGuiUtils.getScreenSize().val());
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!parent.previewExpanded()) return;

        ScreenCoordinate pb = parent.getPreviewBounds();
        if (pb == null) return;

        List<String> items = parent.getSelectedValues();
        if (items.isEmpty()) {
            BaniraClientAccess.runOnClientThread(parent::closePreview);
            return;
        }

        Font font = AbstractGuiUtils.getFont();
        BaniraScreen scr = screen;
        if (scr == null) return;
        BaniraColorConfig theme = scr.getEffectiveTheme();
        int popupBg = theme.popupBg();
        int popupBorder = theme.popupBorder();
        int popupSelected = theme.popupItemSelected();
        int textColor = theme.listItemText();
        int scrollbarBg = theme.scrollbarBg();
        int scrollbarThumb = theme.scrollbarThumb();
        int scrollbarThumbHover = theme.scrollbarThumbHover();

        double mouseX = scr.inputState().mouseX();
        double mouseY = scr.inputState().mouseY();

        int contentHeight = items.size() * ITEM_HEIGHT;
        int visibleHeight = (int) pb.height() - PAD * 2;
        boolean scrollable = contentHeight > visibleHeight;
        int itemAreaWidth = scrollable ? (int) pb.width() - PAD * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN - 2 : (int) pb.width() - 2;
        int contentWidth = itemAreaWidth;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        parent.setPreviewScrollOffset(Math.min(parent.getPreviewScrollOffset(), maxScroll));
        int scrollOffset = parent.getPreviewScrollOffset();
        boolean scrollbarThumbHovered = scrollable && isMouseOverScrollbarThumb(pb, mouseX, mouseY);

        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.TOOLTIP, s -> {
            ShapeDrawArgs bgArgs = ShapeDrawArgs.rect(s, (int) pb.x(), (int) pb.y(), (int) pb.width(), (int) pb.height(), popupBg);
            bgArgs.rect().radius(4).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(bgArgs);

            ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(s, (int) pb.x(), (int) pb.y(), (int) pb.width(), (int) pb.height(), popupBorder);
            borderArgs.rect().radius(4).border(1).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(borderArgs);

            AbstractGuiUtils.pushScissor((int) pb.x() + 1, (int) pb.y() + 1, contentWidth, (int) pb.height() - 2);

            int contentY = (int) pb.y() + PAD - scrollOffset;
            try {
                for (int i = 0; i < items.size(); i++) {
                    int itemY = contentY + i * ITEM_HEIGHT;
                    if (itemY + ITEM_HEIGHT < pb.y() || itemY >= pb.y() + pb.height()) continue;

                    String item = items.get(i);
                    int textMaxWidth = contentWidth - PAD * 2 - TAG_CLOSE_SIZE - 4;
                    String display = font.plainSubstrByWidth(item, textMaxWidth);

                    boolean hovered = mouseX >= pb.x() && mouseX < pb.x() + pb.width()
                            && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

                    if (hovered) {
                        AbstractGuiUtils.fill(s, (int) pb.x() + 1, itemY, contentWidth, ITEM_HEIGHT, popupSelected);
                    }

                    font.draw(s, display, (int) pb.x() + PAD, itemY + (ITEM_HEIGHT - font.lineHeight) / 2f, textColor);

                    int closeX = (int) (pb.x() + contentWidth - PAD - TAG_CLOSE_SIZE);
                    int closeY = itemY + (ITEM_HEIGHT - TAG_CLOSE_SIZE) / 2;
                    boolean closeHovered = mouseX >= closeX && mouseX < closeX + TAG_CLOSE_SIZE
                            && mouseY >= closeY && mouseY < closeY + TAG_CLOSE_SIZE;
                    int closeColor = closeHovered ? 0xFFE53935 : 0xFF999999;
                    AbstractGuiUtils.fill(s, closeX, closeY, TAG_CLOSE_SIZE, TAG_CLOSE_SIZE, closeColor);
                    float r = 2f;
                    int cx = closeX + TAG_CLOSE_SIZE / 2;
                    int cy = closeY + TAG_CLOSE_SIZE / 2;
                    AbstractGuiUtils.drawLine(s, cx - r, cy - r, cx + r, cy + r, 1f, 0xFFFFFFFF);
                    AbstractGuiUtils.drawLine(s, cx + r, cy - r, cx - r, cy + r, 1f, 0xFFFFFFFF);
                }
            } finally {
                AbstractGuiUtils.popScissor();
            }

            if (scrollable) {
                drawScrollbar(s, pb, scrollOffset, contentHeight, visibleHeight, scrollbarBg,
                        scrollbarDragging || scrollbarThumbHovered ? scrollbarThumbHover : scrollbarThumb);
            }
        });
    }

    private void drawScrollbar(PoseStack s, ScreenCoordinate pb, int scrollOffset, int contentHeight, int visibleHeight,
                               int trackColor, int thumbColor) {
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) return;

        int trackX = (int) (pb.x() + pb.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) pb.y() + PAD;
        int trackH = visibleHeight;

        AbstractGuiUtils.fill(s, trackX, trackY, SCROLLBAR_WIDTH, trackH, trackColor);

        double thumbRatio = (double) visibleHeight / contentHeight;
        int thumbH = Math.max(8, (int) (thumbRatio * trackH));
        double scrollRatio = (double) scrollOffset / maxScroll;
        int thumbY = trackY + (int) ((trackH - thumbH) * scrollRatio);
        AbstractGuiUtils.fill(s, trackX, thumbY, SCROLLBAR_WIDTH, thumbH, thumbColor);
    }

    private boolean isMouseOverScrollbarTrack(ScreenCoordinate pb, double mouseX, double mouseY) {
        int trackX = (int) (pb.x() + pb.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) pb.y() + PAD;
        int trackH = (int) pb.height() - PAD * 2;
        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH
                && mouseY >= trackY && mouseY < trackY + trackH;
    }

    private boolean isMouseOverScrollbarThumb(ScreenCoordinate pb, double mouseX, double mouseY) {
        List<String> items = parent.getSelectedValues();
        int contentHeight = items.size() * ITEM_HEIGHT;
        int visibleHeight = (int) pb.height() - PAD * 2;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) return false;

        int trackX = (int) (pb.x() + pb.width() - PAD - SCROLLBAR_WIDTH);
        int trackY = (int) pb.y() + PAD;
        int trackH = visibleHeight;
        double thumbRatio = (double) visibleHeight / contentHeight;
        int thumbH = Math.max(8, (int) (thumbRatio * trackH));
        double scrollRatio = (double) parent.getPreviewScrollOffset() / maxScroll;
        int thumbY = trackY + (int) ((trackH - thumbH) * scrollRatio);

        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        if (event == null || event.button() != 0) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();

        ScreenCoordinate pb = parent.getPreviewBounds();
        if (pb == null) return false;
        if (mouseX < pb.x() || mouseX >= pb.x() + pb.width() || mouseY < pb.y() || mouseY >= pb.y() + pb.height()) {
            parent.closePreview();
            return true;
        }

        List<String> items = parent.getSelectedValues();
        int contentHeight = items.size() * ITEM_HEIGHT;
        int visibleHeight = (int) pb.height() - PAD * 2;
        boolean scrollable = contentHeight > visibleHeight;

        if (scrollable && isMouseOverScrollbarTrack(pb, mouseX, mouseY)) {
            int trackY = (int) pb.y() + PAD;
            int trackH = visibleHeight;
            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            double thumbRatio = (double) visibleHeight / contentHeight;
            int thumbH = Math.max(8, (int) (thumbRatio * trackH));
            double relY = mouseY - trackY;
            int newOffset = (int) ((relY - thumbH / 2.0) / Math.max(1, trackH - thumbH) * maxScroll);
            parent.setPreviewScrollOffset(Math.max(0, Math.min(maxScroll, newOffset)));
            scrollbarDragging = true;
            scrollbarDragStartY = mouseY;
            scrollbarDragStartOffset = parent.getPreviewScrollOffset();
            return true;
        }

        int itemAreaWidth = scrollable ? (int) pb.width() - PAD * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN - 2 : (int) pb.width() - 2;
        int contentY = (int) pb.y() + PAD - parent.getPreviewScrollOffset();
        for (int i = 0; i < items.size(); i++) {
            int itemY = contentY + i * ITEM_HEIGHT;
            if (mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                int closeX = (int) (pb.x() + itemAreaWidth - PAD - TAG_CLOSE_SIZE);
                int closeY = itemY + (ITEM_HEIGHT - TAG_CLOSE_SIZE) / 2;
                if (mouseX >= closeX && mouseX < closeX + TAG_CLOSE_SIZE && mouseY >= closeY && mouseY < closeY + TAG_CLOSE_SIZE) {
                    parent.removeSelectedValueAt(i);
                    return true;
                }
                break;
            }
        }
        return true;
    }

    @Override
    public boolean handleMouseRelease(MouseEvent event) {
        if (event != null && event.button() == 0 && scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        return super.handleMouseRelease(event);
    }

    @Override
    public boolean handleMouseDrag(MouseDragEvent event) {
        if (event == null) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        if (scrollbarDragging && event.button() == 0) {
            ScreenCoordinate pb = parent.getPreviewBounds();
            if (pb != null) {
                List<String> items = parent.getSelectedValues();
                int contentHeight = items.size() * ITEM_HEIGHT;
                int visibleHeight = (int) pb.height() - PAD * 2;
                int maxScroll = Math.max(0, contentHeight - visibleHeight);
                if (maxScroll > 0) {
                    int trackH = visibleHeight;
                    double thumbRatio = (double) visibleHeight / contentHeight;
                    int thumbH = Math.max(8, (int) (thumbRatio * trackH));
                    double deltaY = mouseY - scrollbarDragStartY;
                    int deltaScroll = (int) (deltaY / Math.max(1, trackH - thumbH) * maxScroll);
                    int newOffset = Math.max(0, Math.min(maxScroll, scrollbarDragStartOffset + deltaScroll));
                    parent.setPreviewScrollOffset(newOffset);
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
        if (event == null) return false;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double scrollDelta = event.delta();
        ScreenCoordinate pb = parent.getPreviewBounds();
        if (pb == null) return false;
        if (mouseX < pb.x() || mouseX >= pb.x() + pb.width() || mouseY < pb.y() || mouseY >= pb.y() + pb.height()) {
            return false;
        }
        List<String> items = parent.getSelectedValues();
        int contentHeight = items.size() * ITEM_HEIGHT;
        int visibleHeight = (int) pb.height() - PAD * 2;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) return false;
        int step = ITEM_HEIGHT;
        int current = parent.getPreviewScrollOffset();
        if (scrollDelta > 0) {
            parent.setPreviewScrollOffset(Math.max(0, current - step));
        } else {
            parent.setPreviewScrollOffset(Math.min(maxScroll, current + step));
        }
        return true;
    }

    @Override
    public boolean isMouseInside(double mouseX, double mouseY) {
        return true;
    }

    @Override
    public IWidget getFocusTarget() {
        return parent != null ? parent.getFocusTarget() : this;
    }
}
