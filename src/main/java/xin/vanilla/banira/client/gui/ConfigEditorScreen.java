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
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;
import xin.vanilla.banira.internal.network.NetworkInit;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置编辑界面，支持可视化编辑 ForgeConfigSpec 配置，并可同步修改项至服务端
 */
public class ConfigEditorScreen extends BaniraScreen {

    private static final int PADDING = 12;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int LABEL_WIDTH = 140;
    private static final int CATEGORY_INDENT = 4;
    private static final int ENTRY_INDENT = 20;
    private static final int INDENT_PER_LEVEL = 16;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_PADDING = 12;
    private static final int BUTTON_GAP = 6;

    private final ConfigHolder holder;
    private final Args args;

    private ScrollbarWidget scrollbar;
    private double scrollOffset = 0;
    private int contentHeight = 0;
    private int listTop;
    private int listAreaHeight;
    private int contentLeft;
    private int contentW;
    private int btnY;
    private int contentTotalW;
    private final List<ButtonWidget> bottomButtons = new ArrayList<>();

    /**
     * 已展开的分类路径（默认全部折叠）
     */
    private final Set<String> expandedCategories = new LinkedHashSet<>();
    /**
     * 分类路径 -> 分类标题按钮
     */
    private final Map<String, ButtonWidget> categoryHeaderWidgets = new LinkedHashMap<>();

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
        contentLeft = PADDING;
        contentW = w - PADDING * 2 - SCROLL_WIDTH - SCROLL_GAP;
        contentTotalW = contentW + SCROLL_GAP + SCROLL_WIDTH;
        listTop = PADDING;
        int maxListHeight = Math.max(0, h - PADDING * 2 - BUTTON_HEIGHT - BUTTON_GAP);

        categoryHeaderWidgets.clear();
        entryWidgets.clear();
        bottomButtons.clear();

        int[] yRef = {PADDING};
        for (ConfigHolder.CategoryTreeNode node : holder.getCategoryTree()) {
            buildCategoryTree(node, 0, true, yRef);
        }
        contentHeight = (int) yRef[0] - PADDING;

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

    private void buildCategoryTree(ConfigHolder.CategoryTreeNode node, int depth, boolean parentExpanded, int[] yRef) {
        String catPath = node.getCategoryPath();
        String displayName = node.getDisplayName();
        boolean expanded = expandedCategories.contains(catPath);
        int indent = CATEGORY_INDENT + depth * INDENT_PER_LEVEL;
        int availableW = contentW - indent;

        // categoryPath 为空表示根级配置项，直接渲染条目，不显示分类标题
        boolean isRootEntries = catPath.isEmpty();

        if (!isRootEntries) {
            ButtonWidget catBtn = new ButtonWidget(this);
            catBtn.id("cat_" + catPath.replace(".", "_"));
            catBtn.bounds(new ScreenCoordinate(contentLeft + indent, yRef[0], availableW, ROW_HEIGHT));
            catBtn.text((expanded ? "▼ " : "▶ ") + displayName);
            catBtn.visible(parentExpanded);
            catBtn.onClick(b -> {
                if (expandedCategories.contains(catPath)) {
                    expandedCategories.remove(catPath);
                } else {
                    expandedCategories.add(catPath);
                }
                refreshCategoryExpandState();
            });
            addWidget(catBtn);
            categoryHeaderWidgets.put(catPath, catBtn);
            if (parentExpanded) yRef[0] += ROW_HEIGHT + ROW_GAP;
        }

        // 虚拟根的条目与同级分类按钮左对齐（均使用 indent + INDENT_PER_LEVEL）
        int entryIndent = indent + INDENT_PER_LEVEL;
        int entryW = contentW - entryIndent;
        boolean entriesVisible = parentExpanded && (isRootEntries || expanded);

        for (ConfigEntryDescriptor desc : node.getEntries()) {
            int entryX = contentLeft + entryIndent;
            IConfigEntryWidget widget = createWidgetFor(desc, entryX, (int) yRef[0], entryW, ROW_HEIGHT);
            if (widget != null) {
                entryWidgets.put(desc.getPath(), widget);
                widget.setCategory(catPath);
                widget.setVisible(entriesVisible);
            }
            if (entriesVisible) yRef[0] += ROW_HEIGHT + ROW_GAP;
        }

        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            buildCategoryTree(child, depth + 1, parentExpanded && (isRootEntries || expanded), yRef);
        }
    }

    private void updateLayout() {
        int h = height;
        int maxListHeight = Math.max(0, h - PADDING * 2 - BUTTON_HEIGHT - BUTTON_GAP);

        if (contentHeight <= maxListHeight) {
            listAreaHeight = Math.max(1, contentHeight);
            btnY = h - PADDING - BUTTON_HEIGHT; // 列表未撑满时按钮也固定在底部
            scrollOffset = 0;
            scrollbar.value(0);
            scrollbar.visible(false);
        } else {
            listAreaHeight = maxListHeight;
            btnY = h - PADDING - BUTTON_HEIGHT;
            scrollbar.visible(true);
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + SCROLL_GAP, listTop, SCROLL_WIDTH, listAreaHeight));
            scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
            scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
            scrollOffset = scrollbar.value();
            scrollbar.visibleSize(listAreaHeight);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop, contentTotalW, listAreaHeight));
        }

        int btnTotalW = 0;
        int[] btnWidths = new int[bottomButtons.size()];
        for (int i = 0; i < bottomButtons.size(); i++) {
            ButtonWidget btn = bottomButtons.get(i);
            int w = (int) font.width(btn.text().toString()) + BUTTON_PADDING * 2;
            btnWidths[i] = w;
            btnTotalW += w + (i < bottomButtons.size() - 1 ? BUTTON_GAP : 0);
        }
        double scale = btnTotalW > contentTotalW ? (double) contentTotalW / btnTotalW : 1.0;
        int curX = contentLeft + Math.max(0, (contentTotalW - (int) (btnTotalW * scale)) / 2);
        for (int i = 0; i < bottomButtons.size(); i++) {
            ButtonWidget btn = bottomButtons.get(i);
            int bw = Math.max(20, (int) (btnWidths[i] * scale));
            btn.bounds(new ScreenCoordinate(curX, btnY, bw, BUTTON_HEIGHT));
            curX += bw + BUTTON_GAP;
        }
    }

    /**
     * 仅刷新展开/折叠状态与布局，不重建 widget 树，避免未保存的修改丢失。
     */
    private void refreshCategoryExpandState() {
        refreshCategoryHeaders();
        contentHeight = computeContentHeight();
        updateLayout();
        updateWidgetPositions();
    }

    private void refreshCategoryHeaders() {
        for (ConfigHolder.CategoryTreeNode node : holder.getCategoryTree()) {
            refreshCategoryHeadersRec(node);
        }
    }

    private void refreshCategoryHeadersRec(ConfigHolder.CategoryTreeNode node) {
        if (!node.getCategoryPath().isEmpty()) {
            ButtonWidget btn = categoryHeaderWidgets.get(node.getCategoryPath());
            if (btn != null) {
                boolean expanded = expandedCategories.contains(node.getCategoryPath());
                btn.text((expanded ? "▼ " : "▶ ") + node.getDisplayName());
            }
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            refreshCategoryHeadersRec(child);
        }
    }

    private int computeContentHeight() {
        int[] yRef = {PADDING};
        for (ConfigHolder.CategoryTreeNode node : holder.getCategoryTree()) {
            computeContentHeightRec(node, true, yRef);
        }
        return yRef[0] - PADDING;
    }

    private void computeContentHeightRec(ConfigHolder.CategoryTreeNode node, boolean parentExpanded, int[] yRef) {
        if (!parentExpanded) return;
        boolean isVirtualRoot = node.getCategoryPath().isEmpty();
        if (!isVirtualRoot) yRef[0] += ROW_HEIGHT + ROW_GAP;
        boolean expanded = isVirtualRoot || expandedCategories.contains(node.getCategoryPath());
        if (expanded) {
            yRef[0] += node.getEntries().size() * (ROW_HEIGHT + ROW_GAP);
            for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
                computeContentHeightRec(child, expanded, yRef);
            }
        }
    }

    private void updateWidgetPositions() {
        int[] yRef = {PADDING - (int) scrollOffset};
        for (ConfigHolder.CategoryTreeNode node : holder.getCategoryTree()) {
            updateWidgetPositionsRec(node, 0, true, yRef);
        }
    }

    private void updateWidgetPositionsRec(ConfigHolder.CategoryTreeNode node, int depth, boolean parentExpanded, int[] yRef) {
        String catPath = node.getCategoryPath();
        boolean isVirtualRoot = catPath.isEmpty();
        boolean expanded = isVirtualRoot || expandedCategories.contains(catPath);
        int indent = CATEGORY_INDENT + depth * INDENT_PER_LEVEL;
        int availableW = contentW - indent;
        // 虚拟根的条目与同级分类按钮左对齐
        int entryIndent = indent + INDENT_PER_LEVEL;
        int entryW = contentW - entryIndent;

        ButtonWidget catBtn = categoryHeaderWidgets.get(catPath);
        if (catBtn != null) {
            catBtn.visible(parentExpanded);
            catBtn.bounds(new ScreenCoordinate(contentLeft + indent, yRef[0], availableW, ROW_HEIGHT));
        }
        if (!isVirtualRoot && parentExpanded) yRef[0] += ROW_HEIGHT + ROW_GAP;

        for (ConfigEntryDescriptor desc : node.getEntries()) {
            IConfigEntryWidget ew = entryWidgets.get(desc.getPath());
            if (ew != null) {
                ew.setVisible(parentExpanded && expanded);
                ew.updatePosition(contentLeft + entryIndent, yRef[0], entryW, ROW_HEIGHT);
            }
            if (parentExpanded && expanded) yRef[0] += ROW_HEIGHT + ROW_GAP;
        }

        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            updateWidgetPositionsRec(child, depth + 1, parentExpanded && expanded, yRef);
        }
    }

    private IConfigEntryWidget createWidgetFor(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        switch (desc.getValueType()) {
            case STRING:
                return createStringWidget(desc, x, y, w, rowH);
            case BOOLEAN:
                return createBooleanWidget(desc, x, y, w, rowH);
            case INTEGER:
            case LONG:
            case DOUBLE:
                return createNumberWidget(desc, x, y, w, rowH);
            case ENUM:
                return createEnumWidget(desc, x, y, w, rowH);
            case STRING_LIST:
                return createStringListWidget(desc, x, y, w, rowH);
            default:
                return null;
        }
    }

    private static String getDescriptionText(ConfigEntryDescriptor desc) {
        List<String> tooltip = desc.getTooltip();
        if (tooltip == null || tooltip.isEmpty()) return "";
        return String.join("\n", tooltip);
    }

    private IConfigEntryWidget createStringWidget(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        InputWidget input = new InputWidget(this);
        input.id("cfg_" + desc.getPath().replace(".", "_"));
        input.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, rowH));
        Object raw = holder.get(desc.getPath());
        String str = (raw instanceof String) ? (String) raw : (raw != null ? raw.toString() : "");
        input.value(str);
        input.maxLength(256);
        input.onTextChanged(v -> modifiedValues.put(desc.getPath(), v));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        TooltipWidget tooltip = createEntryTooltip(desc, x, y, w, rowH);
        addWidget(label);
        addWidget(input);
        if (tooltip != null) addWidget(tooltip);
        return new ConfigEntryWidgetAdapter(desc, label, input, tooltip, () -> input.value(), v -> input.value(String.valueOf(v)));
    }

    private TooltipWidget createEntryTooltip(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        String descText = getDescriptionText(desc);
        if (descText.isEmpty()) return null;
        TooltipWidget tooltip = new TooltipWidget(this, new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        tooltip.id("tip_" + desc.getPath().replace(".", "_"));
        tooltip.text(Component.literal(descText));
        tooltip.useTextureDrawing(false);
        return tooltip;
    }

    private IConfigEntryWidget createBooleanWidget(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        boolean val = Boolean.TRUE.equals(holder.get(desc.getPath()));
        ButtonWidget btn = new ButtonWidget(this);
        btn.id("cfg_" + desc.getPath().replace(".", "_"));
        btn.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, rowH));
        btn.text(val ? "§aON" : "§cOFF");
        btn.onClick(b -> {
            boolean newVal = !Boolean.TRUE.equals(holder.get(desc.getPath()));
            holder.set(desc.getPath(), newVal);
            modifiedValues.put(desc.getPath(), newVal);
            btn.text(newVal ? "§aON" : "§cOFF");
        });

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        TooltipWidget tooltip = createEntryTooltip(desc, x, y, w, rowH);
        addWidget(label);
        addWidget(btn);
        if (tooltip != null) addWidget(tooltip);
        return new ConfigEntryWidgetAdapter(desc, label, btn, tooltip, () -> Boolean.TRUE.equals(holder.get(desc.getPath())), v -> {
        });
    }

    private IConfigEntryWidget createNumberWidget(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        InputWidget input = new InputWidget(this);
        input.id("cfg_" + desc.getPath().replace(".", "_"));
        input.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, rowH));
        Object raw = holder.get(desc.getPath());
        input.value(raw != null ? String.valueOf(raw) : "0");
        input.maxLength(32);
        input.validator(s -> {
            try {
                switch (desc.getValueType()) {
                    case INTEGER:
                        int i = Integer.parseInt(s);
                        return (desc.getMinValue() == null || i >= desc.getMinValue().intValue())
                                && (desc.getMaxValue() == null || i <= desc.getMaxValue().intValue());
                    case LONG:
                        long l = Long.parseLong(s);
                        return (desc.getMinValue() == null || l >= desc.getMinValue().longValue())
                                && (desc.getMaxValue() == null || l <= desc.getMaxValue().longValue());
                    case DOUBLE:
                        double d = Double.parseDouble(s);
                        return (desc.getMinValue() == null || d >= desc.getMinValue().doubleValue())
                                && (desc.getMaxValue() == null || d <= desc.getMaxValue().doubleValue());
                    default:
                        return true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        });
        input.onTextChanged(v -> {
            boolean valid = input.validator() != null && input.validator().apply(v);
            input.error(!valid);
            if (valid) {
                input.errorMessage(null);
                try {
                    Object parsed = null;
                    switch (desc.getValueType()) {
                        case INTEGER:
                            parsed = Integer.parseInt(v);
                            break;
                        case LONG:
                            parsed = Long.parseLong(v);
                            break;
                        case DOUBLE:
                            parsed = Double.parseDouble(v);
                            break;
                    }
                    if (parsed != null && !Objects.equals(parsed, holder.get(desc.getPath()))) {
                        modifiedValues.put(desc.getPath(), parsed);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else {
                input.errorMessage(buildNumberRangeError(desc));
                modifiedValues.remove(desc.getPath());
            }
        });
        // 初始化时校验
        boolean initValid = input.validator() != null && input.validator().apply(input.value());
        input.error(!initValid);
        if (!initValid) input.errorMessage(buildNumberRangeError(desc));

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        TooltipWidget tooltip = createEntryTooltip(desc, x, y, w, rowH);
        addWidget(label);
        addWidget(input);
        if (tooltip != null) addWidget(tooltip);
        return new ConfigEntryWidgetAdapter(desc, label, input, tooltip, () -> {
            try {
                switch (desc.getValueType()) {
                    case INTEGER:
                        return Integer.parseInt(input.value());
                    case LONG:
                        return Long.parseLong(input.value());
                    case DOUBLE:
                        return Double.parseDouble(input.value());
                }
            } catch (NumberFormatException e) {
                return holder.get(desc.getPath());
            }
            return holder.get(desc.getPath());
        }, v -> input.value(String.valueOf(v)), () -> !input.error());
    }

    private static String buildNumberRangeError(ConfigEntryDescriptor desc) {
        if (desc.getMinValue() != null && desc.getMaxValue() != null) {
            return "值需在 " + desc.getMinValue() + " 到 " + desc.getMaxValue() + " 之间";
        }
        if (desc.getMinValue() != null) return "值需不小于 " + desc.getMinValue();
        if (desc.getMaxValue() != null) return "值需不大于 " + desc.getMaxValue();
        return "无效数值";
    }

    private IConfigEntryWidget createEnumWidget(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        Object current = holder.get(desc.getPath());
        List<String> options = Arrays.stream(desc.getEnumClass().getEnumConstants())
                .map(e -> ((Enum<?>) e).name())
                .collect(Collectors.toList());

        DropdownSelectWidget dropdown = new DropdownSelectWidget(this);
        dropdown.id("cfg_" + desc.getPath().replace(".", "_"));
        dropdown.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, rowH));
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

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        TooltipWidget tooltip = createEntryTooltip(desc, x, y, w, rowH);
        addWidget(label);
        addWidget(dropdown);
        if (tooltip != null) addWidget(tooltip);
        return new ConfigEntryWidgetAdapter(desc, label, dropdown, tooltip, () -> {
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

    private IConfigEntryWidget createStringListWidget(ConfigEntryDescriptor desc, int x, int y, int w, int rowH) {
        Object raw = holder.get(desc.getPath());
        @SuppressWarnings("unchecked")
        List<String> list = (raw instanceof List) ? (List<String>) raw : null;
        String display = list != null ? String.join(", ", list) : "";

        InputWidget input = new InputWidget(this);
        input.id("cfg_" + desc.getPath().replace(".", "_"));
        input.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, rowH));
        input.value(display);
        input.maxLength(1024);
        input.text(Text.literal("Comma separated"));
        input.onTextChanged(v -> {
            List<String> parsed = Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            modifiedValues.put(desc.getPath(), parsed);
        });

        LabelWidget label = new LabelWidget(this);
        label.id("lbl_" + desc.getPath().replace(".", "_"));
        label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        TooltipWidget tooltip = createEntryTooltip(desc, x, y, w, rowH);
        addWidget(label);
        addWidget(input);
        if (tooltip != null) addWidget(tooltip);
        return new ConfigEntryWidgetAdapter(desc, label, input, tooltip, () -> {
            String s = input.value();
            return Arrays.stream(s.split(",")).map(String::trim).filter(xx -> !xx.isEmpty()).collect(Collectors.toList());
        }, v -> {
            if (v instanceof List) {
                input.value(String.join(", ", (List<String>) v));
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
                case DOUBLE:
                    return Double.parseDouble(value);
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

    @Override
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        int contentTotalW = contentW + SCROLL_GAP + SCROLL_WIDTH;
        AbstractGuiUtils.enableScissor(contentLeft, listTop, contentTotalW, Math.max(1, listAreaHeight));

        if (scrollbar != null && scrollbar.visible()) {
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + SCROLL_GAP, listTop, SCROLL_WIDTH, listAreaHeight));
            if (scrollbar.enabled() && scrollbar.needsUpdate()) scrollbar.update();
            scrollbar.render(stack, partialTicks);
        }

        List<IWidget> deferred = null;
        for (IWidget widget : widgets()) {
            if (widget == scrollbar) continue;
            if (widget.parent() != null || !widget.visible()) continue;
            boolean isTooltip = widget instanceof TooltipWidget;
            boolean isBottomBtn = widget.id() != null && (widget.id().equals("save") || widget.id().equals("sync") || widget.id().equals("close"));
            if (isTooltip || isBottomBtn) {
                if (deferred == null) deferred = new ArrayList<>();
                deferred.add(widget);
            } else {
                if (widget.enabled() && widget.needsUpdate()) widget.update();
                widget.render(stack, partialTicks);
            }
        }

        AbstractGuiUtils.disableScissor();

        if (deferred != null) {
            for (IWidget widget : deferred) {
                if (widget.enabled() && widget.needsUpdate()) widget.update();
                widget.render(stack, partialTicks);
            }
        }
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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

        void updatePosition(int x, int y, int w, int h);

        Object getValue();

        void setValue(Object value);

        /**
         * 是否通过校验（如数字范围），未通过时不应保存
         */
        default boolean isValid() {
            return true;
        }

        void setCategory(String categoryPath);

        void setVisible(boolean visible);
    }

    private static class ConfigEntryWidgetAdapter implements IConfigEntryWidget {
        private final LabelWidget label;
        private final BaseWidget valueWidget;
        private final TooltipWidget tooltipWidget;
        private final java.util.function.Supplier<Object> getter;
        private final java.util.function.Consumer<Object> setter;
        private final java.util.function.Supplier<Boolean> isValidSupplier;

        ConfigEntryWidgetAdapter(ConfigEntryDescriptor desc, LabelWidget label, BaseWidget valueWidget,
                                 TooltipWidget tooltipWidget,
                                 java.util.function.Supplier<Object> getter, java.util.function.Consumer<Object> setter) {
            this(desc, label, valueWidget, tooltipWidget, getter, setter, null);
        }

        ConfigEntryWidgetAdapter(ConfigEntryDescriptor desc, LabelWidget label, BaseWidget valueWidget,
                                 TooltipWidget tooltipWidget,
                                 java.util.function.Supplier<Object> getter, java.util.function.Consumer<Object> setter,
                                 java.util.function.Supplier<Boolean> isValidSupplier) {
            this.label = label;
            this.valueWidget = valueWidget;
            this.tooltipWidget = tooltipWidget;
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
            return valueWidget;
        }

        @Override
        public void updatePosition(int x, int y, int w, int h) {
            if (label.bounds() != null) {
                label.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, h));
            }
            if (valueWidget.bounds() != null) {
                valueWidget.bounds(new ScreenCoordinate(x + LABEL_WIDTH, y, w - LABEL_WIDTH - 4, h));
            }
            if (tooltipWidget != null && tooltipWidget.bounds() != null) {
                tooltipWidget.bounds(new ScreenCoordinate(x, y, LABEL_WIDTH - 4, h));
            }
        }

        @Override
        public Object getValue() {
            return getter.get();
        }

        @Override
        public void setValue(Object value) {
            setter.accept(value);
        }

        @Override
        public void setCategory(String categoryPath) {
            // no-op, category stored in ConfigEditorScreen
        }

        @Override
        public void setVisible(boolean visible) {
            label.visible(visible);
            valueWidget.visible(visible);
            if (tooltipWidget != null) {
                tooltipWidget.visible(visible);
            }
        }
    }
}
