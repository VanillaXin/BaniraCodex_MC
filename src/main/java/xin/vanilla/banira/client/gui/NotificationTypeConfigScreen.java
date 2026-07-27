package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.*;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.ConfigEditorNotifier;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.util.*;

import static xin.vanilla.banira.client.data.BaniraColorToken.BG_SURFACE;

/**
 * 按通知类型配置是否隐藏、显示时长、动画与位置
 */
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
    private static final long DURATION_SLIDER_MIN_MS = 0L;
    private static final long DURATION_SLIDER_MAX_MS = 20000L;
    private static final long DURATION_SLIDER_STEP_MS = 500L;

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
    private Map<String, NotificationTypeSettingsStore.TypeSettings> baselineSettings;
    private Map<String, NotificationTypeSettingsStore.TypeSettings> draftSettings;

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
        ensureDraftInitialized();
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

        ButtonWidget saveBtn = new ButtonWidget(this);
        saveBtn.id("save");
        saveBtn.text(BaniraComponent.get().transClientAuto("notification_type_config_save").toString());
        saveBtn.onClick(b -> saveDraft());
        bottomButtons.add(saveBtn);

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

        TypeTreeNode tree = buildTypeTree(NotificationTypeRegistry.knownTypesSorted());
        appendTypeTreeNodes(root, tree, "");

        root.refreshLayout();
        return root;
    }

    /**
     * 按 {@code .} 分段的前缀树节点；{@link #typesEndingHere} 为恰好在该路径结束的完整类型 id。
     */
    private static final class TypeTreeNode {
        private final Map<String, TypeTreeNode> children = new TreeMap<>();
        private final List<String> typesEndingHere = new ArrayList<>();
    }

    private static TypeTreeNode buildTypeTree(List<String> sortedTypeIds) {
        TypeTreeNode root = new TypeTreeNode();
        for (String typeId : sortedTypeIds) {
            if (typeId == null || typeId.isEmpty()) {
                continue;
            }
            String[] parts = typeId.split("\\.", -1);
            TypeTreeNode cur = root;
            boolean any = false;
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                any = true;
                cur = cur.children.computeIfAbsent(part, k -> new TypeTreeNode());
            }
            if (any) {
                cur.typesEndingHere.add(typeId);
            }
        }
        return root;
    }

    /**
     * 将树挂到父折叠面板下：先本层完整类型（排序），再各段名子树（排序）。
     * 若某段下仅有一个类型且无更深层级，则不额外嵌套一层标题，直接挂该类型的配置面板。
     *
     * @param pathFromRoot 从根到<strong>当前 trie 节点</strong>的 {@code .} 连接路径（与 {@link TypeTreeNode} 深度一致），用于从叶子标题中剔除已由父级分组表达的前缀
     */
    private void appendTypeTreeNodes(CollapsiblePanelWidget parent, TypeTreeNode node, String pathFromRoot) {
        List<String> terminals = new ArrayList<>(node.typesEndingHere);
        Collections.sort(terminals);
        for (String typeId : terminals) {
            parent.addCollapsibleChild(createTypeLeafPanel(parent, typeId, pathFromRoot));
        }
        for (Map.Entry<String, TypeTreeNode> e : node.children.entrySet()) {
            String segment = e.getKey();
            TypeTreeNode childNode = e.getValue();
            String childPath = pathFromRoot.isEmpty() ? segment : pathFromRoot + "." + segment;
            if (childNode.typesEndingHere.size() == 1 && childNode.children.isEmpty()) {
                parent.addCollapsibleChild(createTypeLeafPanel(parent, childNode.typesEndingHere.get(0), childPath));
                continue;
            }
            CollapsiblePanelWidget group = parent.createChildPanel();
            group.text(groupTitle(segment, pathFromRoot)).expanded(false);
            group.contentGap(ROW_GAP);
            group.headerHeight(ROW_HEIGHT);
            group.onExpandChanged(p -> syncContentHeight());
            appendTypeTreeNodes(group, childNode, childPath);
            group.refreshLayout();
            parent.addCollapsibleChild(group);
        }
    }

    /**
     * 叶子标题：去掉与当前 trie 路径重合的前缀，仅保留父级折叠组未展示的后缀（最末一段与路径完全一致时只显示该段）。
     */
    private static String leafTitleStripPrefix(String fullTypeId, String trieNodePath) {
        if (trieNodePath == null || trieNodePath.isEmpty()) {
            return fullTypeId;
        }
        if (fullTypeId.equals(trieNodePath)) {
            return lastPathSegment(fullTypeId);
        }
        String prefix = trieNodePath + ".";
        if (fullTypeId.startsWith(prefix)) {
            return fullTypeId.substring(prefix.length());
        }
        return fullTypeId;
    }

    private static String lastPathSegment(String dotted) {
        int i = dotted.lastIndexOf('.');
        return i < 0 ? dotted : dotted.substring(i + 1);
    }

    private CollapsiblePanelWidget createTypeLeafPanel(CollapsiblePanelWidget parent, String typeId, String trieNodePathForTitle) {
        CollapsiblePanelWidget child = parent.createChildPanel();
        child.text(typeTitle(typeId, trieNodePathForTitle)).expanded(false);
        Component tooltip = NotificationTypeRegistry.tooltipInternal(typeId);
        if (tooltip != null && !tooltip.isEmpty()) {
            child.tooltip(tooltip);
        }
        child.contentGap(ROW_GAP);
        child.headerHeight(ROW_HEIGHT);
        child.onExpandChanged(p -> syncContentHeight());

        NotificationTypeSettingsStore.TypeSettings st = draftSettings(typeId);
        double cw = child.getContentWidth();

        addTypeToggleRow(child, cw, typeId, st);
        addDurationRow(child, cw, typeId, st);
        addAnimationRow(child, cw, typeId, st);
        addPositionRow(child, cw, typeId, st);
        addDisplayModeRow(child, cw, typeId, st);

        child.refreshLayout();
        return child;
    }

    private Text groupTitle(String segment, String pathFromRoot) {
        if (!pathFromRoot.isEmpty()) {
            return Text.literal(segment);
        }
        Component registered = NotificationTypeRegistry.modDisplayNameInternal(segment);
        if (registered != null && !registered.isEmpty()) {
            return Text.from(registered);
        }
        if (BaniraPlatforms.isInstalled()) {
            String loaderName = Banira.platform().modDisplayName(segment);
            if (loaderName != null && !loaderName.trim().isEmpty()) {
                return Text.literal(loaderName);
            }
        }
        return Text.literal(segment);
    }

    private Text typeTitle(String typeId, String trieNodePathForTitle) {
        if (NotificationTypeKeys.DEFAULT.equals(typeId)) {
            return Text.from(BaniraComponent.get().transClientAuto("notification_type_default"));
        }
        return Text.literal(leafTitleStripPrefix(typeId, trieNodePathForTitle));
    }

    private void addTypeToggleRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        LabelWidget label = row.label(BaniraComponent.get().transClientAuto("notification_type_config_hidden").toString());
        ButtonWidget btn = row.toggleButton(st.hidden());
        btn.onClick(b -> {
            boolean next = !draftSettings(typeId).hidden();
            updateDraft(typeId, copySettings(typeId).hidden(next));
            btn.text(toggleText(next));
        });
        panel.addChildAuto(row, ROW_HEIGHT);
    }

    private void addDurationRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        row.label(BaniraComponent.get().transClientAuto("notification_type_config_duration").toString());
        long current = Math.max(DURATION_SLIDER_MIN_MS, st.durationMs());
        SliderWidget slider = row.slider(cw);
        slider.minValue(DURATION_SLIDER_MIN_MS)
                .maxValue(Math.max(DURATION_SLIDER_MAX_MS, current))
                .step(DURATION_SLIDER_STEP_MS)
                .decimalPlaces(0)
                .value(current)
                .valueFormatter(v -> durationLabelForSlider(Math.round(v)));
        slider.onValueChanged(v -> {
            long ms = Math.round(v);
            updateDraft(typeId, copySettings(typeId).durationMs(ms));
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
            updateDraft(typeId, copySettings(typeId).animationName(anim));
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
            updateDraft(typeId, copySettings(typeId).positionName(pos));
        });
        panel.addChildAuto(row, ROW_HEIGHT + 4);
    }

    private void addDisplayModeRow(CollapsiblePanelWidget panel, double cw, String typeId, NotificationTypeSettingsStore.TypeSettings st) {
        TypeRow row = new TypeRow(this, cw, ROW_HEIGHT);
        row.label(BaniraComponent.get().transClientAuto("notification_type_config_display").toString());
        List<DropdownOption> entries = new ArrayList<>();
        for (EnumNotificationTypeDisplayMode m : EnumNotificationTypeDisplayMode.values()) {
            entries.add(enumAsDropdownOption(m));
        }
        String sel = st.displayMode() != null ? st.displayMode().name() : EnumNotificationTypeDisplayMode.OVERLAY.name();
        DropdownSelectWidget dd = row.dropdownEntries(cw, entries, sel);
        dd.onSelectionChanged(vals -> {
            if (vals.isEmpty()) {
                return;
            }
            EnumNotificationTypeDisplayMode m = EnumNotificationTypeDisplayMode.parseOrDefault(vals.get(0));
            updateDraft(typeId, copySettings(typeId).displayMode(m));
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
        return NotificationTypeSettingsStore.copyOf(draftSettings(typeId));
    }

    private void ensureDraftInitialized() {
        if (baselineSettings != null) {
            return;
        }
        baselineSettings = NotificationTypeSettingsStore.get().snapshot();
        draftSettings = copySettingsMap(baselineSettings);
    }

    private NotificationTypeSettingsStore.TypeSettings draftSettings(String typeId) {
        NotificationTypeSettingsStore.TypeSettings settings = draftSettings.get(typeId);
        return settings != null ? settings : new NotificationTypeSettingsStore.TypeSettings();
    }

    private void updateDraft(String typeId, NotificationTypeSettingsStore.TypeSettings settings) {
        NotificationTypeSettingsStore.TypeSettings previous = draftSettings(typeId);
        if (!Objects.equals(previous, settings)) {
            draftSettings.put(typeId, NotificationTypeSettingsStore.copyOf(settings));
        }
    }

    private void saveDraft() {
        NotificationTypeSettingsStore.get().replaceAllAndSave(draftSettings);
        baselineSettings = copySettingsMap(draftSettings);
        ConfigEditorNotifier.show("config_editor_save_success", 2000);
    }

    private int changedSettingCount() {
        Set<String> typeIds = new HashSet<>(baselineSettings.keySet());
        typeIds.addAll(draftSettings.keySet());
        int changed = 0;
        for (String typeId : typeIds) {
            NotificationTypeSettingsStore.TypeSettings baseline = baselineSettings.get(typeId);
            NotificationTypeSettingsStore.TypeSettings draft = draftSettings.get(typeId);
            if (!Objects.equals(baseline != null ? baseline : new NotificationTypeSettingsStore.TypeSettings(),
                    draft != null ? draft : new NotificationTypeSettingsStore.TypeSettings())) {
                changed++;
            }
        }
        return changed;
    }

    private static Map<String, NotificationTypeSettingsStore.TypeSettings> copySettingsMap(
            Map<String, NotificationTypeSettingsStore.TypeSettings> source) {
        Map<String, NotificationTypeSettingsStore.TypeSettings> result = new LinkedHashMap<>();
        source.forEach((typeId, settings) ->
                result.put(typeId, NotificationTypeSettingsStore.copyOf(settings)));
        return result;
    }

    private String durationLabelForSlider(long durationMs) {
        if (durationMs <= 0) {
            return BaniraComponent.get().transClientAuto("notification_type_config_inherit").toString();
        }
        return durationMs + " ms";
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
            btnWidths[i] = font.width(bottomButtons.get(i).text().toString()) + BUTTON_PADDING * 2;
        }

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

    private void updateWidgetPositions() {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset, contentW, contentHeight));
        }
    }

    @Override
    public void onClose() {
        if (args.parentScreen() != null) {
            BaniraClientRuntime.setScreen(args.parentScreen());
        } else {
            super.onClose();
        }
    }

    /**
     * ESC 属于界面级命令，需要先于折叠面板和下拉框处理。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFWKey.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        int changedCount = changedSettingCount();
        if (changedCount == 0) {
            onClose();
        } else {
            ConfigEditorNotifier.show("config_editor_unsaved_changes", 4500, changedCount);
        }
        return true;
    }

    private static final int CARD_RADIUS = 8;
    private static final int CARD_ALPHA = 0xFF;

    @Override
    protected void renderWidgets(PoseStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBg = ColorUtils.applyAlphaToArgb(theme.color(BG_SURFACE), CARD_ALPHA);
        int btnAreaH = BUTTON_HEIGHT + CARD_INNER;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int contentH = btnAreaTop - cardY - CARD_GAP;
        int n = bottomButtons.size();

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

        for (IWidget widget : widgets()) {
            if (widget == contentRootPanel || widget == scrollbar || bottomButtons.contains(widget)) continue;
            if (widget.parent() != null || !widget.visible()) continue;
            if (widget.enabled() && widget.needsUpdate()) widget.update();
            widget.render(stack, partialTicks);
        }
    }

    @Override
    protected void onRender(PoseStack stack, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && contentRootPanel != null && contentRootPanel.visible() && contentRootPanel.enabled()
                && contentRootPanel.isMouseInside(mouseX, mouseY)
                && contentRootPanel.handleMouseScroll(MouseScrollEvent.of(mouseX, mouseY, delta, currentKeyboardModifiers()))) {
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
            l.textEllipsisPosition(EnumEllipsisPosition.END);
            l.textVerticalAlign(EnumAlignment.CENTER);
            l.showFullTextTooltipWhenTruncated(true);
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

        SliderWidget slider(double cw) {
            double lw = Math.max(LABEL_COLUMN_MIN_WIDTH, cw * LABEL_COLUMN_WIDTH_RATIO);
            double vx = lw + GAP_LABEL_TO_VALUE;
            double vw = Math.max(VALUE_AREA_MIN_WIDTH, cw - vx);
            SliderWidget s = new SliderWidget(screen, new ScreenCoordinate(vx, 0, vw, rowHeight()));
            s.showValue(true);
            addChild(s);
            return s;
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
        public void render(PoseStack stack, float partialTicks) {
            if (!visible) return;
            renderChildren(stack, partialTicks);
        }
    }
}
