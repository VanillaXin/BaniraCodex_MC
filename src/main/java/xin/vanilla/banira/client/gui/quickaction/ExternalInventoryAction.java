package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 可由 Banira 或其他兼容宿主展示的一条外部背包操作。 */
@Getter
@Accessors(fluent = true)
public final class ExternalInventoryAction {
    @Nonnull
    private final String id;
    @Nonnull
    private final Component label;
    @Nonnull
    private final QuickIcon icon;
    @Nullable
    private final Consumer<QuickActionContext> onActivate;
    @Nonnull
    private final List<QuickActionContextMenuItem> contextMenuItems;
    @Nonnull
    private final String sourceId;

    public ExternalInventoryAction(@Nonnull String id, @Nonnull Component label,
                                   @Nonnull QuickIcon icon,
                                   @Nullable Consumer<QuickActionContext> onActivate) {
        this(id, label, icon, onActivate, Collections.emptyList(), "");
    }

    public ExternalInventoryAction(@Nonnull String id, @Nonnull Component label,
                                   @Nonnull QuickIcon icon,
                                   @Nullable Consumer<QuickActionContext> onActivate,
                                   @Nonnull List<QuickActionContextMenuItem> contextMenuItems) {
        this(id, label, icon, onActivate, contextMenuItems, "");
    }

    private ExternalInventoryAction(String id, Component label, QuickIcon icon,
                                    Consumer<QuickActionContext> onActivate,
                                    List<QuickActionContextMenuItem> contextMenuItems,
                                    String sourceId) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.onActivate = onActivate;
        this.contextMenuItems = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(contextMenuItems, "contextMenuItems")));
        this.sourceId = sourceId == null ? "" : sourceId;
    }

    ExternalInventoryAction withSourceId(String sourceId) {
        return new ExternalInventoryAction(id, label, icon, onActivate,
                contextMenuItems, sourceId);
    }
}
