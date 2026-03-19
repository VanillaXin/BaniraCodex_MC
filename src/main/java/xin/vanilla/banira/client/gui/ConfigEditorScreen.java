package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.loading.FMLEnvironment;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.enums.EnumTooltipTextureMode;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.internal.network.NetworkInit;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置编辑界面，支持可视化编辑 ForgeConfigSpec 配置，并可同步修改项至服务端
 */
public class ConfigEditorScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 10;
    private static final int CARD_INNER = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int LABEL_WIDTH = 140;
    private static final int RESET_BTN_WIDTH = 18;
    private static final int RESET_BTN_GAP = 2;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_PADDING = 12;
    private static final int BUTTON_GAP = 8;
    private static final int CARD_GAP = 1;

    private final ConfigHolder holder;
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

    /**
     * 路径 -> 当前编辑值（用于追踪修改）
     */
    private final Map<String, Object> modifiedValues = new LinkedHashMap<>();
    /**
     * 路径 -> Widget（用于从 Widget 读回值）
     */
    private final Map<String, IConfigEntryWidget> entryWidgets = new LinkedHashMap<>();

    public ConfigEditorScreen(ConfigHolder holder, Args args) {
        super(Component.transClientAuto(BaniraCodex.MODID, "config_editor_title").toVanilla());
        this.holder = holder;
        this.args = args != null ? args : new Args();
        BaniraScreen.inheritThemeAndSeason(this, args != null ? args.parentScreen() : null, args != null ? args.theme() : null, args != null ? args.season() : null);
    }

    public static void open(ConfigHolder holder, @Nullable Screen parent) {
        if (FMLEnvironment.dist.isClient()) {
            Minecraft.getInstance().setScreen(new ConfigEditorScreen(holder, new Args().parentScreen(parent)));
        }
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
        entryWidgets.clear();
        bottomButtons.clear();

        contentRootPanel = buildContentPanel();
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

        ButtonWidget saveBtn = new ButtonWidget(this);
        saveBtn.id("save");
        saveBtn.text(Component.transClientAuto(BaniraCodex.MODID, "config_editor_save").toString());
        saveBtn.onClick(b -> saveConfig());
        bottomButtons.add(saveBtn);

        if (holder.isServerConfig()) {
            ButtonWidget syncBtn = new ButtonWidget(this);
            syncBtn.id("sync");
            syncBtn.text(Component.transClientAuto(BaniraCodex.MODID, "config_editor_sync").toString());
            syncBtn.onClick(b -> syncToServer());
            bottomButtons.add(syncBtn);
        }

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("close");
        closeBtn.text(Component.transClientAuto(BaniraCodex.MODID, "config_editor_close").toString());
        closeBtn.onClick(b -> onClose());
        bottomButtons.add(closeBtn);

        for (ButtonWidget btn : bottomButtons) {
            addWidget(btn);
        }

        updateLayout();
        updateWidgetPositions();
    }

    /**
     * 使用 CollapsiblePanelWidget 构建配置树
     */
    private CollapsiblePanelWidget buildContentPanel() {
        List<ConfigHolder.CategoryTreeNode> roots = holder.getCategoryTree();
        String rootTitle = holder.getConfigName();
        if (rootTitle == null || rootTitle.isEmpty()) {
            rootTitle = "General";
        }
        if (roots.isEmpty()) {
            CollapsiblePanelWidget empty = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
            empty.text(rootTitle).expanded(true);
            empty.onExpandChanged(p -> syncContentHeight());
            return empty;
        }
        ConfigHolder.CategoryTreeNode rootNode = roots.get(0);
        CollapsiblePanelWidget rootPanel = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
        rootPanel.text(rootTitle).expanded(true);
        rootPanel.contentGap(ROW_GAP);
        rootPanel.headerHeight(ROW_HEIGHT);
        rootPanel.onExpandChanged(p -> syncContentHeight());

        buildPanelContent(rootPanel, rootNode);
        rootPanel.refreshLayout();
        return rootPanel;
    }

    private void buildPanelContent(CollapsiblePanelWidget panel, ConfigHolder.CategoryTreeNode node) {
        double cw = panel.getContentWidth();
        for (ConfigEntryDescriptor desc : node.getEntries()) {
            IConfigEntryWidget adapter = createEntryRow(desc, cw, ROW_HEIGHT);
            if (adapter != null) {
                entryWidgets.put(desc.getPath(), adapter);
                double rowHeight = adapter.getWidget().effectiveHeight() > 0 ? adapter.getWidget().effectiveHeight() : ROW_HEIGHT;
                panel.addChildAuto(adapter.getWidget(), rowHeight);
            }
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            CollapsiblePanelWidget childPanel = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, cw);
            childPanel.text(child.getDisplayName()).expanded(false);
            childPanel.contentGap(ROW_GAP);
            childPanel.headerHeight(ROW_HEIGHT);
            childPanel.onExpandChanged(p -> syncContentHeight());
            buildPanelContent(childPanel, child);
            childPanel.refreshLayout();
            panel.addCollapsibleChild(childPanel);
        }
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
            btnWidths[i] = font.width(bottomButtons.get(i).text().toString()) + BUTTON_PADDING * 2;
        }

        if (n == 3) {
            int segW = (cardW - 2 * CARD_GAP) / 3;
            int contentW = segW - CARD_INNER * 2;
            for (int i = 0; i < n; i++) {
                ButtonWidget btn = bottomButtons.get(i);
                int bw = Math.min(btnWidths[i], Math.max(20, contentW));
                int segX = cardX + i * (segW + CARD_GAP);
                int cx = segX + CARD_INNER + Math.max(0, (contentW - bw) / 2);
                btn.bounds(new ScreenCoordinate(cx, btnY, bw, BUTTON_HEIGHT));
            }
        } else {
            int contentTotal = cardW - CARD_INNER * 2 - CARD_GAP;
            int zoneW = contentTotal / 2;
            int leftRectW = CARD_INNER + zoneW;
            int rightRectW = cardW - leftRectW - CARD_GAP;
            int rightRectX = cardX + leftRectW + CARD_GAP;
            int lastIdx = n - 1;
            int leftTotalW = 0;
            for (int i = 0; i < lastIdx; i++) {
                leftTotalW += btnWidths[i] + (i > 0 ? BUTTON_GAP : 0);
            }
            int rightTotalW = btnWidths[lastIdx];
            double leftScale = leftTotalW > zoneW ? (double) zoneW / leftTotalW : 1.0;
            double rightScale = rightTotalW > zoneW ? (double) zoneW / rightTotalW : 1.0;
            int leftTotalScaled = (int) (leftTotalW * leftScale);
            int curX = cardX + (leftRectW - leftTotalScaled) / 2;
            for (int i = 0; i < n; i++) {
                ButtonWidget btn = bottomButtons.get(i);
                double scale = i < lastIdx ? leftScale : rightScale;
                int bw = Math.max(20, (int) (btnWidths[i] * scale));
                if (i == lastIdx) {
                    curX = rightRectX + (rightRectW - bw) / 2;
                }
                btn.bounds(new ScreenCoordinate(curX, btnY, bw, BUTTON_HEIGHT));
                curX += bw + BUTTON_GAP;
            }
        }
    }

    private void updateWidgetPositions() {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset, contentW, contentHeight));
        }
    }

    /**
     * 创建配置项行（Label + 值控件），返回适配器。行作为子组件加入 CollapsiblePanelWidget。
     */
    private IConfigEntryWidget createEntryRow(ConfigEntryDescriptor desc, double w, int rowH) {
        switch (desc.getValueType()) {
            case STRING:
                return createStringRow(desc, w, rowH);
            case BOOLEAN:
                return createBooleanRow(desc, w, rowH);
            case INTEGER:
            case LONG:
            case DOUBLE:
                return createNumberRow(desc, w, rowH);
            case ENUM:
                return createEnumRow(desc, w, rowH);
            case STRING_LIST:
                return createStringListRow(desc, w, rowH);
            default:
                return null;
        }
    }

    private static String getDescriptionText(ConfigEntryDescriptor desc) {
        List<String> tooltip = desc.getTooltip();
        if (tooltip == null || tooltip.isEmpty()) return "";
        return String.join("\n", tooltip);
    }

    private double valueWidgetWidth(double w) {
        return w - LABEL_WIDTH - 4 - RESET_BTN_WIDTH - RESET_BTN_GAP;
    }

    private double resetBtnX(double w) {
        return w - RESET_BTN_WIDTH;
    }

    private void addResetButton(EntryRowWidget row, ConfigEntryDescriptor desc, double rowW, int rowH,
                                java.util.function.Consumer<Object> setValue) {
        ButtonWidget btn = new ButtonWidget(this);
        btn.id("reset_" + desc.getPath().replace(".", "_"));
        btn.text(Component.transClientAuto(BaniraCodex.MODID, "config_editor_reset").toString());
        btn.bounds(new ScreenCoordinate(resetBtnX(rowW), 0, RESET_BTN_WIDTH, rowH));
        btn.onClick(b -> {
            Object def = desc.getDefaultValue();
            if (def != null) {
                holder.set(desc.getPath(), def);
                modifiedValues.put(desc.getPath(), def);
                setValue.accept(def);
            }
        });
        TooltipWidget resetTip = new TooltipWidget(this, new ScreenCoordinate(resetBtnX(rowW), 0, RESET_BTN_WIDTH, rowH));
        resetTip.id("reset_tip_" + desc.getPath().replace(".", "_"));
        resetTip.text(Component.transClientAuto(BaniraCodex.MODID, "config_editor_reset_tooltip"));
        resetTip.popupAtScreenCoords(true);
        row.addChild(btn);
        row.addChild(resetTip);
    }

    private IConfigEntryWidget createStringRow(ConfigEntryDescriptor desc, double w, int rowH) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, w, rowH));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(0, 0, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        InputWidget input = new InputWidget(this);
        input.id("cfg_" + desc.getPath().replace(".", "_"));
        input.bounds(new ScreenCoordinate(LABEL_WIDTH, 0, valueWidgetWidth(w), rowH));
        Object raw = holder.get(desc.getPath());
        String str = (raw instanceof String) ? (String) raw : (raw != null ? raw.toString() : "");
        input.value(str);
        input.maxLength(256);
        input.onTextChanged(v -> modifiedValues.put(desc.getPath(), v));

        row.addChild(label);
        row.addChild(input);
        addResetButton(row, desc, w, rowH, v -> input.value(String.valueOf(v)));
        TooltipWidget tooltip = createEntryTooltip(desc, 0, 0, w, rowH);
        if (tooltip != null) row.addChild(tooltip);

        return new ConfigEntryWidgetAdapter(desc, row, label, input, tooltip, input::value, v -> input.value(String.valueOf(v)));
    }

    private TooltipWidget createEntryTooltip(ConfigEntryDescriptor desc, double x, double y, double w, int rowH) {
        String descText = getDescriptionText(desc);
        if (descText.isEmpty()) return null;
        TooltipWidget tooltip = new TooltipWidget(this, new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        tooltip.id("tip_" + desc.getPath().replace(".", "_"));
        tooltip.text(Component.literal(descText));
        tooltip.popupAtScreenCoords(true);
        return tooltip;
    }

    private IConfigEntryWidget createBooleanRow(ConfigEntryDescriptor desc, double w, int rowH) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, w, rowH));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(0, 0, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        boolean val = Boolean.TRUE.equals(holder.get(desc.getPath()));
        ButtonWidget btn = new ButtonWidget(this);
        btn.id("cfg_" + desc.getPath().replace(".", "_"));
        btn.bounds(new ScreenCoordinate(LABEL_WIDTH, 0, valueWidgetWidth(w), rowH));
        btn.text(val ? "§aON" : "§cOFF");
        btn.onClick(b -> {
            boolean newVal = !Boolean.TRUE.equals(holder.get(desc.getPath()));
            holder.set(desc.getPath(), newVal);
            modifiedValues.put(desc.getPath(), newVal);
            btn.text(newVal ? "§aON" : "§cOFF");
        });

        row.addChild(label);
        row.addChild(btn);
        addResetButton(row, desc, w, rowH, v -> btn.text(Boolean.TRUE.equals(v) ? "§aON" : "§cOFF"));
        TooltipWidget tooltip = createEntryTooltip(desc, 0, 0, w, rowH);
        if (tooltip != null) row.addChild(tooltip);
        return new ConfigEntryWidgetAdapter(desc, row, label, btn, tooltip, () -> Boolean.TRUE.equals(holder.get(desc.getPath())), v -> {
        });
    }

    private IConfigEntryWidget createNumberRow(ConfigEntryDescriptor desc, double w, int rowH) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, w, rowH));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(0, 0, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        double min = desc.getMinValue() != null ? desc.getMinValue().doubleValue() : 0;
        double max = desc.getMaxValue() != null ? desc.getMaxValue().doubleValue() : 100;
        double step = 1.0;
        if (desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE) {
            double minStep = 1.0 / Math.pow(10, desc.getDecimalPlaces());
            step = Math.max(minStep, (max - min) / 100);
        }

        Object raw = holder.get(desc.getPath());
        double initVal = 0;
        if (raw instanceof Number) {
            initVal = ((Number) raw).doubleValue();
        } else if (raw != null) {
            try {
                initVal = Double.parseDouble(raw.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        initVal = Math.max(min, Math.min(max, initVal));

        SliderWidget slider = new SliderWidget(this);
        slider.id("cfg_" + desc.getPath().replace(".", "_"));
        slider.bounds(new ScreenCoordinate(LABEL_WIDTH, 0, valueWidgetWidth(w), rowH));
        slider.minValue(min).maxValue(max).step(step).value(initVal);
        slider.decimalPlaces(desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE
                ? desc.getDecimalPlaces() : 0);
        slider.onValueChanged(v -> {
            Object parsed = convertSliderValue(desc, v);
            if (parsed != null && !Objects.equals(parsed, holder.get(desc.getPath()))) {
                modifiedValues.put(desc.getPath(), parsed);
            }
        });

        row.addChild(label);
        row.addChild(slider);
        addResetButton(row, desc, w, rowH, v -> {
            double d = v instanceof Number ? ((Number) v).doubleValue() : 0;
            slider.setValue(Math.max(slider.minValue(), Math.min(slider.maxValue(), d)));
        });
        TooltipWidget tooltip = createEntryTooltip(desc, 0, 0, w, rowH);
        if (tooltip != null) row.addChild(tooltip);
        return new ConfigEntryWidgetAdapter(desc, row, label, slider, tooltip, () -> convertSliderValue(desc, slider.value()), v -> {
            double d = v instanceof Number ? ((Number) v).doubleValue() : 0;
            slider.setValue(Math.max(slider.minValue(), Math.min(slider.maxValue(), d)));
        }, () -> true);
    }

    private Object convertSliderValue(ConfigEntryDescriptor desc, double v) {
        switch (desc.getValueType()) {
            case INTEGER:
                return (int) Math.round(v);
            case LONG:
                return (long) Math.round(v);
            case DOUBLE: {
                int dp = desc.getDecimalPlaces();
                double factor = Math.pow(10, dp);
                return Math.round(v * factor) / factor;
            }
            default:
                return v;
        }
    }

    private IConfigEntryWidget createEnumRow(ConfigEntryDescriptor desc, double w, int rowH) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, w, rowH));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(0, 0, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        Object current = holder.get(desc.getPath());
        List<String> options = Arrays.stream(desc.getEnumClass().getEnumConstants())
                .map(e -> ((Enum<?>) e).name())
                .collect(Collectors.toList());

        DropdownSelectWidget dropdown = new DropdownSelectWidget(this);
        dropdown.id("cfg_" + desc.getPath().replace(".", "_"));
        dropdown.bounds(new ScreenCoordinate(LABEL_WIDTH, 0, valueWidgetWidth(w), rowH));
        dropdown.options(options);
        dropdown.selectedValues(Collections.singletonList(current != null ? current.toString() : options.get(0)));
        dropdown.onSelectionChanged(v -> {
            if (!v.isEmpty()) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), v.get(0));
                    modifiedValues.put(desc.getPath(), e);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        row.addChild(label);
        row.addChild(dropdown);
        addResetButton(row, desc, w, rowH, v -> dropdown.selectedValues(Collections.singletonList(v != null ? v.toString() : options.get(0))));
        TooltipWidget tooltip = createEntryTooltip(desc, 0, 0, w, rowH);
        if (tooltip != null) row.addChild(tooltip);
        return new ConfigEntryWidgetAdapter(desc, row, label, dropdown, tooltip, () -> {
            List<String> sel = dropdown.getSelectedValues();
            if (sel.isEmpty()) return holder.get(desc.getPath());
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), sel.get(0));
                return e;
            } catch (Exception ex) {
                return holder.get(desc.getPath());
            }
        }, v -> dropdown.selectedValues(Collections.singletonList(v != null ? v.toString() : options.get(0))));
    }

    private IConfigEntryWidget createStringListRow(ConfigEntryDescriptor desc, double w, int rowH) {
        EntryRowWidget row = new EntryRowWidget(this);

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(0, 0, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        Object raw = holder.get(desc.getPath());
        @SuppressWarnings("unchecked")
        List<String> list = (raw instanceof List) ? (List<String>) raw : null;
        List<Object> items = list != null ? new ArrayList<>(list) : new ArrayList<>();

        TagListEditorWidget tagList = new TagListEditorWidget(this);
        tagList.id("cfg_" + desc.getPath().replace(".", "_"));
        tagList.bounds(new ScreenCoordinate(LABEL_WIDTH, 0, valueWidgetWidth(w), TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagList.itemType(TagListEditorWidget.ItemType.TEXT);
        tagList.items(items);
        tagList.expanded(false);
        tagList.refreshBounds();
        row.bounds(new ScreenCoordinate(0, 0, w, tagList.effectiveHeight()));
        tagList.onExpandChanged(t -> {
            IWidget rowWidget = t.parent();
            if (rowWidget != null && rowWidget instanceof BaseWidget) {
                double newH = t.effectiveHeight();
                ScreenCoordinate b = rowWidget.bounds();
                if (b != null) {
                    ((BaseWidget) rowWidget).bounds(new ScreenCoordinate(b.x(), b.y(), b.width(), newH));
                }
                IWidget panel = rowWidget.parent();
                if (panel instanceof CollapsiblePanelWidget) {
                    ((CollapsiblePanelWidget) panel).refreshLayoutFromChild(rowWidget);
                }
            }
            syncContentHeight();
        });
        tagList.onListChanged(v -> modifiedValues.put(desc.getPath(), v.stream().map(String::valueOf).collect(Collectors.toList())));

        int tagRowH = (int) tagList.effectiveHeight();
        row.addChild(label);
        row.addChild(tagList);
        addResetButton(row, desc, w, tagRowH, v -> {
            if (v instanceof List) {
                tagList.items(((List<?>) v).stream().map(String::valueOf).collect(Collectors.toList()));
            }
        });
        TooltipWidget tooltip = createEntryTooltip(desc, 0, 0, w, rowH);
        if (tooltip != null) row.addChild(tooltip);
        return new ConfigEntryWidgetAdapter(desc, row, label, tagList, tooltip,
                () -> tagList.items().stream().map(String::valueOf).collect(Collectors.toList()),
                v -> {
                    if (v instanceof List) {
                        tagList.items(((List<?>) v).stream().map(String::valueOf).collect(Collectors.toList()));
                    }
                });
    }

    private void saveConfig() {
        collectModifiedFromWidgets();
        if (hasInvalidEntryWidgets()) {
            Notification n = Notification.ofComponent(Component.transClientAuto(BaniraCodex.MODID, "config_editor_validation_failed"));
            n.position(EnumPosition.TOP_RIGHT).durationTime(3000);
            NotificationManager.get().addNotification(n);
            return;
        }
        for (Map.Entry<String, Object> e : modifiedValues.entrySet()) {
            holder.set(e.getKey(), e.getValue());
        }
        modifiedValues.clear();
        try {
            holder.save();
            Notification n = Notification.ofComponent(Component.transClientAuto(BaniraCodex.MODID, "config_editor_save_success"));
            n.position(EnumPosition.TOP_RIGHT).durationTime(2000);
            NotificationManager.get().addNotification(n);
            if (previousScreen() != null) {
                onClose();
            }
        } catch (Exception ex) {
            Notification n = Notification.ofComponent(Component.transClientAuto(BaniraCodex.MODID, "config_editor_save_failed", ex.getMessage()));
            n.position(EnumPosition.TOP_RIGHT).durationTime(4000);
            NotificationManager.get().addNotification(n);
        }
    }

    private void syncToServer() {
        collectModifiedFromWidgets();
        if (hasInvalidEntryWidgets()) {
            Notification n = Notification.ofComponent(Component.transClientAuto(BaniraCodex.MODID, "config_editor_validation_failed"));
            n.position(EnumPosition.TOP_RIGHT).durationTime(3000);
            NotificationManager.get().addNotification(n);
            return;
        }
        if (modifiedValues.isEmpty()) {
            return;
        }
        Map<String, String> toSync = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : modifiedValues.entrySet()) {
            toSync.put(e.getKey(), serializeValue(e.getValue()));
        }
        NetworkInit.HANDLER.getChannel().sendToServer(new ConfigSyncToServer(holder.getConfigName(), toSync));
        modifiedValues.clear();
        for (Map.Entry<String, String> e : toSync.entrySet()) {
            holder.set(e.getKey(), parseValue(e.getKey(), e.getValue()));
        }
    }

    private Object parseValue(String path, String value) {
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc == null) return value;
        try {
            switch (desc.getValueType()) {
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case INTEGER:
                    return Integer.parseInt(value);
                case LONG:
                    return Long.parseLong(value);
                case DOUBLE: {
                    double d = Double.parseDouble(value);
                    int dp = desc.getDecimalPlaces();
                    double factor = Math.pow(10, dp);
                    return Math.round(d * factor) / factor;
                }
                case ENUM:
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), value);
                    return e;
                case STRING_LIST:
                    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }

    private String serializeValue(Object value) {
        if (value instanceof List) {
            return String.join(",", (List<String>) value);
        }
        return String.valueOf(value);
    }

    private void collectModifiedFromWidgets() {
        for (Map.Entry<String, IConfigEntryWidget> e : entryWidgets.entrySet()) {
            if (!e.getValue().isValid()) continue;
            Object v = e.getValue().getValue();
            if (v != null && !Objects.equals(v, holder.get(e.getKey()))) {
                modifiedValues.put(e.getKey(), v);
            }
        }
    }

    private boolean hasInvalidEntryWidgets() {
        return entryWidgets.values().stream().anyMatch(w -> !w.isValid());
    }

    private static final int CARD_RADIUS = 8;
    private static final int CARD_ALPHA = 0xFF;

    @Override
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBg = ColorUtils.applyAlphaToArgb(theme.bgSurface(), CARD_ALPHA);
        int btnAreaH = BUTTON_HEIGHT + CARD_INNER;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int contentH = btnAreaTop - cardY - CARD_GAP;
        int n = bottomButtons.size();

        AbstractGuiUtils.drawRoundedRect(stack, cardX, cardY, cardW, contentH,
                CARD_RADIUS, CARD_RADIUS, 0, 0, cardBg);

        if (n == 3) {
            int segW = (cardW - 2 * CARD_GAP) / 3;
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, segW, btnAreaH,
                    0, 0, CARD_RADIUS, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + segW + CARD_GAP, btnAreaTop, segW, btnAreaH,
                    0, 0, 0, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + 2 * (segW + CARD_GAP), btnAreaTop, segW, btnAreaH,
                    0, 0, 0, CARD_RADIUS, cardBg);
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
            contentRootPanel.render(stack, partialTicks);
        }
        if (scrollbar != null && scrollbar.visible()) {
            if (scrollbar.enabled() && scrollbar.needsUpdate()) scrollbar.update();
            scrollbar.render(stack, partialTicks);
        }

        AbstractGuiUtils.disableScissor();

        for (ButtonWidget btn : bottomButtons) {
            if (btn.visible()) {
                if (btn.enabled() && btn.needsUpdate()) btn.update();
                btn.render(stack, partialTicks);
            }
        }

        // 渲染 overlay 控件，需在 scissor 关闭后渲染以免被裁剪
        for (IWidget widget : widgets()) {
            if (widget == contentRootPanel || widget == scrollbar || bottomButtons.contains(widget)) continue;
            if (widget.parent() != null || !widget.visible()) continue;
            if (widget.enabled() && widget.needsUpdate()) widget.update();
            widget.render(stack, partialTicks);
        }
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && contentRootPanel != null && contentRootPanel.visible() && contentRootPanel.enabled()
                && contentRootPanel.isMouseInside(mouseX, mouseY)
                && contentRootPanel.handleMouseScroll(MouseScrollEvent.of(mouseX, mouseY, delta))) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
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

    public static class Args {
        private Screen parentScreen;
        private BaniraColorConfig theme;
        private EnumSeason season;

        public Args parentScreen(Screen s) {
            parentScreen = s;
            return this;
        }

        public Args theme(BaniraColorConfig t) {
            theme = t;
            return this;
        }

        public Args season(EnumSeason s) {
            season = s;
            return this;
        }

        public Screen parentScreen() {
            return parentScreen;
        }

        public BaniraColorConfig theme() {
            return theme;
        }

        public EnumSeason season() {
            return season;
        }
    }

    private interface IConfigEntryWidget {
        BaseWidget getWidget();

        Object getValue();

        void setValue(Object value);

        default boolean isValid() {
            return true;
        }
    }

    private static class ConfigEntryWidgetAdapter implements IConfigEntryWidget {
        private final BaseWidget rowWidget;
        private final java.util.function.Supplier<Object> getter;
        private final java.util.function.Consumer<Object> setter;
        private final java.util.function.Supplier<Boolean> isValidSupplier;

        ConfigEntryWidgetAdapter(ConfigEntryDescriptor desc, BaseWidget rowWidget, LabelWidget label,
                                 BaseWidget valueWidget, TooltipWidget tooltipWidget,
                                 java.util.function.Supplier<Object> getter, java.util.function.Consumer<Object> setter) {
            this.rowWidget = rowWidget;
            this.getter = getter;
            this.setter = setter;
            this.isValidSupplier = null;
        }

        ConfigEntryWidgetAdapter(ConfigEntryDescriptor desc, BaseWidget rowWidget, LabelWidget label,
                                 BaseWidget valueWidget, TooltipWidget tooltipWidget,
                                 java.util.function.Supplier<Object> getter, java.util.function.Consumer<Object> setter,
                                 java.util.function.Supplier<Boolean> isValidSupplier) {
            this.rowWidget = rowWidget;
            this.getter = getter;
            this.setter = setter;
            this.isValidSupplier = isValidSupplier;
        }

        @Override
        public boolean isValid() {
            return isValidSupplier == null || isValidSupplier.get();
        }

        @Override
        public BaseWidget getWidget() {
            return rowWidget;
        }

        @Override
        public Object getValue() {
            return getter.get();
        }

        @Override
        public void setValue(Object value) {
            setter.accept(value);
        }
    }

    /**
     * 配置项行容器，仅用于容纳 Label + 值控件 + Tooltip。
     * 重写 effectiveHeight 以支持子控件动态变化高度。
     */
    private static class EntryRowWidget extends BaseWidget {
        EntryRowWidget(BaniraScreen screen) {
            super(screen);
        }

        @Override
        public double effectiveHeight() {
            double maxBottom = 0;
            for (IWidget child : children()) {
                if (child == null || !child.visible()) continue;
                ScreenCoordinate b = child.bounds();
                if (b != null) {
                    double bottom = b.y() + child.effectiveHeight();
                    if (bottom > maxBottom) maxBottom = bottom;
                }
            }
            return maxBottom > 0 ? maxBottom : (bounds() != null ? bounds().height() : 0);
        }

        @Override
        protected boolean onMouseClick(MouseEvent event) {
            return true;
        }

        @Override
        public void render(MatrixStack stack, float partialTicks) {
            if (!visible) return;
            renderChildren(stack, partialTicks);
        }
    }
}
