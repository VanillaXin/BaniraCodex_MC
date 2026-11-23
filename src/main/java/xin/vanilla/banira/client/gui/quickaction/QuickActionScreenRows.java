package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.client.gui.event.MouseClickTracker;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;

import java.util.*;

final class QuickActionScreenRows {
    static final class Item {
        final String id, group, title, details, unavailableReason;
        final Runnable select;

        Item(String id, String group, String title, String details, String unavailableReason, Runnable select) {
            this.id = id;
            this.group = group;
            this.title = title;
            this.details = details;
            this.unavailableReason = unavailableReason;
            this.select = select;
        }

        boolean selectable() {
            return unavailableReason.isEmpty();
        }

        String tooltip() {
            return String.join("\n", title, group, details) + (selectable() ? "" : "\n" + unavailableReason);
        }
    }

    static final class Row {
        final String group;
        final boolean expanded;
        final Item item;

        Row(String group, boolean expanded, Item item) {
            this.group = group;
            this.expanded = expanded;
            this.item = item;
        }
    }

    private final Set<String> expanded = new HashSet<>();
    private final Set<String> searchCollapsed = new HashSet<>();
    private String query = "";
    private ConfigSearchQuery filter = ConfigSearchQuery.of("");
    private Item selected;
    private long lastClick;
    private boolean pendingClick, submitted;

    boolean click(Item item, long nowMillis) {
        if (submitted || !item.selectable()) return false;
        long elapsed = nowMillis - lastClick;
        boolean repeated = pendingClick && selected == item && elapsed >= 0
                && elapsed <= MouseClickTracker.DEFAULT_DOUBLE_CLICK_MS;
        selected = item;
        lastClick = nowMillis;
        pendingClick = !repeated;
        return repeated;
    }

    Item selection() {
        return selected;
    }

    Item confirm() {
        if (selected == null || submitted) return null;
        Item result = selected;
        selected = null;
        pendingClick = false;
        submitted = true;
        return result;
    }

    void resetClick() {
        pendingClick = false;
    }

    void toggle(String group) {
        resetClick();
        Set<String> state = filter.isEmpty() ? expanded : searchCollapsed;
        if (!state.add(group)) state.remove(group);
    }

    List<Row> rows(List<Item> items, String query, boolean hideUnavailable) {
        if (!this.query.equals(query)) {
            resetClick();
            this.query = query;
            filter = ConfigSearchQuery.of(query);
            searchCollapsed.clear();
        }
        Map<String, List<Item>> groups = new LinkedHashMap<>();
        for (Item item : items) {
            if ((hideUnavailable && !item.selectable()) || !filter.matches(item.title, item.details, item.group))
                continue;
            groups.computeIfAbsent(item.group, ignored -> new ArrayList<>()).add(item);
        }
        List<Row> result = new ArrayList<>();
        groups.forEach((group, children) -> {
            boolean open = filter.isEmpty() ? expanded.contains(group) : !searchCollapsed.contains(group);
            result.add(new Row(group, open, null));
            if (open) for (Item child : children) result.add(new Row(group, true, child));
        });
        if (selected != null && result.stream().noneMatch(row -> row.item == selected)) {
            selected = null;
            resetClick();
        }
        return result;
    }
}
