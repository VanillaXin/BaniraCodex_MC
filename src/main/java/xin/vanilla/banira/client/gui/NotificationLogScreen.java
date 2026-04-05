package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.event.BaniraClientModSetup;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Notification 日志查看界面，横屏主从布局：左侧类型选择+简洁列表，右侧记录详情
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NotificationLogScreen extends BaniraScreen {

    public static final String FILTER_ALL = "all";
    public static final String FILTER_LOCAL = "local";
    public static final String FILTER_NETWORK = "network";

    private static final int LIST_ROW_HEIGHT = 24;
    private static final int PANEL_MARGIN = 12;
    private static final int FILTER_H = 28;
    private static final int DIVIDER_W = 2;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_GAP = 2;
    private static final float CLOSE_BTN_SIZE = 10f;
    private static final int TYPE_CFG_BTN_H = 22;
    private static final int TYPE_CFG_BTN_GAP = 6;
    private static final double LEFT_RATIO = 0.38;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final Args args;
    private int leftX, leftY, leftW, leftH;
    private int listX, listY, listW, listH;
    private int rightX, rightY, rightW, rightH;
    private int visibleRows;
    private ScrollbarWidget scrollbarWidget;
    private DropdownSelectWidget filterDropdown;
    private double scrollValue = 0;
    private String filterSource = FILTER_ALL;
    private List<NotificationLogEntry> allLogEntries = new ArrayList<>();
    private List<NotificationLogEntry> filteredEntries = new ArrayList<>();
    private int selectedIndex = -1;
    private long pendingSelectLogEntryId;

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
        if (FILTER_ALL.equals(filterSource)) {
            filteredEntries = new ArrayList<>(allLogEntries);
        } else {
            filteredEntries = allLogEntries.stream()
                    .filter(e -> filterSource.equals(e.source()))
                    .collect(Collectors.toList());
        }
        scrollValue = 0;
        selectedIndex = filteredEntries.isEmpty() ? -1 : 0;
        if (scrollbarWidget != null) {
            scrollbarWidget.value(0);
            scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        }
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
        listY = leftY + PANEL_MARGIN + FILTER_H + 6;
        listW = leftW - PANEL_MARGIN * 2 - SCROLL_W - SCROLL_GAP;
        int listAreaH = leftH - PANEL_MARGIN - FILTER_H - 6 - PANEL_MARGIN - TYPE_CFG_BTN_H - TYPE_CFG_BTN_GAP;
        visibleRows = Math.max(1, listAreaH / LIST_ROW_HEIGHT);
        listH = visibleRows * LIST_ROW_HEIGHT;

        String optAll = BaniraComponent.get().transClientAuto("notification_log_filter_all").toString();
        String optLocal = BaniraComponent.get().transClientAuto("notification_log_filter_local").toString();
        String optNetwork = BaniraComponent.get().transClientAuto("notification_log_filter_network").toString();
        filterDropdown = new DropdownSelectWidget(this);
        filterDropdown.id("filter");
        filterDropdown.bounds(new ScreenCoordinate(listX, leftY + PANEL_MARGIN, listW + SCROLL_GAP + SCROLL_W, FILTER_H));
        filterDropdown.options(Arrays.asList(optAll, optLocal, optNetwork));
        filterDropdown.text(BaniraComponent.get().transClientAuto("notification_log_filter").toString());
        filterDropdown.selectedValues(Collections.singletonList(
                FILTER_ALL.equals(filterSource) ? optAll : FILTER_LOCAL.equals(filterSource) ? optLocal : optNetwork));
        filterDropdown.onSelectionChanged(values -> {
            String selected = values.isEmpty() ? "" : values.get(0);
            if (optAll.equals(selected)) filterSource = FILTER_ALL;
            else if (optLocal.equals(selected)) filterSource = FILTER_LOCAL;
            else if (optNetwork.equals(selected)) filterSource = FILTER_NETWORK;
            applyFilter();
        });
        addWidget(filterDropdown);

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
        closeBtn.bounds(new ScreenCoordinate(rightX + rightW - PANEL_MARGIN / 3.5f - CLOSE_BTN_SIZE, rightY + PANEL_MARGIN / 3.5f, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE));
        closeBtn.presetStyleClose();
        closeBtn.radius(CLOSE_BTN_SIZE / 3f);
        closeBtn.padding(1);
        closeBtn.onClick(b -> onClose());
        addWidget(closeBtn);

        ButtonWidget typeCfgBtn = new ButtonWidget(this);
        typeCfgBtn.id("type_cfg");
        typeCfgBtn.text(BaniraComponent.get().transClientAuto("notification_type_config_open").toString());
        typeCfgBtn.bounds(new ScreenCoordinate(listX, leftY + leftH - PANEL_MARGIN - TYPE_CFG_BTN_H, Math.min(listW, 180), TYPE_CFG_BTN_H));
        typeCfgBtn.onClick(b -> Minecraft.getInstance().setScreen(new NotificationTypeConfigScreen(
                new NotificationTypeConfigScreen.Args().parentScreen(this))));
        addWidget(typeCfgBtn);

        applyPendingLogSelection();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().screen == null && BaniraClientModSetup.NOTIFICATION_LOG_KEY.isDown()) {
            Minecraft.getInstance().setScreen(new NotificationLogScreen(null));
        }
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        if (scrollbarWidget != null) {
            scrollbarWidget.maxValue(Math.max(0, filteredEntries.size() - visibleRows));
        }
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
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
    public void onRender(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();

        ShapeDrawArgs leftBg = ShapeDrawArgs.rect(stack, leftX, leftY, leftW, leftH, theme.panelBg());
        leftBg.rect().radius(8, 0, 8, 0).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(leftBg);

        ShapeDrawArgs rightBg = ShapeDrawArgs.rect(stack, rightX, rightY, rightW, rightH, theme.panelBg());
        rightBg.rect().radius(0, 8, 0, 8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(rightBg);

        int startIndex = (int) Math.max(0, Math.min(scrollValue, filteredEntries.size() - visibleRows));
        int endIndex = Math.min(startIndex + visibleRows, filteredEntries.size());
        for (int i = startIndex; i < endIndex; i++) {
            NotificationLogEntry entry = filteredEntries.get(i);
            int rowY = listY + (i - startIndex) * LIST_ROW_HEIGHT;
            renderListRow(stack, entry, i, listX, rowY, listW, LIST_ROW_HEIGHT - 2, theme);
        }

        if (filteredEntries.isEmpty()) {
            FontDrawArgs emptyArgs = FontDrawArgs.ofPopo(Text.transAuto(BaniraCodex.MODID, "notification_log_empty")
                            .stack(stack).font(font))
                    .x(listX + listW / 2 - 50).y(listY + listH / 2 - 8).align(EnumAlignment.CENTER)
                    .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
            LabelWidget.drawLimitedText(emptyArgs);
        }

        renderDetailPane(stack, theme);

        super.renderWidgets(stack, partialTicks);
    }

    private void renderListRow(MatrixStack stack, NotificationLogEntry entry, int index, int x, int y, int w, int h, BaniraColorConfig theme) {
        boolean selected = index == selectedIndex;
        int rowBg = selected ? ColorUtils.applyAlphaToArgb(theme.accent(), 0x40)
                : "network".equals(entry.source()) ? ColorUtils.applyAlphaToArgb(theme.bgTertiary(), 0x30)
                : ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x20);
        ShapeDrawArgs rowRect = ShapeDrawArgs.rect(stack, x, y, w, h, rowBg);
        rowRect.rect().radius(4);
        BaseShapeWidget.drawShape(rowRect);

        int accentW = 3;
        int accentColor = "network".equals(entry.source()) ? theme.accent() : theme.bgTertiary();
        ShapeDrawArgs accentRect = ShapeDrawArgs.rect(stack, x, y, accentW, h, accentColor);
        BaseShapeWidget.drawShape(accentRect);

        int textX = x + 6 + accentW;
        int textW = w - 12 - accentW;
        String timeStr = TIME_FORMAT.format(new Date(entry.timestamp()));
        String contentStr = entry.component().toString();
        if (StringUtils.isNullOrEmptyEx(contentStr)) contentStr = "-";
        String brief = contentStr.length() > 20 ? contentStr.substring(0, 17) + "..." : contentStr;
        String line = timeStr + " " + brief;

        FontDrawArgs args = FontDrawArgs.ofPopo(Text.literal(line).color(selected ? theme.textPrimary() : theme.textSecondary()).stack(stack).font(font))
                .x(textX).y(y + (h - 9) / 2).fontSize(9).maxWidth(textW)
                .position(EnumEllipsisPosition.END).wrap(false)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        LabelWidget.drawLimitedText(args);
    }

    private void renderDetailPane(MatrixStack stack, BaniraColorConfig theme) {
        int pad = 16;
        int x = rightX + pad;
        int y = rightY + pad;
        int w = rightW - pad * 2;

        if (selectedIndex < 0 || selectedIndex >= filteredEntries.size()) {
            FontDrawArgs hint = FontDrawArgs.ofPopo(Text.transAuto(BaniraCodex.MODID, "notification_log_select_hint")
                            .stack(stack).font(font))
                    .x(x + w / 2 - 60).y(y + rightH / 2 - 30).align(EnumAlignment.CENTER)
                    .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
            LabelWidget.drawLimitedText(hint);
            return;
        }

        NotificationLogEntry entry = filteredEntries.get(selectedIndex);
        String timeStr = TIME_FORMAT.format(new Date(entry.timestamp()));
        String sourceDisplay = "network".equals(entry.source())
                ? BaniraComponent.get().transClientAuto("notification_log_source_network").toString()
                : BaniraComponent.get().transClientAuto("notification_log_source_local").toString();

        int lineH = font.lineHeight + 4;
        int curY = y;

        FontDrawArgs timeArgs = FontDrawArgs.ofPopo(Text.literal(timeStr).color(theme.textHint()).stack(stack).font(font))
                .x(x).y(curY).fontSize(10).maxWidth(w).wrap(true)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        LabelWidget.drawLimitedText(timeArgs);
        curY += lineH;

        String metaStr = String.format("[%s] %s | %s | %s | %s | %dms",
                sourceDisplay,
                entry.notificationType() != null ? entry.notificationType() : "default",
                entry.style().name(), entry.positionName(), entry.animationName(), entry.durationTime());
        FontDrawArgs metaArgs = FontDrawArgs.ofPopo(Text.literal(metaStr).color(theme.textSecondary()).stack(stack).font(font))
                .x(x).y(curY).fontSize(10).maxWidth(w).wrap(true)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
        LabelWidget.drawLimitedText(metaArgs);
        curY += lineH + 8;

        String contentStr = entry.component().toString();
        if (contentStr != null && !contentStr.isEmpty()) {
            FontDrawArgs contentArgs = FontDrawArgs.ofPopo(Text.literal(contentStr).color(theme.textPrimary()).stack(stack).font(font))
                    .x(x).y(curY).fontSize(11).maxWidth(w).wrap(true)
                    .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
            LabelWidget.drawLimitedText(contentArgs);
        }
    }

    @Override
    public void onClose() {
        if (args.parentScreen() != null) {
            Minecraft.getInstance().setScreen(args.parentScreen());
        } else {
            super.onClose();
        }
    }
}
