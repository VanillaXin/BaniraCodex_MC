package xin.vanilla.banira.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.client.gui.search.ConfigSearchText;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.ITextWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.InputWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子模组玩家配置页的公共外壳，统一搜索、树结构、滚动与底部操作区。
 */
public abstract class PlayerConfigScreen extends BaniraScreen {
    protected static final int ROW_HEIGHT = 20;
    protected static final int ROW_GAP = 2;

    private static final int CARD_MARGIN = 10;
    private static final int CARD_INNER = 10;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 1;
    private static final int CARD_RADIUS = 8;
    private static final int SEARCH_HEIGHT = 18;
    private static final int SEARCH_GAP = 4;

    private final String modId;
    @Nullable
    private final Screen parentScreen;
    private final Map<CollapsiblePanelWidget, SearchSection> sections = new LinkedHashMap<>();
    private final List<SearchEntry> entries = new ArrayList<>();
    private final Map<CollapsiblePanelWidget, Boolean> expandedBeforeSearch = new IdentityHashMap<>();
    private final List<ButtonWidget> bottomButtons = new ArrayList<>();

    private CollapsiblePanelWidget rootPanel;
    private InputWidget searchInput;
    private ScrollbarWidget scrollbar;
    private String searchText = "";
    private double scrollOffset;
    private int contentHeight;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int contentLeft;
    private int contentWidth;
    private int listTop;
    private int listAreaHeight;

    protected PlayerConfigScreen(String modId, Component title, @Nullable Screen parentScreen,
                                 @Nullable BaniraColorConfig theme, @Nullable EnumSeason season) {
        super(title.toVanilla());
        this.modId = modId;
        this.parentScreen = parentScreen;
        previousScreen(parentScreen);
        BaniraScreen.inheritThemeAndSeason(this, parentScreen, theme, season);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected final void initWidgets() {
        sections.clear();
        entries.clear();
        expandedBeforeSearch.clear();
        bottomButtons.clear();

        cardX = CARD_MARGIN;
        cardY = CARD_MARGIN;
        cardW = width - CARD_MARGIN * 2;
        cardH = height - CARD_MARGIN * 2;
        contentLeft = cardX + CARD_INNER;
        contentWidth = cardW - CARD_INNER * 2 - SCROLL_WIDTH - SCROLL_GAP;
        listTop = cardY + CARD_INNER + SEARCH_HEIGHT + SEARCH_GAP;

        searchInput = new InputWidget(this);
        searchInput.id(modId + "_player_search");
        searchInput.text(BaniraComponent.get().transClientAuto("config_search_hint"));
        searchInput.value(searchText);
        searchInput.onTextChanged(this::applySearchFilter);
        addWidget(searchInput);

        rootPanel = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentWidth);
        rootPanel.text(Text.literal(modId + "-player"));
        rootPanel.expanded(true);
        rootPanel.contentGap(ROW_GAP);
        rootPanel.headerHeight(ROW_HEIGHT);
        rootPanel.onExpandChanged(panel -> syncContentHeight());
        buildPlayerConfig(rootPanel);
        rootPanel.refreshLayout();
        contentHeight = (int) rootPanel.height();
        addWidget(rootPanel);

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id(modId + "_player_scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0);
        scrollbar.onValueChanged(value -> {
            scrollOffset = value;
            updateWidgetPositions();
        });
        addWidget(scrollbar);

        ButtonWidget save = new ButtonWidget(this);
        save.id(modId + "_player_save");
        save.text(BaniraComponent.get().transClientAuto("config_editor_save").toString());
        save.onClick(button -> savePlayerConfig());
        bottomButtons.add(save);

        ButtonWidget close = new ButtonWidget(this);
        close.id(modId + "_player_close");
        close.text(BaniraComponent.get().transClientAuto("config_editor_close").toString());
        close.onClick(button -> onClose());
        bottomButtons.add(close);
        for (ButtonWidget button : bottomButtons) {
            addWidget(button);
        }

        afterPlayerConfigBuilt();
        updateLayout();
        updateWidgetPositions();
        applySearchFilter(searchText);
    }

    /**
     * 向根面板添加子分组和配置行。
     */
    protected abstract void buildPlayerConfig(CollapsiblePanelWidget root);

    protected abstract void savePlayerConfig();

    protected void afterPlayerConfigBuilt() {
    }

    protected final int playerContentWidth() {
        return contentWidth;
    }

    protected final CollapsiblePanelWidget addPlayerSection(CollapsiblePanelWidget parent, String id,
                                                             Component title, @Nullable Component description) {
        CollapsiblePanelWidget section = parent.createChildPanel();
        section.id(id);
        section.text(Text.from(title));
        section.tooltip(description);
        section.expanded(true);
        section.contentGap(ROW_GAP);
        section.headerHeight(ROW_HEIGHT);
        section.onExpandChanged(panel -> syncContentHeight());
        // 由公共外壳统一挂载，避免调用方创建了分组却遗漏加入配置树。
        parent.addCollapsibleChild(section);
        sections.put(section, new SearchSection(section, parent == rootPanel ? null : parent,
                title, description));
        return section;
    }

    protected final void addPlayerRow(CollapsiblePanelWidget parent, BaseWidget row, double height,
                                      @Nullable ITextWidget label, @Nullable TooltipWidget tooltip,
                                      Component title, @Nullable Component description, String... aliases) {
        parent.addChildAuto(row, height);
        entries.add(new SearchEntry(row, parent == rootPanel ? null : parent, label, tooltip,
                title, description, aliases));
    }

    protected final void refreshPlayerConfigLayout() {
        syncContentHeight();
    }

    private void applySearchFilter(String value) {
        searchText = value == null ? "" : value;
        if (rootPanel == null) return;

        ConfigSearchQuery query = ConfigSearchQuery.of(searchText);
        boolean searching = !query.isEmpty();
        String rootTitle = modId + "-player";
        boolean rootMatches = searching && query.matches(rootTitle, modId, "player");

        if (searching && expandedBeforeSearch.isEmpty()) {
            for (CollapsiblePanelWidget section : sections.keySet()) {
                expandedBeforeSearch.put(section, section.expanded());
            }
        }

        Map<CollapsiblePanelWidget, Boolean> sectionMatches = new IdentityHashMap<>();
        for (SearchSection section : sections.values()) {
            boolean selfMatch = searching && query.matches(section.searchTerms());
            sectionMatches.put(section.widget, selfMatch);
            section.widget.text(ConfigSearchText.highlight(section.title.toString(), query,
                    getEffectiveTheme().textPrimary(), getEffectiveTheme().searchMatchText()));
            if (section.description != null) {
                section.widget.tooltip(ConfigSearchText.highlight(section.description.toString(), query,
                        getEffectiveTheme().textPrimary(), getEffectiveTheme().searchMatchText()));
            }
        }

        for (SearchEntry entry : entries) {
            boolean selfMatch = searching && query.matches(entry.searchTerms());
            boolean inheritedMatch = rootMatches || ancestorMatches(entry.parent, sectionMatches);
            entry.widget.visible(!searching || inheritedMatch || selfMatch);
            if (entry.label != null) {
                entry.label.text(ConfigSearchText.highlight(entry.title.toString(), query,
                        getEffectiveTheme().textPrimary(), getEffectiveTheme().searchMatchText()));
            }
            if (entry.tooltip != null && entry.description != null) {
                entry.tooltip.text(ConfigSearchText.highlight(entry.description.toString(), query,
                        getEffectiveTheme().textPrimary(), getEffectiveTheme().searchMatchText()));
            }
        }

        List<SearchSection> reverse = new ArrayList<>(sections.values());
        for (int i = reverse.size() - 1; i >= 0; i--) {
            SearchSection section = reverse.get(i);
            boolean inheritedMatch = rootMatches || ancestorMatches(section.parent, sectionMatches);
            boolean visible = !searching || inheritedMatch || Boolean.TRUE.equals(sectionMatches.get(section.widget))
                    || hasVisibleEntry(section.widget) || hasVisibleChildSection(section.widget);
            section.widget.visible(visible);
            if (searching && visible) section.widget.expanded(true);
            section.widget.reflowVisibleChildren();
        }

        if (!searching && !expandedBeforeSearch.isEmpty()) {
            for (Map.Entry<CollapsiblePanelWidget, Boolean> entry : expandedBeforeSearch.entrySet()) {
                entry.getKey().expanded(entry.getValue());
            }
            expandedBeforeSearch.clear();
        }

        rootPanel.text(ConfigSearchText.highlight(rootTitle, query,
                getEffectiveTheme().textPrimary(), getEffectiveTheme().searchMatchText()));
        rootPanel.reflowVisibleChildren();
        scrollOffset = 0;
        syncContentHeight();
    }

    private boolean ancestorMatches(@Nullable CollapsiblePanelWidget section,
                                    Map<CollapsiblePanelWidget, Boolean> matches) {
        CollapsiblePanelWidget current = section;
        while (current != null) {
            if (Boolean.TRUE.equals(matches.get(current))) return true;
            SearchSection data = sections.get(current);
            current = data != null ? data.parent : null;
        }
        return false;
    }

    private boolean hasVisibleEntry(CollapsiblePanelWidget section) {
        for (SearchEntry entry : entries) {
            if (entry.parent == section && entry.widget.visible()) return true;
        }
        return false;
    }

    private boolean hasVisibleChildSection(CollapsiblePanelWidget section) {
        for (SearchSection child : sections.values()) {
            if (child.parent == section && child.widget.visible()) return true;
        }
        return false;
    }

    private void syncContentHeight() {
        if (rootPanel == null) return;
        rootPanel.refreshLayout();
        contentHeight = (int) rootPanel.height();
        updateLayout();
        updateWidgetPositions();
    }

    private void updateLayout() {
        int buttonAreaHeight = BUTTON_HEIGHT + CARD_INNER;
        int maxListHeight = Math.max(1, cardH - CARD_INNER * 2 - SEARCH_HEIGHT - SEARCH_GAP
                - BUTTON_HEIGHT - BUTTON_GAP);
        searchInput.bounds(new ScreenCoordinate(contentLeft, cardY + CARD_INNER, contentWidth, SEARCH_HEIGHT));

        if (contentHeight <= maxListHeight) {
            listAreaHeight = Math.max(1, contentHeight);
            scrollOffset = 0;
            scrollbar.maxValue(0).value(0).visible(false);
            scrollbar.scrollingCoordinates(new ArrayList<>());
        } else {
            listAreaHeight = maxListHeight;
            scrollbar.visible(true);
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentWidth + SCROLL_GAP,
                    listTop, SCROLL_WIDTH, listAreaHeight));
            scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
            scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
            scrollOffset = scrollbar.value();
            scrollbar.visibleSize(listAreaHeight);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop,
                    contentWidth + SCROLL_GAP + SCROLL_WIDTH, listAreaHeight));
        }

        int buttonAreaTop = cardY + cardH - buttonAreaHeight;
        int zoneWidth = (cardW - CARD_INNER * 2 - BUTTON_GAP) / 2;
        bottomButtons.get(0).bounds(new ScreenCoordinate(cardX + CARD_INNER,
                buttonAreaTop + (buttonAreaHeight - BUTTON_HEIGHT) / 2, zoneWidth, BUTTON_HEIGHT));
        bottomButtons.get(1).bounds(new ScreenCoordinate(cardX + CARD_INNER + zoneWidth + BUTTON_GAP,
                buttonAreaTop + (buttonAreaHeight - BUTTON_HEIGHT) / 2, zoneWidth, BUTTON_HEIGHT));
    }

    private void updateWidgetPositions() {
        if (rootPanel != null) {
            rootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset,
                    contentWidth, contentHeight));
        }
    }

    @Override
    protected void renderWidgets(GuiGraphics stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int background = ColorUtils.applyAlphaToArgb(theme.bgSurface(), 0xFF);
        int buttonAreaHeight = BUTTON_HEIGHT + CARD_INNER;
        int buttonAreaTop = cardY + cardH - buttonAreaHeight;

        AbstractGuiUtils.drawRoundedRect(stack.pose(), cardX, cardY, cardW, buttonAreaTop - cardY - BUTTON_GAP,
                CARD_RADIUS, CARD_RADIUS, 0, 0, background);
        int half = (cardW - BUTTON_GAP) / 2;
        AbstractGuiUtils.drawRoundedRect(stack.pose(), cardX, buttonAreaTop, half, buttonAreaHeight,
                0, 0, CARD_RADIUS, 0, background);
        AbstractGuiUtils.drawRoundedRect(stack.pose(), cardX + half + BUTTON_GAP, buttonAreaTop,
                cardW - half - BUTTON_GAP, buttonAreaHeight, 0, 0, 0, CARD_RADIUS, background);

        AbstractGuiUtils.enableScissor(contentLeft, listTop,
                contentWidth + SCROLL_GAP + SCROLL_WIDTH, Math.max(1, listAreaHeight));
        renderManagedWidget(stack, rootPanel, partialTicks);
        renderManagedWidget(stack, scrollbar, partialTicks);
        AbstractGuiUtils.disableScissor();
        for (ButtonWidget button : bottomButtons) renderManagedWidget(stack, button, partialTicks);
        for (IWidget widget : widgets()) {
            if (widget == rootPanel || widget == scrollbar || bottomButtons.contains(widget)
                    || widget.parent() != null) continue;
            renderManagedWidget(stack, widget, partialTicks);
        }
    }

    private void renderManagedWidget(GuiGraphics stack, @Nullable IWidget widget, float partialTicks) {
        if (widget == null || !widget.visible()) return;
        if (widget.enabled() && widget.needsUpdate()) widget.update();
        widget.render(stack, partialTicks);
    }

    @Override
    protected void onRender(GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0 && rootPanel != null && rootPanel.visible() && rootPanel.enabled()
                && rootPanel.isMouseInside(mouseX, mouseY)
                && rootPanel.handleMouseScroll(MouseScrollEvent.of(mouseX, mouseY, deltaY))) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        if (scrollbar != null && deltaY != 0) {
            double value = Math.max(scrollbar.minValue(),
                    Math.min(scrollbar.maxValue(), scrollbar.value() - deltaY * 20));
            scrollbar.value(value);
            scrollOffset = value;
            updateWidgetPositions();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (parentScreen != null) Minecraft.getInstance().setScreen(parentScreen);
        else super.onClose();
    }

    @Override
    protected ScreenCoordinate closeableWindowBounds() {
        return new ScreenCoordinate(cardX, cardY, cardW, cardH);
    }

    private static final class SearchSection {
        private final CollapsiblePanelWidget widget;
        @Nullable
        private final CollapsiblePanelWidget parent;
        private final Component title;
        @Nullable
        private final Component description;

        private SearchSection(CollapsiblePanelWidget widget, @Nullable CollapsiblePanelWidget parent,
                              Component title, @Nullable Component description) {
            this.widget = widget;
            this.parent = parent;
            this.title = title;
            this.description = description;
        }

        private String[] searchTerms() {
            return description == null
                    ? new String[]{widget.id(), title.toString()}
                    : new String[]{widget.id(), title.toString(), description.toString()};
        }
    }

    private static final class SearchEntry {
        private final BaseWidget widget;
        @Nullable
        private final CollapsiblePanelWidget parent;
        @Nullable
        private final ITextWidget label;
        @Nullable
        private final TooltipWidget tooltip;
        private final Component title;
        @Nullable
        private final Component description;
        private final String[] aliases;

        private SearchEntry(BaseWidget widget, @Nullable CollapsiblePanelWidget parent,
                            @Nullable ITextWidget label, @Nullable TooltipWidget tooltip,
                            Component title, @Nullable Component description, String[] aliases) {
            this.widget = widget;
            this.parent = parent;
            this.label = label;
            this.tooltip = tooltip;
            this.title = title;
            this.description = description;
            this.aliases = aliases != null ? aliases : new String[0];
        }

        private String[] searchTerms() {
            List<String> terms = new ArrayList<>();
            terms.add(widget.id());
            terms.add(title.toString());
            if (description != null) terms.add(description.toString());
            for (String alias : aliases) terms.add(alias);
            return terms.toArray(new String[0]);
        }
    }
}
