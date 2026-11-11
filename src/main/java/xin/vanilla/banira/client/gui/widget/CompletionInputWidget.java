package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.internal.client.ui.TextCompletionLayout;
import xin.vanilla.banira.internal.client.ui.TextCompletionState;

public final class CompletionInputWidget extends InputWidget {
    private final TextCompletionState completion;
    private Popup popup;

    public CompletionInputWidget(BaniraScreen screen, UiTextCompletionProvider provider) {
        super(screen);
        completion = new TextCompletionState(provider);
    }

    @Override
    public void update() {
        super.update();
        completion.update(value(), cursorPosition(), highlightPos(), visible() && enabled() && editable() && focused());
        if (!completion.options().isEmpty() && popup == null) {
            popup = new Popup();
            screen.addOverlayWidget(popup);
        } else if (completion.options().isEmpty()) hidePopup();
    }

    private void hidePopup() {
        if (popup != null) {
            screen.removeOverlayWidget(popup);
            popup = null;
        }
    }

    private void choose(int index) {
        UiTextCompletion choice = completion.choose(index, value(), cursorPosition());
        if (choice != null && choice.apply(value()).length() <= maxLength()) {
            setSelectionRange(choice.end(), choice.start());
            insertText(choice.text());
        }
        hidePopup();
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (event == null) return false;
        int key = event.keyCode();
        if (focused() && !completion.options().isEmpty()) {
            if (key == 264 || key == 265) {
                completion.move(key == 264 ? 1 : -1);
                return true;
            }
            if ((key == 258 && event.modifiers() == 0) || key == 257 || key == 335) {
                choose(completion.selected());
                return true;
            }
            if (key == 256) {
                completion.dismiss();
                hidePopup();
                return true;
            }
        }
        return super.onKeyPress(event);
    }

    private final class Popup extends BaseWidget {
        private int pressed = -1;
        private static final int ROW = 16;

        Popup() {
            super(CompletionInputWidget.this.screen, new ScreenCoordinate(0, 0, CompletionInputWidget.this.screen.width, CompletionInputWidget.this.screen.height));
            renderDepth(EnumRenderDepth.TOOLTIP);
        }

        private int first() {
            int count = Math.max(1, ((int) area().height() - 8) / ROW);
            return completion.selected() / count * count;
        }

        private ScreenCoordinate area() {
            return TextCompletionLayout.bounds(new ScreenCoordinate(CompletionInputWidget.this.absoluteX(),
                            CompletionInputWidget.this.absoluteY(), CompletionInputWidget.this.width(), CompletionInputWidget.this.height()),
                    new ScreenCoordinate(0, 0, screen.width, screen.height), completion.options().size(), ROW);
        }

        private int hit(double x, double y) {
            ScreenCoordinate box = area();
            if (x < box.x() || x >= box.x() + box.width() || y < box.y() + 4 || y >= box.y() + box.height() - 4)
                return -1;
            int index = first() + (int) ((y - box.y() - 4) / ROW);
            return index < completion.options().size() ? index : -1;
        }

        @Override
        public IWidget getFocusTarget() {
            return CompletionInputWidget.this;
        }

        @Override
        public boolean handleMouseClick(MouseEvent event) {
            pressed = hit(event.mouseX(), event.mouseY());
            if (pressed < 0) {
                completion.dismiss();
                hidePopup();
            }
            return event.button() == 0 && pressed >= 0;
        }

        @Override
        public boolean handleMouseRelease(MouseEvent event) {
            int index = hit(event.mouseX(), event.mouseY());
            boolean consume = event.button() == 0 && pressed >= 0;
            if (consume && index == pressed) choose(index);
            pressed = -1;
            return consume;
        }

        @Override
        public boolean handleMouseScroll(MouseScrollEvent event) {
            if (hit(event.mouseX(), event.mouseY()) < 0 || event.delta() == 0) return false;
            completion.move(event.delta() > 0 ? -1 : 1);
            return true;
        }

        @Override
        public void render(PoseStack graphics, float partialTicks) {
            if (completion.options().isEmpty()) return;
            ScreenCoordinate box = area();
            if (box.height() == 0) return;
            var theme = screen.getEffectiveTheme();
            var font = Minecraft.getInstance().font;
            int hovered = hit(screen.inputState().mouseX(), screen.inputState().mouseY());
            AbstractGuiUtils.renderByDepth(graphics, renderDepth(), stack -> {
                AbstractGuiUtils.fill(stack, (int) box.x(), (int) box.y(), (int) box.width(), (int) box.height(), theme.popupBorder());
                AbstractGuiUtils.fill(stack, (int) box.x() + 1, (int) box.y() + 1, (int) box.width() - 2, (int) box.height() - 2, theme.popupBg());
                int count = Math.max(0, ((int) box.height() - 8) / ROW);
                for (int index = first(); index < Math.min(first() + count, completion.options().size()); index++) {
                    int y = (int) box.y() + 4 + (index - first()) * ROW;
                    boolean active = index == completion.selected() || index == hovered;
                    if (active)
                        AbstractGuiUtils.fill(stack, (int) box.x() + 1, y, (int) box.width() - 2, ROW, theme.popupItemSelected());
                    String label = completion.options().get(index).text();
                    String tip = completion.options().get(index).tooltip();
                    if (!tip.isEmpty()) label += "  " + tip;
                    font.draw(stack, font.plainSubstrByWidth(label, (int) box.width() - 8), (int) box.x() + 4,
                            y + 4, active ? theme.popupItemTextSelected() : theme.popupItemText());
                }
            });
        }
    }
}
