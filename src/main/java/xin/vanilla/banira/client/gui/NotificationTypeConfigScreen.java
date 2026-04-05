package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按通知类型配置是否隐藏、显示时长、动画与位置
 */
@OnlyIn(Dist.CLIENT)
public class NotificationTypeConfigScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 10;
    private static final int CARD_INNER = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final double LABEL_COLUMN_WIDTH_RATIO = 0.32;
    private static final double LABEL_COLUMN_MIN_WIDTH = 64;
    private static final int GAP_LABEL_TO_VALUE = 4;
    private static final double VALUE_AREA_MIN_WIDTH = 56;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_PADDING = 12;
    private static final int BUTTON_GAP = 8;
    private static final int CARD_GAP = 1;

    private final Args args;

    private CollapsiblePanelWidget contentRootPanel;
    private ScrollbarWidget scrollbar;
    private double scrollOffset = 0;
    private int contentHeight = 0;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int listTop;
    private int listAreaHeight;
    private int maxListHeight;
    private int contentLeft;
    private int contentW;
    private int btnY;
    private int contentTotalW;
    private final List<ButtonWidget> bottomButtons = new ArrayList<>();

    public NotificationTypeConfigScreen(Args args) {
        super(BaniraComponent.get().transClientAuto("notification_type_config_title").toVanilla());
        this.args = args != null ? args : new Args();
        previousScreen(this.args.parentScreen());
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
    }

    @Override
    protected void initWidgets() {
        int w = width;
        int h = height;
        cardX = CARD_MARGIN;
        cardY = CARD_MARGIN;
        cardW = w - CARD_MARGIN * 2;
        cardH = h - CARD_MARGIN * 2;
        contentLeft = cardX + CARD_INNER;
        contentW = cardW - CARD_INNER * 2 - SCROLL_WIDTH - SCROLL_GAP;
        contentTotalW = contentW + SCROLL_GAP + SCROLL_WIDTH;
        listTop = cardY + CARD_INNER;
        bottomButtons.clear();

        contentRootPanel = buildTypesPanel();
        contentHeight = (int) contentRootPanel.height();
        addWidget(contentRootPanel);

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL);
        scrollbar.minValue(0);
        scrollbar.onValueChanged(v -> {
            scrollOffset = v;
            updateWidgetPositions();
        });
        addWidget(scrollbar);

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("close");
        closeBtn.text(BaniraComponent.get().transClientAuto("notification_type_config_close").toString());
        closeBtn.onClick(b -> onClose());
        bottomButtons.add(closeBtn);

        for (ButtonWidget btn : bottomButtons) {
            addWidget(btn);
        }

        updateLayout();
        updateWidgetPositions();
    }

    private CollapsiblePanelWidget buildTypesPanel() {
        CollapsiblePanelWidget root = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
        root.text(BaniraComponent.get().transClientAuto("notification_type_config_root_title").toString()).expanded(true);
        root.contentGap(ROW_GAP);
        root.headerHeight(ROW_HEIGHT);
        root.onExpandChanged(p -> syncContentHeight());

        List<String> types = NotificationTypeRegistry.knownTypesSorted();
        for (String typeId : types) {
            CollapsiblePanelWidget child = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
            child.text(Text.literal(typeId)).expanded(false);
            child.contentGap(ROW_GAP);
            child.headerHeight(ROW_HEIGHT);
            child.onExpandChanged(p -> syncContentHeight());

            NotificationTypeSettingsStore.TypeSettings st = NotificationTypeSettingsStore.get().getOrCreate(typeId);
            double cw = child.getContentWidth();

            addTypeToggleRow(child, cw, typeId, st);
            addDurationRow(child, cw, typeId, st);
            addAnimationRow(child, cw, typeId, st);
            addPositionRow(child, cw, typeId, st);

            child.refreshLayout();
            root.addCollapsibleChild(child);
        }

        root.refreshLayout();
        return root;
    }

    private void addTypeToggleRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        LabelWidget label = row.label(BaniraComponent.get().transClientAuto("notification_type_config_hidden").toString());
        ButtonWidget btn = row.toggleButton(st.hidden());
        btn.onClick(b -> {
            boolean next = !NotificationTypeSettingsStore.get().getOrCreate(typeId).hidden();
            NotificationTypeSettingsStore.TypeSettings nextSt = copySettings(typeId).hidden(next);
            NotificationTypeSettingsStore.get().put(typeId, nextSt);
            btn.text(toggleText(next));
        });
        panel.addChildAuto(row, ROW_HEIGHT);
    }

    private void addDurationRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        row.label(BaniraComponent.get().transClientAuto("notification_type_config_duration").toString());
        List<String> labels = durationLabels();
        DropdownSelectWidget dd = row.dropdown(cw, labels, durationLabelFor(st.durationMs()));
        dd.onSelectionChanged(vals -> {
            if (vals.isEmpty()) return;
            long ms = parseDurationLabel(vals.get(0));
            NotificationTypeSettingsStore.get().put(typeId, copySettings(typeId).durationMs(ms));
        });
        panel.addChildAuto(row, ROW_HEIGHT + 4);
    }

    private void addAnimationRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        row.label(BaniraComponent.get().transClientAuto("notification_type_config_animation").toString());
        String inherit = BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString();
        List<DropdownOption> entries = new ArrayList<>();
        entries.add(new DropdownOption(inherit));
        for (EnumMoveType t : EnumMoveType.values()) {
            entries.add(enumAsDropdownOption(t));
        }
        String sel = (st.animationName() == null || st.animationName().isEmpty())
                ? inherit
                : st.animationName();
        DropdownSelectWidget dd = row.dropdownEntries(cw, entries, sel);
        dd.onSelectionChanged(vals -> {
            if (vals.isEmpty()) return;
            String v = vals.get(0);
            String anim = inherit.equals(v) ? "" : v;
            NotificationTypeSettingsStore.get().put(typeId, copySettings(typeId).animationName(anim));
        });
        panel.addChildAuto(row, ROW_HEIGHT + 4);
    }

    private void addPositionRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        row.label(BaniraComponent.get().transClientAuto("notification_type_config_position").toString());
        String inherit = BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString();
        List<DropdownOption> entries = new ArrayList<>();
        entries.add(new DropdownOption(inherit));
        for (EnumPosition p : EnumPosition.values()) {
            entries.add(enumAsDropdownOption(p));
        }
        String sel = (st.positionName() == null || st.positionName().isEmpty())
                ? inherit
                : st.positionName();
        DropdownSelectWidget dd = row.dropdownEntries(cw, entries, sel);
        dd.onSelectionChanged(vals -> {
            if (vals.isEmpty()) return;
            String v = vals.get(0);
            String pos = inherit.equals(v) ? "" : v;
            NotificationTypeSettingsStore.get().put(typeId, copySettings(typeId).positionName(pos));
        });
        panel.addChildAuto(row, ROW_HEIGHT + 4);
    }

    /**
     * 与 {@link DropdownSelectWidget#optionsEnum} 一致：有描述时列表显示译文，并带 {@link IEnumDescribable} 悬浮提示。
     */
    private static DropdownOption enumAsDropdownOption(Enum<?> e) {
        if (e instanceof IEnumDescribable) {
            Component desc = ((IEnumDescribable) e).enumDescription();
            if (desc != null && !desc.isEmpty()) {
                String lab = desc.getString(Translator.getClientLanguage());
                return new DropdownOption(e.name(), lab, ItemStack.EMPTY, null, desc.clone());
            }
        }
        return new DropdownOption(e.name());
    }

    private NotificationTypeSettingsStore.TypeSettings copySettings(String typeId) {
        NotificationTypeSettingsStore.TypeSettings s = NotificationTypeSettingsStore.get().getOrCreate(typeId);
        return new NotificationTypeSettingsStore.TypeSettings()
                .hidden(s.hidden())
                .durationMs(s.durationMs())
                .positionName(s.positionName() != null ? s.positionName() : "")
                .animationName(s.animationName() != null ? s.animationName() : "");
    }

    private List<String> durationLabels() {
        List<String> list = new ArrayList<>();
        list.add(BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString());
        list.add(msLabel(2000));
        list.add(msLabel(3000));
        list.add(msLabel(5000));
        list.add(msLabel(8000));
        list.add(msLabel(10000));
        list.add(msLabel(15000));
        list.add(msLabel(20000));
        return list;
    }

    private String msLabel(long ms) {
        return ms + " ms";
    }

    private String durationLabelFor(long durationMs) {
        if (durationMs <= 0) {
            return BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString();
        }
        return msLabel(durationMs);
    }

    private long parseDurationLabel(String label) {
        if (BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString().equals(label)) {
            return 0L;
        }
        try {
            String n = label.replace(" ms", "").trim();
            return Long.parseLong(n);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String toggleText(boolean hidden) {
        return hidden
                ? BaniraComponent.get().transClientAuto("notification_type_config_hidden_on").toString()
                : BaniraComponent.get().transClientAuto("notification_type_config_hidden_off").toString();
    }

    private void syncContentHeight() {
        if (contentRootPanel != null) {
            contentRootPanel.refreshLayout();
            contentHeight = (int) contentRootPanel.height();
            updateLayout();
            updateWidgetPositions();
        }
    }

    private void updateLayout() {
        maxListHeight = Math.max(0, cardH - CARD_INNER * 2 - BUTTON_HEIGHT - CARD_GAP);

        int btnAreaH = BUTTON_HEIGHT + CARD_INNER;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int centeredBtnY = btnAreaTop + (btnAreaH - BUTTON_HEIGHT) / 2;

        if (contentHeight <= maxListHeight) {
            listAreaHeight = Math.max(1, contentHeight);
            btnY = centeredBtnY;
            scrollOffset = 0;
            scrollbar.maxValue(0);
            scrollbar.value(0);
            scrollbar.visible(false);
            scrollbar.scrollingCoordinates(new ArrayList<>());
        } else {
            listAreaHeight = maxListHeight;
            btnY = centeredBtnY;
            scrollbar.visible(true);
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + SCROLL_GAP, listTop, SCROLL_WIDTH, listAreaHeight));
            scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
            scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
            scrollOffset = scrollbar.value();
            scrollbar.visibleSize(listAreaHeight);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop, contentTotalW, listAreaHeight));
        }

        int n = bottomButtons.size();
        int[] btnWidths = new int[n];
        for (int i = 0; i < n; i++) {
            btnWidths[i] = font.width(bottomButtons.get(i).text().content()) + BUTTON_PADDING * 2;
        }
        if (n >= 1) {
            int bw = Math.max(48, btnWidths[0]);
            int cx = cardX + (cardW - bw) / 2;
            bottomButtons.get(0).bounds(new ScreenCoordinate(cx, btnY, bw, BUTTON_HEIGHT));
        }
    }

    private void updateWidgetPositions() {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset, contentW, contentHeight));
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

    private static final int CARD_RADIUS = 8;
    private static final int CARD_ALPHA = 0xFF;

    @Override
    protected void renderWidgets(GuiGraphics graphics, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBg = ColorUtils.applyAlphaToArgb(theme.bgSurface(), CARD_ALPHA);
        int btnAreaH = BUTTON_HEIGHT + CARD_INNER;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int contentH = btnAreaTop - cardY - CARD_GAP;
        int n = bottomButtons.size();
        PoseStack stack = graphics.pose();

        AbstractGuiUtils.drawRoundedRect(stack, cardX, cardY, cardW, contentH,
                CARD_RADIUS, CARD_RADIUS, 0, 0, cardBg);

        if (n == 3) {
            int btnTotal = cardW - 2 * CARD_GAP;
            int segW = btnTotal / 3;
            int seg3W = segW + btnTotal % 3;
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, segW, btnAreaH,
                    0, 0, CARD_RADIUS, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + segW + CARD_GAP, btnAreaTop, segW, btnAreaH,
                    0, 0, 0, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + 2 * (segW + CARD_GAP), btnAreaTop, seg3W, btnAreaH,
                    0, 0, 0, CARD_RADIUS, cardBg);
        } else if (n == 1) {
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, cardW, btnAreaH,
                    0, 0, CARD_RADIUS, CARD_RADIUS, cardBg);
        } else {
            int contentTotal = cardW - CARD_INNER * 2 - CARD_GAP;
            int zoneW = contentTotal / 2;
            int leftRectW = CARD_INNER + zoneW;
            int rightRectW = cardW - leftRectW - CARD_GAP;
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, leftRectW, btnAreaH,
                    0, 0, CARD_RADIUS, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + leftRectW + CARD_GAP, btnAreaTop, rightRectW, btnAreaH,
                    0, 0, 0, CARD_RADIUS, cardBg);
        }

        AbstractGuiUtils.enableScissor(contentLeft, listTop, contentTotalW, Math.max(1, listAreaHeight));

        if (contentRootPanel != null && contentRootPanel.visible()) {
            if (contentRootPanel.enabled() && contentRootPanel.needsUpdate()) contentRootPanel.update();
            contentRootPanel.render(graphics, partialTicks);
        }
        if (scrollbar != null && scrollbar.visible()) {
            if (scrollbar.enabled() && scrollbar.needsUpdate()) scrollbar.update();
            scrollbar.render(graphics, partialTicks);
        }

        AbstractGuiUtils.disableScissor();

        for (ButtonWidget btn : bottomButtons) {
            if (btn.visible()) {
                if (btn.enabled() && btn.needsUpdate()) btn.update();
                btn.render(graphics, partialTicks);
            }
        }

        for (IWidget widget : widgets()) {
            if (widget == contentRootPanel || widget == scrollbar || bottomButtons.contains(widget)) continue;
            if (widget.parent() != null || !widget.visible()) continue;
            if (widget.enabled() && widget.needsUpdate()) widget.update();
            widget.render(graphics, partialTicks);
        }
    }

    @Override
    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderWidgets(graphics, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double delta = deltaY != 0 ? deltaY : deltaX;
        if (delta != 0 && contentRootPanel != null && contentRootPanel.visible() && contentRootPanel.enabled()
                && contentRootPanel.isMouseInside(mouseX, mouseY)
                && contentRootPanel.handleMouseScroll(MouseScrollEvent.of(mouseX, mouseY, delta))) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        if (scrollbar != null && delta != 0) {
            double newVal = scrollbar.value() - delta * 20;
            newVal = Math.max(scrollbar.minValue(), Math.min(scrollbar.maxValue(), newVal));
            scrollbar.value(newVal);
            scrollOffset = newVal;
            updateWidgetPositions();
            return true;
        }
        return false;
    }

    /**
     * 单行：标签 + 控件
     */
    private final class TypeRow extends BaseWidget {
        TypeRow(BaniraScreen screen, double w, int rowH) {
            super(screen, new ScreenCoordinate(0, 0, w, rowH));
        }

        LabelWidget label(String text) {
            double lw = Math.max(LABEL_COLUMN_MIN_WIDTH, width() * LABEL_COLUMN_WIDTH_RATIO);
            LabelWidget l = new LabelWidget(screen, new ScreenCoordinate(0, 0, lw, rowHeight()));
            l.text(Text.literal(text));
            l.textWrap(false);
            l.textVerticalAlign(EnumAlignment.CENTER);
            addChild(l);
            return l;
        }

        ButtonWidget toggleButton(boolean hidden) {
            double lw = Math.max(LABEL_COLUMN_MIN_WIDTH, width() * LABEL_COLUMN_WIDTH_RATIO);
            double vx = lw + GAP_LABEL_TO_VALUE;
            double vw = Math.max(VALUE_AREA_MIN_WIDTH, width() - vx);
            ButtonWidget b = new ButtonWidget(screen, new ScreenCoordinate(vx, 0, vw, rowHeight()));
            b.text(hidden
                    ? BaniraComponent.get().transClientAuto("notification_type_config_hidden_on").toString()
                    : BaniraComponent.get().transClientAuto("notification_type_config_hidden_off").toString());
            addChild(b);
            return b;
        }

        DropdownSelectWidget dropdown(double cw, List<String> options, String selected) {
            double lw = Math.max(LABEL_COLUMN_MIN_WIDTH, cw * LABEL_COLUMN_WIDTH_RATIO);
            double vx = lw + GAP_LABEL_TO_VALUE;
            double vw = Math.max(VALUE_AREA_MIN_WIDTH, cw - vx);
            DropdownSelectWidget d = new DropdownSelectWidget(screen);
            d.bounds(new ScreenCoordinate(vx, 0, vw, rowHeight() + 2));
            d.options(options);
            d.selectedValues(Collections.singletonList(selected));
            addChild(d);
            return d;
        }

        DropdownSelectWidget dropdownEntries(double cw, List<DropdownOption> entries, String selectedValue) {
            double lw = Math.max(LABEL_COLUMN_MIN_WIDTH, cw * LABEL_COLUMN_WIDTH_RATIO);
            double vx = lw + GAP_LABEL_TO_VALUE;
            double vw = Math.max(VALUE_AREA_MIN_WIDTH, cw - vx);
            DropdownSelectWidget d = new DropdownSelectWidget(screen);
            d.bounds(new ScreenCoordinate(vx, 0, vw, rowHeight() + 2));
            d.optionEntries(entries);
            d.selectedValues(Collections.singletonList(selectedValue));
            addChild(d);
            return d;
        }

        private double rowHeight() {
            return bounds() != null ? bounds().height() : ROW_HEIGHT;
        }

        @Override
        public double effectiveHeight() {
            double max = 0;
            for (IWidget c : children()) {
                if (c == null || !c.visible()) continue;
                ScreenCoordinate b = c.bounds();
                if (b != null) {
                    max = Math.max(max, b.y() + c.effectiveHeight());
                }
            }
            return max > 0 ? max : (bounds() != null ? bounds().height() : 0);
        }

        @Override
        protected boolean onMouseClick(MouseEvent event) {
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, float partialTicks) {
            if (!visible) return;
            renderChildren(graphics, partialTicks);
        }
    }
}
