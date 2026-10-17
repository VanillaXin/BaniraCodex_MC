package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;

import java.util.Objects;

public final class UiRowWindow {
    private final UiRowProvider provider;
    private final int limit;
    private int first;

    public UiRowWindow(UiRowProvider provider, int limit) {
        this.provider = Objects.requireNonNull(provider, "provider");
        if (limit < 1 || limit > 120) throw new IllegalArgumentException("UI row realization limit");
        this.limit = limit;
    }

    public void moveTo(int index) { first = Math.max(0, Math.min(index, Math.max(0, total() - 1))); }
    public int total() { return Math.max(0, provider.size()); }
    public int first() { return first; }
    public int realizedCount() { return Math.min(limit, Math.max(0, total() - first)); }
    public String key(int index) { return provider.key(sourceIndex(index)); }
    public void bind(int index, UiView row) { provider.bind(row, sourceIndex(index)); }

    private int sourceIndex(int index) {
        if (index < 0 || index >= realizedCount()) throw new IndexOutOfBoundsException("UI row outside window");
        return first + index;
    }
}
