package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static xin.vanilla.banira.client.data.BaniraColorToken.*;

/**
 * Notification 日志查看界面，横屏主从布局：左侧类型选择+简洁列表，右侧记录详情
 */
public class NotificationLogScreen extends BaniraScreen {

    private static final int LIST_ROW_HEIGHT = 24;
    private static final int PANEL_MARGIN = 12;
    /**
     * 左侧搜索框高度
     */
    private static final int SEARCH_BOX_H = 20;
    private static final int DIVIDER_W = 2;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_GAP = 2;
    private static final float CLOSE_BTN_SIZE = 10f;
    private static final int SCREEN_CARD_MARGIN = 16;
    private static final float CLOSE_BTN_PAD = 6f;
    private static final int TYPE_CFG_BTN_H = 22;
    private static final int TYPE_CFG_BTN_GAP = 6;
    private static final double LEFT_RATIO = 0.38;
    private static final int DETAIL_AFTER_TIME_GAP = 4;
    private static final int DETAIL_AFTER_META_GAP = 8;
    private static final int META_COL_GAP = 12;
    private static final int META_ROW_GAP = 4;
    private static final int META_FONT_SIZE = 10;

    private final Args args;
    private int leftX, leftY, leftW, leftH;
    private int listX, listY, listW, listH;
    private int rightX, rightY, rightW, rightH;
    private int visibleRows;
    private ScrollbarWidget scrollbarWidget;
    private InputWidget searchInput;
    private double scrollValue = 0;
    /**
     * 列表搜索（不区分大小写，匹配正文、来源、类型、样式、位置、动画、时长、时间等）
     */
    private String searchQuery = "";
    private List<NotificationLogEntry> allLogEntries = new ArrayList<>();
    private List<NotificationLogEntry> filteredEntries = new ArrayList<>();
    private int selectedIndex = -1;
    /**
     * 列表区当前悬停的全局条目下标，-1 表示无
     */
    private int listHoverIndex = -1;
    private long pendingSelectLogEntryId;

    /**
     * 详情正文：原版组件换行后的行，用于绘制与 Hover/Click 命中（与 {@link xin.vanilla.banira.client.gui.component.Notification} 一致）
     */
    @Nonnull
    private List<FormattedCharSequence> detailContentLines = Collections.emptyList();
    private double detailContentLeft;
    private double detailContentTop;
    private int detailContentMaxLineW;

    /**
     * 详情区元数据格：截断时悬停展示全文
     */
    private final List<MetaHoverRegion> metaHoverRegions = new ArrayList<>();

    private static final class MetaHoverRegion {
        final int x;
        final int y;
        final int w;
        final int h;
        final String fullText;

        MetaHoverRegion(int x, int y, int w, int h, String fullText) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.fullText = fullText;
        }
    }

    public NotificationLogScreen(Args args) {
        super(BaniraComponent.get().transClientAuto("notification_log_title").toVanilla());
        this.args = args != null ? args : new Args();
        this.pendingSelectLogEntryId = this.args.selectLogEntryId();
        BaniraScreen.inheritThemeAndSeason(this, this.args.parentScreen(), this.args.theme(), this.args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Args {
        @Nullable
        private Screen parentScreen;
        @Nullable
        private BaniraColorConfig theme;
        @Nullable
        private EnumSeason season;
        /**
         * 打开时自动选中指定日志 id
         */
        private long selectLogEntryId;
    }

    @Override
    protected void onInit() {
        allLogEntries = new ArrayList<>(NotificationManager.get().getLog());
        applyFilter();
        if (selectedIndex >= filteredEntries.size()) selectedIndex = -1;
    }

    private void applyPendingLogSelection() {
        if (pendingSelectLogEntryId == 0L) {
            return;
        }
        long selectId = pendingSelectLogEntryId;
        pendingSelectLogEntryId = 0L;
        for (int i = 0; i < filteredEntries.size(); i++) {
            if (filteredEntries.get(i).id() == selectId) {
                selectedIndex = i;
                int row = Math.max(0, i - visibleRows + 1);
                scrollValue = row;
                if (scrollbarWidget != null) {
                    scrollbarWidget.value(row);
                    scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
                }
                break;
            }
        }
    }

    private void applyFilter() {
        filteredEntries = allLogEntries.stream()
                .filter(this::entryMatchesSearch)
                .collect(Collectors.toList());
        scrollValue = 0;
        selectedIndex = filteredEntries.isEmpty() ? -1 : 0;
        if (scrollbarWidget != null) {
            scrollbarWidget.value(0);
            scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        }
    }

    private boolean entryMatchesSearch(NotificationLogEntry e) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        return buildSearchHaystack(e).toLowerCase(Locale.ROOT).contains(q);
    }

    private static String componentPlainSingleLineForLog(@Nullable Component c) {
        if (c == null) {
            return "";
        }
        String s = c.toString();
        if (s == null) {
            return "";
        }
        return s.replace('\r', ' ').replace('\n', ' ');
    }

    private String buildSearchHaystack(NotificationLogEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentPlainSingleLineForLog(e.component())).append('\n');
        sb.append(e.source()).append('\n');
        sb.append(e.notificationType() != null ? e.notificationType() : "").append('\n');
        sb.append(e.style().name()).append('\n');
        sb.append(e.positionName() != null ? e.positionName() : "").append('\n');
        sb.append(e.animationName() != null ? e.animationName() : "").append('\n');
        sb.append(e.durationTime()).append('\n');
        sb.append(DateUtils.toDateTimeString(new Date(e.timestamp())));
        return sb.toString();
    }

    private void applySearchAndReselect(String raw) {
        searchQuery = raw == null ? "" : raw.trim();
        long prevId = selectedIndex >= 0 && selectedIndex < filteredEntries.size()
                ? filteredEntries.get(selectedIndex).id() : -1L;
        filteredEntries = allLogEntries.stream()
                .filter(this::entryMatchesSearch)
                .collect(Collectors.toList());
        if (prevId >= 0L) {
            for (int i = 0; i < filteredEntries.size(); i++) {
                if (filteredEntries.get(i).id() == prevId) {
                    selectedIndex = i;
                    clampScrollToSelection();
                    if (scrollbarWidget != null) {
                        scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
                        scrollbarWidget.value(scrollValue);
                    }
                    return;
                }
            }
        }
        selectedIndex = filteredEntries.isEmpty() ? -1 : 0;
        scrollValue = 0;
        if (scrollbarWidget != null) {
            scrollbarWidget.value(0);
            scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        }
    }

    private void clampScrollToSelection() {
        if (selectedIndex < 0 || filteredEntries.isEmpty()) {
            return;
        }
        int maxScroll = Math.max(0, filteredEntries.size() - visibleRows);
        if (selectedIndex < scrollValue) {
            scrollValue = selectedIndex;
        }
        if (selectedIndex >= scrollValue + visibleRows) {
            scrollValue = selectedIndex - visibleRows + 1;
        }
        scrollValue = Math.max(0, Math.min(maxScroll, scrollValue));
    }

    @Override
    protected void initWidgets() {
        int w = width;
        int h = height;
        int margin = PANEL_MARGIN + 4;

        leftW = (int) (w * LEFT_RATIO) - margin;
        rightW = w - leftW - DIVIDER_W - margin * 2;
        leftH = h - margin * 2;
        rightH = leftH;

        leftX = margin;
        leftY = margin;
        rightX = leftX + leftW + DIVIDER_W;
        rightY = margin;

        listX = leftX + PANEL_MARGIN;
        listY = leftY + PANEL_MARGIN + SEARCH_BOX_H + 6;
        listW = leftW - PANEL_MARGIN * 2 - SCROLL_W - SCROLL_GAP;
        int listAreaH = leftH - PANEL_MARGIN - SEARCH_BOX_H - 6 - PANEL_MARGIN - TYPE_CFG_BTN_H - TYPE_CFG_BTN_GAP;
        visibleRows = Math.max(1, listAreaH / LIST_ROW_HEIGHT);
        listH = visibleRows * LIST_ROW_HEIGHT;

        searchInput = new InputWidget(this);
        searchInput.id("notification_log_search");
        searchInput.bounds(new ScreenCoordinate(listX, leftY + PANEL_MARGIN, listW + SCROLL_GAP + SCROLL_W, SEARCH_BOX_H));
        searchInput.text(Text.transAuto(Banira.MOD_ID, "notification_log_search_hint"));
        searchInput.onTextChanged(this::applySearchAndReselect);
        searchInput.value("");
        addWidget(searchInput);

        scrollbarWidget = new ScrollbarWidget(this);
        scrollbarWidget.id("scroll");
        scrollbarWidget.bounds(new ScreenCoordinate(listX + listW + SCROLL_GAP, listY, SCROLL_W, listH));
        scrollbarWidget.orientation(EnumOrientation.VERTICAL);
        scrollbarWidget.minValue(0);
        scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        scrollbarWidget.visibleSize(visibleRows);
        scrollbarWidget.scrollStep(1.0);
        scrollbarWidget.addScrollHoverArea(new ScreenCoordinate(listX, listY, listW, listH));
        scrollbarWidget.onValueChanged(v -> scrollValue = v);
        addWidget(scrollbarWidget);

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("close");
        closeBtn.bounds(new ScreenCoordinate(width - SCREEN_CARD_MARGIN - CLOSE_BTN_PAD - CLOSE_BTN_SIZE, SCREEN_CARD_MARGIN + CLOSE_BTN_PAD, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE));
        closeBtn.presetStyleClose();
        closeBtn.radius(CLOSE_BTN_SIZE / 3f);
        closeBtn.padding(1);
        closeBtn.onClick(b -> onClose());
        addWidget(closeBtn);

        ButtonWidget typeCfgBtn = new ButtonWidget(this);
        typeCfgBtn.id("type_cfg");
        typeCfgBtn.text(BaniraComponent.get().transClientAuto("notification_type_config_open").toString());
        typeCfgBtn.bounds(new ScreenCoordinate(listX, leftY + leftH - PANEL_MARGIN - TYPE_CFG_BTN_H, Math.min(listW, 180), TYPE_CFG_BTN_H));
        typeCfgBtn.onClick(b -> BaniraClientRuntime.setScreen(new NotificationTypeConfigScreen(
                new NotificationTypeConfigScreen.Args().parentScreen(this))));
        addWidget(typeCfgBtn);

        applyPendingLogSelection();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        if (scrollbarWidget != null) {
            scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        }
    }

    /**
     * 列表区域由 {@link #onMouseClicked} 处理选中；
     * 禁止根级 Widget 在该矩形内抢点击（避免与下拉/滚动条等命中域重叠时无法选中行）
     */
    @Override
    protected boolean shouldWidgetReceiveClick(IWidget widget, MouseEvent event) {
        if (isMouseInListArea(event.mouseX(), event.mouseY())) {
            return false;
        }
        return super.shouldWidgetReceiveClick(widget, event);
    }

    private boolean isMouseInListArea(double mx, double my) {
        return mx >= listX && mx < listX + listW && my >= listY && my < listY + listH;
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        if (eventArgs.button() == 0 && !eventArgs.consumed() && tryHandleDetailContentClick(eventArgs.mouseX(), eventArgs.mouseY())) {
            eventArgs.consumed(true);
        }
        if (eventArgs.button() == 0 && !eventArgs.consumed()) {
            double mx = eventArgs.mouseX();
            double my = eventArgs.mouseY();
            if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
                int startIndex = (int) Math.max(0, Math.min(scrollValue, filteredEntries.size() - visibleRows));
                int relativeRow = (int) ((my - listY) / LIST_ROW_HEIGHT);
                int clickedIndex = startIndex + relativeRow;
                if (clickedIndex >= 0 && clickedIndex < filteredEntries.size()) {
                    selectedIndex = clickedIndex;
                    eventArgs.consumed(true);
                }
            }
        }
        super.onMouseClicked(eventArgs);
    }

    @Override
    public void onRender(PoseStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();

        ShapeDrawArgs leftBg = ShapeDrawArgs.rect(stack, leftX, leftY, leftW, leftH, theme.color(PANEL_BG));
        leftBg.rect().radius(8, 0, 8, 0).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(leftBg);

        ShapeDrawArgs rightBg = ShapeDrawArgs.rect(stack, rightX, rightY, rightW, rightH, theme.color(PANEL_BG));
        rightBg.rect().radius(0, 8, 0, 8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(rightBg);

        listHoverIndex = -1;
        if (!filteredEntries.isEmpty()) {
            double hmx = inputState.mouseX();
            double hmy = inputState.mouseY();
            if (isMouseInListArea(hmx, hmy)) {
                int startIdx = (int) Math.max(0, Math.min(scrollValue, filteredEntries.size() - visibleRows));
                int rel = (int) ((hmy - listY) / LIST_ROW_HEIGHT);
                int hi = startIdx + rel;
                if (hi >= 0 && hi < filteredEntries.size()) {
                    listHoverIndex = hi;
                }
            }
        }

        int startIndex = (int) Math.max(0, Math.min(scrollValue, filteredEntries.size() - visibleRows));
        int endIndex = Math.min(startIndex + visibleRows, filteredEntries.size());
        for (int i = startIndex; i < endIndex; i++) {
            NotificationLogEntry entry = filteredEntries.get(i);
            int rowY = listY + (i - startIndex) * LIST_ROW_HEIGHT;
            renderListRow(stack, entry, i, listX, rowY, listW, LIST_ROW_HEIGHT - 2, theme);
        }

        if (filteredEntries.isEmpty()) {
            FontDrawArgs emptyArgs = FontDrawArgs.ofPopo(Text.transAuto(Banira.MOD_ID, "notification_log_empty")
                            .stack(stack).font(font))
                    .x(listX + listW / 2 - 50).y(listY + listH / 2 - 8).align(EnumAlignment.CENTER)
                    .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
            LabelWidget.drawLimitedText(emptyArgs);
        }

        renderDetailPane(stack, theme);

        super.renderWidgets(stack, partialTicks);
    }

    private void renderListRow(PoseStack stack, NotificationLogEntry entry, int index, int x, int y, int w, int h, BaniraColorConfig theme) {
        boolean selected = index == selectedIndex;
        boolean hovered = index == listHoverIndex && !selected;
        int rowBg = selected ? ColorUtils.applyAlphaToArgb(theme.color(ACCENT), 0x40)
                : hovered ? ColorUtils.applyAlphaToArgb(theme.color(ACCENT), 0x18)
                : "network".equals(entry.source()) ? ColorUtils.applyAlphaToArgb(theme.color(BG_TERTIARY), 0x30)
                : ColorUtils.applyAlphaToArgb(theme.color(BG_SECONDARY), 0x20);
        ShapeDrawArgs rowRect = ShapeDrawArgs.rect(stack, x, y, w, h, rowBg);
        rowRect.rect().radius(4);
        BaseShapeWidget.drawShape(rowRect);

        int accentW = 3;
        int accentColor = "network".equals(entry.source()) ? theme.color(ACCENT) : theme.color(BG_TERTIARY);
        ShapeDrawArgs accentRect = ShapeDrawArgs.rect(stack, x, y, accentW, h, accentColor);
        BaseShapeWidget.drawShape(accentRect);

        int textX = x + 6 + accentW;
        int textW = w - 12 - accentW;
        String language = Translator.getClientLanguage();
        net.minecraft.network.chat.Component rowComponent = ColorUtils.readableVanillaComponentCopy(
                entry.component().toVanilla(language), theme.panelBg());
        if (StringUtils.isNullOrEmptyEx(rowComponent.getString())) {
            rowComponent = new net.minecraft.network.chat.TextComponent("-");
        }
        List<FormattedCharSequence> rowLines = font.split(rowComponent, Math.max(1, textW));
        if (!rowLines.isEmpty()) {
            font.draw(stack, rowLines.get(0), textX, y + (h - font.lineHeight) / 2,
                    selected ? theme.textPrimary() : theme.textSecondary());
        }
    }

    private void renderDetailPane(PoseStack stack, BaniraColorConfig theme) {
        detailContentLines = Collections.emptyList();
        int pad = 16;
        int x = rightX + pad;
        int y = rightY + pad;
        int w = rightW - pad * 2;

        if (selectedIndex < 0 || selectedIndex >= filteredEntries.size()) {
            metaHoverRegions.clear();
            FontDrawArgs hint = FontDrawArgs.ofPopo(Text.transAuto(Banira.MOD_ID, "notification_log_select_hint")
                            .stack(stack).font(font))
                    .x(x + w / 2 - 60).y(y + rightH / 2 - 30).align(EnumAlignment.CENTER)
                    .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
            LabelWidget.drawLimitedText(hint);
            return;
        }

        NotificationLogEntry entry = filteredEntries.get(selectedIndex);
        String timeStr = DateUtils.toDateTimeString(new Date(entry.timestamp()));

        int curY = y;

        FontDrawArgs timeArgs = FontDrawArgs.ofPopo(Text.literal(timeStr).color(theme.color(TEXT_HINT)).stack(stack).font(font))
                .x(x).y(curY).fontSize(10).maxWidth(w).wrap(true)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        KeyValue<Integer, Integer> timeBlock = LabelWidget.calculateLimitedTextSize(timeArgs);
        LabelWidget.drawLimitedText(timeArgs);
        curY += timeBlock.value() + DETAIL_AFTER_TIME_GAP;

        curY += renderDetailMetaRows(stack, x, curY, w, entry, theme);
        curY += DETAIL_AFTER_META_GAP;

        String language = Translator.getClientLanguage();
        net.minecraft.network.chat.Component contentVanilla = ColorUtils.readableVanillaComponentCopy(
                entry.component().toVanilla(language), theme.panelBg());
        if (contentVanilla != null && !StringUtils.isNullOrEmptyEx(contentVanilla.getString())) {
            detailContentLines = font.split(contentVanilla, w);
            detailContentLeft = x;
            detailContentTop = curY;
            detailContentMaxLineW = w;
            float lineY = curY;
            int textColor = theme.color(TEXT_PRIMARY);
            for (FormattedCharSequence line : detailContentLines) {
                font.draw(stack, line, x, lineY, textColor);
                lineY += font.lineHeight;
            }
            if (!isAnyDropdownSelectOpen()) {
                Style hoverSt = styleAtDetailContentPoint(inputState.mouseX(), inputState.mouseY());
                if (hoverSt != null && hoverSt.getHoverEvent() != null) {
                    net.minecraft.network.chat.Component tipVanilla = NotificationStyleInteractionHelper.hoverTextOrNull(hoverSt);
                    if (tipVanilla != null) {
                        deferThemedTooltipWidget(theme, (int) inputState.mouseX(), (int) inputState.mouseY(),
                                new Text(BaniraComponent.get().object(tipVanilla)));
                    }
                }
            }
        }

        if (!isAnyDropdownSelectOpen()) {
            maybeRenderMetaHoverTooltip(theme);
        }
    }

    /**
     * 延迟绘制 {@link TooltipWidget}（屏幕坐标、当前界面主题/季节），避免 scissor 与矩阵层级导致错位或被挡。
     */
    private void deferThemedTooltipWidget(BaniraColorConfig theme, int tipX, int tipY, Text tipText) {
        if (isAnyDropdownSelectOpen()) {
            return;
        }
        final BaniraColorConfig tipTheme = theme != null ? theme : getEffectiveTheme();
        final EnumSeason tipSeason = season();
        final boolean useTexture = tipTheme.tooltipUseTexture();
        addDeferredTooltipRender(s -> {
            s.pushPose();
            s.last().pose().setIdentity();
            TooltipWidget.drawPopupMessage(s,
                    FontDrawArgs.ofPopo(tipText.stack(s).font(font)).x(tipX).y(tipY).popupUseTexture(useTexture),
                    tipTheme, tipSeason);
            s.popPose();
        });
    }

    private void maybeRenderMetaHoverTooltip(BaniraColorConfig theme) {
        double mx = inputState.mouseX();
        double my = inputState.mouseY();
        for (MetaHoverRegion r : metaHoverRegions) {
            if (mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h) {
                deferThemedTooltipWidget(theme, (int) mx, (int) my, Text.literal(r.fullText));
                break;
            }
        }
    }

    private int renderDetailMetaRows(PoseStack stack, int x, int y, int w, NotificationLogEntry entry, BaniraColorConfig theme) {
        metaHoverRegions.clear();
        BaniraComponent tr = BaniraComponent.get();
        String sourceVal = "network".equals(entry.source())
                ? tr.transClientAuto("notification_log_source_network").toString()
                : tr.transClientAuto("notification_log_source_local").toString();
        String typeVal = entry.notificationType() != null ? entry.notificationType() : "default";
        String styleVal = entry.style().name();
        String posVal = entry.positionName() != null ? entry.positionName() : "";
        String animVal = entry.animationName() != null ? entry.animationName() : "";
        String durVal = entry.durationTime() + " ms";

        int colW = Math.max(48, (w - META_COL_GAP) / 2);
        int h = 0;
        h += drawMetaPairRow(stack, x, y + h, colW, theme,
                tr.transClientAuto("notification_log_meta_source").toString(), sourceVal,
                tr.transClientAuto("notification_log_meta_type").toString(), typeVal);
        h += drawMetaPairRow(stack, x, y + h, colW, theme,
                tr.transClientAuto("notification_log_meta_style").toString(), styleVal,
                tr.transClientAuto("notification_log_meta_position").toString(), posVal);
        h += drawMetaPairRow(stack, x, y + h, colW, theme,
                tr.transClientAuto("notification_log_meta_animation").toString(), animVal,
                tr.transClientAuto("notification_log_meta_duration").toString(), durVal);
        return h;
    }

    private boolean metaLineTruncated(String line, int colW) {
        if (StringUtils.isNullOrEmptyEx(line)) {
            return false;
        }
        return font.width(line) > colW;
    }

    private int drawMetaPairRow(PoseStack stack, int x, int y, int colW, BaniraColorConfig theme,
                                String label1, String value1, String label2, String value2) {
        String s1 = label1 + "：" + value1;
        String s2 = label2 + "：" + value2;
        FontDrawArgs a1 = FontDrawArgs.ofPopo(Text.literal(s1).color(theme.color(TEXT_SECONDARY)).stack(stack).font(font))
                .x(x).y(y).fontSize(META_FONT_SIZE).maxWidth(colW).wrap(false)
                .position(EnumEllipsisPosition.END)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        FontDrawArgs a2 = FontDrawArgs.ofPopo(Text.literal(s2).color(theme.color(TEXT_SECONDARY)).stack(stack).font(font))
                .x(x + colW + META_COL_GAP).y(y).fontSize(META_FONT_SIZE).maxWidth(colW).wrap(false)
                .position(EnumEllipsisPosition.END)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        int rowH = META_FONT_SIZE;
        if (metaLineTruncated(s1, colW)) {
            metaHoverRegions.add(new MetaHoverRegion(x, y, colW, rowH, value1));
        }
        if (metaLineTruncated(s2, colW)) {
            metaHoverRegions.add(new MetaHoverRegion(x + colW + META_COL_GAP, y, colW, rowH, value2));
        }
        LabelWidget.drawLimitedText(a1);
        LabelWidget.drawLimitedText(a2);
        return rowH + META_ROW_GAP;
    }

    @Nullable
    private Style styleAtDetailContentPoint(double guiMouseX, double guiMouseY) {
        if (detailContentLines.isEmpty()) {
            return null;
        }
        if (guiMouseX < detailContentLeft || guiMouseY < detailContentTop) {
            return null;
        }
        if (guiMouseX >= detailContentLeft + detailContentMaxLineW) {
            return null;
        }
        int lineCount = detailContentLines.size();
        double blockH = lineCount * font.lineHeight;
        if (guiMouseY >= detailContentTop + blockH) {
            return null;
        }
        int lineIndex = (int) ((guiMouseY - detailContentTop) / font.lineHeight);
        if (lineIndex < 0 || lineIndex >= lineCount) {
            return null;
        }
        FormattedCharSequence proc = detailContentLines.get(lineIndex);
        int rx = (int) (guiMouseX - detailContentLeft);
        return font.getSplitter().componentStyleAtWidth(proc, rx);
    }

    private boolean tryHandleDetailContentClick(double mouseX, double mouseY) {
        Style st = styleAtDetailContentPoint(mouseX, mouseY);
        return st != null && NotificationStyleInteractionHelper.tryClickStyle(st);
    }

    @Override
    public void onClose() {
        if (args.parentScreen() != null) {
            BaniraClientRuntime.setScreen(args.parentScreen());
        } else {
            super.onClose();
        }
    }

    @Override
    protected ScreenCoordinate closeableWindowBounds() {
        return new ScreenCoordinate(SCREEN_CARD_MARGIN, SCREEN_CARD_MARGIN,
                width - SCREEN_CARD_MARGIN * 2, height - SCREEN_CARD_MARGIN * 2);
    }
}
