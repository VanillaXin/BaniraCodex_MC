package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.IReorderingProcessor;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可滚动的只读长文本界面，适合帮助、规则说明和迁移提示。
 */
public final class ReadOnlyTextScreen extends BaniraScreen {
    private static final int MARGIN = 12;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;
    private static final int RADIUS = 6;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 4;

    private final Args args;
    private final List<IReorderingProcessor> lines = new ArrayList<>();
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int maxScroll;
    private int scroll;

    public ReadOnlyTextScreen(@Nonnull Args args) {
        super(args.title() != null ? args.title() : BaniraComponent.get().literal(""));
        this.args = args;
        previousScreen(args.parentScreen());
        BaniraScreen.inheritThemeAndSeason(this, args.parentScreen(), args.theme(), args.season());
    }

    /**
     * 只读文本页参数。每个段落会独立换行，并在段落间保留一行间距。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        @Nullable
        private Screen parentScreen;
        @Nullable
        private BaniraColorConfig theme;
        @Nullable
        private EnumSeason season;
        @Nullable
        private Component title;
        private List<Component> paragraphs = Collections.emptyList();
        @Nullable
        private Component closeText;
    }

    @Override
    protected void initWidgets() {
        int cardX = MARGIN;
        int cardY = MARGIN;
        int cardWidth = Math.max(160, width - MARGIN * 2);
        int cardHeight = Math.max(100, height - MARGIN * 2);
        contentX = cardX + PADDING;
        contentY = cardY + PADDING + TITLE_HEIGHT + GAP;
        contentWidth = cardWidth - PADDING * 2;
        contentHeight = cardHeight - PADDING * 3 - TITLE_HEIGHT - BUTTON_HEIGHT - GAP * 2;

        LabelWidget title = new LabelWidget(this);
        title.bounds(new ScreenCoordinate(contentX, cardY + PADDING, contentWidth, TITLE_HEIGHT));
        title.text(args.title() != null ? args.title() : BaniraComponent.get().literal(""));
        title.textWrap(false);
        title.textVerticalAlign(EnumAlignment.CENTER);
        addWidget(title);

        ButtonWidget close = new ButtonWidget(this);
        close.bounds(new ScreenCoordinate(contentX,
                cardY + cardHeight - PADDING - BUTTON_HEIGHT,
                contentWidth, BUTTON_HEIGHT));
        close.text(args.closeText() != null
                ? args.closeText()
                : BaniraComponent.get().transClientAuto("close"));
        close.onClick(button -> onClose());
        addWidget(close);

        rebuildLines();
    }

    private void rebuildLines() {
        lines.clear();
        List<Component> paragraphs = args.paragraphs() != null
                ? args.paragraphs()
                : Collections.emptyList();
        for (int i = 0; i < paragraphs.size(); i++) {
            Component paragraph = paragraphs.get(i);
            if (paragraph != null) {
                lines.addAll(font.split(paragraph.toVanilla(),
                        Math.max(1, contentWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP)));
            }
            if (i + 1 < paragraphs.size()) {
                lines.add(IReorderingProcessor.EMPTY);
            }
        }
        int totalHeight = lines.size() * font.lineHeight;
        maxScroll = Math.max(0, totalHeight - contentHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        AbstractGui.fill(stack, 0, 0, width, height, 0x55000000);
        BaniraColorConfig theme = getEffectiveTheme();
        int cardColor = ColorUtils.applyAlphaToArgb(theme.bgSurface(), 0xFF);
        AbstractGuiUtils.drawRoundedRect(stack, MARGIN, MARGIN,
                width - MARGIN * 2, height - MARGIN * 2,
                RADIUS, RADIUS, RADIUS, RADIUS, cardColor);

        AbstractGuiUtils.enableScissor(contentX, contentY, contentWidth, Math.max(1, contentHeight));
        int color = theme.textPrimary() | 0xFF000000;
        for (int i = 0; i < lines.size(); i++) {
            int y = contentY + i * font.lineHeight - scroll;
            if (y + font.lineHeight >= contentY && y < contentY + contentHeight) {
                font.draw(stack, lines.get(i), contentX, y, color);
            }
        }
        AbstractGuiUtils.disableScissor();
        renderScrollbar(stack, theme);
        renderWidgets(stack, partialTicks);
    }

    private void renderScrollbar(MatrixStack stack, BaniraColorConfig theme) {
        if (maxScroll <= 0) {
            return;
        }
        int x = contentX + contentWidth - SCROLLBAR_WIDTH;
        AbstractGui.fill(stack, x, contentY, x + SCROLLBAR_WIDTH, contentY + contentHeight,
                ColorUtils.applyAlphaToArgb(theme.border(), 0x55));
        int totalHeight = contentHeight + maxScroll;
        int thumbHeight = Math.max(10, contentHeight * contentHeight / totalHeight);
        int thumbY = contentY + scroll * (contentHeight - thumbHeight) / maxScroll;
        AbstractGui.fill(stack, x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                theme.accentHover() | 0xFF000000);
    }

    @Override
    protected void onMouseScrolled(MouseScrolledHandleArgs eventArgs) {
        if (eventArgs.mouseX() < contentX || eventArgs.mouseX() >= contentX + contentWidth
                || eventArgs.mouseY() < contentY || eventArgs.mouseY() >= contentY + contentHeight) {
            return;
        }
        scroll = Math.max(0, Math.min(maxScroll,
                scroll - (int) Math.signum(eventArgs.delta()) * font.lineHeight * 3));
        eventArgs.consumed(true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(args.parentScreen());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
